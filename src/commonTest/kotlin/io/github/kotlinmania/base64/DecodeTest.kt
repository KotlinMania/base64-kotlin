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

class DecodeTest {
    @Test
    fun decodeIntoNonemptyVecDoesntClobberExistingPrefix() {
        val random = Random(0xDEC0DE)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val engine = randomEngine(random)
            val encoded = engine.encode(input)
            val prefix = randomBytes(random, random.nextInt(0, 1000)).toMutableList()
            val decodedWithPrefix = prefix.toMutableList()
            val decodedWithoutPrefix = mutableListOf<Byte>()

            engine.decodeVec(encoded.encodeToByteArray(), decodedWithPrefix).getOrThrow()
            engine.decodeVec(encoded.encodeToByteArray(), decodedWithoutPrefix).getOrThrow()

            assertEquals(prefix.size + decodedWithoutPrefix.size, decodedWithPrefix.size)
            assertContentEquals(input, decodedWithoutPrefix.toByteArray())

            val expected = prefix.toMutableList()
            expected.addAll(decodedWithoutPrefix)
            assertContentEquals(expected.toByteArray(), decodedWithPrefix.toByteArray())
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
                val input = "AAAA".repeat(numPrefixQuads) + suffix
                assertTrue(engine.decode(input.encodeToByteArray()).isSuccess)
            }
        }
    }

    @Test
    fun decodeSliceOutputLengthErrors() {
        for (numQuads in 1 until 100) {
            val input = "AAAA".repeat(numQuads).encodeToByteArray()
            var output = ByteArray((numQuads - 1) * 3)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(STANDARD.decodeSlice(input, output).exceptionOrNull())

            output = output.copyOf(output.size + 1)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(STANDARD.decodeSlice(input, output).exceptionOrNull())

            output = output.copyOf(output.size + 1)
            assertIs<DecodeSliceError.OutputSliceTooSmall>(STANDARD.decodeSlice(input, output).exceptionOrNull())

            output = output.copyOf(output.size + 1)
            assertEquals(numQuads * 3, STANDARD.decodeSlice(input, output).getOrThrow())
        }
    }

    @Test
    fun decodeError() {
        assertEquals("Invalid symbol 0, offset 0.", DecodeError.InvalidByte(0, 0).toString())
        assertEquals("Invalid input length: 0", DecodeError.InvalidLength(0).toString())
        assertEquals("Invalid last symbol 0, offset 0.", DecodeError.InvalidLastSymbol(0, 0).toString())
        assertEquals("Invalid padding", DecodeError.InvalidPadding.toString())
    }

    @Test
    fun decodeSliceError() {
        assertEquals("Output slice too small", DecodeSliceError.OutputSliceTooSmall.toString())
        assertEquals(
            "DecodeError: Invalid padding",
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding).toString(),
        )
        assertEquals(
            DecodeError.InvalidPadding,
            DecodeSliceError.DecodeErrorVariant(DecodeError.InvalidPadding).cause,
        )
    }

    @Test
    fun deprecatedFns() {
        assertContentEquals(byteArrayOf(), decode(byteArrayOf()).getOrThrow())
        assertContentEquals(byteArrayOf(), decodeEngine(byteArrayOf(), STANDARD).getOrThrow())

        val vec = mutableListOf<Byte>()
        decodeEngineVec(byteArrayOf(), vec, STANDARD).getOrThrow()
        assertEquals(emptyList(), vec)

        assertEquals(0, decodeEngineSlice(byteArrayOf(), ByteArray(0), STANDARD).getOrThrow())
    }

    @Test
    fun decodedLenEst() {
        assertEquals(3, decodedLenEstimate(4))
    }

    private fun doDecodeSliceDoesntClobberExistingPrefixOrSuffix(
        callDecode: (GeneralPurpose, ByteArray, ByteArray) -> Int,
    ) {
        val random = Random(0xD511CE)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val engine = randomEngine(random)
            val encoded = engine.encode(input).encodeToByteArray()
            val decodeBuffer = randomBytes(random, 5000)
            val originalDecodeBuffer = decodeBuffer.copyOf()

            // Unported: shared-backing mutable subslice prefix preservation has no faithful ByteArray view.
            val bytesWritten = callDecode(engine, encoded, decodeBuffer)

            assertEquals(input.size, bytesWritten)
            assertContentEquals(input, decodeBuffer.copyOfRange(0, bytesWritten))
            assertContentEquals(
                originalDecodeBuffer.copyOfRange(bytesWritten, originalDecodeBuffer.size),
                decodeBuffer.copyOfRange(bytesWritten, decodeBuffer.size),
            )
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

    private fun randomBytes(
        random: Random,
        size: Int,
    ): ByteArray = ByteArray(size).also { random.nextBytes(it) }
}
