// port-lint: tests engine/tests.rs
package io.github.kotlinmania.base64.engine

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.EncodeSliceError
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
    fun roundtripRandom() {
        val rng = Random(0xE099)
        repeat(500) {
            val engine = randomEngine(rng)
            val original = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(original)
            val encoded = ByteArray(encodedLen(original.size, engine.config().encodePadding()) ?: error("unexpected overflow"))
            val decoded = ByteArray(original.size)

            val encodedLen = engine.encodeSlice(original, encoded).getOrThrow()
            val decodedLen = engine.decodeSliceUnchecked(encoded.copyOf(encodedLen), decoded).getOrThrow()

            assertEquals(original.size, decodedLen)
            assertContentEquals(original, decoded.copyOf(decodedLen))
        }
    }

    @Test
    fun upstreamEngineTemplatesRunAcrossAllWrappers() {
        forAllEngines { wrapper ->
            runRfcVectors(wrapper)
            runRoundtripRandom(wrapper)
            runEncodeDoesntWriteExtraBytes(wrapper)
            runEncodeSliceFitsPrecisely(wrapper)
            runDecodeDoesntWriteExtraBytes(wrapper)
            runInvalidLastSymbolChecks(wrapper)
            runLastQuadInvalidLengthChecks(wrapper)
            runLastQuadInvalidByteChecks(wrapper)
            runRepresentativeTwoSymbolTailChecks(wrapper)
            runRepresentativeThreeSymbolTailChecks(wrapper)
            runTrailingBitsForgivingChecks(wrapper)
            runInvalidByteChecks(wrapper)
            runPaddingModeChecks(wrapper)
            runDecodeIntoSlicePreciseChecks(wrapper)
            runLengthEstimateChecks(wrapper)
            runCheckedDecodeSliceOutputLengthChecks(wrapper)
        }

        forAllEnginesExceptDecoderReader { wrapper ->
            runNoPaddingTrailingByteModeChecks(wrapper)
            runTooMuchPaddingMalleabilityCheck(wrapper)
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
            val b64 =
                buildString {
                    repeat(padPosition) { append('A') }
                    repeat(4 - (padPosition % 4)) { append('=') }
                }.encodeToByteArray()
            val decoded = ByteArray(padPosition)
            val result = engine.internalDecode(b64, decoded)
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
                    val decodedEstimate = engine.decodedLenEstimate(encodedLen)
                    assertTrue(decodedEstimate >= origLen)
                    assertTrue(decodedEstimate - origLen < 3)
                }
            }
        }
    }

    @Test
    fun estimateViaU128Inflation() {
        val encodedLengths = (0 until 1000) + ((Int.MAX_VALUE - 1000)..Int.MAX_VALUE)

        for (encodedLen in encodedLengths) {
            val lenWide = encodedLen.toLong()
            val estimate = standard().decodedLenEstimate(encodedLen)

            assertEquals((lenWide + 3L) / 4L * 3L, estimate.toLong())
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
                            .internalDecode(encoded, decodeBuf)
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
        engine: EngineSubject,
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

    private fun runRfcVectors(wrapper: EngineWrapper) {
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
        val engine = wrapper.standard()
        val engineNoPadding = wrapper.standardUnpadded()

        for ((orig, encoded) in data) {
            val input = orig.bytes()
            val encodedWithoutPadding = encoded.trimEnd('=')
            val encodeBuf = ByteArray(8)
            val encodeLen = engineNoPadding.internalEncode(input, encodeBuf)
            assertEquals(encodedWithoutPadding, encodeBuf.copyOf(encodeLen).decodeToString())
            assertContentEquals(
                input,
                engineNoPadding.decode(encodedWithoutPadding.bytes()).getOrThrow(),
            )
            if (encoded.contains('=')) {
                assertDecodeError(DecodeError.InvalidPadding, engineNoPadding.decode(encoded.bytes()))
            }

            val paddedBuf = ByteArray(8)
            val paddedLen = engine.internalEncode(input, paddedBuf)
            val padLen = addPadding(paddedLen, paddedBuf, paddedLen)
            assertContentEquals(encoded.bytes(), paddedBuf.copyOf(paddedLen + padLen))
            assertContentEquals(input, engine.decode(encoded.bytes()).getOrThrow())
            if (encoded.contains('=')) {
                assertDecodeError(DecodeError.InvalidPadding, engine.decode(encodedWithoutPadding.bytes()))
            }
        }
    }

    private fun runRoundtripRandom(wrapper: EngineWrapper) {
        val rng = Random(0xA100)
        repeat(100) {
            val engine = wrapper.random(rng)
            val input = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(input)
            val encodedSize = encodedLen(input.size, engine.config().encodePadding()) ?: error("unexpected overflow")
            val encoded = ByteArray(encodedSize)
            val decoded = ByteArray(input.size)

            val encodedLen = engine.encodeSlice(input, encoded).getOrThrow()
            val decodedLen = engine.decodeSliceUnchecked(encoded.copyOf(encodedLen), decoded).getOrThrow()

            assertEquals(input.size, decodedLen)
            assertContentEquals(input, decoded.copyOf(decodedLen))
        }
    }

    private fun runEncodeDoesntWriteExtraBytes(wrapper: EngineWrapper) {
        val rng = Random(0xA101)
        repeat(100) {
            val engine = wrapper.random(rng)
            val input = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(input)
            val output = ByteArray(1024 + input.size * 2) { rng.nextInt(0, 256).toByte() }
            val backup = output.copyOf()

            val encodedLenNoPad = engine.internalEncode(input, output)
            assertEquals(encodedLen(input.size, false), encodedLenNoPad)
            assertContentEquals(backup.copyOfRange(encodedLenNoPad, backup.size), output.copyOfRange(encodedLenNoPad, output.size))
            val padLen = if (engine.config().encodePadding()) addPadding(encodedLenNoPad, output, encodedLenNoPad) else 0
            assertContentEquals(input, engine.decode(output.copyOfRange(0, encodedLenNoPad + padLen)).getOrThrow())
        }
    }

    private fun runEncodeSliceFitsPrecisely(wrapper: EngineWrapper) {
        val rng = Random(0xA102)
        repeat(100) {
            val engine = wrapper.random(rng)
            val input = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(input)
            val output = ByteArray(encodedLen(input.size, engine.config().encodePadding()) ?: error("unexpected overflow"))
            assertEquals(output.size, engine.encodeSlice(input, output).getOrThrow())
            assertContentEquals(input, engine.decode(output).getOrThrow())
        }
    }

    private fun runDecodeDoesntWriteExtraBytes(wrapper: EngineWrapper) {
        val rng = Random(0xA103)
        repeat(100) {
            val engine = wrapper.random(rng)
            val input = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(input)
            val encoded = engine.encode(input).bytes()
            val prefixLen = 64
            val output = ByteArray(prefixLen + input.size + 64) { rng.nextInt(0, 256).toByte() }
            val backup = output.copyOf()
            val window = ByteArray(output.size - prefixLen)
            val decodedLen = engine.decodeSliceUnchecked(encoded, window).getOrThrow()
            window.copyInto(output, prefixLen, 0, decodedLen)

            assertEquals(input.size, decodedLen)
            assertContentEquals(input, output.copyOfRange(prefixLen, prefixLen + decodedLen))
            assertContentEquals(backup.copyOfRange(0, prefixLen), output.copyOfRange(0, prefixLen))
            assertContentEquals(backup.copyOfRange(prefixLen + decodedLen, backup.size), output.copyOfRange(prefixLen + decodedLen, output.size))
        }
    }

    private fun runInvalidLastSymbolChecks(wrapper: EngineWrapper) {
        val engine = wrapper.standard()
        assertContentEquals(byteArrayOf(0x89.toByte(), 0x85.toByte()), engine.decode("iYU=".bytes()).getOrThrow())
        assertContentEquals(byteArrayOf(0xFF.toByte()), engine.decode("/w==".bytes()).getOrThrow())
        for ((suffix, offset) in listOf("/x==" to 1, "/z==" to 1, "/0==" to 1, "/9==" to 1, "/+==" to 1, "//==" to 1, "iYV=" to 2, "iYW=" to 2, "iYX=" to 2)) {
            for (prefixQuads in 0..32) {
                val encoded = prefixedString("AAAA", prefixQuads, suffix)
                assertDecodeError(DecodeError.InvalidLastSymbol(encoded.length - 4 + offset, suffix.bytes()[offset]), engine.decode(encoded.bytes()))
            }
        }
    }

    private fun runLastQuadInvalidLengthChecks(wrapper: EngineWrapper) {
        for (len in (0 until 32).map { it * 4 + 1 }) {
            for (mode in allPadModes()) {
                val input = MutableList(len) { 'A'.code.toByte() }
                val engine = wrapper.standardWithPadMode(true, mode)
                assertDecodeError(DecodeError.InvalidLength(len), engine.decode(input.toByteArray()))
                repeat(3) {
                    input.add(PAD_BYTE)
                    assertDecodeError(DecodeError.InvalidByte(len, PAD_BYTE), engine.decode(input.toByteArray()))
                }
            }
        }
    }

    private fun runLastQuadInvalidByteChecks(wrapper: EngineWrapper) {
        for (prefixLen in (0 until 32).map { it * 4 }) {
            for (mode in allPadModes()) {
                val input = MutableList(prefixLen) { 'A'.code.toByte() }
                input.add('*'.code.toByte())
                val engine = wrapper.standardWithPadMode(true, mode)
                assertDecodeError(DecodeError.InvalidByte(prefixLen, '*'.code.toByte()), engine.decode(input.toByteArray()))
                repeat(3) {
                    input.add(PAD_BYTE)
                    assertDecodeError(DecodeError.InvalidByte(prefixLen, '*'.code.toByte()), engine.decode(input.toByteArray()))
                }
            }
        }
    }

    private fun runRepresentativeTwoSymbolTailChecks(wrapper: EngineWrapper) {
        val engine = wrapper.standard()
        val valid = mutableMapOf<String, ByteArray>()
        for (byte in 0..255) {
            val b64 = ByteArray(4)
            assertEquals(2, engine.internalEncode(byteArrayOf(byte.toByte()), b64))
            addPadding(2, b64, 2)
            valid[b64.decodeToString()] = byteArrayOf(byte.toByte())
        }

        for (prefixQuads in listOf(0, 1, 32)) {
            val prefix = prefixedBytes("AAAA", prefixQuads, "")
            for (s1 in representativeSymbols()) {
                for (s2 in representativeSymbols()) {
                    val symbols = byteArrayOf(s1, s2, PAD_BYTE, PAD_BYTE)
                    val expected = valid[symbols.decodeToString()]
                    if (expected != null) {
                        val decoded = engine.decode(prefix + symbols).getOrThrow()
                        assertContentEquals(expected, decoded.copyOfRange(prefixQuads * 3, decoded.size))
                    } else {
                        assertDecodeError(DecodeError.InvalidLastSymbol(1, s2), engine.decode(symbols))
                    }
                }
            }
        }
    }

    private fun runRepresentativeThreeSymbolTailChecks(wrapper: EngineWrapper) {
        val engine = wrapper.standard()
        val valid = mutableMapOf<String, ByteArray>()
        for (b1 in 0..255) {
            for (b2 in 0..255) {
                val bytes = byteArrayOf(b1.toByte(), b2.toByte())
                val b64 = ByteArray(4)
                assertEquals(3, engine.internalEncode(bytes, b64))
                addPadding(3, b64, 3)
                valid[b64.decodeToString()] = bytes
            }
        }

        for (prefixQuads in listOf(0, 1, 8)) {
            val prefix = prefixedBytes("AAAA", prefixQuads, "")
            for (s1 in representativeSymbols()) {
                for (s2 in representativeSymbols()) {
                    for (s3 in representativeSymbols()) {
                        val symbols = byteArrayOf(s1, s2, s3, PAD_BYTE)
                        val expected = valid[symbols.decodeToString()]
                        if (expected != null) {
                            val decoded = engine.decode(prefix + symbols).getOrThrow()
                            assertContentEquals(expected, decoded.copyOfRange(prefixQuads * 3, decoded.size))
                        } else {
                            assertDecodeError(DecodeError.InvalidLastSymbol(2, s3), engine.decode(symbols))
                        }
                    }
                }
            }
        }
    }

    private fun runTrailingBitsForgivingChecks(wrapper: EngineWrapper) {
        val strict = wrapper.standard()
        val forgiving = wrapper.standardAllowTrailingBits()
        var prefix = ""
        repeat(32) {
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

    private fun runInvalidByteChecks(wrapper: EngineWrapper) {
        val rng = Random(0xA104)
        repeat(100) {
            val alphabet = randomAlphabet(rng)
            val engine = wrapper.randomAlphabet(rng, alphabet)
            val input = ByteArray(rng.nextInt(1, 1000))
            rng.nextBytes(input)
            val encoded = engine.encode(input).bytes()
            val invalidByte = generateSequence { rng.nextInt(0, 256).toByte() }.first { it !in alphabet.symbols && it != PAD_BYTE }
            val invalidIndex = rng.nextInt(0, input.size)
            encoded[invalidIndex] = invalidByte
            assertDecodeError(DecodeError.InvalidByte(invalidIndex, invalidByte), engine.decodeSliceUnchecked(encoded, ByteArray(input.size)))
        }
    }

    private fun runPaddingModeChecks(wrapper: EngineWrapper) {
        val canonical = wrapper.standardWithPadMode(true, DecodePaddingMode.RequireCanonical)
        assertAllSuffixesOk(canonical, listOf("/w==", "iYU=", "AAAA"))
        for (prefixQuads in 0..32) {
            for (suffix in listOf("/w", "/w=", "iYU")) {
                assertDecodeError(DecodeError.InvalidPadding, canonical.decode(prefixedBytes("AAAA", prefixQuads, suffix)))
            }
        }

        val noPadding = wrapper.standardWithPadMode(true, DecodePaddingMode.RequireNone)
        assertAllSuffixesOk(noPadding, listOf("/w", "iYU", "AAAA"))
        for (prefixQuads in 0..32) {
            for (suffix in listOf("/w=", "/w==", "iYU=")) {
                assertDecodeError(DecodeError.InvalidPadding, noPadding.decode(prefixedBytes("AAAA", prefixQuads, suffix)))
            }
        }

        assertAllSuffixesOk(
            wrapper.standardWithPadMode(true, DecodePaddingMode.Indifferent),
            listOf("/w", "/w=", "/w==", "iYU", "iYU=", "AAAA"),
        )
    }

    private fun runDecodeIntoSlicePreciseChecks(wrapper: EngineWrapper) {
        val rng = Random(0xA105)
        repeat(100) {
            val engine = wrapper.random(rng)
            val input = ByteArray(rng.nextInt(0, 1000))
            rng.nextBytes(input)
            val encoded = engine.encode(input).bytes()
            val decoded = ByteArray(input.size)
            assertEquals(input.size, engine.decodeSliceUnchecked(encoded, decoded).getOrThrow())
            assertContentEquals(input, decoded)
            decoded.fill(0)
            assertEquals(input.size, engine.decodeSlice(encoded, decoded).getOrThrow())
            assertContentEquals(input, decoded)
        }
    }

    private fun runLengthEstimateChecks(wrapper: EngineWrapper) {
        for (engine in listOf(wrapper.standard(), wrapper.standardUnpadded())) {
            for (padding in listOf(true, false)) {
                for (origLen in 0 until 1000) {
                    val encodedLen = encodedLen(origLen, padding) ?: error("unexpected overflow")
                    val decodedEstimate = engine.decodedLenEstimate(encodedLen)
                    assertTrue(decodedEstimate >= origLen)
                    assertTrue(decodedEstimate - origLen < 3)
                }
            }
        }
    }

    private fun runCheckedDecodeSliceOutputLengthChecks(wrapper: EngineWrapper) {
        val rng = Random(0xA106)
        for (originalLen in 0 until 64) {
            val original = ByteArray(originalLen)
            rng.nextBytes(original)
            for (mode in allPadModes()) {
                val engine = wrapper.standardWithPadMode(mode != DecodePaddingMode.RequireNone, mode)
                val encoded = engine.encode(original).bytes()
                for (decodeBufLen in 0 until originalLen) {
                    val decodeBuf = ByteArray(decodeBufLen)
                    assertEquals(DecodeSliceError.OutputSliceTooSmall, engine.decodeSlice(encoded, decodeBuf).exceptionOrNull())
                    assertEquals(DecodeSliceError.OutputSliceTooSmall, engine.internalDecode(encoded, decodeBuf).exceptionOrNull())
                }
                val decodeBuf = ByteArray(originalLen)
                assertEquals(originalLen, engine.decodeSlice(encoded, decodeBuf).getOrThrow())
                assertContentEquals(original, decodeBuf)
            }
        }
    }

    private fun runNoPaddingTrailingByteModeChecks(wrapper: EngineWrapper) {
        for (mode in allPadModes()) {
            doInvalidTrailingByte(wrapper.standardWithPadMode(true, mode))
            doInvalidTrailingPaddingAsInvalidByteAtFirstPadding(wrapper.standardWithPadMode(true, mode))
        }
    }

    private fun runTooMuchPaddingMalleabilityCheck(wrapper: EngineWrapper) {
        assertDecodeError(DecodeError.InvalidByte(6, PAD_BYTE), wrapper.standard().decode("SGVsbA====".bytes()))
    }

    private fun doInvalidTrailingByte(engine: EngineSubject) {
        for (lastByte in listOf('*'.code.toByte(), '\n'.code.toByte())) {
            for (numPrefixQuads in 0..255) {
                val input = prefixedBytes("ABCD", numPrefixQuads, "Cg==").toMutableList()
                input.add(lastByte)
                assertDecodeError(DecodeError.InvalidByte(numPrefixQuads * 4 + 4, lastByte), engine.decode(input.toByteArray()))
            }
        }
    }

    private fun doInvalidTrailingPaddingAsInvalidByteAtFirstPadding(engine: EngineSubject) {
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
        engine: EngineSubject,
        suffixes: List<String>,
    ) {
        for (numPrefixQuads in 0..255) {
            for (suffix in suffixes) {
                assertTrue(engine.decode(prefixedBytes("AAAA", numPrefixQuads, suffix)).isSuccess)
            }
        }
    }

    private fun assertTolerantDecode(
        engine: EngineSubject,
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

    private fun standard(): EngineSubject = GeneralPurposeWrapper.standard()

    private fun standardUnpadded(): EngineSubject =
        GeneralPurposeWrapper.standardUnpadded()

    private fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): EngineSubject =
        GeneralPurposeWrapper.standardWithPadMode(encodePad, decodePaddingMode)

    private fun standardAllowTrailingBits(): EngineSubject =
        GeneralPurposeWrapper.standardAllowTrailingBits()

    private fun randomEngine(
        rng: Random,
        alphabet: Alphabet = randomAlphabet(rng),
    ): EngineSubject = GeneralPurposeSubject(GeneralPurpose(alphabet, randomConfig(rng)))

    private fun allEngines(): List<EngineWrapper> =
        listOf(
            GeneralPurposeWrapper,
            NaiveWrapper,
            DecoderReaderEngineWrapper,
        )

    private fun allEnginesExceptDecoderReader(): List<EngineWrapper> =
        listOf(
            GeneralPurposeWrapper,
            NaiveWrapper,
        )

    private fun forAllEngines(block: (EngineWrapper) -> Unit) {
        for (wrapper in allEngines()) {
            block(wrapper)
        }
    }

    private fun forAllEnginesExceptDecoderReader(block: (EngineWrapper) -> Unit) {
        for (wrapper in allEnginesExceptDecoderReader()) {
            block(wrapper)
        }
    }

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

    private fun representativeSymbols(): ByteArray =
        byteArrayOf(
            'A'.code.toByte(),
            'B'.code.toByte(),
            'Z'.code.toByte(),
            'a'.code.toByte(),
            'z'.code.toByte(),
            '0'.code.toByte(),
            '9'.code.toByte(),
            '+'.code.toByte(),
            '/'.code.toByte(),
        )
}

private interface EngineWrapper {
    fun standard(): EngineSubject

    fun standardUnpadded(): EngineSubject

    fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): EngineSubject

    fun standardAllowTrailingBits(): EngineSubject

    fun random(rng: Random): EngineSubject =
        randomAlphabet(rng).let { alphabet -> randomAlphabet(rng, alphabet) }

    fun randomAlphabet(
        rng: Random,
        alphabet: Alphabet,
    ): EngineSubject
}

private object GeneralPurposeWrapper : EngineWrapper {
    override fun standard(): EngineSubject =
        GeneralPurposeSubject(STANDARD_ENGINE)

    override fun standardUnpadded(): EngineSubject =
        GeneralPurposeSubject(
            GeneralPurpose(
                STANDARD,
                GeneralPurposeConfig()
                    .withEncodePadding(false)
                    .withDecodePaddingMode(DecodePaddingMode.RequireNone),
            ),
        )

    override fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): EngineSubject =
        GeneralPurposeSubject(
            GeneralPurpose(
                STANDARD,
                GeneralPurposeConfig()
                    .withEncodePadding(encodePad)
                    .withDecodePaddingMode(decodePaddingMode),
            ),
        )

    override fun standardAllowTrailingBits(): EngineSubject =
        GeneralPurposeSubject(
            GeneralPurpose(
                STANDARD,
                GeneralPurposeConfig().withDecodeAllowTrailingBits(true),
            ),
        )

    override fun randomAlphabet(
        rng: Random,
        alphabet: Alphabet,
    ): EngineSubject =
        GeneralPurposeSubject(GeneralPurpose(alphabet, randomConfig(rng)))
}

private object NaiveWrapper : EngineWrapper {
    override fun standard(): EngineSubject =
        NaiveSubject(
            Naive(
                STANDARD,
                NaiveConfig(
                    shouldEncodePadding = true,
                    decodeAllowTrailingBits = false,
                    decodePaddingMode = DecodePaddingMode.RequireCanonical,
                ),
            ),
        )

    override fun standardUnpadded(): EngineSubject =
        NaiveSubject(
            Naive(
                STANDARD,
                NaiveConfig(
                    shouldEncodePadding = false,
                    decodeAllowTrailingBits = false,
                    decodePaddingMode = DecodePaddingMode.RequireNone,
                ),
            ),
        )

    override fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): EngineSubject =
        NaiveSubject(
            Naive(
                STANDARD,
                NaiveConfig(
                    shouldEncodePadding = encodePad,
                    decodeAllowTrailingBits = false,
                    decodePaddingMode = decodePaddingMode,
                ),
            ),
        )

    override fun standardAllowTrailingBits(): EngineSubject =
        NaiveSubject(
            Naive(
                STANDARD,
                NaiveConfig(
                    shouldEncodePadding = true,
                    decodeAllowTrailingBits = true,
                    decodePaddingMode = DecodePaddingMode.RequireCanonical,
                ),
            ),
        )

    override fun randomAlphabet(
        rng: Random,
        alphabet: Alphabet,
    ): EngineSubject {
        val mode =
            when (rng.nextInt(3)) {
                0 -> DecodePaddingMode.Indifferent
                1 -> DecodePaddingMode.RequireCanonical
                else -> DecodePaddingMode.RequireNone
            }
        return NaiveSubject(
            Naive(
                alphabet,
                NaiveConfig(
                    shouldEncodePadding =
                        when (mode) {
                            DecodePaddingMode.Indifferent -> rng.nextBoolean()
                            DecodePaddingMode.RequireCanonical -> true
                            DecodePaddingMode.RequireNone -> false
                        },
                    decodeAllowTrailingBits = rng.nextBoolean(),
                    decodePaddingMode = mode,
                ),
            ),
        )
    }
}

