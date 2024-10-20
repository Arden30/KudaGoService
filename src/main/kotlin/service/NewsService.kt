package arden.java.service

import arden.java.dsl.getAllNews
import arden.java.model.AllNews
import arden.java.model.News
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

@OptIn(DelicateCoroutinesApi::class)
class NewsService(private val client: HttpClient) {
    private val logger = LoggerFactory.getLogger(NewsService::class.java)
    private val url = "https://kudago.com/public-api/v1.4/news/"
    private val location = "spb"
    private val nThreads = 3
    private val nWorkers = 5
    private val workerDispatcher = newFixedThreadPoolContext(nThreads, "Threads")
    private var isFirstRun = true

    suspend fun getNews(page: Int): List<News> {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val response: HttpResponse = client
                .get(url) {
                    parameter("location", location)
                    parameter("text_format", "text")
                    parameter("expand", "place")
                    parameter(
                        "fields",
                        "id,title,place,description,site_url,favorites_count,comments_count,publication_date"
                    )
                    parameter("page", page)
                }
            val allNews = json.decodeFromString<AllNews>(response.bodyAsText())

            return allNews.results
        } catch (exc: Exception) {
            logger.error("Error while sending request to API", exc)
        }

        return emptyList()
    }

    private fun saveNews(path: String, news: Collection<News>) {
        try {
            val filePath: Path = Paths.get(path)

            if (!Files.isDirectory(filePath.parent)) {
                throw IllegalArgumentException("Invalid file path")
            }

            if (isFirstRun) {
                if (Files.exists(filePath)) {
                    logger.info("News file already exists, it will be overwritten by new data")
                    Files.delete(filePath)
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
                }
                isFirstRun = false
            }

            csvWriter().open(File(path), append = true) {
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

    private fun launchWorkers(channel: Channel<List<News>>, nWorkers: Int, count: Int) {
        val atomicCounter = AtomicInteger(count)
        for (i in 1..nWorkers) {
            CoroutineScope(workerDispatcher).launch {
                var page = i
                while (true) {
                    val news = getNews(page)

                    if (news.isNotEmpty()) {
                        if (atomicCounter.get() == 0) {
                            logger.info("All news are found, closing channel")
                            channel.close()
                            break
                        } else if (atomicCounter.get() - news.size < 0) {
                            channel.send(news.take(atomicCounter.get()))
                            logger.info("Sending {} news to the channel", atomicCounter.get())
                            atomicCounter.set(0)
                            break
                        } else {
                            logger.info("Sending {} news to the channel", news.size)
                            channel.send(news)
                        }
                    }

                    atomicCounter.getAndAdd(-news.size)
                    page += nWorkers
                }
            }
        }
    }

    suspend fun getAndSaveAllNews(filePath: String, count: Int = 100): List<News> {
        val channel = Channel<List<News>>()
        val newsList = mutableListOf<News>()

        launchWorkers(channel, nWorkers, count)
        for (news in channel) {
            newsList.addAll(news)
            if (news.isEmpty()) {
                break
            }
            saveNews(filePath, news)
            logger.info("Saved {} news", news.size)
        }

        return newsList
    }
}

// with lists & cycles
fun List<News>.getMostRatedNewsWithCycles(count: Int, period: ClosedRange<LocalDate>): List<News> {
    val news = mutableListOf<News>()
    var i = -1
    while (i++ < this.size - 1) {
        val localDate = parseDate(this[i].date)
        if (localDate in period) {
            news.add(this[i])
        }
    }

    return news.sortedByDescending { it.rating }
        .take(count)
}

// with sequences
fun List<News>.getMostRatedNewsWithSequences(count: Int, period: ClosedRange<LocalDate>): List<News> {
    return this.asSequence()
        .filter { news: News ->
            val localDate = parseDate(news.date)
            localDate in period
        }.sortedByDescending { it.rating }
        .take(count)
        .toList()
}

fun parseDate(date: Long): LocalDate = Instant.ofEpochSecond(date).atZone(ZoneOffset.UTC).toLocalDate()