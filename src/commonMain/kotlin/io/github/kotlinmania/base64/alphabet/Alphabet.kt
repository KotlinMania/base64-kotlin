// port-lint: source src/alphabet.rs
package io.github.kotlinmania.base64.alphabet

import io.github.kotlinmania.base64.PAD_BYTE

/**
 * Provides [Alphabet] and constants for alphabets commonly used in the wild.
 */

private const val ALPHABET_SIZE: Int = 64

/**
 * An alphabet defines the 64 ASCII characters (symbols) used for base64.
 *
 * Common alphabets are provided as constants, and custom alphabets can be made via [new].
 *
 * # Examples
 *
 * Building and using a custom Alphabet:
 *
 * ```kotlin
 * val custom = Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 *
 * val engine = GeneralPurpose(custom, io.github.kotlinmania.base64.engine.general_purpose.PAD)
 * ```
 *
 * Building a top-level `val`:
 *
 * ```kotlin
 * val custom: Alphabet =
 *     Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 * ```
 *
 * Building lazily:
 *
 * ```kotlin
 * val custom: Alphabet by lazy {
 *     Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 * }
 * ```
 */
public class Alphabet internal constructor(internal val symbols: ByteArray) {

    /** Create a [String] from the symbols in the [Alphabet]. */
    public fun asStr(): String = symbols.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alphabet) return false
        return symbols.contentEquals(other.symbols)
    }

    override fun hashCode(): Int = symbols.contentHashCode()

    override fun toString(): String = "Alphabet { symbols: ${asStr()} }"

    public companion object {
        /**
         * Performs no checks so that it can run as cheaply as possible.
         * Used only for known-valid strings.
         */
        private fun fromStrUnchecked(alphabet: String): Alphabet {
            val symbols = ByteArray(ALPHABET_SIZE)
            val sourceBytes = alphabet.encodeToByteArray()

            // a way to copy that's allowed in const fn
            var index = 0
            while (index < ALPHABET_SIZE) {
                symbols[index] = sourceBytes[index]
                index += 1
            }

            return Alphabet(symbols)
        }

        /**
         * Create an [Alphabet] from a string of 64 unique printable ASCII bytes.
         *
         * The `=` byte is not allowed as it is used for padding.
         */
        public fun new(alphabet: String): Result<Alphabet> {
            val bytes = alphabet.encodeToByteArray()
            if (bytes.size != ALPHABET_SIZE) {
                return Result.failure(ParseAlphabetError.InvalidLength)
            }

            run {
                var index = 0
                while (index < ALPHABET_SIZE) {
                    val byte = bytes[index]

                    // must be ascii printable. 127 (DEL) is commonly considered printable
                    // for some reason but clearly unsuitable for base64.
                    if (!(byte >= 32.toByte() && byte <= 126.toByte())) {
                        return Result.failure(ParseAlphabetError.UnprintableByte(byte))
                    }
                    // = is assumed to be padding, so cannot be used as a symbol
                    if (byte == PAD_BYTE) {
                        return Result.failure(ParseAlphabetError.ReservedByte(byte))
                    }

                    // Check for duplicates while staying within what const allows.
                    // It's n^2, but only over 64 hot bytes, and only once, so it's likely in the single digit
                    // microsecond range.

                    var probeIndex = 0
                    while (probeIndex < ALPHABET_SIZE) {
                        if (probeIndex == index) {
                            probeIndex += 1
                            continue
                        }

                        val probeByte = bytes[probeIndex]

                        if (byte == probeByte) {
                            return Result.failure(ParseAlphabetError.DuplicatedByte(byte))
                        }

                        probeIndex += 1
                    }

                    index += 1
                }
            }

            return Result.success(fromStrUnchecked(alphabet))
        }
    }
}

/** Possible errors when constructing an [Alphabet] from a [String]. */
public sealed class ParseAlphabetError : Exception() {
    /** Alphabets must be 64 ASCII bytes. */
    public object InvalidLength : ParseAlphabetError() {
        override val message: String = "Invalid length - must be 64 bytes"
    }

    /** All bytes must be unique. */
    public data class DuplicatedByte(val byte: Byte) : ParseAlphabetError() {
        override val message: String get() = "Duplicated byte: ${formatByteHex(byte)}"
    }

    /** All bytes must be printable (in the range `[32, 126]`). */
    public data class UnprintableByte(val byte: Byte) : ParseAlphabetError() {
        override val message: String get() = "Unprintable byte: ${formatByteHex(byte)}"
    }

    /** `=` cannot be used. */
    public data class ReservedByte(val byte: Byte) : ParseAlphabetError() {
        override val message: String get() = "Reserved byte: ${formatByteHex(byte)}"
    }

    override fun toString(): String = message ?: super.toString()
}

private fun formatByteHex(b: Byte): String {
    val v = b.toInt() and 0xFF
    val hex = v.toString(16)
    return "0x" + (if (hex.length < 2) "0$hex" else hex)
}

/**
 * The standard alphabet (with `+` and `/`) specified in
 * [RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648#section-4).
 */
public val STANDARD: Alphabet = Alphabet.new(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
).getOrThrow()

/**
 * The URL-safe alphabet (with `-` and `_`) specified in
 * [RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648#section-5).
 */
public val URL_SAFE: Alphabet = Alphabet.new(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",
).getOrThrow()

/**
 * The `crypt(3)` alphabet (with `.` and `/` as the _first_ two characters).
 *
 * Not standardized, but folk wisdom on the net asserts that this alphabet is what crypt uses.
 */
public val CRYPT: Alphabet = Alphabet.new(
    "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
).getOrThrow()

/** The bcrypt alphabet. */
public val BCRYPT: Alphabet = Alphabet.new(
    "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
).getOrThrow()

/**
 * The alphabet used in IMAP-modified UTF-7 (with `+` and `,`).
 *
 * See [RFC 3501](https://tools.ietf.org/html/rfc3501#section-5.1.3).
 */
public val IMAP_MUTF7: Alphabet = Alphabet.new(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+,",
).getOrThrow()

/**
 * The alphabet used in BinHex 4.0 files.
 *
 * See [BinHex 4.0 Definition](http://files.stairways.com/other/binhex-40-specs-info.txt).
 */
public val BIN_HEX: Alphabet = Alphabet.new(
    "!\"#\$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`abcdefhijklmpqr",
).getOrThrow()
