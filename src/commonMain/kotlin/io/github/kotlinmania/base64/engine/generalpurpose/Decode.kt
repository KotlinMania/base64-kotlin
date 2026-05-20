// port-lint: source engine/general_purpose/decode.rs
package io.github.kotlinmania.base64.engine.generalpurpose

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.PAD_BYTE
import io.github.kotlinmania.base64.engine.DecodeEstimate
import io.github.kotlinmania.base64.engine.DecodeMetadata
import io.github.kotlinmania.base64.engine.DecodePaddingMode

/** The decoded-size estimate used by the general-purpose engine. */
public class GeneralPurposeEstimate internal constructor(encodedLen: Int) : DecodeEstimate {
    internal val rem: Int = encodedLen % 4
    private val conservativeDecodedLen: Int = (encodedLen / 4 + if (rem > 0) 1 else 0) * 3

    override fun decodedLenEstimate(): Int = conservativeDecodedLen
}

internal fun decodeHelper(
    input: ByteArray,
    estimate: GeneralPurposeEstimate,
    output: ByteArray,
    decodeTable: IntArray,
    decodeAllowTrailingBits: Boolean,
    paddingMode: DecodePaddingMode,
): Result<DecodeMetadata> {
    val completeQuadsLen = completeQuadsLen(
        input = input,
        inputLenRem = estimate.rem,
        outputLen = output.size,
        decodeTable = decodeTable,
    ).getOrElse { return Result.failure(it) }

    val unrolledInputChunkSize = 32
    val unrolledOutputChunkSize = unrolledInputChunkSize / 4 * 3
    val completeQuadsAfterUnrolledChunksLen = completeQuadsLen % unrolledInputChunkSize
    val unrolledLoopLen = completeQuadsLen - completeQuadsAfterUnrolledChunksLen

    var inputIndex = 0
    var outputIndex = 0
    while (inputIndex < unrolledLoopLen) {
        val chunkError = decodeChunk8(input, inputIndex, decodeTable, output, outputIndex)
            ?: decodeChunk8(input, inputIndex + 8, decodeTable, output, outputIndex + 6)
            ?: decodeChunk8(input, inputIndex + 16, decodeTable, output, outputIndex + 12)
            ?: decodeChunk8(input, inputIndex + 24, decodeTable, output, outputIndex + 18)
        if (chunkError != null) {
            return Result.failure(DecodeSliceError.fromDecodeError(chunkError))
        }

        inputIndex += unrolledInputChunkSize
        outputIndex += unrolledOutputChunkSize
    }

    while (inputIndex < completeQuadsLen) {
        val chunkError = decodeChunk4(input, inputIndex, decodeTable, output, outputIndex)
        if (chunkError != null) {
            return Result.failure(DecodeSliceError.fromDecodeError(chunkError))
        }

        inputIndex += 4
        outputIndex += 3
    }

    return decodeSuffix(
        input = input,
        inputIndex = completeQuadsLen,
        output = output,
        outputIndex = completeQuadsLen / 4 * 3,
        decodeTable = decodeTable,
        decodeAllowTrailingBits = decodeAllowTrailingBits,
        paddingMode = paddingMode,
    )
}

internal fun completeQuadsLen(
    input: ByteArray,
    inputLenRem: Int,
    outputLen: Int,
    decodeTable: IntArray,
): Result<Int> {
    if (inputLenRem == 1) {
        val lastByte = input[input.size - 1]
        if (lastByte != PAD_BYTE && decodeTable[lastByte.unsigned()] == INVALID_VALUE) {
            return Result.failure(
                DecodeSliceError.fromDecodeError(DecodeError.InvalidByte(input.size - 1, lastByte)),
            )
        }
    }

    val completeQuadsLen = (input.size - inputLenRem)
        .coerceAtLeast(0)
        .let { if (inputLenRem == 0) (it - 4).coerceAtLeast(0) else it }

    if (outputLen < completeQuadsLen / 4 * 3) {
        return Result.failure(DecodeSliceError.OutputSliceTooSmall)
    }

    return Result.success(completeQuadsLen)
}

private fun decodeChunk8(
    input: ByteArray,
    indexAtStartOfInput: Int,
    decodeTable: IntArray,
    output: ByteArray,
    outputIndex: Int,
): DecodeError? {
    var accum = 0L
    var index = 0
    while (index < 8) {
        val byte = input[indexAtStartOfInput + index]
        val morsel = decodeTable[byte.unsigned()]
        if (morsel == INVALID_VALUE) {
            return DecodeError.InvalidByte(indexAtStartOfInput + index, byte)
        }
        accum = accum or (morsel.toLong() shl (58 - index * 6))
        index += 1
    }

    output[outputIndex] = (accum ushr 56).toByte()
    output[outputIndex + 1] = (accum ushr 48).toByte()
    output[outputIndex + 2] = (accum ushr 40).toByte()
    output[outputIndex + 3] = (accum ushr 32).toByte()
    output[outputIndex + 4] = (accum ushr 24).toByte()
    output[outputIndex + 5] = (accum ushr 16).toByte()

    return null
}

private fun decodeChunk4(
    input: ByteArray,
    indexAtStartOfInput: Int,
    decodeTable: IntArray,
    output: ByteArray,
    outputIndex: Int,
): DecodeError? {
    var accum = 0
    var index = 0
    while (index < 4) {
        val byte = input[indexAtStartOfInput + index]
        val morsel = decodeTable[byte.unsigned()]
        if (morsel == INVALID_VALUE) {
            return DecodeError.InvalidByte(indexAtStartOfInput + index, byte)
        }
        accum = accum or (morsel shl (26 - index * 6))
        index += 1
    }

    output[outputIndex] = (accum ushr 24).toByte()
    output[outputIndex + 1] = (accum ushr 16).toByte()
    output[outputIndex + 2] = (accum ushr 8).toByte()

    return null
}