private object DecoderReaderEngineWrapper : EngineWrapper {
    override fun standard(): EngineSubject =
        DecoderReaderSubject(GeneralPurposeWrapper.standard())

    override fun standardUnpadded(): EngineSubject =
        DecoderReaderSubject(GeneralPurposeWrapper.standardUnpadded())

    override fun standardWithPadMode(
        encodePad: Boolean,
        decodePaddingMode: DecodePaddingMode,
    ): EngineSubject =
        DecoderReaderSubject(GeneralPurposeWrapper.standardWithPadMode(encodePad, decodePaddingMode))

    override fun standardAllowTrailingBits(): EngineSubject =
        DecoderReaderSubject(GeneralPurposeWrapper.standardAllowTrailingBits())

    override fun randomAlphabet(
        rng: Random,
        alphabet: Alphabet,
    ): EngineSubject =
        DecoderReaderSubject(GeneralPurposeWrapper.randomAlphabet(rng, alphabet))
}

private interface EngineSubject {
    fun internalEncode(
        input: ByteArray,
        output: ByteArray,
    ): Int

    fun decodedLenEstimate(inputLen: Int): Int

    fun internalDecode(
        input: ByteArray,
        output: ByteArray,
    ): Result<DecodeMetadata>

    fun config(): Config

    fun encode(input: ByteArray): String {
        val encodedSize = encodedLen(input.size, config().encodePadding()) ?: error("unexpected overflow")
        val output = ByteArray(encodedSize)
        val bytesWritten = internalEncode(input, output)
        if (config().encodePadding()) {
            addPadding(bytesWritten, output, bytesWritten)
        }
        return output.decodeToString()
    }

