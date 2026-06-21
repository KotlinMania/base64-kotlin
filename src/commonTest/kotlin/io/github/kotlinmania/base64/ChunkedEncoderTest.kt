// port-lint: tests chunked_encoder.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.STANDARD as STANDARD_ALPHABET
import io.github.kotlinmania.base64.alphabet.URL_SAFE as URL_SAFE_ALPHABET
import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig
import io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.PAD
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkedEncoderTest {
    @Test
    fun chunkedEncodeEmpty() {
        assertEquals("", chunkedEncodeStr(byteArrayOf(), PAD))
    }

    @Test
    fun chunkedEncodeIntermediateFastLoop() {
        assertEquals("Zm9vYmFyYmF6cXV4", chunkedEncodeStr("foobarbazqux".encodeToByteArray(), PAD))
    }

    @Test
    fun chunkedEncodeFastLoop() {
        assertEquals(
            "Zm9vYmFyYmF6cXV4cXV1eGNvcmdlZ3JhdWx0Z2FycGx5eg==",
            chunkedEncodeStr("foobarbazquxquuxcorgegraultgarplyz".encodeToByteArray(), PAD),
        )
    }

    @Test
    fun chunkedEncodeSlowLoopOnly() {
        assertEquals("Zm9vYmFy", chunkedEncodeStr("foobar".encodeToByteArray(), PAD))
    }

    @Test
    fun chunkedEncodeMatchesNormalEncodeRandomStringSink() {
        val helper = StringSinkTestHelper()
        chunkedEncodeMatchesNormalEncodeRandom(helper)
    }

    private fun chunkedEncodeMatchesNormalEncodeRandom(sinkTestHelper: SinkTestHelper) {
        val rng = Random(0x5EED)
        val engines =
            listOf(
                GeneralPurpose(STANDARD_ALPHABET, PAD),
                GeneralPurpose(STANDARD_ALPHABET, NO_PAD),
                GeneralPurpose(URL_SAFE_ALPHABET, PAD),
                GeneralPurpose(URL_SAFE_ALPHABET, NO_PAD),
            )

        repeat(256) {
            val input = ByteArray(rng.nextInt(1, 10_000))
            rng.nextBytes(input)
            val engine = engines[rng.nextInt(engines.size)]

            assertEquals(
                engine.encode(input),
                sinkTestHelper.encodeToString(engine, input),
                "input len=${input.size}",
            )
        }
    }

    private fun chunkedEncodeStr(bytes: ByteArray, config: GeneralPurposeConfig): String {
        val output = StringBuilder()
        val sink = StringSink.new(output)
        val engine = GeneralPurpose(STANDARD_ALPHABET, config)
        val encoder = ChunkedEncoder.new(engine)
        encoder.encode(bytes, sink).getOrThrow()
        return output.toString()
    }

    private interface SinkTestHelper {
        fun <C : Config, D : DecodeEstimate> encodeToString(
            engine: Engine<C, D>,
            bytes: ByteArray,
        ): String
    }

    private class StringSinkTestHelper : SinkTestHelper {
        override fun <C : Config, D : DecodeEstimate> encodeToString(
            engine: Engine<C, D>,
            bytes: ByteArray,
        ): String {
            val encoder = ChunkedEncoder.new(engine)
            val output = StringBuilder()
            val sink = StringSink.new(output)
            encoder.encode(bytes, sink).getOrThrow()
            return output.toString()
        }
    }
}
