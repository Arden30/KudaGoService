package arden.java.model

import kotlinx.serialization.*
import kotlin.math.exp

@Serializable
data class AllNews(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<News>
)

@Serializable
data class News(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String,
    @SerialName("place") val place: Place? = null,
    @SerialName("description") val description: String,
    @SerialName("site_url") val siteUrl: String,
    @SerialName("favorites_count") val favoritesCount: Long,
    @SerialName("comments_count") val commentsCount: Long,
    @SerialName("publication_date") var date: Long,
) {
    val rating: Double by lazy {
        1 / (1 + exp((-(favoritesCount / (commentsCount + 1))).toDouble()))
    }
}

@Serializable
data class Place(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null
) {

    override fun toString(): String = title ?: id.toString()
}