    fun encodeSlice(
        input: ByteArray,
        output: ByteArray,
    ): Result<Int> {
        val encodedSize = encodedLen(input.size, config().encodePadding()) ?: error("unexpected overflow")
        if (output.size < encodedSize) {
            return Result.failure(EncodeSliceError.OutputSliceTooSmall)
        }
        val bytesWritten = internalEncode(input, output)
        if (config().encodePadding()) {
            addPadding(bytesWritten, output, bytesWritten)
        }
        return Result.success(encodedSize)
    }

    fun encodeString(
        input: ByteArray,
        output: StringBuilder,
    ) {
        output.append(encode(input))
    }

    fun decode(input: ByteArray): Result<ByteArray> {
        val output = ByteArray(decodedLenEstimate(input.size))
        return internalDecode(input, output)
            .fold(
                onSuccess = { Result.success(output.copyOf(it.decodedLen)) },
                onFailure = {
                    if (it is DecodeSliceError.DecodeErrorVariant) {
                        Result.failure(it.error)
                    } else {
                        Result.failure(it)
                    }
                },
            )
    }

    fun decodeSlice(
        input: ByteArray,
        output: ByteArray,
    ): Result<Int> =
        internalDecode(input, output).map { it.decodedLen }

    fun decodeSliceUnchecked(
        input: ByteArray,
        output: ByteArray,
    ): Result<Int> =
        internalDecode(input, output)
            .fold(
                onSuccess = { Result.success(it.decodedLen) },
                onFailure = {
                    if (it is DecodeSliceError.DecodeErrorVariant) {
                        Result.failure(it.error)
                    } else {
                        Result.failure(it)
                    }
                },
            )

