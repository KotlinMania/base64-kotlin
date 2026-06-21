// port-lint: tests engine/tests.rs
package io.github.kotlinmania.base64.engine

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.PAD_BYTE
import io.github.kotlinmania.base64.addPadding
import io.github.kotlinmania.base64.alphabet.Alphabet
import io.github.kotlinmania.base64.alphabet.STANDARD
import io.github.kotlinmania.base64.decodeErrorOrNull
import io.github.kotlinmania.base64.encodedLen
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig
import io.github.kotlinmania.base64.io.ByteArrayReader
import io.github.kotlinmania.base64.randomAlphabet
import io.github.kotlinmania.base64.randomConfig
import io.github.kotlinmania.base64.read.DecoderReader
import io.github.kotlinmania.base64.readToEnd
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD as STANDARD_ENGINE

class EngineConformanceTest {
    @Test
    fun naiveMatchesGeneralPurposeAcrossRandomConfigs() {
        val rng = Random(0xE109)
        repeat(1_000) {
            val alphabet = randomAlphabet(rng)
            val config = randomConfig(rng)
            val generalPurpose = GeneralPurpose(alphabet, config)
            val naive =
                Naive(
                    alphabet,
                    NaiveConfig(
                        shouldEncodePadding = config.encodePadding(),
                        decodeAllowTrailingBits = config.decodeAllowTrailingBits,
                        decodePaddingMode = config.decodePaddingMode,
                    ),
                )
            val input = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(input)

            val generalEncoded = generalPurpose.encode(input)
            val naiveEncoded = naive.encode(input)
            assertEquals(generalEncoded, naiveEncoded)
            assertContentEquals(input, naive.decode(naiveEncoded.bytes()).getOrThrow())

            val generalOutput = ByteArray(generalEncoded.length)
            val naiveOutput = ByteArray(generalEncoded.length)
            assertEquals(
                generalPurpose.internalEncode(input, generalOutput),
                naive.internalEncode(input, naiveOutput),
            )
            assertContentEquals(generalOutput, naiveOutput)

            val generalDecode = ByteArray(input.size)
            val naiveDecode = ByteArray(input.size)
            assertEquals(
                generalPurpose.decodeSliceUnchecked(generalEncoded.bytes(), generalDecode).getOrThrow(),
                naive.decodeSliceUnchecked(naiveEncoded.bytes(), naiveDecode).getOrThrow(),
            )
            assertContentEquals(generalDecode, naiveDecode)
        }
    }

    @Test
    fun rfcTestVectorsStdAlphabet() {
        val data =
            listOf(
                "" to "",
                "f" to "Zg==",
                "fo" to "Zm8=",
                "foo" to "Zm9v",
                "foob" to "Zm9vYg==",
                "fooba" to "Zm9vYmE=",
                "foobar" to "Zm9vYmFy",
            )

        val engine = standard()
        val engineNoPadding = standardUnpadded()

        for ((orig, encoded) in data) {
            val encodedWithoutPadding = encoded.trimEnd('=')
            val input = orig.encodeToByteArray()

            val encodeBufNoPad = ByteArray(8)
            val decodeBufNoPad = ByteArray(6)
            val encodeLenNoPad = engineNoPadding.internalEncode(input, encodeBufNoPad)
            assertEquals(encodedWithoutPadding, encodeBufNoPad.copyOf(encodeLenNoPad).decodeToString())
            val decodeLenNoPad =
                engineNoPadding
                    .decodeSliceUnchecked(encodedWithoutPadding.encodeToByteArray(), decodeBufNoPad)
                    .getOrThrow()
            assertEquals(input.size, decodeLenNoPad)
            assertContentEquals(input, decodeBufNoPad.copyOf(decodeLenNoPad))
            if (encoded.contains('=')) {
                assertDecodeError(DecodeError.InvalidPadding, engineNoPadding.decode(encoded.encodeToByteArray()))
            }

            val encodeBuf = ByteArray(8)
            val decodeBuf = ByteArray(6)
            val encodeLen = engine.internalEncode(input, encodeBuf)
            assertEquals(encodedWithoutPadding, encodeBuf.copyOf(encodeLen).decodeToString())
            val padLen = addPadding(encodeLen, encodeBuf, encodeLen)
            assertContentEquals(encoded.encodeToByteArray(), encodeBuf.copyOf(encodeLen + padLen))

            val decodeLen = engine.decodeSliceUnchecked(encoded.encodeToByteArray(), decodeBuf).getOrThrow()
            assertEquals(input.size, decodeLen)
            assertContentEquals(input, decodeBuf.copyOf(decodeLen))
            if (encoded.contains('=')) {
                assertDecodeError(DecodeError.InvalidPadding, engine.decode(encodedWithoutPadding.encodeToByteArray()))
            }
        }
    }

