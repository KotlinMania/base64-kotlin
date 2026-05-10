// port-lint: source src/engine/mod.rs
package io.github.kotlinmania.base64.engine

import io.github.kotlinmania.base64.ChunkedEncoder
import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.EncodeSliceError
import io.github.kotlinmania.base64.StringSink
import io.github.kotlinmania.base64.encodeWithPadding
import io.github.kotlinmania.base64.encodedLen

/**
 * Provides the [Engine] abstraction and out of the box implementations.
 */

// pub use general_purpose::{GeneralPurpose, GeneralPurposeConfig};
//
// Re-export tracking — callers should reference
// io.github.kotlinmania.base64.engine.general_purpose.GeneralPurpose and
// io.github.kotlinmania.base64.engine.general_purpose.GeneralPurposeConfig directly.

/**
 * An [Engine] provides low-level encoding and decoding operations that all other higher-level parts
 * of the API use. Users of the library will generally not need to implement this.
 *
 * Different implementations offer different characteristics. The library currently ships with
 * `GeneralPurpose` that offers good speed and works on any CPU, with more choices coming later,
 * like a constant-time one when side channel resistance is called for, and vendor-specific
 * vectorized ones for more speed.
 *
 * See `general_purpose.STANDARD_NO_PAD` if you just want standard base64. Otherwise, when possible,
 * it's recommended to store the engine in a `val` so that references to it won't pose any
 * lifetime issues, and to avoid repeating the cost of engine setup.
 *
 * Since almost nobody will need to implement `Engine`, docs for internal methods are hidden.
 */
// When adding an implementation of Engine, include them in the engine test suite:
// - add an implementation of [engine.tests.EngineWrapper]
// - add the implementation to the `allEngines` helper
// All tests run on all engines listed in the helper.
public interface Engine<C : Config, D : DecodeEstimate> {

    /**
     * This is not meant to be called directly; it is only for [Engine] implementors.
     * See the other `encode*` functions on this interface.
     *
     * Encode the [input] bytes into the [output] buffer based on the mapping in `encodeTable`.
     *
     * [output] will be long enough to hold the encoded data.
     *
     * Returns the number of bytes written.
     *
     * No padding should be written; that is handled separately.
     *
     * Must not write any bytes into the output slice other than the encoded data.
     */
    public fun internalEncode(input: ByteArray, output: ByteArray): Int

    /**
     * This is not meant to be called directly; it is only for [Engine] implementors.
     *
     * As an optimization to prevent the decoded length from being calculated twice, it is
     * sometimes helpful to have a conservative estimate of the decoded size before doing the
     * decoding, so this calculation is done separately and passed to [decode] as needed.
     */
    public fun internalDecodedLenEstimate(inputLen: Int): D

    /**
     * This is not meant to be called directly; it is only for [Engine] implementors.
     * See the other `decode*` functions on this interface.
     *
     * Decode [input] base64 bytes into the [output] buffer.
     *
     * [decodeEstimate] is the result of [internalDecodedLenEstimate], which is passed in to avoid
     * calculating it again (expensive on short inputs).
     *
     * Each complete 4-byte chunk of encoded data decodes to 3 bytes of decoded data, but this
     * function must also handle the final possibly partial chunk.
     * If the input length is not a multiple of 4, or uses padding bytes to reach a multiple of 4,
     * the trailing 2 or 3 bytes must decode to 1 or 2 bytes, respectively, as per the
     * [RFC](https://tools.ietf.org/html/rfc4648#section-3.5).
     *
     * Decoding must not write any bytes into the output slice other than the decoded data.
     *
     * Non-canonical trailing bits in the final tokens or non-canonical padding must be reported as
     * errors unless the engine is configured otherwise.
     */
    public fun internalDecode(
        input: ByteArray,
        output: ByteArray,
        decodeEstimate: D,
    ): Result<DecodeMetadata>

    /** Returns the config for this engine. */
    public fun config(): C

    /**
     * Encode arbitrary octets as base64 using the provided [Engine].
     * Returns a [String].
     *
     * # Example
     *
     * ```kotlin
     * import io.github.kotlinmania.base64.engine.general_purpose
     *
     * val b64 = general_purpose.STANDARD.encode("hello world~".encodeToByteArray())
     * println(b64)
     *
     * val customEngine: GeneralPurpose =
     *     GeneralPurpose(alphabet.URL_SAFE, general_purpose.NO_PAD)
     *
     * val b64Url = customEngine.encode("hello internet~".encodeToByteArray())
     * ```
     */
    public fun encode(input: ByteArray): String {
        val encodedSize = encodedLen(input.size, config().encodePadding())
            ?: error("integer overflow when calculating buffer size")

        val buf = ByteArray(encodedSize)

        encodeWithPadding(input, buf, this, encodedSize)

        return buf.decodeToString()
    }

