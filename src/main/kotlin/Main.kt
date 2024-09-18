package arden.java

import arden.java.service.NewsService
import arden.java.service.getMostRatedNewsWithSequences
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

suspend fun main() = runBlocking {
    val newsService = NewsService()
    val allNews = newsService.getNews()

    val dotenv = Dotenv.configure().load()
    newsService.saveNews(dotenv["ALL_NEWS_PATH_CSV"], allNews)
    newsService.writeNewsInFile(allNews, dotenv["ALL_NEWS_PATH_MD"])

    val mostRatedNews = allNews.getMostRatedNewsWithSequences(20, LocalDate.of(2024, 9, 10)..LocalDate.of(2024, 9, 18))
    newsService.writeNewsInFile(mostRatedNews, dotenv["MOST_RATED_NEWS_PATH_MD"])
}