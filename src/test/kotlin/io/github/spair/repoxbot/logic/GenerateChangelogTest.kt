@file:Suppress("ClassName")

package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import io.github.spair.repoxbot.dto.PullRequest
import io.github.spair.repoxbot.dto.PullRequestAction
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val AUTHOR_NAME = "Author Name"
private const val PR_NUM = 12
private const val PR_LINK = "https://pr-link.com"

internal class `Test changelog generation from pull request` {

    private val entry = ChangelogEntry("rscadd", "Test entry.")

    @Test
    fun `When no changelog`() {
        assertNull(generateChangelog(createPullRequest("empty body")))
    }

    @Test
    fun `When no entries`() {
        assertTrue(generateChangelog(createPullRequest("body\n:cl: author"))!!.isEmpty())
    }

    @Test
    fun `When has author and 1 entry`() {
        val expected = Changelog("custom author", PR_LINK, PR_NUM, listOf(entry))
        val body = """
            body
            :cl: custom author
            - rscadd: Test entry.
        """.trimIndent()
        assertEquals(expected, generateChangelog(createPullRequest(body)))
    }

    @Test
    fun `When has no author and multiple entries`() {
        val expected = Changelog(AUTHOR_NAME, PR_LINK, PR_NUM, listOf(entry, entry))
        val body = """
            body
            :cl:
            - rscadd: test entry
            - rscadd: Test entry.
        """.trimIndent()
        assertEquals(expected, generateChangelog(createPullRequest(body)))
    }

    @Test
    fun `When has non typical entry`() {
        val expected = Changelog(AUTHOR_NAME, PR_LINK, PR_NUM, listOf(ChangelogEntry("rscadd", "Test entry!")))
        val body = """
            body
            :cl:
            * rscadd: test entry!
        """.trimIndent()
        assertEquals(expected, generateChangelog(createPullRequest(body)))
    }

    @Test
    fun `When has link`() {
        val expected = Changelog(AUTHOR_NAME, PR_LINK, PR_NUM, listOf(ChangelogEntry("rscadd", "Test entry! [link:$PR_LINK]")))
        val body = """
            body
            :cl:
            * rscadd[link]: test entry!
        """.trimIndent()
        assertEquals(expected, generateChangelog(createPullRequest(body)))
    }

    private fun createPullRequest(body: String): PullRequest = PullRequest(
        PullRequestAction.MERGED, AUTHOR_NAME, PR_NUM, "Title",
        PR_LINK, "Diff Link", body, "Sender"
    )
}
