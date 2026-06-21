// port-lint: source write/encoder.rs
package io.github.kotlinmania.base64.write

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.io.ByteWriter
import io.github.kotlinmania.base64.io.StreamErrorKind

public const val ENCODER_BUF_SIZE: Int = 1024

private const val MAX_INPUT_LEN: Int = ENCODER_BUF_SIZE / 4 * 3
private const val MIN_ENCODE_CHUNK_SIZE: Int = 3

public class EncoderWriter<C : Config, D : DecodeEstimate, W : ByteWriter>(
    delegate: W,
    private val engine: Engine<C, D>,
) : ByteWriter {
    private var delegate: W? = delegate
    private val extraInput = ByteArray(MIN_ENCODE_CHUNK_SIZE)
    private var extraInputOccupiedLen = 0
    private val output = ByteArray(ENCODER_BUF_SIZE)
    private var outputOccupiedLen = 0

    public fun finish(): Result<W> {
        if (delegate == null) {
            throw IllegalStateException("Encoder has already had finish() called")
        }

        writeFinalLeftovers().getOrElse { return Result.failure(it) }
        val writer = delegate ?: throw IllegalStateException("Writer must be present")
        delegate = null
        return Result.success(writer)
    }

    private fun writeFinalLeftovers(): Result<Unit> {
        if (delegate == null) {
            return Result.success(Unit)
        }

        writeAllEncodedOutput().getOrElse { return Result.failure(it) }

        if (extraInputOccupiedLen > 0) {
            val encodedLen =
                engine
                    .encodeSlice(extraInput.copyOfRange(0, extraInputOccupiedLen), output)
                    .getOrThrow()
            outputOccupiedLen = encodedLen

            writeAllEncodedOutput().getOrElse { return Result.failure(it) }
            extraInputOccupiedLen = 0
        }

        return Result.success(Unit)
    }

    private fun writeToDelegate(currentOutputLen: Int): Result<Unit> {
        val writer = delegate ?: throw IllegalStateException("Writer must be present")
        val consumed =
            writer.write(output, 0, currentOutputLen)
                .getOrElse { return Result.failure(it) }

        require(consumed in 0..currentOutputLen)
        if (consumed < currentOutputLen) {
            outputOccupiedLen = currentOutputLen - consumed
            output.copyInto(output, 0, consumed, currentOutputLen)
        } else {
            outputOccupiedLen = 0
        }

        return Result.success(Unit)
    }

    private fun writeAllEncodedOutput(): Result<Unit> {
        while (outputOccupiedLen > 0) {
            val remainingLen = outputOccupiedLen
            val result = writeToDelegate(remainingLen)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                if (error is io.github.kotlinmania.base64.io.StreamError &&
                    error.kind == StreamErrorKind.Interrupted
                ) {
                    continue
                }
                return result
            }
        }

        return Result.success(Unit)
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        require(offset >= 0)
        require(length >= 0)
        require(offset + length <= buffer.size)

        if (delegate == null) {
            throw IllegalStateException("Cannot write more after calling finish()")
        }

        if (length == 0) {
            return Result.success(0)
        }

        if (outputOccupiedLen > 0) {
            val currentLen = outputOccupiedLen
            return writeToDelegate(currentLen).map { 0 }
        }

        var extraInputReadLen = 0
        var inputOffset = offset
        var inputLen = length
        val origExtraLen = extraInputOccupiedLen
        var encodedSize = 0
        var maxInputLen = MAX_INPUT_LEN

        if (extraInputOccupiedLen > 0) {
            if (inputLen + extraInputOccupiedLen >= MIN_ENCODE_CHUNK_SIZE) {
                extraInputReadLen = MIN_ENCODE_CHUNK_SIZE - extraInputOccupiedLen
                buffer.copyInto(
                    extraInput,
                    extraInputOccupiedLen,
                    inputOffset,
                    inputOffset + extraInputReadLen,
                )

                val len = engine.internalEncode(extraInput, output)
                check(len == 4)

                inputOffset += extraInputReadLen
                inputLen -= extraInputReadLen
                extraInputOccupiedLen = 0
                encodedSize = 4
                maxInputLen = MAX_INPUT_LEN - MIN_ENCODE_CHUNK_SIZE
            } else {
                extraInput[extraInputOccupiedLen] = buffer[inputOffset]
                extraInputOccupiedLen += 1
                return Result.success(1)
            }
        } else if (inputLen < MIN_ENCODE_CHUNK_SIZE) {
            buffer.copyInto(extraInput, 0, inputOffset, inputOffset + inputLen)
            extraInputOccupiedLen = inputLen
            return Result.success(inputLen)
        }

        val inputCompleteChunksLen = inputLen - (inputLen % MIN_ENCODE_CHUNK_SIZE)
        val inputChunksToEncodeLen = minOf(inputCompleteChunksLen, maxInputLen)
        if (inputChunksToEncodeLen > 0) {
            val encodedInput = ByteArray(inputChunksToEncodeLen / MIN_ENCODE_CHUNK_SIZE * 4)
            val encodedInputLen =
                engine.internalEncode(
                    buffer.copyOfRange(inputOffset, inputOffset + inputChunksToEncodeLen),
                    encodedInput,
                )
            encodedInput.copyInto(
                output,
                encodedSize,
                0,
                encodedInputLen,
            )
            encodedSize += encodedInputLen
        }

        return writeToDelegate(encodedSize)
            .map { extraInputReadLen + inputChunksToEncodeLen }
            .recoverCatching { error ->
                extraInputOccupiedLen = origExtraLen
                throw error
            }
    }

    override fun flush(): Result<Unit> {
        writeAllEncodedOutput().getOrElse { return Result.failure(it) }
        return (delegate ?: throw IllegalStateException("Writer must be present")).flush()
    }

    public fun intoInner(): W =
        delegate ?: throw IllegalStateException("Encoder has already had finish() called")

    public fun close(): Result<Unit> = writeFinalLeftovers()

    public companion object {
        public fun <C : Config, D : DecodeEstimate, W : ByteWriter> new(
            delegate: W,
            engine: Engine<C, D>,
        ): EncoderWriter<C, D, W> = EncoderWriter(delegate, engine)
    }
}
