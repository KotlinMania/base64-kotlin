// port-lint: source chunked_encoder.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine

/** The output mechanism for [ChunkedEncoder]'s encoded bytes. */
public interface Sink {
    /** Handle a chunk of encoded base64 data (as UTF-8 bytes). */
    public fun writeEncodedBytes(encoded: ByteArray): Result<Unit>
}

/** A base64 encoder that emits encoded bytes in chunks without heap allocation. */
public class ChunkedEncoder<C : Config, D : DecodeEstimate>(
    private val engine: Engine<C, D>,
) {
    public fun encode(bytes: ByteArray, sink: Sink): Result<Unit> {
        val bufSize = 1024
        val chunkSize = bufSize / 4 * 3

        val buf = ByteArray(bufSize)
        var pos = 0
        while (pos < bytes.size) {
            val end = minOf(pos + chunkSize, bytes.size)
            val chunk = bytes.copyOfRange(pos, end)
            var len = engine.internalEncode(chunk, buf)
            if (chunk.size != chunkSize && engine.config().encodePadding()) {
                // Final, potentially partial, chunk.
                // Only need to consider if padding is needed on a partial chunk since full chunk
                // is a multiple of 3, which therefore won't be padded.
                // Pad output to multiple of four bytes if required by config.
                len += addPadding(len, buf, len)
            }
            val writeResult = sink.writeEncodedBytes(buf.copyOfRange(0, len))
            if (writeResult.isFailure) return writeResult
            pos = end
        }

        return Result.success(Unit)
    }

    public companion object {
        public fun <C : Config, D : DecodeEstimate> new(engine: Engine<C, D>): ChunkedEncoder<C, D> =
            ChunkedEncoder(engine)
    }
}

// A really simple sink that just appends to a string
internal class StringSink(
    private val string: StringBuilder,
) : Sink {
    override fun writeEncodedBytes(encoded: ByteArray): Result<Unit> {
        string.append(encoded.decodeToString())

        return Result.success(Unit)
    }

    companion object {
        fun new(string: StringBuilder): StringSink = StringSink(string)
    }
}