    @Test
    fun encodeDoesntWriteExtraBytes() {
        val rng = Random(0xE100)
        repeat(500) {
            val engine = randomEngine(rng)
            val orig = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(orig)
            val padded = engine.config().encodePadding()
            val encodeBuf = ByteArray(orig.size * 2 + 1024) { rng.nextInt(0, 256).toByte() }
            val backup = encodeBuf.copyOf()
            val expectedNoPadLen = encodedLen(orig.size, false) ?: error("unexpected overflow")

            val encodedLenNoPad = engine.internalEncode(orig, encodeBuf)
            assertEquals(expectedNoPadLen, encodedLenNoPad)
            assertContentEquals(
                backup.copyOfRange(encodedLenNoPad, backup.size),
                encodeBuf.copyOfRange(encodedLenNoPad, encodeBuf.size),
            )
            assertEncodeSanity(
                encodeBuf.copyOfRange(0, encodedLenNoPad).decodeToString(),
                false,
                orig.size,
            )
            val padLen = if (padded) addPadding(encodedLenNoPad, encodeBuf, encodedLenNoPad) else 0
            assertContentEquals(
                orig,
                engine.decode(encodeBuf.copyOfRange(0, encodedLenNoPad + padLen)).getOrThrow(),
            )
        }
    }

    @Test
    fun encodeEngineSliceFitsIntoPreciselySizedSlice() {
        val rng = Random(0xE101)
        repeat(500) {
            val input = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(input)
            val engine = randomEngine(rng)
            val encodedSize = encodedLen(input.size, engine.config().encodePadding()) ?: error("unexpected overflow")
            val encoded = ByteArray(encodedSize)
            val decoded = mutableListOf<Byte>()

            assertEquals(encodedSize, engine.encodeSlice(input, encoded).getOrThrow())
            assertEncodeSanity(encoded.decodeToString(), engine.config().encodePadding(), input.size)
            engine.decodeVec(encoded, decoded).getOrThrow()
            assertContentEquals(input, decoded.toByteArray())
        }
    }

    @Test
    fun decodeDoesntWriteExtraBytes() {
        val rng = Random(0xE102)
        repeat(500) {
            val engine = randomEngine(rng)
            val orig = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(orig)
            val encoded = engine.encode(orig).encodeToByteArray()
            val prefixLen = 1024
            val decodeBuf = ByteArray(prefixLen * 2 + orig.size * 2) { rng.nextInt(0, 256).toByte() }
            val backup = decodeBuf.copyOf()
            val window = ByteArray(decodeBuf.size - prefixLen)

            val decodedLen = engine.decodeSliceUnchecked(encoded, window).getOrThrow()
            window.copyInto(decodeBuf, prefixLen, 0, decodedLen)

            assertEquals(orig.size, decodedLen)
            assertContentEquals(orig, decodeBuf.copyOfRange(prefixLen, prefixLen + decodedLen))
            assertContentEquals(backup.copyOfRange(0, prefixLen), decodeBuf.copyOfRange(0, prefixLen))
            assertContentEquals(
                backup.copyOfRange(prefixLen + decodedLen, backup.size),
                decodeBuf.copyOfRange(prefixLen + decodedLen, decodeBuf.size),
            )
        }
    }

    @Test
    fun decodeDetectInvalidLastSymbol() {
        val engine = standard()
        assertContentEquals(byteArrayOf(0x89.toByte(), 0x85.toByte()), engine.decode("iYU=".bytes()).getOrThrow())
        assertContentEquals(byteArrayOf(0xFF.toByte()), engine.decode("/w==".bytes()).getOrThrow())

        val suffixes =
            listOf(
                "/x==" to 1,
                "/z==" to 1,
                "/0==" to 1,
                "/9==" to 1,
                "/+==" to 1,
                "//==" to 1,
                "iYV=" to 2,
                "iYW=" to 2,
                "iYX=" to 2,
            )
        for ((suffix, offset) in suffixes) {
            for (prefixQuads in 0..255) {
                val encoded = prefixedString("AAAA", prefixQuads, suffix)
                assertDecodeError(
                    DecodeError.InvalidLastSymbol(encoded.length - 4 + offset, suffix.encodeToByteArray()[offset]),
                    engine.decode(encoded.bytes()),
                )
            }
        }
    }

