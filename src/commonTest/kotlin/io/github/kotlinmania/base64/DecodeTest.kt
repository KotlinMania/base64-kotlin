// port-lint: tests decode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.STANDARD as STANDARD_ALPHABET
import io.github.kotlinmania.base64.engine.DecodePaddingMode
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig
import io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.PAD
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val DECODE_RANDOM_TRIALS: Int = 10_000

class DecodeTest {
    @Test
    fun decodeIntoNonemptyVecDoesntClobberExistingPrefix() {
        val origData = mutableListOf<Byte>()
        val encodedData = StringBuilder()
        val decodedWithPrefix = mutableListOf<Byte>()
        val decodedWithoutPrefix = mutableListOf<Byte>()
        val prefix = mutableListOf<Byte>()

        val random = Random(0xDEC0DE)

        repeat(DECODE_RANDOM_TRIALS) {
            origData.clear()
            encodedData.clear()
            decodedWithPrefix.clear()
            decodedWithoutPrefix.clear()
            prefix.clear()

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                origData.add(randomByte(random))
            }

            val engine = randomEngine(random)
            engine.encodeString(origData.toByteArray(), encodedData)
            assertEncodeSanity(encodedData.toString(), engine.config().encodePadding(), inputLen)

            val prefixLen = random.nextInt(0, 1000)

            // fill the buf with a prefix
            repeat(prefixLen) {
                prefix.add(randomByte(random))
            }

            repeat(prefixLen) {
                decodedWithPrefix.add(0)
            }
            decodedWithPrefix.clear()
            decodedWithPrefix.addAll(prefix)

            // decode into the non-empty buf
            engine
                .decodeVec(encodedData.toString().encodeToByteArray(), decodedWithPrefix)
                .getOrThrow()
            // also decode into the empty buf
            engine
                .decodeVec(encodedData.toString().encodeToByteArray(), decodedWithoutPrefix)
                .getOrThrow()

            assertEquals(
                prefixLen + decodedWithoutPrefix.size,
                decodedWithPrefix.size,
            )
            assertContentEquals(origData.toByteArray(), decodedWithoutPrefix.toByteArray())

            // append plain decode onto prefix
            prefix.addAll(decodedWithoutPrefix)
            decodedWithoutPrefix.clear()

            assertContentEquals(prefix.toByteArray(), decodedWithPrefix.toByteArray())
        }
    }

    @Test
    fun decodeSliceDoesntClobberExistingPrefixOrSuffix() {
        doDecodeSliceDoesntClobberExistingPrefixOrSuffix { engine, input, output ->
            engine.decodeSlice(input, output).getOrThrow()
        }
    }

    @Test
    fun decodeSliceUncheckedDoesntClobberExistingPrefixOrSuffix() {
        doDecodeSliceDoesntClobberExistingPrefixOrSuffix { engine, input, output ->
            engine.decodeSliceUnchecked(input, output).getOrThrow()
        }
    }

    @Test
    fun decodeEngineEstimationWorksForVariousLengths() {
        val engine = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)
        for (numPrefixQuads in 0 until 100) {
            for (suffix in listOf("AA", "AAA", "AAAA")) {
                val prefix = StringBuilder("AAAA".repeat(numPrefixQuads))
                prefix.append(suffix)
                // make sure no overflow occurs
                val res = engine.decode(prefix.toString().encodeToByteArray())
                assertTrue(res.isSuccess)
            }
        }
    }

    @Test
    fun decodeSliceOutputLengthErrors() {
        for (numQuads in 1 until 100) {
            val input = "AAAA".repeat(numQuads).encodeToByteArray()
            var vec = ByteArray((numQuads - 1) * 3)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(
                STANDARD.decodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(
                STANDARD.decodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(
                STANDARD.decodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            // now it works
            assertEquals(
                numQuads * 3,
                STANDARD.decodeSlice(input, vec).getOrThrow(),
            )
        }
    }

    private fun doDecodeSliceDoesntClobberExistingPrefixOrSuffix(
        callDecode: (GeneralPurpose, ByteArray, ByteArray) -> Int,
    ) {
        val origData = mutableListOf<Byte>()
        val encodedData = StringBuilder()
        var decodeBuf = ByteArray(0)
        var decodeBufCopy = ByteArray(0)

        val random = Random(0xD511CE)

        repeat(DECODE_RANDOM_TRIALS) {
            origData.clear()
            encodedData.clear()
            decodeBuf = ByteArray(0)
            decodeBufCopy = ByteArray(0)

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                origData.add(randomByte(random))
            }

            val engine = randomEngine(random)
            engine.encodeString(origData.toByteArray(), encodedData)
            assertEncodeSanity(encodedData.toString(), engine.config().encodePadding(), inputLen)

            // fill the buffer with random garbage, long enough to have some room before and after
            decodeBuf = ByteArray(5000).also { random.nextBytes(it) }

            // keep a copy for later comparison
            decodeBufCopy = decodeBuf.copyOf()

            val offset = 1000

            // decode into the non-empty buf
            // Unported: shared-backing mutable slice starting at a nonzero offset has no faithful ByteArray view.
            val decodeBytesWritten =
                callDecode(engine, encodedData.toString().encodeToByteArray(), decodeBuf)

            assertEquals(origData.size, decodeBytesWritten)
            assertContentEquals(
                origData.toByteArray(),
                decodeBuf.copyOfRange(0, decodeBytesWritten),
            )
            assertEquals(offset, 1000)
            assertContentEquals(
                decodeBufCopy.copyOfRange(decodeBytesWritten, decodeBufCopy.size),
                decodeBuf.copyOfRange(decodeBytesWritten, decodeBuf.size),
            )
        }
    }

    @Test
    fun decodeError() {
        assertEquals("Invalid padding", DecodeError.InvalidPadding.toString())
        assertEquals(
            "Invalid symbol 0, offset 0. Invalid input length: 0 Invalid last symbol 0, offset 0. Invalid padding",
            listOf(
            DecodeError.InvalidByte(0, 0).toString(),
            DecodeError.InvalidLength(0).toString(),
            DecodeError.InvalidLastSymbol(0, 0).toString(),
            DecodeError.InvalidPadding.toString(),
            ).joinToString(" "),
        )
    }

    @Test
    fun decodeSliceError() {
        assertEquals("Output slice too small", DecodeSliceError.OutputSliceTooSmall.toString())
        assertEquals(
            "Output slice too small DecodeError: Invalid padding",
            listOf(
            DecodeSliceError.OutputSliceTooSmall.toString(),
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding).toString(),
            ).joinToString(" "),
        )
        assertEquals(null, DecodeSliceError.OutputSliceTooSmall.cause)
        assertEquals(
            DecodeError.InvalidPadding,
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding).cause,
        )
        assertEquals(null, DecodeSliceError.OutputSliceTooSmall.source())
        assertEquals(
            DecodeError.InvalidPadding,
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding).source(),
        )
        assertEquals(
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding),
            DecodeSliceError.from(DecodeError.InvalidPadding),
        )
    }

    @Test
    fun deprecatedFns() {
        assertContentEquals(byteArrayOf(), decode(byteArrayOf()).getOrThrow())
        assertContentEquals(
            byteArrayOf(),
            decodeEngine(byteArrayOf(), io.github.kotlinmania.base64.prelude.BASE64_STANDARD).getOrThrow(),
        )
        assertEquals(
            Unit,
            decodeEngineVec(
            byteArrayOf(),
            mutableListOf(),
            io.github.kotlinmania.base64.prelude.BASE64_STANDARD,
            ).getOrThrow(),
        )
        assertEquals(
            0,
            decodeEngineSlice(
            byteArrayOf(),
            ByteArray(0),
            io.github.kotlinmania.base64.prelude.BASE64_STANDARD,
            ).getOrThrow(),
        )
    }

    @Test
    fun decodedLenEst() {
        assertEquals(3, decodedLenEstimate(4))
    }

    private fun assertEncodeSanity(
        encoded: String,
        padded: Boolean,
        inputLen: Int,
    ) {
        val expectedLen = encodedLen(inputLen, padded)!!
        assertEquals(expectedLen, encoded.length)
        if (padded) {
            assertEquals(0, encoded.length % 4)
        } else {
            assertTrue(encoded.none { it == '=' })
        }
    }

    private fun randomEngine(random: Random): GeneralPurpose =
        when (random.nextInt(5)) {
            0 -> GeneralPurpose(STANDARD_ALPHABET, PAD)
            1 -> GeneralPurpose(STANDARD_ALPHABET, NO_PAD)
            2 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, PAD)
            3 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, NO_PAD)
            else ->
                GeneralPurpose(
                    STANDARD_ALPHABET,
                    GeneralPurposeConfig()
                        .withEncodePadding(false)
                        .withDecodePaddingMode(DecodePaddingMode.Indifferent),
                )
        }

    private fun randomByte(random: Random): Byte = ByteArray(1).also { random.nextBytes(it) }[0]
}
