// port-lint: source src/alphabet.rs
// Provides [Alphabet] and constants for alphabets commonly used in the wild.

package io.github.kotlinmania.base64.alphabet

import io.github.kotlinmania.base64.PAD_BYTE

private const val ALPHABET_SIZE: Int = 64

/**
 * An alphabet defines the 64 ASCII characters (symbols) used for base64.
 *
 * Common alphabets are provided as constants, and custom alphabets
 * can be made via [Alphabet.new] or the [Alphabet.tryFrom] companion.
 *
 * # Examples
 *
 * Building and using a custom Alphabet:
 *
 * ```
 * val custom = Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 *
 * val engine = GeneralPurpose(custom, generalPurpose.PAD)
 * ```
 *
 * Building a top-level constant:
 *
 * ```
 * import io.github.kotlinmania.base64.alphabet.Alphabet
 *
 * val CUSTOM: Alphabet =
 *     Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 * ```
 *
 * Building lazily:
 *
 * ```
 * import io.github.kotlinmania.base64.alphabet.Alphabet
 *
 * val CUSTOM: Alphabet by lazy {
 *     Alphabet.new("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/").getOrThrow()
 * }
 * ```
 */
// Lifted from upstream attributes: derives Clone, Debug, Eq, PartialEq.
class Alphabet internal constructor(
    internal val symbols: ByteArray,
) {
    /**
     * Create a String from the symbols in the [Alphabet].
     */
    fun asString(): String = symbols.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alphabet) return false
        return symbols.contentEquals(other.symbols)
    }

    override fun hashCode(): Int = symbols.contentHashCode()

    override fun toString(): String = "Alphabet { symbols: ${asString()} }"

    companion object {
        /**
         * Performs no checks so that it can match the upstream's const-fn shape.
         * Used only for known-valid strings.
         */
        private fun fromStrUnchecked(alphabet: String): Alphabet {
            val symbols = ByteArray(ALPHABET_SIZE)
            val sourceBytes = alphabet.encodeToByteArray()

            // element-by-element copy mirroring the upstream while-loop
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
        fun new(alphabet: String): Result<Alphabet> {
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
                    val unsigned = byte.toInt() and 0xFF
                    if (!(unsigned in 32..126)) {
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

        /**
         * Try to construct an [Alphabet] from a [String]. Equivalent to [new].
         */
        fun tryFrom(value: String): Result<Alphabet> = new(value)

        // ---- upstream module-level constants ----

        /**
         * The standard alphabet (with `+` and `/`) specified in [RFC 4648][].
         *
         * [RFC 4648]: https://datatracker.ietf.org/doc/html/rfc4648#section-4
         */
        val STANDARD: Alphabet = fromStrUnchecked(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
        )

        /**
         * The URL-safe alphabet (with `-` and `_`) specified in [RFC 4648][].
         *
         * [RFC 4648]: https://datatracker.ietf.org/doc/html/rfc4648#section-5
         */
        val URL_SAFE: Alphabet = fromStrUnchecked(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",
        )

        /**
         * The `crypt(3)` alphabet (with `.` and `/` as the _first_ two characters).
         *
         * Not standardized, but folk wisdom on the net asserts that this alphabet is what crypt uses.
         */
        val CRYPT: Alphabet = fromStrUnchecked(
            "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
        )

        /**
         * The bcrypt alphabet.
         */
        val BCRYPT: Alphabet = fromStrUnchecked(
            "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
        )

        /**
         * The alphabet used in IMAP-modified UTF-7 (with `+` and `,`).
         *
         * See [RFC 3501](https://tools.ietf.org/html/rfc3501#section-5.1.3)
         */
        val IMAP_MUTF7: Alphabet = fromStrUnchecked(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+,",
        )

        /**
         * The alphabet used in BinHex 4.0 files.
         *
         * See [BinHex 4.0 Definition](http://files.stairways.com/other/binhex-40-specs-info.txt)
         */
        val BIN_HEX: Alphabet = fromStrUnchecked(
            "!\"#$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`abcdefhijklmpqr",
        )
    }
}

/**
 * Possible errors when constructing an [Alphabet] from a [String].
 */
// Lifted from upstream attributes: derives Debug, Eq, PartialEq; implements std::error::Error.
sealed class ParseAlphabetError(message: String) : RuntimeException(message) {
    /** Alphabets must be 64 ASCII bytes */
    object InvalidLength : ParseAlphabetError("Invalid length - must be 64 bytes")

    /** All bytes must be unique */
    data class DuplicatedByte(val b: Byte) : ParseAlphabetError("Duplicated byte: ${formatPaddedHex(b)}")

    /** All bytes must be printable (in the range `[32, 126]`). */
    data class UnprintableByte(val b: Byte) : ParseAlphabetError("Unprintable byte: ${formatPaddedHex(b)}")

    /** `=` cannot be used */
    data class ReservedByte(val b: Byte) : ParseAlphabetError("Reserved byte: ${formatPaddedHex(b)}")
}

// Format a byte the way Rust's `{:#04x}` does: lowercase hex, two digits, leading `0x`.
private fun formatPaddedHex(b: Byte): String {
    val unsigned = b.toInt() and 0xFF
    return "0x" + unsigned.toString(16).padStart(2, '0')
}
