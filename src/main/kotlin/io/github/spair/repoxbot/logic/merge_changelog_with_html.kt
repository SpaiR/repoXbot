package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import io.github.spair.repoxbot.dto.ChangelogEntry
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val CHANGELOGS_ID = "changelogs"
private const val CHANGELOG_CLASS = "changelog"

private const val CHANGELOG_ELEMENT_TEMPLATE = "<li class=\"%s\">%s</li>"

private const val DATA_DATE = "data-date"
private const val DATA_AUTHOR = "data-author"

private const val DATE_ROW_TEMPLATE = "<div class=\"row\" data-date=\"%s\"></div>"
private const val DATE_ELEMENT_TEMPLATE = "<div class=\"col-lg-12\"><h3 class=\"row-header\">%s</h3></div>"

private const val AUTHOR_COLUMN_TEMPLATE = "<div data-author=\"%s\"></div>"
private const val AUTHOR_ELEMENT_TEMPLATE = "<h4 class=\"author\">%s:</h4><ul class=\"changelog\"></ul>"

private const val READ_MORE_TEMPLATE = "<a class=\"btn btn-xs btn-success link-btn\" href=\"$1\">Read More</a>"

private const val COL_LARGE = "col-lg-12"

private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.YYYY")

fun mergeChangelogWithHtml(changelog: Changelog, html: String): String {
    val parsedChangelog = Jsoup.parse(html)
    val currentChangelogs = parsedChangelog.getElementById(CHANGELOGS_ID)

    val currentDateValue = LocalDate.now().format(FORMATTER)
    var currentDate = getCurrentDate(currentChangelogs, currentDateValue)

    if (currentDate == null) {
        currentChangelogs.prepend(DATE_ROW_TEMPLATE.format(currentDateValue))
        currentDate = getCurrentDate(currentChangelogs, currentDateValue)!!
        currentDate.append(DATE_ELEMENT_TEMPLATE.format(currentDateValue))
    }

    addChangelogToCurrentDate(changelog, currentDate)

    return parsedChangelog.html().trimEnds()
}

private fun addChangelogToCurrentDate(changelog: Changelog, currentDate: Element) {
    val columnAddTo = currentDate.getElementsByClass(COL_LARGE).first()
    var authorElement = getAuthorElement(columnAddTo, changelog.author)

    if (authorElement == null) {
        columnAddTo.append(AUTHOR_COLUMN_TEMPLATE.format(changelog.author))
        authorElement = getAuthorElement(columnAddTo, changelog.author)!!
        authorElement.append(AUTHOR_ELEMENT_TEMPLATE.format(changelog.author))
    }

    addChangelogEntries(changelog.entries, authorElement)
}

private fun addChangelogEntries(changelogEntries: List<ChangelogEntry>, elAddTo: Element) {
    elAddTo.getElementsByClass(CHANGELOG_CLASS).first().let { changelogElement ->
        changelogEntries.forEach { entry ->
            changelogElement.append(String.format(CHANGELOG_ELEMENT_TEMPLATE, entry.className, entry.changeText.linkify()))
        }
    }
}

private fun getCurrentDate(el: Element, currentDateValue: String): Element? {
    return el.getElementsByAttributeValue(DATA_DATE, currentDateValue).first()
}

private fun getAuthorElement(el: Element, author: String): Element? {
    return el.getElementsByAttributeValue(DATA_AUTHOR, author).first()
}

private fun String.linkify() = replace("\\[link:(.*)]".toRegex(), READ_MORE_TEMPLATE)

private fun String.trimEnds(): String = this.apply { reader().forEachLine { line -> line.trimEnd() } }
