package ani.saikou.parsers.anime

import ani.saikou.client
import ani.saikou.media.Media
import ani.saikou.parsers.*
import ani.saikou.sortByTitle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder

class AnimeWorld : AnimeParser() {

    override val name = "AnimeWorld"
    override val saveName = "animeworld_to"
    override val hostUrl = "https://www.animeworld.tv"
    override val isDubAvailableSeparately = true
    override val allowsPreloading = false

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val document = client.get("$hostUrl$animeLink").document

        val widget = document.select(".widget.servers")
        return widget.select(".server[data-name=\"9\"] .episode").map {
            val num = it.select("a").attr("data-episode-num")
            val id = "$hostUrl/api/episode/info?id=${it.select("a").attr("data-id")}"
            Episode(number = num, link = id)
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val link = client.get(episodeLink).parsed<AWHtmlResponse>().link ?: return emptyList()
        return  listOf(VideoServer("AnimeWorld", link))

    }

    @Serializable
    data class AWHtmlResponse(
        @SerialName("grabber") val link: String? = null
    )

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = AnimeWorldExtractor(server)
    class AnimeWorldExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            val type = if (server.embed.url.contains("m3u8")) VideoType.M3U8 else VideoType.CONTAINER
            return VideoContainer(
                listOf(
                    Video(null, type, server.embed)
                )
            )

        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = query.replace(" ", "+")
        val document = client.get(
            "$hostUrl${if (selectDub) "/filter?language=it" else "/filter?language=jp&"}&keyword=$encoded"
        ).document
        return document.select(".film-list > .item").map {
            val anchor = it.select("a.name").firstOrNull() ?: throw Error("Error")
            val title = anchor.text()
            val link = anchor.attr("href")
            val cover = it.select("a.poster img").attr("src")
            ShowResponse(title, link, cover)
        }
    }
    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        val response = search(mediaObj.nameRomaji!!) + search(mediaObj.name!!)
        if (response.isNotEmpty()) {
            val media = response.first {
                client.get(hostUrl + it.link).document.selectFirst("#anilist-button")?.attr("href")?.substringAfterLast("/")
                    ?.toInt() == mediaObj.id
            }
            saveShowResponse(mediaObj.id, media, true)
        }
        return loadSavedShowResponse(mediaObj.id)
    }



}