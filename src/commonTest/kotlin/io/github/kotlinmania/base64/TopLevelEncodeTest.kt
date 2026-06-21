// port-lint: tests tests/encode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.URL_SAFE
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.PAD
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import kotlin.test.Test
import kotlin.test.assertEquals

class TopLevelEncodeTest {
    private fun compareEncode(
        expected: String,
        target: ByteArray,
    ) {
        assertEquals(expected, STANDARD.encode(target))
    }

    @Test
    fun encodeAllAscii() {
        val ascii = ByteArray(128) { it.toByte() }

        compareEncode(
            "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7P" +
                "D0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn8" +
                "=",
            ascii,
        )
    }

    @Test
    fun encodeAllBytes() {
        val bytes = ByteArray(256) { it.toByte() }

        compareEncode(
            "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7P" +
                "D0+P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn" +
                "+AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq+wsbKztLW2t7i5uru8vb6" +
                "/wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t/g4eLj5OXm5+jp6uvs7e7v8PHy8/T19vf4+fr7/P3+/w==",
            bytes,
        )
    }

    @Test
    fun encodeAllBytesUrl() {
        val bytes = ByteArray(256) { it.toByte() }

        assertEquals(
            "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8gISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0" +
                "-P0BBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWltcXV5fYGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn" +
                "-AgYKDhIWGh4iJiouMjY6PkJGSk5SVlpeYmZqbnJ2en6ChoqOkpaanqKmqq6ytrq" +
                "-wsbKztLW2t7i5uru8vb6_wMHCw8TFxsfIycrLzM3Oz9DR0tPU1dbX2Nna29zd3t_g4eLj5OXm5-jp6uvs7e7v8PHy" +
                "8_T19vf4-fr7_P3-_w==",
            GeneralPurpose(URL_SAFE, PAD).encode(bytes),
        )
    }

    @Test
    fun encodedLenUnpadded() {
        assertEquals(0, encodedLen(0, false))
        assertEquals(2, encodedLen(1, false))
        assertEquals(3, encodedLen(2, false))
        assertEquals(4, encodedLen(3, false))
        assertEquals(6, encodedLen(4, false))
        assertEquals(7, encodedLen(5, false))
        assertEquals(8, encodedLen(6, false))
        assertEquals(10, encodedLen(7, false))
    }

    @Test
    fun encodedLenPadded() {
        assertEquals(0, encodedLen(0, true))
        assertEquals(4, encodedLen(1, true))
        assertEquals(4, encodedLen(2, true))
        assertEquals(4, encodedLen(3, true))
        assertEquals(8, encodedLen(4, true))
        assertEquals(8, encodedLen(5, true))
        assertEquals(8, encodedLen(6, true))
        assertEquals(12, encodedLen(7, true))
    }

    @Test
    fun encodedLenOverflow() {
        val maxSize = Int.MAX_VALUE / 4 * 3 + 2
        assertEquals(2, maxSize % 3)
        assertEquals(Int.MAX_VALUE, encodedLen(maxSize, false))
        assertEquals(null, encodedLen(maxSize + 1, false))
    }
}
