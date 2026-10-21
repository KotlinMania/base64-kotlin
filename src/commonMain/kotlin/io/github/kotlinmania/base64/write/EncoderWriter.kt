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

/**
 * A [ByteWriter] implementation that base64 encodes data before delegating to the wrapped writer.
 *
 * Because base64 has special handling for the end of the input data (padding, etc), there's a
 * [finish] method on this type that encodes any leftover input bytes and adds padding if
 * appropriate.
 *
 * # Panics
 *
 * Calling [write] (or related methods) or [finish] after [finish] has completed without
 * error is invalid and will throw an [IllegalStateException].
 *
 * # Errors
 *
 * Base64 encoding itself does not generate errors, but errors from the wrapped writer will be
 * returned as per the contract of [ByteWriter].
 *
 * # Performance
 *
 * It has some minor performance loss compared to encoding slices (a couple percent).
 * It does not do any heap allocation.
 */
public class EncoderWriter<C : Config, D : DecodeEstimate, W : ByteWriter>(
    delegate: W,
    private val engine: Engine<C, D>,
) : ByteWriter {
    private var delegate: W? = delegate
    private val extraInput = ByteArray(MIN_ENCODE_CHUNK_SIZE)
    private var extraInputOccupiedLen = 0
    private val output = ByteArray(ENCODER_BUF_SIZE)
    private var outputOccupiedLen = 0

    /**
     * Encode all remaining buffered data and write it, including any trailing incomplete input
     * triples and associated padding.
     *
     * Once this succeeds, no further writes or calls to this method are allowed.
     *
     * This may write to the delegate writer multiple times if the delegate writer does not accept
     * all input provided to its [write] each invocation.
     *
     * Returns the writer that this was constructed around.
     */
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
            writer
                .write(output, 0, currentOutputLen)
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

    /**
     * Encode input and then write to the delegate writer.
     *
     * Under non-error circumstances, this returns [Result.success] with the value being the number of bytes
     * of [buffer] consumed.
     *
     * If the previous call to [write] provided more (encoded) data than the delegate writer could
     * accept in a single call to its [write], the remaining data is buffered. As long as buffered
     * data is present, subsequent calls to [write] will try to write the remaining buffered data
     * to the delegate and return either `Result.success(0)` -- and therefore not consume any of the input -- or
     * an error.
     *
     * # Errors
     *
     * Any errors emitted by the delegate writer are returned.
     */
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

    /**
     * Because this is usually treated as OK to call multiple times, it will *not* flush any
     * incomplete chunks of input or write padding.
     *
     * # Errors
     *
     * The first error that is not of [StreamErrorKind.Interrupted] will be returned.
     */
    override fun flush(): Result<Unit> {
        writeAllEncodedOutput().getOrElse { return Result.failure(it) }
        return (delegate ?: throw IllegalStateException("Writer must be present")).flush()
    }

    /**
     * Unwraps this [EncoderWriter], returning the base writer it writes base64 encoded output
     * to.
     *
     * Normally this method should not be needed, since [finish] returns the inner writer if
     * it completes successfully. That will also ensure all data has been flushed, which the
     * [intoInner] function does *not* do.
     *
     * Calling this method after [finish] has completed successfully will throw an [IllegalStateException],
     * since the writer has already been returned.
     */
    public fun intoInner(): W =
        delegate ?: throw IllegalStateException("Encoder has already had finish() called")

    public fun close(): Result<Unit> = writeFinalLeftovers()

    override fun toString(): String =
        "EncoderWriter(" +
            "extraInput=${extraInput.contentToString()}, " +
            "extraInputOccupiedLen=$extraInputOccupiedLen, " +
            "outputPrefix=${output.copyOfRange(0, minOf(5, output.size)).contentToString()}, " +
            "outputOccupiedLen=$outputOccupiedLen" +
            ")"

    internal fun fmt(): String = toString()

    public companion object {
        /**
         * Create a new encoder that will write to the provided delegate writer.
         */
        public fun <C : Config, D : DecodeEstimate, W : ByteWriter> new(
            delegate: W,
            engine: Engine<C, D>,
        ): EncoderWriter<C, D, W> = EncoderWriter(delegate, engine)
    }
}
