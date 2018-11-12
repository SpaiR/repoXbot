@file:Suppress("ClassName")

package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val currentDateValue = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.YYYY"))

internal class `Test merge changelog with html` {

    private val changelog = Changelog("author name", "pr-link.com", 12,
        listOf(
            ChangelogEntry("rscadd", "Test entry 1! [link:pr-link.com]"),
            ChangelogEntry("rscdel", "Test entry 2.")
        )
    )

    @Test
    fun `When no current date entry`() {
        val html = "<div id=\"changelogs\"></div>"
        val result = mergeChangelogWithHtml(changelog, html)
        val expected = """
            |<html>
            | <head></head>
            | <body>
            |  <div id="changelogs">
            |   <div class="row" data-date="$currentDateValue">
            |    <div class="col-lg-12">
            |     <h3 class="row-header">$currentDateValue</h3>
            |     <div data-author="author name">
            |      <h4 class="author">author name:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry 1! <a class="btn btn-xs btn-success link-btn" href="pr-link.com">Read More</a></li>
            |       <li class="rscdel">Test entry 2.</li>
            |      </ul>
            |     </div>
            |    </div>
            |   </div>
            |  </div>
            | </body>
            |</html>
            |
        """.trimMargin()
        assertEquals(expected, result)
    }

    @Test
    fun `When has current date entry, but no current author`() {
        val html = """
            |<html>
            | <head></head>
            | <body>
            |  <div id="changelogs">
            |   <div class="row" data-date="$currentDateValue">
            |    <div class="col-lg-12">
            |     <h3 class="row-header">$currentDateValue</h3>
            |     <div data-author="Another author">
            |      <h4 class="author">Another author:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry.</li>
            |      </ul>
            |     </div>
            |    </div>
            |   </div>
            |  </div>
            | </body>
            |</html>
            |
        """.trimMargin()
        val result = mergeChangelogWithHtml(changelog, html)
        val expected = """
            |<html>
            | <head></head>
            | <body>
            |  <div id="changelogs">
            |   <div class="row" data-date="$currentDateValue">
            |    <div class="col-lg-12">
            |     <h3 class="row-header">$currentDateValue</h3>
            |     <div data-author="Another author">
            |      <h4 class="author">Another author:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry.</li>
            |      </ul>
            |     </div>
            |     <div data-author="author name">
            |      <h4 class="author">author name:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry 1! <a class="btn btn-xs btn-success link-btn" href="pr-link.com">Read More</a></li>
            |       <li class="rscdel">Test entry 2.</li>
            |      </ul>
            |     </div>
            |    </div>
            |   </div>
            |  </div>
            | </body>
            |</html>
            |
        """.trimMargin()
        assertEquals(expected, result)
    }

    @Test
    fun `When has current date entry and current author`() {
        val html = """
            |<html>
            | <head></head>
            | <body>
            |  <div id="changelogs">
            |   <div class="row" data-date="$currentDateValue">
            |    <div class="col-lg-12">
            |     <h3 class="row-header">$currentDateValue</h3>
            |     <div data-author="author name">
            |      <h4 class="author">author name:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry.</li>
            |      </ul>
            |     </div>
            |    </div>
            |   </div>
            |  </div>
            | </body>
            |</html>
            |
        """.trimMargin()
        val result = mergeChangelogWithHtml(changelog, html)
        val expected = """
            |<html>
            | <head></head>
            | <body>
            |  <div id="changelogs">
            |   <div class="row" data-date="$currentDateValue">
            |    <div class="col-lg-12">
            |     <h3 class="row-header">$currentDateValue</h3>
            |     <div data-author="author name">
            |      <h4 class="author">author name:</h4>
            |      <ul class="changelog">
            |       <li class="rscadd">Test entry.</li>
            |       <li class="rscadd">Test entry 1! <a class="btn btn-xs btn-success link-btn" href="pr-link.com">Read More</a></li>
            |       <li class="rscdel">Test entry 2.</li>
            |      </ul>
            |     </div>
            |    </div>
            |   </div>
            |  </div>
            | </body>
            |</html>
            |
        """.trimMargin()
        assertEquals(expected, result)
    }
}
