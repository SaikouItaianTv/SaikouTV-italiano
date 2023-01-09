package ani.saikou.parsers.manga

import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import ani.saikou.toast
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ComickFun : MangaParser() {

    override val name = "ComickFun"
    override val saveName = "comick_fun"
    override val hostUrl = "https://api.comick.fun"

    override suspend fun search(query: String): List<ShowResponse> {
        val resp = Mapper.parse<List<SearchData>>(client.get("https://api.comick.fun/search?q=${encode(query)}&tachiyomi=true").text)
        return resp.map { manga ->
            val mangaLink = "$hostUrl/comic/${manga.id}/chapter?tachiyomi=true"
            ShowResponse(
                name = manga.title, link = mangaLink, coverUrl = manga.cover_url, otherNames = manga.md_titles.map { it.title },
                extra = mapOf("slug" to manga.slug)
            ) // need this slug for loadChapters
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        // You only need "hid" from here
        val resp = client.get(mangaLink).parsed<MangaChapterData>()
        // Contains other languages too. So filter it
        val filtered = resp.chapters.filter { chapter -> chapter.lang == "en" }
        val buildManifestId = getBuildManifest()
        // Maybe useful in future if website changes/breaks
        if (buildManifestId == null) {
            toast("getBuildManifest() returned null")
        }
        val weirdUrl =
            "https://comick.fun/_next/data/${buildManifestId}/comic/${extra!!["slug"]}/${filtered[0].hid}-chapter-0-en.json"
        val secondResp = Mapper.parse<WeirdUrlData>(client.get(weirdUrl).text)
        return secondResp.pageProps.chapters.reversed().map {
            val chapterLink = "$hostUrl/chapter/${it.hid}?tachiyomi=true"
            MangaChapter(number = it.chap.toString(), link = chapterLink, title = null)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val resp = client.get(chapterLink).parsed<MangaImageData>()
        return resp.chapter.images.map { MangaImage(url = it.url) }
    }

    companion object {

        private var lastChecked = 0L
        private var buildManifestId: String? = null

        private suspend fun getBuildManifest(): String? {
            buildManifestId =
                if (buildManifestId != null && (lastChecked - System.currentTimeMillis()) < 1000 * 60 * 30) buildManifestId
                else {
                    lastChecked = System.currentTimeMillis()
                    val document = client.get("https://comick.fun/").text
                    val buildIdRe = Regex("buildId\":\"(.+?)\"")
                    buildIdRe.find(document, 0)?.groupValues?.get(1)
                }
            return buildManifestId
        }

    }

    @Serializable
    private data class WeirdUrlData(@SerialName("pageProps") val pageProps: Data) {

        @Serializable
        data class Data(@SerialName("chapters") val chapters: List<Chapter>) {

            @Serializable
            data class Chapter(
                @SerialName("chap") val chap: String?, // chapter number
                @SerialName("hid") val hid: String,
            )
        }
    }

    @Serializable
    private data class SearchData(
        @SerialName("title") val title: String,
        @SerialName("id") val id: Int,
        @SerialName("slug") val slug: String,
        @SerialName("md_titles") val md_titles: List<MdTitles>, // other titles
        @SerialName("cover_url") val cover_url: String,
    ) {
        @Serializable
        data class MdTitles(
            @SerialName("title") val title: String,
        )
    }

    @Serializable
    private data class MangaChapterData(
        @SerialName("chapters") val chapters: List<Chap>
    )

    @Serializable
    private data class Chap(
        @SerialName("chap") val chap: String?,  // chapter number
        @SerialName("title") val title: String?,
        @SerialName("lang") val lang: String?,  // may contain other lang too, so filter "en" using this
        @SerialName("hid") val hid: String,
    )

    @Serializable
    private data class MangaImageData(@SerialName("chapter") val chapter: Chapter) {

        @Serializable
        data class Chapter(@SerialName("images") val images: List<Image>) {

            @Serializable
            data class Image(@SerialName("url") val url: String)
        }
    }

}