    /**
     * Encode arbitrary octets as base64 into a supplied [StringBuilder].
     * Writes into the supplied [StringBuilder], which may allocate if its internal buffer isn't
     * big enough.
     *
     * # Example
     *
     * ```kotlin
     * import io.github.kotlinmania.base64.engine.general_purpose
     *
     * val customEngine: GeneralPurpose =
     *     GeneralPurpose(alphabet.URL_SAFE, general_purpose.NO_PAD)
     *
     * val buf = StringBuilder()
     * general_purpose.STANDARD.encodeString("hello world~".encodeToByteArray(), buf)
     * println(buf)
     *
     * buf.clear()
     * customEngine.encodeString("hello internet~".encodeToByteArray(), buf)
     * println(buf)
     * ```
     */
    public fun encodeString(input: ByteArray, outputBuf: StringBuilder) {
        val sink = StringSink(outputBuf)

        ChunkedEncoder(this)
            .encode(input, sink)
    }

    /**
     * Encode arbitrary octets as base64 into a supplied slice.
     * Writes into the supplied output buffer.
     *
     * This is useful if you wish to avoid allocation entirely (e.g. encoding into a stack-resident
     * or statically-allocated buffer).
     *
     * # Example
     *
     * ```kotlin
     * import io.github.kotlinmania.base64.engine.general_purpose
     *
     * val s = "hello internet!".encodeToByteArray()
     * // make sure we'll have a slice big enough for base64 + padding
     * val buf = ByteArray(s.size * 4 / 3 + 4)
     *
     * val bytesWritten = general_purpose.STANDARD.encodeSlice(s, buf).getOrThrow()
     *
     * // copy out just what was written
     * val written = buf.copyOf(bytesWritten)
     *
     * check(s.contentEquals(general_purpose.STANDARD.decode(written).getOrThrow()))
     * ```
     */
    public fun encodeSlice(input: ByteArray, outputBuf: ByteArray): Result<Int> {
        val encodedSize = encodedLen(input.size, config().encodePadding())
            ?: error("Int overflow when calculating buffer size")

        if (outputBuf.size < encodedSize) {
            return Result.failure(EncodeSliceError.OutputSliceTooSmall)
        }

        encodeWithPadding(input, outputBuf, this, encodedSize)

        return Result.success(encodedSize)
    }

    /**
     * Decode the input into a new [ByteArray].
     *
     * # Example
     *
     * ```kotlin
     * import io.github.kotlinmania.base64.engine.general_purpose
     *
     * val bytes = general_purpose.STANDARD
     *     .decode("aGVsbG8gd29ybGR+Cg==".encodeToByteArray()).getOrThrow()
     * println(bytes.toList())
     *
     * // custom engine setup
     * val bytesUrl = GeneralPurpose(alphabet.URL_SAFE, general_purpose.NO_PAD)
     *     .decode("aGVsbG8gaW50ZXJuZXR-Cg".encodeToByteArray()).getOrThrow()
     * println(bytesUrl.toList())
     * ```
     */
    public fun decode(input: ByteArray): Result<ByteArray> {
        val estimate = internalDecodedLenEstimate(input.size)
        val buffer = ByteArray(estimate.decodedLenEstimate())

        return internalDecode(input, buffer, estimate)
            .fold(
                onSuccess = { dm -> Result.success(buffer.copyOf(dm.decodedLen)) },
                onFailure = { e ->
                    when (e) {
                        is DecodeSliceError.DecodeErrorVariant -> Result.failure(e.error)
                        is DecodeSliceError.OutputSliceTooSmall ->
                            error("ByteArray is sized conservatively")
                        else -> Result.failure(e)
                    }
                },
            )
    }

    /**
     * Decode the [input] into the supplied [buffer].
     *
     * Writes into the supplied [MutableList], which may allocate if its internal buffer isn't big
     * enough. Returns a [Result] containing [Unit].
     *
     * # Example
     *
     * ```kotlin
     * import io.github.kotlinmania.base64.engine.general_purpose
     *
     * val customEngine: GeneralPurpose =
     *     GeneralPurpose(alphabet.URL_SAFE, general_purpose.PAD)
     *
     * val buffer = mutableListOf<Byte>()
     * // with the default engine
     * general_purpose.STANDARD
     *     .decodeVec("aGVsbG8gd29ybGR+Cg==".encodeToByteArray(), buffer).getOrThrow()
     * println(buffer)
     *
     * buffer.clear()
     *
     * // with a custom engine
     * customEngine.decodeVec(
     *     "aGVsbG8gaW50ZXJuZXR-Cg==".encodeToByteArray(),
     *     buffer,
     * ).getOrThrow()
     * println(buffer)
     * ```
     */
    public fun decodeVec(input: ByteArray, buffer: MutableList<Byte>): Result<Unit> {
        val startingOutputLen = buffer.size
        val estimate = internalDecodedLenEstimate(input.size)

        val totalLenEstimate = estimate.decodedLenEstimate() + startingOutputLen
        if (totalLenEstimate < startingOutputLen) {
            error("Overflow when calculating output buffer length")
        }

        val tempBuf = ByteArray(estimate.decodedLenEstimate())

        return internalDecode(input, tempBuf, estimate)
            .fold(
                onSuccess = { dm ->
                    for (i in 0 until dm.decodedLen) {
                        buffer.add(tempBuf[i])
                    }
                    Result.success(Unit)
                },
                onFailure = { e ->
                    when (e) {
                        is DecodeSliceError.DecodeErrorVariant -> Result.failure(e.error)
                        is DecodeSliceError.OutputSliceTooSmall ->
                            error("ByteArray is sized conservatively")
                        else -> Result.failure(e)
                    }
                },
            )
    }

