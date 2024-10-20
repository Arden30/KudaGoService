import arden.java.service.NewsService
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class KotlinServiceTest {
    @Test
    fun `test getNews returns valid news list`() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{
                      "count": 2,
                      "next": "https://kudago.com/public-api/v1.4/news/?page=2",
                      "previous": null,
                      "results": [
                        {
                          "id": 101,
                          "title": "Amazing Event in the City",
                          "place": {
                            "id": 1,
                            "title": "Central Park"
                          },
                          "description": "An amazing event that will take place in the heart of the city.",
                          "site_url": "https://kudago.com/news/101/",
                          "favorites_count": 100,
                          "comments_count": 50,
                          "publication_date": 1697740800
                        },
                        {
                          "id": 102,
                          "title": "Music Festival",
                          "place": {
                            "id": 2,
                            "title": "City Stadium"
                          },
                          "description": "Join us for a thrilling music festival with live performances.",
                          "site_url": "https://kudago.com/news/102/",
                          "favorites_count": 200,
                          "comments_count": 75,
                          "publication_date": 1697827200
                        }
                      ]
                    }""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }

        val newsService = NewsService(client)
        val result = newsService.getNews(1)

        assertEquals(2, result.size)
        assertEquals("Amazing Event in the City", result[0].title)
        assertEquals("Music Festival", result[1].title)
    }
}