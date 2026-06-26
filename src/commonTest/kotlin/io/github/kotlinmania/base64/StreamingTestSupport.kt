package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.alphabet.Alphabet
import io.github.kotlinmania.base64.alphabet.BCRYPT
import io.github.kotlinmania.base64.alphabet.BIN_HEX
import io.github.kotlinmania.base64.alphabet.CRYPT
import io.github.kotlinmania.base64.alphabet.IMAP_MUTF7
import io.github.kotlinmania.base64.alphabet.STANDARD
import io.github.kotlinmania.base64.alphabet.URL_SAFE
import io.github.kotlinmania.base64.engine.DecodePaddingMode
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurpose
import io.github.kotlinmania.base64.engine.generalpurpose.GeneralPurposeConfig
import io.github.kotlinmania.base64.io.ByteReader
import io.github.kotlinmania.base64.io.ByteWriter
import io.github.kotlinmania.base64.io.StreamError
import io.github.kotlinmania.base64.io.StreamErrorKind
import kotlin.random.Random

internal fun randomConfig(rng: Random): GeneralPurposeConfig {
    val mode =
        when (rng.nextInt(3)) {
            0 -> DecodePaddingMode.Indifferent
            1 -> DecodePaddingMode.RequireCanonical
            else -> DecodePaddingMode.RequireNone
        }

    return GeneralPurposeConfig()
        .withEncodePadding(
            when (mode) {
                DecodePaddingMode.Indifferent -> rng.nextBoolean()
                DecodePaddingMode.RequireCanonical -> true
                DecodePaddingMode.RequireNone -> false
            },
        ).withDecodePaddingMode(mode)
        .withDecodeAllowTrailingBits(rng.nextBoolean())
}

internal fun randomAlphabet(rng: Random): Alphabet = ALPHABETS[rng.nextInt(ALPHABETS.size)]

internal fun randomEngine(rng: Random): GeneralPurpose =
    GeneralPurpose(randomAlphabet(rng), randomConfig(rng))

internal fun ByteReader.readToEnd(output: MutableList<Byte>): Result<Int> {
    val buffer = ByteArray(1024)
    var total = 0
    while (true) {
        val read = read(buffer).getOrElse { return Result.failure(it) }
        if (read == 0) {
            return Result.success(total)
        }
        for (index in 0 until read) {
            output.add(buffer[index])
        }
        total += read
    }
}

internal fun ByteWriter.writeAll(buffer: ByteArray): Result<Unit> {
    var consumed = 0
    while (consumed < buffer.size) {
        val written =
            write(buffer, consumed, buffer.size - consumed).getOrElse {
                return Result.failure(it)
            }
        consumed += written
    }
    return Result.success(Unit)
}

internal fun Throwable.decodeErrorOrNull(): DecodeError? =
    (this as? StreamError)?.inner as? DecodeError

internal fun interrupted(): StreamError =
    StreamError(StreamErrorKind.Interrupted, "interrupted")

private val ALPHABETS: List<Alphabet> =
    listOf(
        URL_SAFE,
        STANDARD,
        CRYPT,
        BCRYPT,
        IMAP_MUTF7,
        BIN_HEX,
    )
