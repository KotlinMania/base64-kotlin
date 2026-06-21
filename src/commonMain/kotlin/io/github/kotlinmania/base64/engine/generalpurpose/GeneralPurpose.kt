// port-lint: source engine/general_purpose/mod.rs
package io.github.kotlinmania.base64.engine.generalpurpose

import io.github.kotlinmania.base64.alphabet.Alphabet
import io.github.kotlinmania.base64.engine.Config
import io.github.kotlinmania.base64.engine.DecodeMetadata
import io.github.kotlinmania.base64.engine.DecodePaddingMode
import io.github.kotlinmania.base64.engine.Engine
import io.github.kotlinmania.base64.alphabet.STANDARD as STANDARD_ALPHABET
import io.github.kotlinmania.base64.alphabet.URL_SAFE as URL_SAFE_ALPHABET

internal const val INVALID_VALUE: Int = 255

/**
 * A general-purpose base64 engine.
 *
 * It uses no vector CPU instructions, so it works on every Kotlin target, and it is not
 * constant-time. For cryptographic keys, prefer a constant-time engine when one is available.
 */
public class GeneralPurpose(
    alphabet: Alphabet,
    private val config: GeneralPurposeConfig,
) : Engine<GeneralPurposeConfig, GeneralPurposeEstimate> {
    private val encodeTable: ByteArray = makeEncodeTable(alphabet)
    internal val decodeTable: IntArray = makeDecodeTable(alphabet)

    override fun internalEncode(input: ByteArray, output: ByteArray): Int {
        var inputIndex = 0
        var outputIndex = 0

        while (inputIndex + 3 <= input.size) {
            val b0 = input[inputIndex].unsigned()
            val b1 = input[inputIndex + 1].unsigned()
            val b2 = input[inputIndex + 2].unsigned()

            output[outputIndex] = encodeTable[b0 ushr 2]
            output[outputIndex + 1] = encodeTable[((b0 shl 4) or (b1 ushr 4)) and LOW_SIX_BITS]
            output[outputIndex + 2] = encodeTable[((b1 shl 2) or (b2 ushr 6)) and LOW_SIX_BITS]
            output[outputIndex + 3] = encodeTable[b2 and LOW_SIX_BITS]

            inputIndex += 3
            outputIndex += 4
        }

        when (input.size - inputIndex) {
            2 -> {
                val b0 = input[inputIndex].unsigned()
                val b1 = input[inputIndex + 1].unsigned()

                output[outputIndex] = encodeTable[b0 ushr 2]
                output[outputIndex + 1] = encodeTable[((b0 shl 4) or (b1 ushr 4)) and LOW_SIX_BITS]
                output[outputIndex + 2] = encodeTable[(b1 shl 2) and LOW_SIX_BITS]
                outputIndex += 3
            }

            1 -> {
                val b0 = input[inputIndex].unsigned()

                output[outputIndex] = encodeTable[b0 ushr 2]
                output[outputIndex + 1] = encodeTable[(b0 shl 4) and LOW_SIX_BITS]
                outputIndex += 2
            }
        }

        return outputIndex
    }

    override fun internalDecodedLenEstimate(inputLen: Int): GeneralPurposeEstimate =
        GeneralPurposeEstimate(inputLen)

    override fun internalDecode(
        input: ByteArray,
        output: ByteArray,
        decodeEstimate: GeneralPurposeEstimate,
    ): Result<DecodeMetadata> =
        decodeHelper(
            input = input,
            estimate = decodeEstimate,
            output = output,
            decodeTable = decodeTable,
            decodeAllowTrailingBits = config.decodeAllowTrailingBits,
            paddingMode = config.decodePaddingMode,
        )

    override fun config(): GeneralPurposeConfig = config
}

