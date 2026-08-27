// port-lint: source write/encoder_string_writer.rs
package io.github.kotlinmania.base64.write

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.io.ByteWriter

public interface StrConsumer {
    public fun consume(buffer: String)
}

public class StringBuilderConsumer(
    private val string: StringBuilder,
) : StrConsumer {
    override fun consume(buffer: String) {
        string.append(buffer)
    }

    public fun intoString(): String = string.toString()
}

private class Utf8SingleCodeUnitWriter<S : StrConsumer>(
    val strConsumer: S,
) : ByteWriter {
    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> {
        strConsumer.consume(buffer.decodeToString(offset, offset + length))
        return Result.success(length)
    }
}

public class EncoderStringWriter<C : Config, D : DecodeEstimate, S : StrConsumer> private constructor(
    private val encoder: EncoderWriter<C, D, Utf8SingleCodeUnitWriter<S>>,
) : ByteWriter {
    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Result<Int> = encoder.write(buffer, offset, length)

    override fun flush(): Result<Unit> = encoder.flush()

    public fun intoInner(): S =
        encoder
            .finish()
            .getOrThrow()
            .strConsumer

    public companion object {
        public fun <C : Config, D : DecodeEstimate> new(
            engine: Engine<C, D>,
        ): EncoderStringWriter<C, D, StringBuilderConsumer> =
            fromConsumer(StringBuilderConsumer(StringBuilder()), engine)

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
