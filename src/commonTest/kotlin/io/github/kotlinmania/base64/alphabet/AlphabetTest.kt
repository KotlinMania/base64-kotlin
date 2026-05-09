// port-lint: source src/alphabet.rs
package io.github.kotlinmania.base64.alphabet

import kotlin.test.Test
import kotlin.test.assertEquals

class AlphabetTest {
    @Test
    fun detectsDuplicateStart() {
        assertEquals(
            ParseAlphabetError.DuplicatedByte('A'.code.toByte()),
            Alphabet.new("AACDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")
                .exceptionOrNull(),
        )
    }

    @Test
    fun detectsDuplicateEnd() {
        assertEquals(
            ParseAlphabetError.DuplicatedByte('/'.code.toByte()),
            Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789//")
                .exceptionOrNull(),
        )
    }

    @Test
    fun detectsDuplicateMiddle() {
        assertEquals(
            ParseAlphabetError.DuplicatedByte('Z'.code.toByte()),
            Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZZbcdefghijklmnopqrstuvwxyz0123456789+/")
                .exceptionOrNull(),
        )
    }

    @Test
    fun detectsLength() {
        assertEquals(
            ParseAlphabetError.InvalidLength,
            Alphabet.new(
                "xxxxxxxxxABCDEFGHIJKLMNOPQRSTUVWXYZZbcdefghijklmnopqrstuvwxyz0123456789+/",
            ).exceptionOrNull(),
        )
    }

    @Test
    fun detectsPadding() {
        assertEquals(
            ParseAlphabetError.ReservedByte('='.code.toByte()),
            Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+=")
                .exceptionOrNull(),
        )
    }

    @Test
    fun detectsUnprintable() {
        // form feed
        assertEquals(
            ParseAlphabetError.UnprintableByte(0x0c.toByte()),
            Alphabet.new("BCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")
                .exceptionOrNull(),
        )
    }

    @Test
    fun sameAsUnchecked() {
        assertEquals(
            STANDARD,
            Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/")
                .getOrThrow(),
        )
    }

    @Test
    fun strSameAsInput() {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val a = Alphabet.new(alphabet).getOrThrow()
        assertEquals(alphabet, a.asStr())
    }
}
