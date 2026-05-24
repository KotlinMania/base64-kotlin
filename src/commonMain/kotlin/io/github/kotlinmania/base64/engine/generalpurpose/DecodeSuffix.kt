// port-lint: source engine/general_purpose/decode_suffix.rs
package io.github.kotlinmania.base64.engine.generalpurpose

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.DecodeSliceError
import io.github.kotlinmania.base64.PAD_BYTE
import io.github.kotlinmania.base64.engine.DecodeMetadata
import io.github.kotlinmania.base64.engine.DecodePaddingMode

internal fun decodeSuffix(
    input: ByteArray,
    inputIndex: Int,
    output: ByteArray,
    outputIndex: Int,
    decodeTable: IntArray,
    decodeAllowTrailingBits: Boolean,
    paddingMode: DecodePaddingMode,
): Result<DecodeMetadata> {
    var writeIndex = outputIndex
    var morselsInLeftover = 0
    var paddingBytesCount = 0
    var firstPaddingOffset = 0
    var lastSymbol: Byte = 0
    val morsels = IntArray(4)

    var leftoverIndex = 0
    while (inputIndex + leftoverIndex < input.size) {
        val byte = input[inputIndex + leftoverIndex]

        if (byte == PAD_BYTE) {
            if (leftoverIndex < 2) {
                return Result.failure(
                    DecodeSliceError.fromDecodeError(
                        DecodeError.InvalidByte(inputIndex + leftoverIndex, byte),
                    ),
                )
            }

            if (paddingBytesCount == 0) {
                firstPaddingOffset = leftoverIndex
            }
            paddingBytesCount += 1
            leftoverIndex += 1
            continue
        }

        if (paddingBytesCount > 0) {
            return Result.failure(
                DecodeSliceError.fromDecodeError(
                    DecodeError.InvalidByte(inputIndex + firstPaddingOffset, PAD_BYTE),
                ),
            )
        }

        lastSymbol = byte
        val morsel = decodeTable[byte.unsigned()]
        if (morsel == INVALID_VALUE) {
            return Result.failure(
                DecodeSliceError.fromDecodeError(
                    DecodeError.InvalidByte(inputIndex + leftoverIndex, byte),
                ),
            )
        }

        morsels[morselsInLeftover] = morsel
        morselsInLeftover += 1
        leftoverIndex += 1
    }

    if (input.isNotEmpty() && morselsInLeftover < 2) {
        return Result.failure(
            DecodeSliceError.fromDecodeError(DecodeError.InvalidLength(inputIndex + morselsInLeftover)),
        )
    }

    when (paddingMode) {
        DecodePaddingMode.Indifferent -> Unit
        DecodePaddingMode.RequireCanonical -> {
            if ((paddingBytesCount + morselsInLeftover) % 4 != 0) {
                return Result.failure(DecodeSliceError.fromDecodeError(DecodeError.InvalidPadding))
            }
        }

        DecodePaddingMode.RequireNone -> {
            if (paddingBytesCount > 0) {
                return Result.failure(DecodeSliceError.fromDecodeError(DecodeError.InvalidPadding))
            }
        }
    }

    val leftoverBytesToAppend = morselsInLeftover * 6 / 8
    var leftoverNum = (morsels[0] shl 26) or
        (morsels[1] shl 20) or
        (morsels[2] shl 14) or
        (morsels[3] shl 8)

    val mask = -1 ushr (leftoverBytesToAppend * 8)
    if (!decodeAllowTrailingBits && (leftoverNum and mask) != 0) {
        return Result.failure(
            DecodeSliceError.fromDecodeError(
                DecodeError.InvalidLastSymbol(inputIndex + morselsInLeftover - 1, lastSymbol),
            ),
        )
    }

    repeat(leftoverBytesToAppend) {
        if (writeIndex >= output.size) {
            return Result.failure(DecodeSliceError.OutputSliceTooSmall)
        }
        output[writeIndex] = (leftoverNum ushr 24).toByte()
        leftoverNum = leftoverNum shl 8
        writeIndex += 1
    }

    return Result.success(
        DecodeMetadata.new(
            decodedBytes = writeIndex,
            paddingIndex = if (paddingBytesCount > 0) inputIndex + firstPaddingOffset else null,
        ),
    )
}
