// port-lint: source src/encode.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.Engine

// Pre-port note (will be removed when general_purpose lands): the upstream module also exposes a
// set of deprecated free functions (`encode`, `encode_engine`, `encode_engine_string`,
// `encode_engine_slice`) and the `STANDARD` engine. Those are intentionally left untranslated
// here until `engine/general_purpose/mod.rs` is ported, since they reference
// `engine::general_purpose::STANDARD`.

/**
 * B64-encode and pad (if configured).
 *
 * This helper exists to avoid recalculating encodedSize, which is relatively expensive on short
 * inputs.
 *
 * [expectedEncodedSize] is the encoded size calculated for [input].
 *
 * [output] must be of size [expectedEncodedSize].
 *
 * All bytes in [output] will be written to since it is exactly the size of the output.
 */
internal fun <C : Config, D : DecodeEstimate> encodeWithPadding(
    input: ByteArray,
    output: ByteArray,
    engine: Engine<C, D>,
    expectedEncodedSize: Int,
) {
    check(expectedEncodedSize == output.size)

    val b64BytesWritten = engine.internalEncode(input, output)

    val paddingBytes = if (engine.config().encodePadding()) {
        addPadding(b64BytesWritten, output, b64BytesWritten)
    } else {
        0
    }

    val encodedBytes = b64BytesWritten + paddingBytes
    if (encodedBytes < b64BytesWritten) {
        error("Int overflow when calculating b64 length")
    }

    check(expectedEncodedSize == encodedBytes)
}

/**
 * Calculate the base64 encoded length for a given input length, optionally including any
 * appropriate padding bytes.
 *
 * Returns `null` if the encoded length can't be represented in `Int`. This will happen for
 * input lengths in approximately the top quarter of the range of `Int`.
 */
public fun encodedLen(bytesLen: Int, padding: Boolean): Int? {
    val rem = bytesLen % 3

    val completeInputChunks = bytesLen / 3
    // checked multiply: fail if the result overflows Int
    val completeChunkOutput =
        if (completeInputChunks <= Int.MAX_VALUE / 4) {
            completeInputChunks * 4
        } else {
            return null
        }

    return if (rem > 0) {
        if (padding) {
            if (completeChunkOutput <= Int.MAX_VALUE - 4) completeChunkOutput + 4 else null
        } else {
            val encodedRem = when (rem) {
                1 -> 2
                // only other possible remainder is 2
                else -> 3
            }
            if (completeChunkOutput <= Int.MAX_VALUE - encodedRem) {
                completeChunkOutput + encodedRem
            } else {
                null
            }
        }
    } else {
        completeChunkOutput
    }
}

/**
 * Write padding characters.
 * [unpaddedOutputLen] is the size of the unpadded but base64 encoded data.
 * [output] is the slice where padding should be written, of length at least 2 (starting at
 * [outputOffset]).
 *
 * Returns the number of padding bytes written.
 */
internal fun addPadding(unpaddedOutputLen: Int, output: ByteArray, outputOffset: Int = 0): Int {
    val padBytes = (4 - (unpaddedOutputLen % 4)) % 4
    // for just a couple bytes, this has better performance than using
    // .fill(), or iterating over mutable refs, which call memset()
    for (i in 0 until padBytes) {
        output[outputOffset + i] = PAD_BYTE
    }

    return padBytes
}

/** Errors that can occur while encoding into a slice. */
public sealed class EncodeSliceError : Exception() {
    /** The provided slice is too small. */
    public object OutputSliceTooSmall : EncodeSliceError() {
        override val message: String = "Output slice too small"
    }

    override fun toString(): String = message ?: super.toString()
}
