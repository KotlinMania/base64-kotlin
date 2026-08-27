// port-lint: source engine/naive.rs
package io.github.kotlinmania.base64.engine

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.alphabet.Alphabet
import io.github.kotlinmania.base64.engine.generalpurpose.INVALID_VALUE
import io.github.kotlinmania.base64.engine.generalpurpose.completeQuadsLen
import io.github.kotlinmania.base64.engine.generalpurpose.decodeSuffix
import io.github.kotlinmania.base64.engine.generalpurpose.unsigned

internal class Naive(
    alphabet: Alphabet,
    private val config: NaiveConfig,
) : Engine<NaiveConfig, NaiveEstimate> {
    private val encodeTable: ByteArray = encodeTable(alphabet)
    private val decodeTable: IntArray = decodeTable(alphabet)

    override fun internalEncode(input: ByteArray, output: ByteArray): Int {
        var inputIndex = 0
        var outputIndex = 0
        val completeChunkLen = input.size - input.size % ENCODE_INPUT_CHUNK_SIZE

        while (inputIndex < completeChunkLen) {
            val chunkInt =
                (input[inputIndex].unsigned() shl 16) or
                    (input[inputIndex + 1].unsigned() shl 8) or
                    input[inputIndex + 2].unsigned()

            output[outputIndex] = encodeTable[chunkInt ushr 18]
            output[outputIndex + 1] = encodeTable[(chunkInt ushr 12) and LOW_SIX_BITS]
            output[outputIndex + 2] = encodeTable[(chunkInt ushr 6) and LOW_SIX_BITS]
            output[outputIndex + 3] = encodeTable[chunkInt and LOW_SIX_BITS]

            inputIndex += ENCODE_INPUT_CHUNK_SIZE
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

    override fun internalDecodedLenEstimate(inputLen: Int): NaiveEstimate =
        NaiveEstimate(inputLen)

    override fun internalDecode(
        input: ByteArray,
        output: ByteArray,
        decodeEstimate: NaiveEstimate,
    ): Result<DecodeMetadata> {
        val completeNonterminalQuadsLen =
            completeQuadsLen(
                input = input,
                inputLenRem = decodeEstimate.rem,
                outputLen = output.size,
                decodeTable = decodeTable,
            ).getOrElse { return Result.failure(it) }

        var inputIndex = 0
        var outputIndex = 0
        while (inputIndex < completeNonterminalQuadsLen) {
            val decodedInt =
                decodeByteIntoInt(inputIndex, input[inputIndex]).getOrElse {
                    return Result.failure(DecodeSliceError.fromDecodeError(it as DecodeError))
                } shl 18 or
                    (
                        decodeByteIntoInt(inputIndex + 1, input[inputIndex + 1]).getOrElse {
                            return Result.failure(DecodeSliceError.fromDecodeError(it as DecodeError))
                        } shl 12
                    ) or
                    (
                        decodeByteIntoInt(inputIndex + 2, input[inputIndex + 2]).getOrElse {
                            return Result.failure(DecodeSliceError.fromDecodeError(it as DecodeError))
                        } shl 6
                    ) or
                    decodeByteIntoInt(inputIndex + 3, input[inputIndex + 3]).getOrElse {
                        return Result.failure(DecodeSliceError.fromDecodeError(it as DecodeError))
                    }

            output[outputIndex] = (decodedInt ushr 16).toByte()
            output[outputIndex + 1] = (decodedInt ushr 8).toByte()
            output[outputIndex + 2] = decodedInt.toByte()

            inputIndex += DECODE_INPUT_CHUNK_SIZE
            outputIndex += 3
        }

        return decodeSuffix(
            input = input,
            inputIndex = completeNonterminalQuadsLen,
            output = output,
            outputIndex = completeNonterminalQuadsLen / 4 * 3,
            decodeTable = decodeTable,
            decodeAllowTrailingBits = config.decodeAllowTrailingBits,
            paddingMode = config.decodePaddingMode,
        )
    }

    override fun config(): NaiveConfig = config

    private fun decodeByteIntoInt(
        offset: Int,
        byte: Byte,
    ): Result<Int> {
        val decoded = decodeTable[byte.unsigned()]
        return if (decoded == INVALID_VALUE) {
            Result.failure(DecodeError.InvalidByte(offset, byte))
        } else {
            Result.success(decoded)
        }
    }

    internal fun decodeByteIntoU32(
        offset: Int,
        byte: Byte,
    ): Result<Int> = decodeByteIntoInt(offset, byte)

    internal companion object {
        fun new(
            alphabet: Alphabet,
            config: NaiveConfig,
        ): Naive = Naive(alphabet, config)

        const val ENCODE_INPUT_CHUNK_SIZE = 3
        const val DECODE_INPUT_CHUNK_SIZE = 4
        const val LOW_SIX_BITS = 0x3F
    }
}

internal class NaiveEstimate(
    inputLen: Int,
) : DecodeEstimate {
    internal val rem: Int = inputLen % 4
    private val completeChunkLen: Int = inputLen - rem

    override fun decodedLenEstimate(): Int =
        (completeChunkLen / 4 + if (rem > 0) 1 else 0) * 3
}

internal class NaiveConfig(
    private val shouldEncodePadding: Boolean,
    internal val decodeAllowTrailingBits: Boolean,
    internal val decodePaddingMode: DecodePaddingMode,
) : Config {
    override fun encodePadding(): Boolean = shouldEncodePadding
}

private fun encodeTable(alphabet: Alphabet): ByteArray {
    val table = ByteArray(64)
    for (index in 0 until 64) {
        table[index] = alphabet.symbols[index]
    }
    return table
}

private fun decodeTable(alphabet: Alphabet): IntArray {
    val table = IntArray(256) { INVALID_VALUE }
    for (index in 0 until 64) {
        table[alphabet.symbols[index].unsigned()] = index
    }
    return table
}