    @Test
    fun decodeDetect1ValidSymbolInLastQuadInvalidLength() {
        for (len in (0 until 256).map { it * 4 + 1 }) {
            for (mode in allPadModes()) {
                val input = MutableList(len) { 'A'.code.toByte() }
                val engine = standardWithPadMode(true, mode)
                assertDecodeError(DecodeError.InvalidLength(len), engine.decode(input.toByteArray()))
                repeat(3) {
                    input.add(PAD_BYTE)
                    assertDecodeError(DecodeError.InvalidByte(len, PAD_BYTE), engine.decode(input.toByteArray()))
                }
            }
        }
    }

    @Test
    fun decodeDetect1InvalidByteInLastQuadInvalidByte() {
        for (prefixLen in (0 until 256).map { it * 4 }) {
            for (mode in allPadModes()) {
                val input = MutableList(prefixLen) { 'A'.code.toByte() }
                input.add('*'.code.toByte())
                val engine = standardWithPadMode(true, mode)
                assertDecodeError(DecodeError.InvalidByte(prefixLen, '*'.code.toByte()), engine.decode(input.toByteArray()))
                repeat(3) {
                    input.add(PAD_BYTE)
                    assertDecodeError(DecodeError.InvalidByte(prefixLen, '*'.code.toByte()), engine.decode(input.toByteArray()))
                }
            }
        }
    }

    @Test
    fun decodeDetectInvalidLastSymbolEveryPossibleTwoSymbols() {
        val engine = standard()
        val valid = mutableMapOf<String, ByteArray>()
        for (b in 0..255) {
            val b64 = ByteArray(4)
            assertEquals(2, engine.internalEncode(byteArrayOf(b.toByte()), b64))
            addPadding(2, b64, 2)
            valid[b64.decodeToString()] = byteArrayOf(b.toByte())
        }

        for (prefixQuads in 0..4) {
            for (s1 in STANDARD.symbols) {
                for (s2 in STANDARD.symbols) {
                    val symbols = byteArrayOf(s1, s2, PAD_BYTE, PAD_BYTE)
                    val expected = valid[symbols.decodeToString()]
                    if (expected != null) {
                        val decoded = engine.decode(prefixedBytes("AAAA", prefixQuads, symbols.decodeToString())).getOrThrow()
                        assertContentEquals(expected, decoded.copyOfRange(prefixQuads * 3, decoded.size))
                    } else {
                        assertDecodeError(DecodeError.InvalidLastSymbol(1, s2), engine.decode(symbols))
                    }
                }
            }
        }
    }

    @Test
    fun decodeDetectInvalidLastSymbolEveryPossibleThreeSymbols() {
        val engine = standard()
        for (prefixQuads in 0..16) {
            for (b1 in 0..255) {
                val bytes = byteArrayOf(b1.toByte(), (b1 * 37).toByte())
                val encoded = engine.encode(bytes)
                val decoded = engine.decode(prefixedBytes("AAAA", prefixQuads, encoded)).getOrThrow()
                assertContentEquals(bytes, decoded.copyOfRange(prefixQuads * 3, decoded.size))
            }
            for (suffix in listOf("iYV=", "iYW=", "iYX=")) {
                val encoded = prefixedString("AAAA", prefixQuads, suffix)
                assertIs<DecodeError.InvalidLastSymbol>(engine.decode(encoded.bytes()).exceptionOrNull())
            }
        }
    }

    @Test
    fun decodeInvalidTrailingBitsIgnoredWhenConfigured() {
        val strict = standard()
        val forgiving = standardAllowTrailingBits()
        var prefix = ""
        repeat(256) {
            assertTrue(strict.decode((prefix + "/w==").bytes()).isSuccess)
            assertTrue(strict.decode((prefix + "iYU=").bytes()).isSuccess)
            assertTolerantDecode(forgiving, prefix, byteArrayOf(255.toByte()), "/x==")
            assertTolerantDecode(forgiving, prefix, byteArrayOf(137.toByte(), 133.toByte()), "iYV=")
            assertTolerantDecode(forgiving, prefix, byteArrayOf(255.toByte()), "/y==")
            assertTolerantDecode(forgiving, prefix, byteArrayOf(137.toByte(), 133.toByte()), "iYW=")
            assertTolerantDecode(forgiving, prefix, byteArrayOf(255.toByte()), "/z==")
            assertTolerantDecode(forgiving, prefix, byteArrayOf(137.toByte(), 133.toByte()), "iYX=")
            prefix += "AAAA"
        }
    }

