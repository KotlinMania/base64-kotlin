// port-lint: source write/encoder_string_writer.rs
package io.github.kotlinmania.base64.write

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.io.ByteWriter

/**
 * An abstraction around consuming [String]s produced by base64 encoding.
 */
public interface StrConsumer {
    /**
     * Consume the base64 encoded data in [buffer].
     */
    public fun consume(buffer: String)
}

/**
 * Pushes the string onto the end of the [StringBuilder].
 */
public class StringBuilderConsumer(
    private val string: StringBuilder,
) : StrConsumer {
    override fun consume(buffer: String) {
        string.append(buffer)
    }

    public fun intoString(): String = string.toString()
}

/**
 * A [ByteWriter] that only can handle bytes that are valid single-byte UTF-8 code units.
 *
 * This is safe because we only use it when writing base64, which is always valid UTF-8.
 */
private class Utf8SingleCodeUnitWriter<S : StrConsumer>(
    val strConsumer: S,
) : ByteWriter {
    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        // Because we expect all input to be valid utf-8 individual bytes, we can encode any buffer
        // length
        strConsumer.consume(buffer.decodeToString(offset, offset + length))
        return Result.success(length)
    }
}

/**
 * A [ByteWriter] implementation that base64-encodes data using the provided config and accumulates the
 * resulting base64 utf8 string in a [StrConsumer] implementation (typically [StringBuilderConsumer]), which is
 * then exposed via [intoInner].
 *
 * # Performance
 *
 * Because it has to validate that the base64 is UTF-8, it is about 80% as fast as writing plain
 * bytes to a [ByteWriter].
 */
public class EncoderStringWriter<C : Config, D : DecodeEstimate, S : StrConsumer> private constructor(
    private val encoder: EncoderWriter<C, D, Utf8SingleCodeUnitWriter<S>>,
) : ByteWriter {
    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> = encoder.write(buffer, offset, length)

    override fun flush(): Result<Unit> = encoder.flush()

    /**
     * Encode all remaining buffered data, including any trailing incomplete input triples and
     * associated padding.
     *
     * Returns the base64-encoded form of the accumulated written data.
     */
    public fun intoInner(): S =
        encoder
            .finish()
            .getOrThrow()
            .strConsumer

    public companion object {
        /**
         * Create an [EncoderStringWriter] that will encode into a new [StringBuilderConsumer] with the provided config.
         */
        public fun <C : Config, D : DecodeEstimate> new(
            engine: Engine<C, D>,
        ): EncoderStringWriter<C, D, StringBuilderConsumer> =
            fromConsumer(StringBuilderConsumer(StringBuilder()), engine)

        /**
         * Create an [EncoderStringWriter] that will append to the provided [StrConsumer].
         */
        public fun <C : Config, D : DecodeEstimate, S : StrConsumer> fromConsumer(
            strConsumer: S,
            engine: Engine<C, D>,
        ): EncoderStringWriter<C, D, S> =
            EncoderStringWriter(
                EncoderWriter(
                    Utf8SingleCodeUnitWriter(strConsumer),
                    engine,
                ),
            )
    }
}