/** Contains configuration parameters for base64 encoding and decoding. */
public class GeneralPurposeConfig(
    private val shouldEncodePadding: Boolean = true,
    internal val decodeAllowTrailingBits: Boolean = false,
    internal val decodePaddingMode: DecodePaddingMode = DecodePaddingMode.RequireCanonical,
) : Config {
    /**
     * Create a new config based on this one with an updated padding setting.
     */
    public fun withEncodePadding(padding: Boolean): GeneralPurposeConfig =
        GeneralPurposeConfig(
            shouldEncodePadding = padding,
            decodeAllowTrailingBits = decodeAllowTrailingBits,
            decodePaddingMode = decodePaddingMode,
        )

    /**
     * Create a new config based on this one with an updated trailing-bit policy.
     */
    public fun withDecodeAllowTrailingBits(allow: Boolean): GeneralPurposeConfig =
        GeneralPurposeConfig(
            shouldEncodePadding = shouldEncodePadding,
            decodeAllowTrailingBits = allow,
            decodePaddingMode = decodePaddingMode,
        )

    /**
     * Create a new config based on this one with an updated padding mode.
     */
    public fun withDecodePaddingMode(mode: DecodePaddingMode): GeneralPurposeConfig =
        GeneralPurposeConfig(
            shouldEncodePadding = shouldEncodePadding,
            decodeAllowTrailingBits = decodeAllowTrailingBits,
            decodePaddingMode = mode,
        )

    override fun encodePadding(): Boolean = shouldEncodePadding

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneralPurposeConfig) return false
        return shouldEncodePadding == other.shouldEncodePadding &&
            decodeAllowTrailingBits == other.decodeAllowTrailingBits &&
            decodePaddingMode == other.decodePaddingMode
    }

    override fun hashCode(): Int {
        var result = shouldEncodePadding.hashCode()
        result = 31 * result + decodeAllowTrailingBits.hashCode()
        result = 31 * result + decodePaddingMode.hashCode()
        return result
    }

    override fun toString(): String =
        "GeneralPurposeConfig(" +
            "encodePadding=$shouldEncodePadding, " +
            "decodeAllowTrailingBits=$decodeAllowTrailingBits, " +
            "decodePaddingMode=$decodePaddingMode" +
            ")"
}

/** Include padding bytes when encoding and require canonical padding when decoding. */
public val PAD: GeneralPurposeConfig = GeneralPurposeConfig()

/** Do not add padding when encoding and require no padding when decoding. */
public val NO_PAD: GeneralPurposeConfig =
    GeneralPurposeConfig()
        .withEncodePadding(false)
        .withDecodePaddingMode(DecodePaddingMode.RequireNone)

/** A [GeneralPurpose] engine using the standard alphabet and [PAD] config. */
public val STANDARD: GeneralPurpose = GeneralPurpose(STANDARD_ALPHABET, PAD)

/** A [GeneralPurpose] engine using the standard alphabet and [NO_PAD] config. */
public val STANDARD_NO_PAD: GeneralPurpose = GeneralPurpose(STANDARD_ALPHABET, NO_PAD)

/** A [GeneralPurpose] engine using the URL-safe alphabet and [PAD] config. */
public val URL_SAFE: GeneralPurpose = GeneralPurpose(URL_SAFE_ALPHABET, PAD)

/** A [GeneralPurpose] engine using the URL-safe alphabet and [NO_PAD] config. */
public val URL_SAFE_NO_PAD: GeneralPurpose = GeneralPurpose(URL_SAFE_ALPHABET, NO_PAD)

private const val LOW_SIX_BITS: Int = 0x3F

private fun makeEncodeTable(alphabet: Alphabet): ByteArray {
    val encodeTable = ByteArray(64)
    var index = 0
    while (index < 64) {
        encodeTable[index] = alphabet.symbols[index]
        index += 1
    }
    return encodeTable
}

private fun makeDecodeTable(alphabet: Alphabet): IntArray {
    val decodeTable = IntArray(256) { INVALID_VALUE }
    var index = 0
    while (index < 64) {
        decodeTable[alphabet.symbols[index].unsigned()] = index
        index += 1
    }
    return decodeTable
}

internal fun Byte.unsigned(): Int = toInt() and 0xFF
