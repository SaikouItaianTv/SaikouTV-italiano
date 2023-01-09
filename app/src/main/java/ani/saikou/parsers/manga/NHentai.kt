package ani.saikou.parsers.manga

import androidx.core.text.isDigitsOnly
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NHentai : MangaParser() {

    override val name = "NHentai"
    override val saveName = "n_hentai"
    override val hostUrl = "https://nhentai.net"
    override val isNSFW = true

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val id = mangaLink.substringAfter("g/")
        val json = client.get("https://nhentai.net/api/gallery/$id").parsed<MangaResponse>()
        // There's really no "chapter(s)" in nhentai afaik. So here it's being returned as if it's the first chapter.
        return listOf(
            MangaChapter(
                number = "1",
                link = "$hostUrl/g/$id",
                title = json.title.pretty
            )
        )
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val id = chapterLink.substringAfter("g/")
        val json = client.get("$hostUrl/api/gallery/$id").parsed<MangaResponse>()
        val ext = ext(json.images.pages[0].t)
        return (1 until (json.images.pages.size - 1)).map {
            MangaImage("https://i.nhentai.net/galleries/${json.media_id}/$it.$ext")
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        return if (query.startsWith("#") || query.isDigitsOnly()) {
            val id = query.replace("#", "")
            val document = client.get("$hostUrl/g/$id").document.select("div#content div#bigcontainer.container")
            val coverUrl = document.select("div#cover a img.lazyload").attr("data-src")
            val title = document.select("div#info-block div#info h1.title span.pretty").text()
            listOf(
                ShowResponse(
                    name = title,
                    link = "$hostUrl/g/$id",
                    coverUrl = coverUrl,
                )
            )
        } else {
            val finalQuery = "$query language:english"
            val resp = client.get("$hostUrl/api/galleries/search?query=${encode(finalQuery)}")
            if (!resp.text.startsWith("{")) throw Exception("NHentai Added CloudFlare Protection, Please use a different Source.")
            resp.parsed<SearchResponse>().result.map {
                ShowResponse(
                    name = it.title.pretty,
                    link = "$hostUrl/g/${it.id}",
                    coverUrl = "https://t.nhentai.net/galleries/${it.media_id}/cover.jpg",
                )
            }
        }
    }


    // convert to proper extension from API
    private fun ext(t: String): String {
        return when (t) {
            "j"  -> "jpg"
            "p"  -> "png"
            "g"  -> "gif"
            else -> "jpg" // unreachable
        }
    }

    @Serializable
    private data class SearchResponse(
        @SerialName("result") val result: List<Result>,
    ) {

        @Serializable
        data class Result(
            @SerialName("id") val id: Int,
            @SerialName("media_id") val media_id: Int,
            @SerialName("title") val title: Title,
        ) {

            @Serializable
            data class Title(
                @SerialName("english") val english: String,
                @SerialName("japanese") val japanese: String,
                @SerialName("pretty") val pretty: String
            )
        }
    }

    @Serializable
    private data class MangaResponse(
        @SerialName("media_id") val media_id: Int,
        @SerialName("title") val title: Title,
        @SerialName("images") val images: Pages
    ) {

        @Serializable
        data class Title(@SerialName("pretty") val pretty: String)

        @Serializable
        data class Pages(@SerialName("pages") val pages: List<Page>) {

            @Serializable
            data class Page(
                @SerialName("t") val t: String, // extension
                @SerialName("w") val w: Int,    // width
                @SerialName("h") val h: Int     // height
            )
        }
    }
}
