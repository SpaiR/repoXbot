@file:Suppress("ClassName")

package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val changelog = Changelog("author", "link.com", 12, listOf(
    ChangelogEntry("rscadd", "123"),
    ChangelogEntry("bugfix", "123")
))
private val diffText = """
            diff --git a/maps/test.dmm b/maps/test.dmm
            new file mode 100644
            index 0000000..f228909
            --- /dev/null
            +++ b/maps/test.dmm
            @@ -0,0 +1 @@
            +qwerty
            \ No newline at end of file
        """.trimIndent()

internal class `Test get labels methods` {

    @Test
    fun `When getting from changelog and classes map is empty`() {
        assertTrue(getLabelsFromChangelog(changelog, emptyMap()).isEmpty())
    }

    @Test
    fun `When getting from changelog and classes map is not empty`() {
        val result = getLabelsFromChangelog(changelog, mapOf(
            "rscadd" to "AddLabel",
            "bugfix" to ""
        ))
        assertEquals(setOf("AddLabel"), result)
    }

    @Test
    fun `When getting from diff text and diff paths labels map is empty`() {
        assertTrue(getLabelsFromDiffText(diffText, emptyMap()).isEmpty())
    }

    @Test
    fun `When getting from diff text and diff paths labels map is not empty`() {
        val result = getLabelsFromDiffText(diffText, mapOf(
            "^diff.+/maps/.+\\.dmm$" to "MapLabel"
        ))
        assertEquals(setOf("MapLabel"), result)
    }
}