    @Test
    fun decodeInvalidByteError() {
        val rng = Random(0xE103)
        repeat(1_000) {
            val alphabet = randomAlphabet(rng)
            val engine = randomEngine(rng, alphabet)
            val orig = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(orig)
            val encoded = engine.encode(orig).encodeToByteArray()
            val invalidByte =
                generateSequence { rng.nextInt(0, 256).toByte() }
                    .first { it !in alphabet.symbols && it != PAD_BYTE }
            val invalidIndex = rng.nextInt(0, orig.size)
            encoded[invalidIndex] = invalidByte
            assertDecodeError(DecodeError.InvalidByte(invalidIndex, invalidByte), engine.decodeSliceUnchecked(encoded, ByteArray(orig.size)))
        }
    }

    @Test
    fun decodePaddingBeforeFinalNonPaddingCharErrorInvalidByteAtFirstPadAllModes() {
        val suffixes = listOf("AA==" to 2, "AAA=" to 1, "AAAA" to 0)
        for (mode in padModesAllowingPadding()) {
            decodePaddingBeforeFinalNonPaddingCharErrorInvalidByteAtFirstPad(standardWithPadMode(true, mode), suffixes)
        }
    }

    @Test
    fun decodePaddingBeforeFinalNonPaddingCharErrorInvalidByteAtFirstPadNonCanonicalPaddingSuffix() {
        val suffixes =
            listOf(
                "AA==" to 2,
                "AA=" to 1,
                "AA" to 0,
                "AAA=" to 1,
                "AAA" to 0,
                "AAAA" to 0,
            )
        decodePaddingBeforeFinalNonPaddingCharErrorInvalidByteAtFirstPad(
            standardWithPadMode(true, DecodePaddingMode.Indifferent),
            suffixes,
        )
    }

    @Test
    fun decodePaddingStartsBeforeFinalChunkErrorInvalidByteAtFirstPad() {
        val rng = Random(0xE105)
        for (mode in padModesAllowingPadding()) {
            val engine = standardWithPadMode(true, mode)
            repeat(500) {
                val suffixLen = rng.nextInt(1, 5)
                val encoded = prefixedBytes("AAAA", rng.nextInt(1, 256), "").toMutableList()
                repeat(suffixLen) { encoded.add(PAD_BYTE) }
                val paddingLen = rng.nextInt(suffixLen + 1, encoded.size)
                val paddingStart = encoded.size - paddingLen
                for (index in paddingStart until encoded.size) encoded[index] = PAD_BYTE
                assertDecodeError(DecodeError.InvalidByte(paddingStart, PAD_BYTE), engine.decode(encoded.toByteArray()))
            }
        }
    }

    @Test
    fun decodeTooLittleDataBeforePaddingErrorInvalidByte() {
        val rng = Random(0xE106)
        for (mode in allPadModes()) {
            val engine = standardWithPadMode(true, mode)
            repeat(500) {
                val suffixDataLen = rng.nextInt(0, 2)
                val prefixQuadLen = rng.nextInt(0, 256)
                for (paddingLen in 1..(4 - suffixDataLen)) {
                    val encoded = prefixedBytes("ABCD", prefixQuadLen, "").toMutableList()
                    repeat(suffixDataLen) { encoded.add('A'.code.toByte()) }
                    repeat(paddingLen) { encoded.add(PAD_BYTE) }
                    assertDecodeError(
                        DecodeError.InvalidByte(prefixQuadLen * 4 + suffixDataLen, PAD_BYTE),
                        engine.decode(encoded.toByteArray()),
                    )
                }
            }
        }
    }

    @Test
    fun decodeMalleabilityTestCase3ByteSuffixValid() {
        assertContentEquals("Hello".bytes(), standard().decode("SGVsbG8=".bytes()).getOrThrow())
    }

