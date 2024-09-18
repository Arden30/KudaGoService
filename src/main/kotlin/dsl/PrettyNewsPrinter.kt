package arden.java.dsl

import arden.java.model.News
import arden.java.service.parseDate

class PrettyNewsPrinter {
    private val content = StringBuilder()

    fun newsItem(news: News) {
        content.append(
            readme {
                header(3) { +news.title }

                text {
                    +("${bold("Id:")} ${ news.id}")
                    +("${bold("Place:")} ${ news.place ?: "Place is undefined"}\n")
                    +("${bold("Description:")} ${ news.description}\n")
                    +("${bold("Site url:")} ${ link(news.siteUrl, news.title)}")
                    +("${bold("Favorites:")} ${ news.favoritesCount}")
                    +("${bold("Comments:")} ${ news.commentsCount}")
                    +("${bold("Publication date:")} ${ parseDate(news.date)}")
                    +("${bold("Rating:")} ${ news.rating ?: "Rating is undefined"}\n")
                }
            }
        )
    }

    override fun toString(): String = content.toString()
}

fun List<News>.getAllNews(): String {
    val prettyNewsPrinter = PrettyNewsPrinter()
    this.forEach {
        prettyNewsPrinter.newsItem(it)
    }

    return prettyNewsPrinter.toString()
}