    fun decodeVec(
        input: ByteArray,
        output: MutableList<Byte>,
    ): Result<Unit> =
        decode(input).map { decoded ->
            for (byte in decoded) {
                output.add(byte)
            }
        }
}

private class GeneralPurposeSubject(
    private val engine: GeneralPurpose,
) : EngineSubject {
    override fun internalEncode(
        input: ByteArray,
        output: ByteArray,
    ): Int = engine.internalEncode(input, output)

    override fun decodedLenEstimate(inputLen: Int): Int =
        engine.internalDecodedLenEstimate(inputLen).decodedLenEstimate()

    override fun internalDecode(
        input: ByteArray,
        output: ByteArray,
    ): Result<DecodeMetadata> =
        engine.internalDecode(input, output, engine.internalDecodedLenEstimate(input.size))

    override fun config(): Config = engine.config()
}

private class NaiveSubject(
    private val engine: Naive,
) : EngineSubject {
    override fun internalEncode(
        input: ByteArray,
        output: ByteArray,
    ): Int = engine.internalEncode(input, output)

    override fun decodedLenEstimate(inputLen: Int): Int =
        engine.internalDecodedLenEstimate(inputLen).decodedLenEstimate()

    override fun internalDecode(
        input: ByteArray,
        output: ByteArray,
    ): Result<DecodeMetadata> =
        engine.internalDecode(input, output, engine.internalDecodedLenEstimate(input.size))

    override fun config(): Config = engine.config()
}

