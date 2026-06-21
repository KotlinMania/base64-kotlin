// port-lint: tests encode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.IMAP_MUTF7
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.PAD
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import io.github.kotlinmania.base64.engine.generalpurpose.URL_SAFE_NO_PAD
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncodeTest {
    @Test
    fun encodedSizeCorrectStandard() {
        assertEncodedLength(0, 0, STANDARD, true)

        assertEncodedLength(1, 4, STANDARD, true)
        assertEncodedLength(2, 4, STANDARD, true)
        assertEncodedLength(3, 4, STANDARD, true)

        assertEncodedLength(4, 8, STANDARD, true)
        assertEncodedLength(5, 8, STANDARD, true)
        assertEncodedLength(6, 8, STANDARD, true)

        assertEncodedLength(7, 12, STANDARD, true)
        assertEncodedLength(8, 12, STANDARD, true)
        assertEncodedLength(9, 12, STANDARD, true)

        assertEncodedLength(54, 72, STANDARD, true)

        assertEncodedLength(55, 76, STANDARD, true)
        assertEncodedLength(56, 76, STANDARD, true)
        assertEncodedLength(57, 76, STANDARD, true)

        assertEncodedLength(58, 80, STANDARD, true)
    }

    @Test
    fun encodedSizeCorrectNoPad() {
        assertEncodedLength(0, 0, URL_SAFE_NO_PAD, false)

        assertEncodedLength(1, 2, URL_SAFE_NO_PAD, false)
        assertEncodedLength(2, 3, URL_SAFE_NO_PAD, false)
        assertEncodedLength(3, 4, URL_SAFE_NO_PAD, false)

        assertEncodedLength(4, 6, URL_SAFE_NO_PAD, false)
        assertEncodedLength(5, 7, URL_SAFE_NO_PAD, false)
        assertEncodedLength(6, 8, URL_SAFE_NO_PAD, false)

        assertEncodedLength(7, 10, URL_SAFE_NO_PAD, false)
        assertEncodedLength(8, 11, URL_SAFE_NO_PAD, false)
        assertEncodedLength(9, 12, URL_SAFE_NO_PAD, false)

        assertEncodedLength(54, 72, URL_SAFE_NO_PAD, false)

        assertEncodedLength(55, 74, URL_SAFE_NO_PAD, false)
        assertEncodedLength(56, 75, URL_SAFE_NO_PAD, false)
        assertEncodedLength(57, 76, URL_SAFE_NO_PAD, false)

        assertEncodedLength(58, 78, URL_SAFE_NO_PAD, false)
    }

    @Test
    fun encodeEngineStringIntoNonemptyBufferDoesntClobberPrefix() {
        val random = Random(0xE11C0DE)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val prefix = "#".repeat(random.nextInt(0, 1000))
            val encodedDataNoPrefix = StringBuilder()
            val encodedDataWithPrefix = StringBuilder(prefix)
            val decoded = mutableListOf<Byte>()
            val engine = randomEngine(random)

            engine.encodeString(input, encodedDataNoPrefix)
            engine.encodeString(input, encodedDataWithPrefix)

            assertEquals(encodedDataNoPrefix.length + prefix.length, encodedDataWithPrefix.length)
            assertEncodeSanity(encodedDataNoPrefix.toString(), engine.config().encodePadding(), input.size)
            assertEncodeSanity(
                encodedDataWithPrefix.substring(prefix.length),
                engine.config().encodePadding(),
                input.size,
            )

            assertEquals(prefix + encodedDataNoPrefix.toString(), encodedDataWithPrefix.toString())

            engine.decodeVec(encodedDataNoPrefix.toString().encodeToByteArray(), decoded).getOrThrow()
            assertContentEquals(input, decoded.toByteArray())
        }
    }

    @Test
    fun encodeEngineSliceIntoNonemptyBufferDoesntClobberSuffix() {
        val random = Random(0x51CE)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val encodedData = randomBytes(random, 10 * input.size + 16)
            val encodedDataOriginalState = encodedData.copyOf()
            val decoded = mutableListOf<Byte>()
            val engine = randomEngine(random)
            val encodedSize = encodedLen(input.size, engine.config().encodePadding())!!

            assertEquals(encodedSize, engine.encodeSlice(input, encodedData).getOrThrow())
            assertEncodeSanity(
                encodedData.copyOfRange(0, encodedSize).decodeToString(),
                engine.config().encodePadding(),
                input.size,
            )
            assertContentEquals(
                encodedDataOriginalState.copyOfRange(encodedSize, encodedDataOriginalState.size),
                encodedData.copyOfRange(encodedSize, encodedData.size),
            )

            engine.decodeVec(encodedData.copyOfRange(0, encodedSize), decoded).getOrThrow()
            assertContentEquals(input, decoded.toByteArray())
        }
    }

    @Test
    fun encodeToSliceRandomValidUtf8() {
        val random = Random(0x70511CE)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val engine = randomEngine(random)
            val encodedSize = encodedLen(input.size, engine.config().encodePadding())!!
            val output = randomBytes(random, encodedSize)
            val originalOutput = output.copyOf()

            val bytesWritten = engine.internalEncode(input, output)

            assertContentEquals(
                originalOutput.copyOfRange(bytesWritten, originalOutput.size),
                output.copyOfRange(bytesWritten, output.size),
            )
            output.copyOfRange(0, bytesWritten).decodeToString()
        }
    }

    @Test
    fun encodeWithPaddingRandomValidUtf8() {
        val random = Random(0x0DA7A)

        repeat(512) {
            val input = randomBytes(random, random.nextInt(0, 1000))
            val engine = randomEngine(random)
            val encodedSize = encodedLen(input.size, engine.config().encodePadding())!!
            val output = randomBytes(random, encodedSize + 1000)
            val originalOutput = output.copyOf()

            encodeWithPadding(input, output, engine, encodedSize)

            assertContentEquals(
                originalOutput.copyOfRange(encodedSize, originalOutput.size),
                output.copyOfRange(encodedSize, output.size),
            )
            output.copyOfRange(0, encodedSize).decodeToString()
        }
    }

    @Test
    fun addPaddingRandomValidUtf8() {
        val random = Random(0x0AD)

        for (unpaddedOutputLen in 0 until 20) {
            val output = randomBytes(random, 100)
            val originalOutput = output.copyOf()

            val bytesWritten = addPadding(unpaddedOutputLen, output)

            assertContentEquals(
                originalOutput.copyOfRange(bytesWritten, originalOutput.size),
                output.copyOfRange(bytesWritten, output.size),
            )
            output.copyOfRange(0, bytesWritten).decodeToString()
        }
    }

    @Test
    fun encodeImap() {
        assertEquals(
            GeneralPurpose(IMAP_MUTF7, NO_PAD).encode(byteArrayOf(0xFB.toByte(), 0xFF.toByte())),
            GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, NO_PAD)
                .encode(byteArrayOf(0xFB.toByte(), 0xFF.toByte()))
                .map { if (it == '/') ',' else it }
                .joinToString(""),
        )
    }

    @Test
    fun deprecatedFns() {
        assertEquals("Zm9v", encode("foo".encodeToByteArray()))
        assertEquals("Zm9v", encodeEngine("foo".encodeToByteArray(), STANDARD))
        val output = StringBuilder()
        encodeEngineString("foo".encodeToByteArray(), output, STANDARD)
        assertEquals("Zm9v", output.toString())

        val outputSlice = ByteArray(4)
        assertEquals(4, encodeEngineSlice("foo".encodeToByteArray(), outputSlice, STANDARD).getOrThrow())
        assertEquals("Zm9v", outputSlice.decodeToString())
    }

    private fun assertEncodedLength(
        inputLen: Int,
        encLen: Int,
        engine: Engine<*, *>,
        padded: Boolean,
    ) {
        assertEquals(encLen, encodedLen(inputLen, padded))

        val bytes = randomBytes(Random(inputLen), inputLen)
        val encoded = engine.encode(bytes)
        assertEncodeSanity(encoded, padded, inputLen)

        assertEquals(encLen, encoded.length)
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
        when (random.nextInt(4)) {
            0 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, PAD)
            1 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, NO_PAD)
            2 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, PAD)
            else -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, NO_PAD)
        }

    private fun randomBytes(
        random: Random,
        size: Int,
    ): ByteArray = ByteArray(size).also { random.nextBytes(it) }
}