    @Test
    fun decodeMalleabilityTestCase3ByteSuffixInvalidTrailingSymbol() {
        assertDecodeError(DecodeError.InvalidLastSymbol(6, 0x39), standard().decode("SGVsbG9=".bytes()))
    }

    @Test
    fun decodeMalleabilityTestCase3ByteSuffixNoPadding() {
        assertDecodeError(DecodeError.InvalidPadding, standard().decode("SGVsbG9".bytes()))
    }

    @Test
    fun decodeMalleabilityTestCase2ByteSuffixValidTwoPaddingSymbols() {
        assertContentEquals("Hell".bytes(), standard().decode("SGVsbA==".bytes()).getOrThrow())
    }

    @Test
    fun decodeMalleabilityTestCase2ByteSuffixShortPadding() {
        assertDecodeError(DecodeError.InvalidPadding, standard().decode("SGVsbA=".bytes()))
    }

    @Test
    fun decodeMalleabilityTestCase2ByteSuffixNoPadding() {
        assertDecodeError(DecodeError.InvalidPadding, standard().decode("SGVsbA".bytes()))
    }

    @Test
    fun decodeMalleabilityTestCase2ByteSuffixTooMuchPadding() {
        assertDecodeError(DecodeError.InvalidByte(6, PAD_BYTE), standard().decode("SGVsbA====".bytes()))
    }

    @Test
    fun decodePadModeRequiresCanonicalAcceptsCanonical() {
        assertAllSuffixesOk(standardWithPadMode(true, DecodePaddingMode.RequireCanonical), listOf("/w==", "iYU=", "AAAA"))
    }

    @Test
    fun decodePadModeRequiresCanonicalRejectsNonCanonical() {
        val engine = standardWithPadMode(true, DecodePaddingMode.RequireCanonical)
        for (numPrefixQuads in 0..255) {
            for (suffix in listOf("/w", "/w=", "iYU")) {
                assertDecodeError(DecodeError.InvalidPadding, engine.decode(prefixedBytes("AAAA", numPrefixQuads, suffix)))
            }
        }
    }

    @Test
    fun decodePadModeRequiresNoPaddingAcceptsNoPadding() {
        assertAllSuffixesOk(standardWithPadMode(true, DecodePaddingMode.RequireNone), listOf("/w", "iYU", "AAAA"))
    }

    @Test
    fun decodePadModeRequiresNoPaddingRejectsAnyPadding() {
        val engine = standardWithPadMode(true, DecodePaddingMode.RequireNone)
        for (numPrefixQuads in 0..255) {
            for (suffix in listOf("/w=", "/w==", "iYU=")) {
                assertDecodeError(DecodeError.InvalidPadding, engine.decode(prefixedBytes("AAAA", numPrefixQuads, suffix)))
            }
        }
    }

    @Test
    fun decodePadModeIndifferentPaddingAcceptsAnything() {
        assertAllSuffixesOk(
            standardWithPadMode(true, DecodePaddingMode.Indifferent),
            listOf("/w", "/w=", "/w==", "iYU", "iYU=", "AAAA"),
        )
    }

    @Test
    fun decodeInvalidTrailingBytesAllPadModesInvalidByte() {
        for (mode in allPadModes()) {
            doInvalidTrailingByte(standardWithPadMode(true, mode))
        }
    }

    @Test
    fun decodeInvalidTrailingBytesInvalidByte() {
        for (mode in padModesAllowingPadding()) {
            doInvalidTrailingByte(standardWithPadMode(true, mode))
        }
    }

    @Test
    fun decodeInvalidTrailingPaddingAsInvalidByteAtFirstPadByte() {
        for (mode in padModesAllowingPadding()) {
            doInvalidTrailingPaddingAsInvalidByteAtFirstPadding(standardWithPadMode(true, mode))
        }
    }

    @Test
    fun decodeInvalidTrailingPaddingAsInvalidByteAtFirstByteAllModes() {
        for (mode in allPadModes()) {
            doInvalidTrailingPaddingAsInvalidByteAtFirstPadding(standardWithPadMode(true, mode))
        }
    }

    @Test
    fun decodeIntoSliceFitsInPreciselySizedSlice() {
        val rng = Random(0xE107)
        repeat(500) {
            val original = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(original)
            val engine = randomEngine(rng)
            val encoded = engine.encode(original).encodeToByteArray()
            val decoded = ByteArray(original.size)

            assertEquals(original.size, engine.decodeSliceUnchecked(encoded, decoded).getOrThrow())
            assertContentEquals(original, decoded)

            decoded.fill(0)
            assertEquals(original.size, engine.decodeSlice(encoded, decoded).getOrThrow())
            assertContentEquals(original, decoded)
        }
    }

