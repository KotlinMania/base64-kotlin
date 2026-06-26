// port-lint: tests tests.rs
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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CoreTest {
    @Test
    fun roundtripRandomConfigShort() {
        roundtripRandomConfig(0 until 50, 10_000, Random(0xB640))
    }

    @Test
    fun roundtripRandomConfigLong() {
        roundtripRandomConfig(0 until 1000, 10_000, Random(0xB641))
    }

    private fun assertEncodeSanity(
        encoded: String,
        padded: Boolean,
        inputLen: Int,
    ) {
        val inputRem = inputLen % 3
        val expectedPaddingLen =
            if (inputRem > 0) {
                if (padded) 3 - inputRem else 0
            } else {
                0
            }

        val expectedEncodedLen = encodedLen(inputLen, padded)

        assertEquals(expectedEncodedLen, encoded.length)
        assertEquals(expectedPaddingLen, encoded.count { it == '=' })
        assertEquals(encoded, encoded.encodeToByteArray().decodeToString())
    }

    private fun roundtripRandomConfig(
        inputLenRange: IntRange,
        iterations: Int,
        rng: Random,
    ) {
        val encodedBuf = StringBuilder()

        repeat(iterations) {
            encodedBuf.clear()

            val inputLen = rng.nextInt(inputLenRange.first, inputLenRange.last + 1)
            val engine = randomEngine(rng)
            val inputBuf = ByteArray(inputLen)
            rng.nextBytes(inputBuf)

            engine.encodeString(inputBuf, encodedBuf)

            assertEncodeSanity(encodedBuf.toString(), engine.config().encodePadding(), inputLen)
            assertContentEquals(inputBuf, engine.decode(encodedBuf.toString().encodeToByteArray()).getOrThrow())
        }
    }

    private fun randomConfig(rng: Random): GeneralPurposeConfig {
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

    private fun randomAlphabet(rng: Random): Alphabet = ALPHABETS[rng.nextInt(ALPHABETS.size)]

    private fun randomEngine(rng: Random): GeneralPurpose =
        GeneralPurpose(randomAlphabet(rng), randomConfig(rng))

    private companion object {
        val ALPHABETS: List<Alphabet> =
            listOf(
                URL_SAFE,
                STANDARD,
                CRYPT,
                BCRYPT,
                IMAP_MUTF7,
                BIN_HEX,
            )
    }
}