    /**
     * Decode the input into the provided output slice.
     *
     * Returns the number of bytes written to the slice, or an error if [output] is smaller than
     * the estimated decoded length.
     *
     * This will not write any bytes past exactly what is decoded (no stray garbage bytes at the
     * end).
     *
     * See [io.github.kotlinmania.base64.decodedLenEstimate] for calculating buffer sizes.
     *
     * See [decodeSliceUnchecked] for a version that throws instead of returning an error if the
     * output buffer is too small.
     */
    public fun decodeSlice(input: ByteArray, output: ByteArray): Result<Int> {
        return internalDecode(
            input,
            output,
            internalDecodedLenEstimate(input.size),
        ).map { it.decodedLen }
    }

    /**
     * Decode the input into the provided output slice.
     *
     * Returns the number of bytes written to the slice.
     *
     * This will not write any bytes past exactly what is decoded (no stray garbage bytes at the
     * end).
     *
     * See [io.github.kotlinmania.base64.decodedLenEstimate] for calculating buffer sizes.
     *
     * See [decodeSlice] for a version that returns an error instead of throwing if the output
     * buffer is too small.
     *
     * # Throws
     *
     * Throws if the provided output buffer is too small for the decoded data.
     */
    public fun decodeSliceUnchecked(input: ByteArray, output: ByteArray): Result<Int> {
        return internalDecode(
            input,
            output,
            internalDecodedLenEstimate(input.size),
        ).fold(
            onSuccess = { Result.success(it.decodedLen) },
            onFailure = { e ->
                when (e) {
                    is DecodeSliceError.DecodeErrorVariant -> Result.failure(e.error)
                    is DecodeSliceError.OutputSliceTooSmall -> error("Output slice is too small")
                    else -> Result.failure(e)
                }
            },
        )
    }
}

/** The minimal level of configuration that engines must support. */
public interface Config {
    /**
     * Returns `true` if padding should be added after the encoded output.
     *
     * Padding is added outside the engine's encode() since the engine may be used
     * to encode only a chunk of the overall output, so it can't always know when
     * the output is "done" and would therefore need padding (if configured).
     */
    // It could be provided as a separate parameter when encoding, but that feels like
    // leaking an implementation detail to the user, and it's hopefully more convenient
    // to have to only pass one thing (the engine) to any part of the API.
    public fun encodePadding(): Boolean
}

/**
 * The decode estimate used by an engine implementation. Users do not need to interact with this;
 * it is only for engine implementors.
 *
 * Implementors may store relevant data here when constructing this to avoid having to calculate
 * them again during actual decoding.
 */
public interface DecodeEstimate {
    /**
     * Returns a conservative (err on the side of too big) estimate of the decoded length to use
     * for pre-allocating buffers, etc.
     *
     * The estimate must be no larger than the next largest complete triple of decoded bytes.
     * That is, the final quad of tokens to decode may be assumed to be complete with no padding.
     */
    public fun decodedLenEstimate(): Int
}

/**
 * Controls how pad bytes are handled when decoding.
 *
 * Each [Engine] must support at least the behavior indicated by
 * [DecodePaddingMode.RequireCanonical], and may support other modes.
 */
public enum class DecodePaddingMode {
    /** Canonical padding is allowed, but any fewer padding bytes than that is also allowed. */
    Indifferent,

    /** Padding must be canonical (0, 1, or 2 `=` as needed to produce a 4 byte suffix). */
    RequireCanonical,

    /** Padding must be absent — for when you want predictable padding, without any wasted bytes. */
    RequireNone,
}

/** Metadata about the result of a decode operation. */
public class DecodeMetadata internal constructor(
    /** Number of decoded bytes output. */
    internal val decodedLen: Int,
    /** Offset of the first padding byte in the input, if any. */
    internal val paddingOffset: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodeMetadata) return false
        return decodedLen == other.decodedLen && paddingOffset == other.paddingOffset
    }

    override fun hashCode(): Int {
        var result = decodedLen
        result = 31 * result + (paddingOffset ?: -1)
        return result
    }

    override fun toString(): String = "DecodeMetadata(decodedLen=$decodedLen, paddingOffset=$paddingOffset)"

    internal companion object {
        internal fun new(decodedBytes: Int, paddingIndex: Int?): DecodeMetadata =
            DecodeMetadata(decodedLen = decodedBytes, paddingOffset = paddingIndex)
    }
}
