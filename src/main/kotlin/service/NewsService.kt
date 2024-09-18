package arden.java.service

import arden.java.dsl.getAllNews
import arden.java.model.AllNews
import arden.java.model.News
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.exp

class NewsService {
    private val logger = LoggerFactory.getLogger(NewsService::class.java)
    private val url = "https://kudago.com/public-api/v1.4/news/"
    private val location = "spb"

    suspend fun getNews(count: Int = 100): List<News> {
        val json = Json { ignoreUnknownKeys = true }
        val client = HttpClient(CIO)
        val response: HttpResponse = client
            .get(url) {
                parameter("location", location)
                parameter("text_format", "text")
                parameter("expand", "place")
                parameter(
                    "fields",
                    "id,title,place,description,site_url,favorites_count,comments_count,publication_date"
                )
                parameter("page_size", count)
            }
        val allNews = json.decodeFromString<AllNews>(response.bodyAsText())

        return allNews.results
    }

    fun saveNews(path: String, news: Collection<News>) {
        try {
            val filePath: Path = Paths.get(path)
            if (Files.exists(filePath)) {
                throw IllegalArgumentException("File already exists")
            }

            if (!Files.isDirectory(filePath.parent)) {
                throw IllegalArgumentException("Invalid file path")
            }

            csvWriter().open(File(path)) {
                writeRow(
                    "id",
                    "title",
                    "place",
                    "description",
                    "site_url",
                    "favorites_count",
                    "comments_count",
                    "publication_date"
                )

                for (newsItem in news) {
                    writeRow(
                        newsItem.id,
                        newsItem.title,
                        newsItem.place,
                        newsItem.description,
                        newsItem.siteUrl,
                        newsItem.favoritesCount,
                        newsItem.commentsCount,
                        parseDate(newsItem.date)
                    )
                }
            }

            logger.info("News were saved in: {}", filePath.fileName)
        } catch (exc: Exception) {
            logger.error("Error while saving news: {}", exc.message)
        }
    }

    fun writeNewsInFile(list: List<News>, path: String) {
        val filePath = Path.of(path)
        Files.newBufferedWriter(filePath)
            .use { bufferedWriter -> bufferedWriter.write(list.getAllNews()) }
        logger.info("Result was saved in: {}", filePath.fileName)
    }
}

// with lists & cycles
fun List<News>.getMostRatedNewsWithCycles(count: Int, period: ClosedRange<LocalDate>): List<News> {
    var i = -1
    while (i++ < this.size - 1) {
        val localDate = parseDate(this[i].date)
        if (localDate in period) {
            this[i].rating = 1 / (1 + exp((-(this[i].favoritesCount / (this[i].commentsCount + 1))).toDouble()))
        }
    }

    return this.filter { it.rating != null }
        .sortedByDescending { it.rating }
        .take(count)
}

// with sequences
fun List<News>.getMostRatedNewsWithSequences(count: Int, period: ClosedRange<LocalDate>): List<News> {
    return this.asSequence()
        .map { news: News ->
            val localDate = parseDate(news.date)
            if (localDate in period) {
                news.rating = 1 / (1 + exp((-(news.favoritesCount / (news.commentsCount + 1))).toDouble()))
            }
            news
        }.filter { it.rating != null }
        .sortedByDescending { it.rating }
        .take(count)
        .toList()
}

fun parseDate(date: Long): LocalDate = Instant.ofEpochSecond(date).atZone(ZoneOffset.UTC).toLocalDate()