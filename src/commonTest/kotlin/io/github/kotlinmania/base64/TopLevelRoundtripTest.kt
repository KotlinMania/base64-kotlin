// port-lint: tests tests/tests.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.STANDARD as STANDARD_ALPHABET
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.NO_PAD
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TopLevelRoundtripTest {
    private fun roundtripRandom(
        byteBuf: MutableList<Byte>,
        strBuf: StringBuilder,
        engine: Engine<*, *>,
        byteLen: Int,
        approxValuesPerByte: Int,
        maxRounds: Int,
    ) {
        val numRounds = calculateNumberOfRounds(byteLen, approxValuesPerByte, maxRounds)
        val random = Random(0xB64 + byteLen)
        val decodeBuf = mutableListOf<Byte>()

        repeat(numRounds) {
            byteBuf.clear()
            strBuf.clear()
            decodeBuf.clear()
            while (byteBuf.size < byteLen) {
                byteBuf.add(randomByte(random))
            }

            engine.encodeString(byteBuf.toByteArray(), strBuf)
            engine.decodeVec(strBuf.toString().encodeToByteArray(), decodeBuf).getOrThrow()

            assertContentEquals(byteBuf.toByteArray(), decodeBuf.toByteArray())
        }
    }

    private fun calculateNumberOfRounds(
        byteLen: Int,
        approxValuesPerByte: Int,
        max: Int,
    ): Int {
        var prod = approxValuesPerByte.toLong()

        repeat(byteLen) {
            if (prod > max) {
                return max
            }

            prod = saturatingMultiply(prod, prod, max.toLong())
        }

        return prod.toInt()
    }

    @Test
    fun roundtripRandomShortStandard() {
        val byteBuf = mutableListOf<Byte>()
        val strBuf = StringBuilder()

        for (inputLen in 0 until 40) {
            roundtripRandom(byteBuf, strBuf, STANDARD, inputLen, 4, 10000)
        }
    }

    @Test
    fun roundtripRandomWithFastLoopStandard() {
        val byteBuf = mutableListOf<Byte>()
        val strBuf = StringBuilder()

        for (inputLen in 40 until 100) {
            roundtripRandom(byteBuf, strBuf, STANDARD, inputLen, 4, 1000)
        }
    }

    @Test
    fun roundtripRandomShortNoPadding() {
        val byteBuf = mutableListOf<Byte>()
        val strBuf = StringBuilder()

        val engine = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)
        for (inputLen in 0 until 40) {
            roundtripRandom(byteBuf, strBuf, engine, inputLen, 4, 10000)
        }
    }

    @Test
    fun roundtripRandomNoPadding() {
        val byteBuf = mutableListOf<Byte>()
        val strBuf = StringBuilder()

        val engine = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)

        for (inputLen in 40 until 100) {
            roundtripRandom(byteBuf, strBuf, engine, inputLen, 4, 1000)
        }
    }

    @Test
    fun roundtripDecodeTrailing10Bytes() {
        for (numQuads in 0 until 25) {
            val s = StringBuilder("ABCD".repeat(numQuads))
            s.append("EFGHIJKLZg")

            val engine = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)
            val decoded = engine.decode(s.toString().encodeToByteArray()).getOrThrow()
            assertEquals(numQuads * 3 + 7, decoded.size)

            assertEquals(s.toString(), engine.encode(decoded))
        }
    }

    @Test
    fun displayWrapperMatchesNormalEncode() {
        val bytes = ByteArray(256) { it.toByte() }

        assertEquals(
            STANDARD.encode(bytes),
            Base64Display.new(bytes, STANDARD).toString(),
        )
    }

    @Test
    fun encodeEngineSliceErrorWhenBufferTooSmall() {
        for (numTriples in 1 until 100) {
            val input = "AAA".repeat(numTriples).encodeToByteArray()
            var vec = ByteArray((numTriples - 1) * 4)
            assertIs<EncodeSliceError.OutputSliceTooSmall>(
                STANDARD.encodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertIs<EncodeSliceError.OutputSliceTooSmall>(
                STANDARD.encodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertIs<EncodeSliceError.OutputSliceTooSmall>(
                STANDARD.encodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertIs<EncodeSliceError.OutputSliceTooSmall>(
                STANDARD.encodeSlice(input, vec).exceptionOrNull(),
            )
            vec = vec.copyOf(vec.size + 1)
            assertEquals(
                numTriples * 4,
                STANDARD.encodeSlice(input, vec).getOrThrow(),
            )
        }
    }

    private fun randomByte(random: Random): Byte = ByteArray(1).also { random.nextBytes(it) }[0]

    private fun saturatingMultiply(
        left: Long,
        right: Long,
        ceiling: Long,
    ): Long {
        if (left == 0L || right == 0L) {
            return 0L
        }
        if (left > ceiling / right) {
            return ceiling + 1
        }
        return left * right
    }
}
