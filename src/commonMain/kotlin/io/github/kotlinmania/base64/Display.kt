// port-lint: source display.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine

/**
 * A convenience wrapper for rendering bytes as base64 text without manually allocating the encoded
 * string first.
 */
public class Base64Display<C : Config, D : DecodeEstimate>(
    private val bytes: ByteArray,
    engine: Engine<C, D>,
) {
    private val chunkedEncoder: ChunkedEncoder<C, D> = ChunkedEncoder(engine)

    public fun fmt(formatter: StringBuilder): Result<Unit> {
        val sink = FormatterSink(formatter)
        return chunkedEncoder.encode(bytes, sink)
    }

    override fun toString(): String {
        val output = StringBuilder()
        fmt(output).getOrThrow()
        return output.toString()
    }

    public companion object {
        public fun <C : Config, D : DecodeEstimate> new(
            bytes: ByteArray,
            engine: Engine<C, D>,
        ): Base64Display<C, D> = Base64Display(bytes, engine)
    }
}

internal class FormatterSink(
    private val formatter: StringBuilder,
) : Sink {
    override fun writeEncodedBytes(encoded: ByteArray): Result<Unit> {
        formatter.append(encoded.decodeToString())
        return Result.success(Unit)
    }
}