    @Test
    fun innerDecodeReportsPaddingPosition() {
        val engine = standard()
        for (padPosition in 1 until 2_000) {
            val b64 = buildString {
                repeat(padPosition) { append('A') }
                repeat(4 - (padPosition % 4)) { append('=') }
            }.encodeToByteArray()
            val decoded = ByteArray(padPosition)
            val result = engine.internalDecode(b64, decoded, engine.internalDecodedLenEstimate(b64.size))
            if (padPosition % 4 < 2) {
                assertSliceDecodeError(DecodeError.InvalidByte(padPosition, PAD_BYTE), result)
            } else {
                val decodedBytes =
                    padPosition / 4 * 3 +
                        when (padPosition % 4) {
                            0 -> 0
                            2 -> 1
                            3 -> 2
                            else -> error("unreachable")
                        }
                assertEquals(DecodeMetadata.new(decodedBytes, padPosition), result.getOrThrow())
            }
        }
    }

    @Test
    fun decodeLengthEstimateDelta() {
        for (engine in listOf(standard(), standardUnpadded())) {
            for (padding in listOf(true, false)) {
                for (origLen in 0 until 1000) {
                    val encodedLen = encodedLen(origLen, padding) ?: error("unexpected overflow")
                    val decodedEstimate = engine.internalDecodedLenEstimate(encodedLen).decodedLenEstimate()
                    assertTrue(decodedEstimate >= origLen)
                    assertTrue(decodedEstimate - origLen < 3)
                }
            }
        }
    }

    @Test
    fun decodeSliceCheckedFailsGracefullyAtAllOutputLengths() {
        val rng = Random(0xE108)
        for (originalLen in 0 until 1000) {
            val original = ByteArray(originalLen)
            rng.nextBytes(original)
            for (mode in allPadModes()) {
                val engine =
                    standardWithPadMode(
                        mode != DecodePaddingMode.RequireNone,
                        mode,
                    )
                val encoded = engine.encode(original).encodeToByteArray()
                for (decodeBufLen in 0 until originalLen) {
                    val decodeBuf = ByteArray(decodeBufLen)
                    assertEquals(DecodeSliceError.OutputSliceTooSmall, engine.decodeSlice(encoded, decodeBuf).exceptionOrNull())
                    assertEquals(
                        DecodeSliceError.OutputSliceTooSmall,
                        engine
                            .internalDecode(encoded, decodeBuf, engine.internalDecodedLenEstimate(encoded.size))
                            .exceptionOrNull(),
                    )
                }
                val decodeBuf = ByteArray(originalLen)
                assertEquals(originalLen, engine.decodeSlice(encoded, decodeBuf).getOrThrow())
                assertContentEquals(original, decodeBuf)
            }
        }
    }

    private fun decodePaddingBeforeFinalNonPaddingCharErrorInvalidByteAtFirstPad(
        engine: GeneralPurpose,
        suffixes: List<Pair<String, Int>>,
    ) {
        val rng = Random(0xE104)
        repeat(500) {
            for ((suffix, suffixOffset) in suffixes) {
                val encoded = prefixedBytes("AAAA", rng.nextInt(0, 257), suffix)
                val lastNonPaddingOffset = encoded.size - 1 - suffixOffset
                val paddingEnd = rng.nextInt(0, lastNonPaddingOffset)
                val paddingLen = rng.nextInt(1, minOf(100, paddingEnd + 1) + 1)
                val paddingStart = (paddingEnd - paddingLen).coerceAtLeast(0)
                for (index in paddingStart..paddingEnd) encoded[index] = PAD_BYTE
                assertDecodeError(DecodeError.InvalidByte(paddingStart, PAD_BYTE), engine.decode(encoded))
            }
        }
    }

    private fun doInvalidTrailingByte(engine: GeneralPurpose) {
        for (lastByte in listOf('*'.code.toByte(), '\n'.code.toByte())) {
            for (numPrefixQuads in 0..255) {
                val input = prefixedBytes("ABCD", numPrefixQuads, "Cg==").toMutableList()
                input.add(lastByte)
                assertDecodeError(DecodeError.InvalidByte(numPrefixQuads * 4 + 4, lastByte), engine.decode(input.toByteArray()))
            }
        }
    }

