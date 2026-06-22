// port-lint: source decode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD

/** Errors that can occur while decoding. */
public sealed class DecodeError : Exception() {
    /**
     * An invalid byte was found in the input. The offset and offending byte are provided.
     *
     * Padding characters (`=`) interspersed in the encoded form are invalid, as they may only
     * be present as the last 0-2 bytes of input.
     *
     * This error may also indicate that extraneous trailing input bytes are present, causing
     * otherwise valid padding to no longer be the last bytes of input.
     */
    public data class InvalidByte(
        val index: Int,
        val byte: Byte,
    ) : DecodeError() {
        override val message: String get() = "Invalid symbol $byte, offset $index."
        override fun toString(): String = message
    }

    /**
     * The length of the input, as measured in valid base64 symbols, is invalid.
     * There must be 2-4 symbols in the last input quad.
     */
    public data class InvalidLength(
        val len: Int,
    ) : DecodeError() {
        override val message: String get() = "Invalid input length: $len"
        override fun toString(): String = message
    }

    /**
     * The last non-padding input symbol's encoded 6 bits have nonzero bits that will be discarded.
     * This is indicative of corrupted or truncated Base64.
     * Unlike [InvalidByte], which reports symbols that aren't in the alphabet,
     * this error is for symbols that are in the alphabet but represent nonsensical encodings.
     */
    public data class InvalidLastSymbol(
        val index: Int,
        val byte: Byte,
    ) : DecodeError() {
        override val message: String get() = "Invalid last symbol $byte, offset $index."
        override fun toString(): String = message
    }

    /**
     * The nature of the padding was not as configured: absent or incorrect when it must be
     * canonical, or present when it must be absent, etc.
     */
    public object InvalidPadding : DecodeError() {
        override val message: String = "Invalid padding"
    }

    override fun toString(): String = message ?: super.toString()

    internal fun fmt(): String = toString()
}

/** Errors that can occur while decoding into a slice. */
public sealed class DecodeSliceError : Exception() {
    /** A [DecodeError] occurred. */
    public data class DecodeErrorVariant(
        val error: DecodeError,
    ) : DecodeSliceError() {
        override val message: String get() = "DecodeError: $error"
        override val cause: Throwable get() = error
        override fun toString(): String = message
    }

    /** The provided slice is too small. */
    public object OutputSliceTooSmall : DecodeSliceError() {
        override val message: String = "Output slice too small"
    }

    override fun toString(): String = message ?: super.toString()

    public fun source(): DecodeError? =
        (this as? DecodeErrorVariant)?.error

    internal fun fmt(): String = toString()

    public companion object {
        /** Lift a [DecodeError] into a [DecodeSliceError]. */
        public fun fromDecodeError(e: DecodeError): DecodeSliceError = DecodeErrorVariant(e)

        /** Lift a [DecodeError] into a [DecodeSliceError]. */
        public fun from(e: DecodeError): DecodeSliceError = fromDecodeError(e)
    }
}

/**
 * Decode base64 using the standard engine.
 *
 * See [Engine.decode].
 */
public fun decode(input: ByteArray): Result<ByteArray> = STANDARD.decode(input)

/**
 * Decode from string reference as octets using the specified [Engine].
 *
 * See [Engine.decode].
 * Returns a [Result] containing a [ByteArray].
 */
public fun <C : Config, D : DecodeEstimate> decodeEngine(
    input: ByteArray,
    engine: Engine<C, D>,
): Result<ByteArray> = engine.decode(input)

/**
 * Decode from string reference as octets.
 *
 * See [Engine.decodeVec].
 */
public fun <C : Config, D : DecodeEstimate> decodeEngineVec(
    input: ByteArray,
    buffer: MutableList<Byte>,
    engine: Engine<C, D>,
): Result<Unit> = engine.decodeVec(input, buffer)

/**
 * Decode the input into the provided output slice.
 *
 * See [Engine.decodeSlice].
 */
public fun <C : Config, D : DecodeEstimate> decodeEngineSlice(
    input: ByteArray,
    output: ByteArray,
    engine: Engine<C, D>,
): Result<Int> = engine.decodeSlice(input, output)

/**
 * Returns a conservative estimate of the decoded size of [encodedLen] base64 symbols, rounded up
 * to the next group of 3 decoded bytes.
 *
 * The resulting length will be a safe choice for the size of a decode buffer, but may have up to
 * 2 trailing bytes that won't end up being needed.
 */
public fun decodedLenEstimate(encodedLen: Int): Int =
    STANDARD
        .internalDecodedLenEstimate(encodedLen)
        .decodedLenEstimate()
