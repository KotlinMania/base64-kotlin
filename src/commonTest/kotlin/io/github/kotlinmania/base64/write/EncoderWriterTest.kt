// port-lint: tests write/encoder_tests.rs
package io.github.kotlinmania.base64.write

import io.github.kotlinmania.base64.alphabet.STANDARD as STANDARD_ALPHABET
import io.github.kotlinmania.base64.alphabet.URL_SAFE
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.PAD
import io.github.kotlinmania.base64.interrupted
import io.github.kotlinmania.base64.io.ByteWriter
import io.github.kotlinmania.base64.io.MutableByteArrayWriter
import io.github.kotlinmania.base64.io.StreamError
import io.github.kotlinmania.base64.io.StreamErrorKind
import io.github.kotlinmania.base64.randomEngine
import io.github.kotlinmania.base64.writeAll
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class EncoderWriterTest {
    @Test
    fun encodeThreeBytes() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(3, enc.write("abc".encodeToByteArray()).getOrThrow())
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encodeNineBytesTwoWrites() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(6, enc.write("abcdef".encodeToByteArray()).getOrThrow())
        assertEquals(3, enc.write("ghi".encodeToByteArray()).getOrThrow())
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcdefghi".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encodeOneThenTwoBytes() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(2, enc.write("bc".encodeToByteArray()).getOrThrow())
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encodeOneThenFiveBytes() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(5, enc.write("bcdef".encodeToByteArray()).getOrThrow())
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcdef".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encode123Bytes() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(2, enc.write("bc".encodeToByteArray()).getOrThrow())
        assertEquals(3, enc.write("def".encodeToByteArray()).getOrThrow())
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcdef".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encodeWithPadding() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        enc.writeAll("abcd".encodeToByteArray()).getOrThrow()
        enc.flush().getOrThrow()
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcd".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun encodeWithPaddingMultipleWrites() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(2, enc.write("bc".encodeToByteArray()).getOrThrow())
        assertEquals(3, enc.write("def".encodeToByteArray()).getOrThrow())
        assertEquals(1, enc.write("g".encodeToByteArray()).getOrThrow())
        enc.flush().getOrThrow()
        enc.close().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcdefg".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun finishWritesExtraByte() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, URL_SAFE_ENGINE)
        assertEquals(6, enc.write("abcdef".encodeToByteArray()).getOrThrow())
        assertEquals(1, enc.write("g".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(URL_SAFE_ENGINE.encode("abcdefg".encodeToByteArray()), writer.toByteArray().decodeToString())
    }

    @Test
    fun writePartialChunkEncodesPartialChunk() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(2, enc.write("ab".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("ab".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(3, writer.toByteArray().size)
    }

    @Test
    fun write1ChunkEncodesCompleteChunk() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(3, enc.write("abc".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(4, writer.toByteArray().size)
    }

    @Test
    fun write1ChunkAndPartialEncodesOnlyCompleteChunk() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(3, enc.write("abcd".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(4, writer.toByteArray().size)
    }

    @Test
    fun write2PartialsToExactlyCompleteChunkEncodesCompleteChunk() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(2, enc.write("bc".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(4, writer.toByteArray().size)
    }

    @Test
    fun writePartialThenEnoughToCompleteChunkButNotCompleteAnotherChunkEncodesCompleteChunkWithoutConsumingRemaining() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(2, enc.write("bcd".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abc".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(4, writer.toByteArray().size)
    }

    @Test
    fun writePartialThenEnoughToCompleteChunkAndAnotherChunkEncodesCompleteChunks() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(5, enc.write("bcdef".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abcdef".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(8, writer.toByteArray().size)
    }

    @Test
    fun writePartialThenEnoughToCompleteChunkAndAnotherChunkAndAnotherPartialChunkEncodesOnlyCompleteChunks() {
        val writer = MutableByteArrayWriter()
        val enc = EncoderWriter(writer, NO_PAD_ENGINE)
        assertEquals(1, enc.write("a".encodeToByteArray()).getOrThrow())
        assertEquals(5, enc.write("bcdefe".encodeToByteArray()).getOrThrow())
        enc.finish().getOrThrow()
        assertEquals(NO_PAD_ENGINE.encode("abcdef".encodeToByteArray()), writer.toByteArray().decodeToString())
        assertEquals(8, writer.toByteArray().size)
    }

    // Rust drop finalization has no faithful Kotlin common equivalent because object finalization is nondeterministic.

    @Test
    fun everyPossibleSplitOfInput() {
        val rng = Random(0xE001)
        val size = 500

        for (i in 0 until size) {
            val origData = ByteArray(size)
            rng.nextBytes(origData)
            val streamEncoded = MutableByteArrayWriter()
            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)

            val streamEncoder = EncoderWriter(streamEncoded, engine)
            streamEncoder.writeAll(origData.copyOfRange(0, i)).getOrThrow()
            streamEncoder.writeAll(origData.copyOfRange(i, origData.size)).getOrThrow()
            streamEncoder.close().getOrThrow()

            assertEquals(normalEncoded, streamEncoded.toByteArray().decodeToString())
        }
    }

    @Test
    fun encodeRandomConfigMatchesNormalEncodeReasonableInputLen() {
        doEncodeRandomConfigMatchesNormalEncode(ENCODER_BUF_SIZE * 2)
    }

    @Test
    fun encodeRandomConfigMatchesNormalEncodeTinyInputLen() {
        doEncodeRandomConfigMatchesNormalEncode(10)
    }

    @Test
    fun retryingWritesThatErrorWithInterruptedWorks() {
        val rng = Random(0xE002)

        repeat(20) {
            val origLen = rng.nextInt(100, 1_000)
            val origData = ByteArray(origLen)
            rng.nextBytes(origData)

            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)
            val streamEncoded = MutableByteArrayWriter()
            val interruptingWriter = InterruptingWriter(streamEncoded, Random(0xEE00 + it), 0.8)
            val streamEncoder = EncoderWriter(interruptingWriter, engine)

            var bytesConsumed = 0
            while (bytesConsumed < origLen) {
                val inputLen = minOf(rng.nextInt(0, 10), origLen - bytesConsumed)
                retryInterruptedWriteAll(
                    streamEncoder,
                    origData.copyOfRange(bytesConsumed, bytesConsumed + inputLen),
                ).getOrThrow()
                bytesConsumed += inputLen
            }

            while (true) {
                val result = streamEncoder.finish()
                if (result.isSuccess) break
                val error = result.exceptionOrNull()
                if (error !is StreamError || error.kind != StreamErrorKind.Interrupted) {
                    throw error ?: AssertionError("unknown failure")
                }
            }

            assertEquals(origLen, bytesConsumed)
            assertEquals(normalEncoded, streamEncoded.toByteArray().decodeToString())
        }
    }

    @Test
    fun writesThatOnlyWritePartOfInputAndSometimesInterruptProduceCorrectEncodedData() {
        val rng = Random(0xE003)

        repeat(20) {
            val origLen = rng.nextInt(100, 1_000)
            val origData = ByteArray(origLen)
            rng.nextBytes(origData)

            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)
            val streamEncoded = MutableByteArrayWriter()
            val partialWriter =
                PartialInterruptingWriter(streamEncoded, Random(0xEF00 + it), 0.1, 0.1)
            val streamEncoder = EncoderWriter(partialWriter, engine)

            var bytesConsumed = 0
            while (bytesConsumed < origLen) {
                val inputLen = minOf(rng.nextInt(0, 100), origLen - bytesConsumed)
                val result = streamEncoder.write(origData, bytesConsumed, inputLen)
                if (result.isSuccess) {
                    bytesConsumed += result.getOrThrow()
                } else {
                    val error = result.exceptionOrNull()
                    if (error !is StreamError || error.kind != StreamErrorKind.Interrupted) {
                        throw error ?: AssertionError("unknown failure")
                    }
                }
            }

            streamEncoder.finish().getOrThrow()
            assertEquals(origLen, bytesConsumed)
            assertEquals(normalEncoded, streamEncoded.toByteArray().decodeToString())
        }
    }

    private fun retryInterruptedWriteAll(
        writer: ByteWriter,
        buffer: ByteArray,
    ): Result<Unit> {
        var bytesConsumed = 0
        while (bytesConsumed < buffer.size) {
            val result = writer.write(buffer, bytesConsumed, buffer.size - bytesConsumed)
            if (result.isSuccess) {
                bytesConsumed += result.getOrThrow()
            } else {
                val error = result.exceptionOrNull()
                if (error is StreamError && error.kind == StreamErrorKind.Interrupted) {
                    continue
                }
                return Result.failure(error ?: IllegalStateException("unknown failure"))
            }
        }
        return Result.success(Unit)
    }

    private fun doEncodeRandomConfigMatchesNormalEncode(maxInputLen: Int) {
        val rng = Random(0xE004 + maxInputLen)

        repeat(100) {
            val origLen = rng.nextInt(100, 5_000)
            val origData = ByteArray(origLen)
            rng.nextBytes(origData)

            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)
            val streamEncoded = MutableByteArrayWriter()
            val streamEncoder = EncoderWriter(streamEncoded, engine)

            var bytesConsumed = 0
            while (bytesConsumed < origLen) {
                val inputLen = minOf(rng.nextInt(0, maxInputLen), origLen - bytesConsumed)
                streamEncoder
                    .writeAll(origData.copyOfRange(bytesConsumed, bytesConsumed + inputLen))
                    .getOrThrow()
                bytesConsumed += inputLen
            }

            streamEncoder.finish().getOrThrow()
            assertEquals(origLen, bytesConsumed)
            assertEquals(normalEncoded, streamEncoded.toByteArray().decodeToString())
        }
    }

    private class InterruptingWriter(
        private val writer: ByteWriter,
        private val rng: Random,
        private val fraction: Double,
    ) : ByteWriter {
        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Result<Int> {
            if (rng.nextDouble(0.0, 1.0) <= fraction) {
                return Result.failure(interrupted())
            }
            return writer.write(buffer, offset, length)
        }

        override fun flush(): Result<Unit> {
            if (rng.nextDouble(0.0, 1.0) <= fraction) {
                return Result.failure(interrupted())
            }
            return writer.flush()
        }
    }

    private class PartialInterruptingWriter(
        private val writer: ByteWriter,
        private val rng: Random,
        private val fullInputFraction: Double,
        private val noInterruptFraction: Double,
    ) : ByteWriter {
        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Result<Int> {
            if (rng.nextDouble(0.0, 1.0) > noInterruptFraction) {
                return Result.failure(interrupted())
            }

            return if (rng.nextDouble(0.0, 1.0) <= fullInputFraction || length <= 1) {
                writer.write(buffer, offset, length)
            } else {
                writer.write(buffer, offset, rng.nextInt(0, length))
            }
        }

        override fun flush(): Result<Unit> = writer.flush()
    }

    private companion object {
        val URL_SAFE_ENGINE = GeneralPurpose(URL_SAFE, PAD)
        val NO_PAD_ENGINE = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)
    }
}