private class DecoderReaderSubject(
    private val engine: EngineSubject,
) : EngineSubject {
    override fun internalEncode(
        input: ByteArray,
        output: ByteArray,
    ): Int = engine.internalEncode(input, output)

    override fun decodedLenEstimate(inputLen: Int): Int =
        engine.decodedLenEstimate(inputLen)

    override fun internalDecode(
        input: ByteArray,
        output: ByteArray,
    ): Result<DecodeMetadata> {
        val reader = DecoderReader(ByteArrayReader(input), engine.asEngine())
        val buffer = mutableListOf<Byte>()
        val first = ByteArray(input.size)
        val firstRead =
            reader
                .read(first)
                .getOrElse { return Result.failure(it.decodeErrorOrNull() ?: it) }
        for (index in 0 until firstRead) {
            buffer.add(first[index])
        }
        reader
            .readToEnd(buffer)
            .getOrElse { return Result.failure(it.decodeErrorOrNull() ?: it) }

        if (output.size < buffer.size) {
            return Result.failure(DecodeSliceError.OutputSliceTooSmall)
        }
        for (index in buffer.indices) {
            output[index] = buffer[index]
        }
        val paddingOffset = input.indexOf(PAD_BYTE).takeIf { it >= 0 }
        return Result.success(DecodeMetadata.new(buffer.size, paddingOffset))
    }

    override fun config(): Config = engine.config()
}

private fun EngineSubject.asEngine(): Engine<Config, DecodeEstimate> =
    object : Engine<Config, DecodeEstimate> {
        override fun internalEncode(
            input: ByteArray,
            output: ByteArray,
        ): Int = this@asEngine.internalEncode(input, output)

        override fun internalDecodedLenEstimate(inputLen: Int): DecodeEstimate =
            object : DecodeEstimate {
                override fun decodedLenEstimate(): Int =
                    this@asEngine.decodedLenEstimate(inputLen)
            }

        override fun internalDecode(
            input: ByteArray,
            output: ByteArray,
            decodeEstimate: DecodeEstimate,
        ): Result<DecodeMetadata> =
            this@asEngine.internalDecode(input, output)

        override fun config(): Config =
            this@asEngine.config()
    }
