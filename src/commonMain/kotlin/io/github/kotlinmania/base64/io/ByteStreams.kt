package io.github.kotlinmania.base64.io

public enum class StreamErrorKind {
    Interrupted,
    InvalidData,
    Other,
}

public class StreamError(
    public val kind: StreamErrorKind,
    message: String,
    public val inner: Throwable? = null,
) : Exception(message, inner)

public interface ByteReader {
    public fun read(
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size - offset,
    ): Result<Int>
}

public interface ByteWriter {
    public fun write(
        buffer: ByteArray,
        offset: Int = 0,
        length: Int = buffer.size - offset,
    ): Result<Int>

    public fun flush(): Result<Unit> = Result.success(Unit)
}

public class ByteArrayReader(
    private val bytes: ByteArray,
) : ByteReader {
    private var offset: Int = 0

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

        val copied = minOf(length, bytes.size - this.offset)
        if (copied <= 0) {
            return Result.success(0)
        }

        bytes.copyInto(buffer, offset, this.offset, this.offset + copied)
        this.offset += copied
        return Result.success(copied)
    }
}

public class MutableByteArrayWriter : ByteWriter {
    private val bytes = mutableListOf<Byte>()

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        require(offset >= 0)
        require(length >= 0)
        require(offset + length <= buffer.size)

        for (index in offset until offset + length) {
            bytes.add(buffer[index])
        }
        return Result.success(length)
    }

    public fun toByteArray(): ByteArray = bytes.toByteArray()
}
