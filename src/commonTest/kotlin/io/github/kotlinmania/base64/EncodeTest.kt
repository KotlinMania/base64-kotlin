// port-lint: tests encode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.IMAP_MUTF7
import io.github.kotlinmania.base64.engine.Config
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

private const val RANDOM_TRIALS: Int = 10_000

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
    fun encodedSizeOverflow() {
        assertEquals(null, encodedLen(Int.MAX_VALUE, true))
    }

    @Test
    fun encodeEngineStringIntoNonemptyBufferDoesntClobberPrefix() {
        val origData = mutableListOf<Byte>()
        val prefix = StringBuilder()
        val encodedDataNoPrefix = StringBuilder()
        val encodedDataWithPrefix = StringBuilder()
        val decoded = mutableListOf<Byte>()

        val random = Random(0xE11C0DE)

        repeat(RANDOM_TRIALS) {
            origData.clear()
            prefix.clear()
            encodedDataNoPrefix.clear()
            encodedDataWithPrefix.clear()
            decoded.clear()

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                origData.add(randomByte(random))
            }

            val prefixLen = random.nextInt(0, 1000)
            repeat(prefixLen) {
                // getting convenient random single-byte printable chars that aren't base64 is
                // annoying
                prefix.append('#')
            }
            encodedDataWithPrefix.append(prefix)

            val engine = randomEngine(random)
            engine.encodeString(origData.toByteArray(), encodedDataNoPrefix)
            engine.encodeString(origData.toByteArray(), encodedDataWithPrefix)

            assertEquals(
                encodedDataNoPrefix.length + prefixLen,
                encodedDataWithPrefix.length,
            )
            assertEncodeSanity(
                encodedDataNoPrefix.toString(),
                engine.config().encodePadding(),
                inputLen,
            )
            assertEncodeSanity(
                encodedDataWithPrefix.substring(prefixLen),
                engine.config().encodePadding(),
                inputLen,
            )

            // append plain encode onto prefix
            prefix.append(encodedDataNoPrefix)

            assertEquals(prefix.toString(), encodedDataWithPrefix.toString())

            engine
                .decodeVec(encodedDataNoPrefix.toString().encodeToByteArray(), decoded)
                .getOrThrow()
            assertContentEquals(origData.toByteArray(), decoded.toByteArray())
        }
    }

    @Test
    fun encodeEngineSliceIntoNonemptyBufferDoesntClobberSuffix() {
        val origData = mutableListOf<Byte>()
        var encodedData = ByteArray(0)
        var encodedDataOriginalState = ByteArray(0)
        val decoded = mutableListOf<Byte>()

        val random = Random(0x51CE)

        repeat(RANDOM_TRIALS) {
            origData.clear()
            encodedData = ByteArray(0)
            encodedDataOriginalState = ByteArray(0)
            decoded.clear()

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                origData.add(randomByte(random))
            }

            // plenty of existing garbage in the encoded buffer
            encodedData = ByteArray(10 * inputLen).also { random.nextBytes(it) }

            encodedDataOriginalState = encodedData.copyOf()

            val engine = randomEngine(random)

            val encodedSize = encodedLen(inputLen, engine.config().encodePadding())!!

            assertEquals(
                encodedSize,
                engine.encodeSlice(origData.toByteArray(), encodedData).getOrThrow(),
            )

            assertEncodeSanity(
                encodedData.copyOfRange(0, encodedSize).decodeToString(),
                engine.config().encodePadding(),
                inputLen,
            )

            assertContentEquals(
                encodedDataOriginalState.copyOfRange(encodedSize, encodedDataOriginalState.size),
                encodedData.copyOfRange(encodedSize, encodedData.size),
            )

            engine
                .decodeVec(encodedData.copyOfRange(0, encodedSize), decoded)
                .getOrThrow()
            assertContentEquals(origData.toByteArray(), decoded.toByteArray())
        }
    }

    @Test
    fun encodeToSliceRandomValidUtf8() {
        val input = mutableListOf<Byte>()
        var output = ByteArray(0)

        val random = Random(0x70511CE)

        repeat(RANDOM_TRIALS) {
            input.clear()
            output = ByteArray(0)

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                input.add(randomByte(random))
            }

            val config = randomConfig(random)
            val engine = randomEngine(random)

            // fill up the output buffer with garbage
            val encodedSize = encodedLen(inputLen, config.encodePadding())!!
            output = ByteArray(encodedSize).also { random.nextBytes(it) }

            val origOutputBuf = output.copyOf()

            val bytesWritten = engine.internalEncode(input.toByteArray(), output)

            // make sure the part beyond bytesWritten is the same garbage it was before
            assertContentEquals(
                origOutputBuf.copyOfRange(bytesWritten, origOutputBuf.size),
                output.copyOfRange(bytesWritten, output.size),
            )

            // make sure the encoded bytes are UTF-8
            output.copyOfRange(0, bytesWritten).decodeToString()
        }
    }

    @Test
    fun encodeWithPaddingRandomValidUtf8() {
        val input = mutableListOf<Byte>()
        var output = ByteArray(0)

        val random = Random(0x0DA7A)

        repeat(RANDOM_TRIALS) {
            input.clear()
            output = ByteArray(0)

            val inputLen = random.nextInt(0, 1000)

            repeat(inputLen) {
                input.add(randomByte(random))
            }

            val engine = randomEngine(random)

            // fill up the output buffer with garbage
            val encodedSize = encodedLen(inputLen, engine.config().encodePadding())!!
            output = ByteArray(encodedSize + 1000).also { random.nextBytes(it) }

            val origOutputBuf = output.copyOf()

            encodeWithPadding(input.toByteArray(), output, engine, encodedSize)

            // make sure the part beyond b64 is the same garbage it was before
            assertContentEquals(
                origOutputBuf.copyOfRange(encodedSize, origOutputBuf.size),
                output.copyOfRange(encodedSize, output.size),
            )

            // make sure the encoded bytes are UTF-8
            output.copyOfRange(0, encodedSize).decodeToString()
        }
    }

    @Test
    fun addPaddingRandomValidUtf8() {
        var output = ByteArray(0)

        val random = Random(0x0AD)

        // cover our bases for length % 4
        for (unpaddedOutputLen in 0 until 20) {
            output = ByteArray(0)

            // fill output with random
            output = ByteArray(100).also { random.nextBytes(it) }

            val origOutputBuf = output.copyOf()

            val bytesWritten = addPadding(unpaddedOutputLen, output)

            // make sure the part beyond bytesWritten is the same garbage it was before
            assertContentEquals(
                origOutputBuf.copyOfRange(bytesWritten, origOutputBuf.size),
                output.copyOfRange(bytesWritten, output.size),
            )

            // make sure the encoded bytes are UTF-8
            output.copyOfRange(0, bytesWritten).decodeToString()
        }
    }

    private fun assertEncodedLength(
        inputLen: Int,
        encLen: Int,
        engine: Engine<*, *>,
        padded: Boolean,
    ) {
        assertEquals(encLen, encodedLen(inputLen, padded))

        val bytes = mutableListOf<Byte>()
        val random = Random(inputLen)

        repeat(inputLen) {
            bytes.add(randomByte(random))
        }

        val encoded = engine.encode(bytes.toByteArray())
        assertEncodeSanity(encoded, padded, inputLen)

        assertEquals(encLen, encoded.length)
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

    private fun randomConfig(random: Random): Config =
        when (random.nextInt(2)) {
            0 -> PAD
            else -> NO_PAD
        }

    private fun randomEngine(random: Random): GeneralPurpose =
        when (random.nextInt(4)) {
            0 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, PAD)
            1 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, NO_PAD)
            2 -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, PAD)
            else -> GeneralPurpose(io.github.kotlinmania.base64.alphabet.URL_SAFE, NO_PAD)
        }

    private fun randomByte(random: Random): Byte = ByteArray(1).also { random.nextBytes(it) }[0]
}
