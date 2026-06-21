// port-lint: source read/decoder.rs
package io.github.kotlinmania.base64.read

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.PAD_BYTE
import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.io.ByteReader
import io.github.kotlinmania.base64.io.StreamError
import io.github.kotlinmania.base64.io.StreamErrorKind

public const val DECODER_BUF_SIZE: Int = 1024

private const val BASE64_CHUNK_SIZE: Int = 4
private const val DECODED_CHUNK_SIZE: Int = 3

public class DecoderReader<C : Config, D : DecodeEstimate, R : ByteReader>(
    private val inner: R,
    private val engine: Engine<C, D>,
) : ByteReader {
    private val b64Buffer = ByteArray(DECODER_BUF_SIZE)
    private var b64Offset: Int = 0
    private var b64Len: Int = 0
    private val decodedChunkBuffer = ByteArray(DECODED_CHUNK_SIZE)
    private var decodedOffset: Int = 0
    private var decodedLen: Int = 0
    private var inputConsumedLen: Int = 0
    private var paddingOffset: Int? = null

    private fun flushDecodedBuf(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        val copyLen = minOf(decodedLen, length)
        decodedChunkBuffer.copyInto(buffer, offset, decodedOffset, decodedOffset + copyLen)
        decodedOffset += copyLen
        decodedLen -= copyLen
        return Result.success(copyLen)
    }

    private fun readFromDelegate(): Result<Int> {
        val readOffset = b64Offset + b64Len
        return inner.read(b64Buffer, readOffset, DECODER_BUF_SIZE - readOffset)
            .onSuccess { read -> b64Len += read }
    }

    private fun decodeToBuf(
        b64LenToDecode: Int,
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        val b64ToDecode = b64Buffer.copyOfRange(b64Offset, b64Offset + b64LenToDecode)
        val target =
            if (offset == 0 && length == buffer.size) {
                buffer
            } else {
                ByteArray(length)
            }

        val decodeMetadata =
            engine
                .internalDecode(
                    b64ToDecode,
                    target,
                    engine.internalDecodedLenEstimate(b64LenToDecode),
                )
                .getOrElse { error ->
                    val decodeError =
                        when (error) {
                            is DecodeSliceError.DecodeErrorVariant ->
                                adjustDecodeError(error.error)
                            DecodeSliceError.OutputSliceTooSmall ->
                                throw IllegalStateException("buffer is sized correctly")
                            else -> error
                        }
                    return Result.failure(
                        StreamError(StreamErrorKind.InvalidData, decodeError.toString(), decodeError),
                    )
                }

        if ((offset != 0 || length != buffer.size) && decodeMetadata.decodedLen > 0) {
            target.copyInto(buffer, offset, 0, decodeMetadata.decodedLen)
        }

        val previousPaddingOffset = paddingOffset
        if (previousPaddingOffset != null && decodeMetadata.decodedLen > 0) {
            val error = DecodeError.InvalidByte(previousPaddingOffset, PAD_BYTE)
            return Result.failure(StreamError(StreamErrorKind.InvalidData, error.toString(), error))
        }

        paddingOffset =
            paddingOffset
                ?: decodeMetadata.paddingOffset?.let { padding -> inputConsumedLen + padding }
        inputConsumedLen += b64LenToDecode
        b64Offset += b64LenToDecode
        b64Len -= b64LenToDecode

        return Result.success(decodeMetadata.decodedLen)
    }

    private fun adjustDecodeError(error: DecodeError): DecodeError =
        when (error) {
            is DecodeError.InvalidByte ->
                if (error.byte == PAD_BYTE && paddingOffset != null) {
                    DecodeError.InvalidByte(paddingOffset ?: error.index, PAD_BYTE)
                } else {
                    DecodeError.InvalidByte(inputConsumedLen + error.index, error.byte)
                }

            is DecodeError.InvalidLength ->
                DecodeError.InvalidLength(inputConsumedLen + error.len)

            is DecodeError.InvalidLastSymbol ->
                DecodeError.InvalidLastSymbol(inputConsumedLen + error.index, error.byte)

            DecodeError.InvalidPadding -> DecodeError.InvalidPadding
        }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        require(offset >= 0)
        require(length >= 0)
        require(offset + length <= buffer.size)

        if (length == 0) {
            return Result.success(0)
        }

        if (decodedLen > 0) {
            return flushDecodedBuf(buffer, offset, length)
        }

        var atEof = false
        while (b64Len < BASE64_CHUNK_SIZE) {
            b64Buffer.copyInto(b64Buffer, 0, b64Offset, b64Offset + b64Len)
            b64Offset = 0

            val read = readFromDelegate().getOrElse { return Result.failure(it) }
            if (read == 0) {
                atEof = true
                break
            }
        }

        if (b64Len == 0) {
            return Result.success(0)
        }

        return if (length < DECODED_CHUNK_SIZE) {
            val decodedChunk = ByteArray(DECODED_CHUNK_SIZE)
            val toDecode = minOf(b64Len, BASE64_CHUNK_SIZE)
            val decoded =
                decodeToBuf(toDecode, decodedChunk, 0, decodedChunk.size)
                    .getOrElse { return Result.failure(it) }

            decodedChunk.copyInto(decodedChunkBuffer, 0, 0, decoded)
            decodedOffset = 0
            decodedLen = decoded

            flushDecodedBuf(buffer, offset, length)
        } else {
            val b64BytesThatCanDecodeIntoBuf = length / DECODED_CHUNK_SIZE * BASE64_CHUNK_SIZE
            val b64BytesAvailableToDecode =
                if (atEof) b64Len else b64Len - b64Len % BASE64_CHUNK_SIZE
            val actualDecodeLen =
                minOf(b64BytesThatCanDecodeIntoBuf, b64BytesAvailableToDecode)

            decodeToBuf(actualDecodeLen, buffer, offset, length)
        }
    }

    public fun intoInner(): R = inner

    override fun toString(): String =
        "DecoderReader(" +
            "b64Offset=$b64Offset, " +
            "b64Len=$b64Len, " +
            "decodedChunkBuffer=${decodedChunkBuffer.contentToString()}, " +
            "decodedOffset=$decodedOffset, " +
            "decodedLen=$decodedLen, " +
            "inputConsumedLen=$inputConsumedLen, " +
            "paddingOffset=$paddingOffset" +
            ")"

    internal fun fmt(): String = toString()

    public companion object {
        /** Create a decoder that reads encoded bytes from [reader]. */
        public fun <C : Config, D : DecodeEstimate, R : ByteReader> new(
            reader: R,
            engine: Engine<C, D>,
        ): DecoderReader<C, D, R> = DecoderReader(reader, engine)
    }
}