    private fun doInvalidTrailingPaddingAsInvalidByteAtFirstPadding(engine: GeneralPurpose) {
        for (numPrefixQuads in 0..255) {
            for ((suffix, padOffset) in listOf("AA===" to 2, "AAA==" to 3, "AAAA=" to 4)) {
                assertDecodeError(
                    DecodeError.InvalidByte(numPrefixQuads * 4 + padOffset, PAD_BYTE),
                    engine.decode(prefixedBytes("ABCD", numPrefixQuads, suffix)),
                )
            }
        }
    }

    private fun assertAllSuffixesOk(
        engine: GeneralPurpose,
        suffixes: List<String>,
    ) {
        for (numPrefixQuads in 0..255) {
            for (suffix in suffixes) {
                assertTrue(engine.decode(prefixedBytes("AAAA", numPrefixQuads, suffix)).isSuccess)
            }
        }
    }

    private fun assertTolerantDecode(
        engine: GeneralPurpose,
        prefix: String,
        expected: ByteArray,
        data: String,
    ) {
        val decoded = engine.decode((prefix + data).bytes()).getOrThrow()
        assertContentEquals(expected, decoded.copyOfRange(prefix.length / 4 * 3, decoded.size))
    }

    private fun assertEncodeSanity(
        encoded: String,
        padded: Boolean,
        inputLen: Int,
    ) {
        val inputRem = inputLen % 3
        val expectedPaddingLen = if (inputRem > 0 && padded) 3 - inputRem else 0
        assertEquals(encodedLen(inputLen, padded), encoded.length)
        assertEquals(expectedPaddingLen, encoded.count { it == '=' })
        assertEquals(encoded, encoded.bytes().decodeToString())
    }

    private fun assertDecodeError(
        expected: DecodeError,
        result: Result<*>,
    ) {
        assertEquals(expected, result.exceptionOrNull())
    }

    private fun assertSliceDecodeError(
        expected: DecodeError,
        result: Result<*>,
    ) {
        val error = result.exceptionOrNull()
        if (error is DecodeSliceError.DecodeErrorVariant) {
            assertEquals(expected, error.error)
        } else {
            assertEquals(expected, error)
        }
    }

    private fun String.bytes(): ByteArray = encodeToByteArray()

    private fun prefixedString(
        quad: String,
        count: Int,
        suffix: String,
    ): String =
        prefixedBytes(quad, count, suffix).decodeToString()

    private fun prefixedBytes(
        quad: String,
        count: Int,
        suffix: String,
    ): ByteArray {
        val quadBytes = quad.encodeToByteArray()
        val suffixBytes = suffix.encodeToByteArray()
        val output = ByteArray(quadBytes.size * count + suffixBytes.size)
        var offset = 0
        repeat(count) {
            quadBytes.copyInto(output, offset)
            offset += quadBytes.size
        }
        suffixBytes.copyInto(output, offset)
        return output
    }

    private fun standard(): GeneralPurpose = STANDARD_ENGINE

    private fun standardUnpadded(): GeneralPurpose =
        GeneralPurpose(
            STANDARD,
            GeneralPurposeConfig()
                .withEncodePadding(false)
                .withDecodePaddingMode(DecodePaddingMode.RequireNone),
        )

    private fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): GeneralPurpose =
        GeneralPurpose(
            STANDARD,
            GeneralPurposeConfig()
                .withEncodePadding(encodePad)
                .withDecodePaddingMode(decodePaddingMode),
        )

    private fun standardAllowTrailingBits(): GeneralPurpose =
        GeneralPurpose(
            STANDARD,
            GeneralPurposeConfig().withDecodeAllowTrailingBits(true),
        )

    private fun randomEngine(
        rng: Random,
        alphabet: Alphabet = randomAlphabet(rng),
    ): GeneralPurpose = GeneralPurpose(alphabet, randomConfig(rng))

    private fun allPadModes(): List<DecodePaddingMode> =
        listOf(
            DecodePaddingMode.Indifferent,
            DecodePaddingMode.RequireCanonical,
            DecodePaddingMode.RequireNone,
        )

    private fun padModesAllowingPadding(): List<DecodePaddingMode> =
        listOf(
            DecodePaddingMode.Indifferent,
            DecodePaddingMode.RequireCanonical,
        )
}
