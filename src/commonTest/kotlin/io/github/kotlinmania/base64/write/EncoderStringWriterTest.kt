// port-lint: tests write/encoder_string_writer.rs
package io.github.kotlinmania.base64.write

import io.github.kotlinmania.base64.randomEngine
import io.github.kotlinmania.base64.writeAll
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class EncoderStringWriterTest {
    @Test
    fun everyPossibleSplitOfInput() {
        val rng = Random(0x5101)
        val size = 500

        for (i in 0 until size) {
            val origData = ByteArray(size)
            rng.nextBytes(origData)

            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)

            val streamEncoder = EncoderStringWriter.new(engine)
            streamEncoder.writeAll(origData.copyOfRange(0, i)).getOrThrow()
            streamEncoder.writeAll(origData.copyOfRange(i, origData.size)).getOrThrow()
            val streamEncoded = streamEncoder.intoInner().intoString()

            assertEquals(normalEncoded, streamEncoded)
        }
    }

    @Test
    fun incrementalWrites() {
        val rng = Random(0x5102)
        val size = 500

        repeat(size) {
            val origData = ByteArray(size)
            rng.nextBytes(origData)

            val engine = randomEngine(rng)
            val normalEncoded = engine.encode(origData)
            val streamEncoder = EncoderStringWriter.new(engine)

            var offset = 0
            while (offset < size) {
                val nibbleSize = minOf(rng.nextInt(0, 65), size - offset)
                val len = streamEncoder.write(origData, offset, nibbleSize).getOrThrow()
                offset += len
            }

            val streamEncoded = streamEncoder.intoInner().intoString()
            assertEquals(normalEncoded, streamEncoded)
        }
    }
}
