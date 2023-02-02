package ani.saikou.parsers.manga

import androidx.core.text.isDigitsOnly
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import kotlinx.serialization.Serializable
import android.util.Base64

class NHentai : MangaParser() {

    /*
    Note: This source is using a private API.
    - Images are taken from /searchid endpoint as of now, this might change when nhentai gets fixed permanently.
     */
    override val name = "NHentai"
    override val saveName = "n_hentai"
    override val hostUrl = base64Decode("aHR0cHM6Ly9oZW50YWkuYmlnYml0cy5ldS5vcmc=") // pls no abuse
    override val isNSFW = true

    override suspend fun search(query: String): List<ShowResponse> {
        if (query.startsWith("#") || query.isDigitsOnly()) {
            val id = query.replace("#", "")
            val json = client.get("$hostUrl/searchid?id=$id", referer = "ani.saikou").parsed<IdResponse>()
            return listOf(ShowResponse(
                name = json.title.pretty,
                link = id,
                coverUrl = "https://t.nhentai.net/galleries/${json.mediaId}/cover.jpg"
            ))
        } else {
            val json = client.get("$hostUrl/search?q=${encode(query)}", referer = "ani.saikou").parsed<SearchResponse>()
            return json.result.map {
                ShowResponse(
                    name = it.title.pretty,
                    link = "${it.id}",
                    coverUrl = "https://t.nhentai.net/galleries/${it.mediaId}/cover.jpg"
                )
            }
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val json = client.get("$hostUrl/searchid?id=$mangaLink", referer = "ani.saikou").parsed<IdResponse>()
        // There's no chapter(s) in nhentai. So we have to return here as the "first" chapter.
        return listOf(MangaChapter(
            number = "1",
            link = "$hostUrl/searchid?id=$mangaLink",
            title = json.title.pretty
        ))
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val json = client.get(chapterLink, referer = "ani.saikou").parsed<IdResponse>()
        val ext = ext(json.images.pages[0].t)
        return (0 until json.images.pages.size).mapIndexed { i, _ ->
            MangaImage("https://i.nhentai.net/galleries/${json.mediaId}/${i+1}.$ext")
        }
    }

    // helper method to convert to proper extension from API
    private fun ext(t: String): String {
        return when (t) {
            "j"  -> "jpg"
            "p"  -> "png"
            "g"  -> "gif"
            else -> "jpg" // unreachable
        }
    }

    private fun base64Decode(string: String): String {
        return String(Base64.decode(string, Base64.DEFAULT), Charsets.ISO_8859_1)
    }

    @Serializable
    private data class IdResponse(
        val id: Int,
        val mediaId: Int,
        val title: Title,
        val images: Image,
    ) {

        @Serializable
        data class Title(
            val english: String,
            val pretty: String,
        )

        @Serializable
            data class Image(
                val pages: List<Page>
            ) {

                @Serializable
                data class Page(
                    val t: String,
                    val w: Int,
                    val h: Int,
                )
            }
        }

    @Serializable
    private data class SearchResponse(
        val result: List<Result>
    ) {
        @Serializable
        data class Result(
            val id: Int,
            val mediaId: Int,
            val title: IdResponse.Title,
        )
    }

}
