@file:Suppress("ClassName")

package io.github.spair.repoxbot.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ExtensionsTest {

    @Test
    fun md5() {
        assertEquals("737df71b613e1d594cf8a6065c085581", "Simple text to be hashed.".md5())
    }
}
