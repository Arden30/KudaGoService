package arden.java

import arden.java.service.NewsService
import arden.java.service.getMostRatedNewsWithSequences
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

suspend fun main() = runBlocking {
    val dotenv = Dotenv.configure().load()
    val newsService = NewsService(HttpClient(CIO))

    val start = System.currentTimeMillis()
    val allNews = newsService.getAndSaveAllNews(dotenv["ALL_NEWS_PATH_CSV"], 50)
    val end = System.currentTimeMillis()
    println("Time taken: " + (end - start) + " ms")
    newsService.writeNewsInFile(allNews, dotenv["ALL_NEWS_PATH_MD"])

    val mostRatedNews = allNews.getMostRatedNewsWithSequences(20, LocalDate.of(2024, 9, 10)..LocalDate.of(2024, 10, 18))
    newsService.writeNewsInFile(mostRatedNews, dotenv["MOST_RATED_NEWS_PATH_MD"])
}