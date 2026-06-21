// port-lint: source engine/general_purpose/decode.rs
package io.github.kotlinmania.base64.engine.generalpurpose

import io.github.kotlinmania.base64.DecodeError
import io.github.kotlinmania.base64.prelude.BASE64_STANDARD_NO_PAD
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeneralPurposeTest {
    @Test
    fun standardEngineEncodesAndDecodes() {
        val encoded = STANDARD.encode("hello world~".encodeToByteArray())

        assertEquals("aGVsbG8gd29ybGR+", encoded)
        assertContentEquals("hello world~".encodeToByteArray(), STANDARD.decode(encoded.encodeToByteArray()).getOrThrow())
    }

    @Test
    fun preludeNoPadEngineMatchesUpstreamExample() {
        assertEquals("c29tZSBieXRlcw", BASE64_STANDARD_NO_PAD.encode("some bytes".encodeToByteArray()))
    }

    @Test
    fun estimateShortLengths() {
        val cases =
            listOf(
                0..0 to 0,
                1..4 to 3,
                5..8 to 6,
                9..12 to 9,
                13..16 to 12,
                17..20 to 15,
            )

        for ((range, decodedLenEstimate) in cases) {
            for (encodedLen in range) {
                val estimate = GeneralPurposeEstimate(encodedLen)
                assertEquals(decodedLenEstimate, estimate.decodedLenEstimate())
            }
        }
    }

    @Test
    fun rejectsNonCanonicalTrailingBits() {
        val failure = STANDARD.decode("/x==".encodeToByteArray()).exceptionOrNull()

        assertIs<DecodeError.InvalidLastSymbol>(failure)
    }
}
