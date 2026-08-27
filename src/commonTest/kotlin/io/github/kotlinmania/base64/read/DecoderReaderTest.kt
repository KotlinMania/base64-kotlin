// port-lint: tests read/decoder_tests.rs
package io.github.kotlinmania.base64.read

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.PAD_BYTE
import io.github.kotlinmania.base64.decodeErrorOrNull
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import io.github.kotlinmania.base64.io.ByteArrayReader
import io.github.kotlinmania.base64.io.ByteReader
import io.github.kotlinmania.base64.randomAlphabet
import io.github.kotlinmania.base64.randomConfig
import io.github.kotlinmania.base64.randomEngine
import io.github.kotlinmania.base64.readToEnd
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecoderReaderTest {
    @Test
    fun simple() {
        val tests =
            listOf(
                "0".encodeToByteArray() to "MA==".encodeToByteArray(),
                "01".encodeToByteArray() to "MDE=".encodeToByteArray(),
                "012".encodeToByteArray() to "MDEy".encodeToByteArray(),
                "0123".encodeToByteArray() to "MDEyMw==".encodeToByteArray(),
                "01234".encodeToByteArray() to "MDEyMzQ=".encodeToByteArray(),
                "012345".encodeToByteArray() to "MDEyMzQ1".encodeToByteArray(),
                "0123456".encodeToByteArray() to "MDEyMzQ1Ng==".encodeToByteArray(),
                "01234567".encodeToByteArray() to "MDEyMzQ1Njc=".encodeToByteArray(),
                "012345678".encodeToByteArray() to "MDEyMzQ1Njc4".encodeToByteArray(),
                "0123456789".encodeToByteArray() to "MDEyMzQ1Njc4OQ==".encodeToByteArray(),
            )

        for ((expected, base64Data) in tests) {
            for (n in 1..base64Data.size) {
                val decoder = DecoderReader(ByteArrayReader(base64Data), STANDARD)
                val got = mutableListOf<Byte>()
                val buffer = ByteArray(n)
                while (true) {
                    val read = decoder.read(buffer).getOrThrow()
                    if (read == 0) break
                    for (index in 0 until read) got.add(buffer[index])
                }
                assertContentEquals(expected, got.toByteArray())
            }
        }
    }

    @Test
    fun trailingJunk() {
        val tests =
            listOf(
                "MDEyMzQ1Njc4*!@#$%^&".encodeToByteArray(),
                "MDEyMzQ1Njc4OQ== ".encodeToByteArray(),
            )

        for (base64Data in tests) {
            for (n in 1..base64Data.size) {
                val decoder = DecoderReader(ByteArrayReader(base64Data), STANDARD)
                val buffer = ByteArray(n)
                var sawError = false
                while (true) {
                    val result = decoder.read(buffer)
                    if (result.isFailure) {
                        sawError = true
                        break
                    }
                    if (result.getOrThrow() == 0) break
                }
                assertTrue(sawError)
            }
        }
    }

    @Test
    fun handlesShortReadFromDelegate() {
        val rng = Random(0xD001)
        val bytes = mutableListOf<Byte>()
        val decoded = mutableListOf<Byte>()

        repeat(250) {
            bytes.clear()
            decoded.clear()
            val size = rng.nextInt(0, 10 * DECODER_BUF_SIZE)
            val input = ByteArray(size)
            rng.nextBytes(input)
            bytes.addAll(input.asList())

            val engine = randomEngine(rng)
            val b64 = engine.encode(input)
            val shortReader = RandomShortRead(ByteArrayReader(b64.encodeToByteArray()), rng)
            val decoder = DecoderReader(shortReader, engine)

            val decodedLen = decoder.readToEnd(decoded).getOrThrow()
            assertEquals(size, decodedLen)
            assertContentEquals(input, decoded.toByteArray())
        }
    }

    @Test
    fun readInShortIncrements() {
        val rng = Random(0xD002)
        val decoded = ByteArray(10 * DECODER_BUF_SIZE * 3)

        repeat(250) {
            val size = rng.nextInt(0, 10 * DECODER_BUF_SIZE)
            val bytes = ByteArray(size)
            rng.nextBytes(bytes)
            decoded.fill(0)

            val engine = randomEngine(rng)
            val decoder = DecoderReader(ByteArrayReader(engine.encode(bytes).encodeToByteArray()), engine)

            consumeWithShortReadsAndValidate(rng, bytes, decoded, decoder)
        }
    }

    @Test
    fun readInShortIncrementsWithShortDelegateReads() {
        val rng = Random(0xD003)
        val decoded = ByteArray(10 * DECODER_BUF_SIZE * 3)

        repeat(250) {
            val size = rng.nextInt(0, 10 * DECODER_BUF_SIZE)
            val bytes = ByteArray(size)
            rng.nextBytes(bytes)
            decoded.fill(0)

            val engine = randomEngine(rng)
            val decoder = DecoderReader(ByteArrayReader(engine.encode(bytes).encodeToByteArray()), engine)
            val shortReader = RandomShortRead(decoder, Random(0xE000 + it))

            consumeWithShortReadsAndValidate(rng, bytes, decoded, shortReader)
        }
    }

    @Test
    fun reportsInvalidLastSymbolCorrectly() {
        val rng = Random(0xD004)

        repeat(25) {
            val size = rng.nextInt(1, 10 * DECODER_BUF_SIZE)
            val bytes = ByteArray(size)
            rng.nextBytes(bytes)

            val config = randomConfig(rng)
            val alphabet = randomAlphabet(rng)
            val engine = GeneralPurpose(alphabet, config.withEncodePadding(false))
            val b64Bytes = engine.encode(bytes).encodeToByteArray()

            for (symbol in alphabet.symbols) {
                val streamDecoded = mutableListOf<Byte>()
                val bulkDecoded = mutableListOf<Byte>()
                b64Bytes[b64Bytes.lastIndex] = symbol
                val bulkError =
                    engine.decodeVec(b64Bytes, bulkDecoded).exceptionOrNull() as? DecodeError
                val streamError =
                    DecoderReader(ByteArrayReader(b64Bytes), engine)
                        .readToEnd(streamDecoded)
                        .exceptionOrNull()
                        ?.decodeErrorOrNull()

                assertEquals(bulkError, streamError)
            }
        }
    }

    @Test
    fun reportsInvalidByteCorrectly() {
        val rng = Random(0xD005)

        repeat(250) {
            val size = rng.nextInt(1, 10 * DECODER_BUF_SIZE)
            val bytes = ByteArray(size)
            rng.nextBytes(bytes)

            val engine = GeneralPurpose(io.github.kotlinmania.base64.alphabet.STANDARD, randomConfig(rng))
            val b64Bytes = engine.encode(bytes).encodeToByteArray()
            b64Bytes[rng.nextInt(b64Bytes.size)] = '*'.code.toByte()

            val streamDecoded = mutableListOf<Byte>()
            val bulkDecoded = mutableListOf<Byte>()

            val readDecodeErr =
                DecoderReader(ByteArrayReader(b64Bytes), engine)
                    .readToEnd(streamDecoded)
                    .exceptionOrNull()
                    ?.decodeErrorOrNull()
            val bulkDecodeErr = engine.decodeVec(b64Bytes, bulkDecoded).exceptionOrNull()

            assertEquals(bulkDecodeErr, readDecodeErr)
        }
    }

    @Test
    fun internalPaddingErrorWithShortReadConcatenatedTextsInvalidByteError() {
        val rng = Random(0xD006)
        val engine = STANDARD

        repeat(250) {
            val size = rng.nextInt(2, 10 * DECODER_BUF_SIZE)
            val bytes = ByteArray(size)
            rng.nextBytes(bytes)

            var split: Int
            while (true) {
                split = rng.nextInt(1, size)
                if (split % 3 != 0) break
            }

            val first = engine.encode(bytes.copyOfRange(0, split))
            val badBytePos = first.indexOf('=')
            val b64 = first + engine.encode(bytes.copyOfRange(split, bytes.size))
            val b64Bytes = b64.encodeToByteArray()
            val readLen = rng.nextInt(1, 10)
            val wrappedReader = ShortRead(ByteArrayReader(b64Bytes), readLen)
            val readerDecoded = mutableListOf<Byte>()
            val bulkDecoded = mutableListOf<Byte>()

            val readDecodeErr =
                DecoderReader(wrappedReader, engine)
                    .readToEnd(readerDecoded)
                    .exceptionOrNull()
                    ?.decodeErrorOrNull()
            val bulkDecodeErr = engine.decodeVec(b64Bytes, bulkDecoded).exceptionOrNull()

            assertEquals(bulkDecodeErr, readDecodeErr, "read len: $readLen, bad byte pos: $badBytePos")
            assertEquals(
                DecodeError.InvalidByte(
                    split / 3 * 4 +
                        when (split % 3) {
                            1 -> 2
                            2 -> 3
                            else -> error("unreachable")
                        },
                    PAD_BYTE,
                ),
                readDecodeErr,
            )
        }
    }

    @Test
    fun internalPaddingAnywhereError() {
        val rng = Random(0xD007)
        val engine = STANDARD

        repeat(250) {
            val bytes = ByteArray(10 * DECODER_BUF_SIZE)
            rng.nextBytes(bytes)
            val b64Bytes = engine.encode(bytes).encodeToByteArray()
            b64Bytes[rng.nextInt(0, bytes.size - 4)] = PAD_BYTE

            val readLen = rng.nextInt(1, 10)
            val wrappedReader = ShortRead(ByteArrayReader(b64Bytes), readLen)
            val readerDecoded = mutableListOf<Byte>()

            assertTrue(
                DecoderReader(wrappedReader, engine)
                    .readToEnd(readerDecoded)
                    .isFailure,
            )
        }
    }

    private fun consumeWithShortReadsAndValidate(
        rng: Random,
        expectedBytes: ByteArray,
        decoded: ByteArray,
        shortReader: ByteReader,
    ) {
        var totalRead = 0
        while (true) {
            assertTrue(totalRead <= expectedBytes.size)
            if (totalRead == expectedBytes.size) {
                assertContentEquals(expectedBytes, decoded.copyOfRange(0, totalRead))
                assertEquals(0, shortReader.read(decoded).getOrThrow())
                assertContentEquals(expectedBytes, decoded.copyOfRange(0, totalRead))
                break
            }
            val decodeLen = rng.nextInt(1, maxOf(2, expectedBytes.size * 2))
            val read = shortReader.read(decoded, totalRead, decodeLen).getOrThrow()
            totalRead += read
        }
    }

    private class RandomShortRead(
        private val delegate: ByteReader,
        private val rng: Random,
    ) : ByteReader {
        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Result<Int> {
            val effectiveLen = minOf(rng.nextInt(1, 20), length)
            return delegate.read(buffer, offset, effectiveLen)
        }
    }

    private class ShortRead(
        private val delegate: ByteReader,
        private val maxReadLen: Int,
    ) : ByteReader {
        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Result<Int> = delegate.read(buffer, offset, minOf(maxReadLen, length))
    }
}
