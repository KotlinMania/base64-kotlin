// port-lint: tests display.rs
package io.github.kotlinmania.base64

import io.github.kotlinmania.base64.engine.generalpurpose.STANDARD
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayTest {
    @Test
    fun basicDisplay() {
        assertEquals(
            "~${'$'}Zm9vYmFy#*",
            "~${'$'}${Base64Display.new("foobar".encodeToByteArray(), STANDARD)}#*",
        )
        assertEquals(
            "~${'$'}Zm9vYmFyZg==#*",
            "~${'$'}${Base64Display.new("foobarf".encodeToByteArray(), STANDARD)}#*",
        )
    }

    @Test
    fun displayEncodeMatchesNormalEncode() {
        val helper = DisplaySinkTestHelper()
        val cases =
            listOf(
                "",
                "f",
                "fo",
                "foo",
                "foob",
                "fooba",
                "foobar",
                "hello world~",
                "some bytes",
            )

        for (case in cases) {
            val bytes = case.encodeToByteArray()
            assertEquals(STANDARD.encode(bytes), helper.encodeToString(bytes))
        }
    }

    private class DisplaySinkTestHelper {
        fun encodeToString(bytes: ByteArray): String = Base64Display.new(bytes, STANDARD).toString()
    }
}
