package io.github.spair.repoxbot.logic

import io.github.spair.repoxbot.dto.Changelog
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.YYYY")

fun mergeChangelogWithHtml(changelog: Changelog, html: String): String {
    val parsedChangelog = Jsoup.parse(html)
    val currentChangelogs = parsedChangelog.getElementById("changelogs")

    val currentDateValue = LocalDate.now().format(FORMATTER)
    var currentDate = getCurrentDate(currentChangelogs, currentDateValue)

    if (currentDate == null) {
        currentChangelogs.prepend("<div class=\"row\" data-date=\"$currentDateValue\"></div>")
        currentDate = getCurrentDate(currentChangelogs, currentDateValue)!!
        currentDate.append("<div class=\"col-lg-12\"><h3 class=\"row-header\">$currentDateValue</h3></div>")
    }

    val columnAddTo = currentDate.getElementsByClass("col-lg-12").first()
    var authorElement = getAuthorElement(columnAddTo, changelog.author)

    if (authorElement == null) {
        columnAddTo.append("<div data-author=\"${changelog.author}\"></div>")
        authorElement = getAuthorElement(columnAddTo, changelog.author)!!
        authorElement.append("<h4 class=\"author\">${changelog.author}:</h4><ul class=\"changelog\"></ul>")
    }

    authorElement.getElementsByClass("changelog").first().let { changelogElement ->
        changelog.entries.forEach { entry ->
            changelogElement.append("<li class=\"${entry.className}\">${entry.changeText.linkify()}</li>")
        }
    }

    return parsedChangelog.html().trimEnds()
}

private fun getCurrentDate(el: Element, currentDateValue: String): Element? {
    return el.getElementsByAttributeValue("data-date", currentDateValue).first()
}

private fun getAuthorElement(el: Element, author: String): Element? {
    return el.getElementsByAttributeValue("data-author", author).first()
}

private fun String.linkify() = replace("\\[link:(.*)]".toRegex(), "<a class=\"btn btn-xs btn-success link-btn\" href=\"$1\">Read More</a>")

private fun String.trimEnds(): String = buildString { reader().forEachLine { append(it.trimEnd()).append("\n") } }
