package ani.saikou.parsers.anime

import ani.saikou.client
import ani.saikou.media.Media
import ani.saikou.parsers.*
import org.jsoup.nodes.Document

class AnimeSaturn : AnimeParser() {

    override val name = "AnimeSaturn"
    override val saveName = "animesaturn_to"
    override val hostUrl = "https://www.animesaturn.cc"
    override val isDubAvailableSeparately = true
    override val allowsPreloading = false

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val response = client.get(animeLink).document

        return response.select("a.bottone-ep").mapNotNull{
            var episode = it.text().split(" ")[1]
            if(episode.contains("."))
            if(episode.contains("-"))
                episode = episode.split("-")[0]
            Episode(
                    link = it.attr("href"),
                    number = episode
                )
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val page = client.get(episodeLink).document
        val epLink = page.select("div.card-body > a[href]").find {it1 ->
            it1.attr("href").contains("watch?")
        }?.attr("href")
        val episodePage = client.get(epLink!!).document
        val episodeUrl: String?

        if(episodePage.select("video.afterglow > source").isNotEmpty()) //Old player
            episodeUrl = episodePage.select("video.afterglow > source").first()!!.attr("src")

        else{                                                                   //New player
            val script = episodePage.select("script").find {
                it.toString().contains("jwplayer('player_hls').setup({")
            }!!.toString()
            episodeUrl = script.split(" ").find { it.contains(".m3u8") and !it.contains(".replace") }!!.replace("\"","").replace(",", "")

        }

        return  listOf(VideoServer("AnimeSaturn", episodeUrl?.trim()!!))

    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = AniPlayExtractor(server)
    class AniPlayExtractor(override val server: VideoServer) : VideoExtractor() {
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
        val response = client.get("$hostUrl/animelist?search=$encoded").document
        return response.select("div.item-archivio").toList()
            .filter { (it.select("a.badge-archivio").first()!!.text().contains("(ITA)") === selectDub) }
            .map {
            val title = it.select("a.badge-archivio").first()!!.text()
            val link = it.select("a.badge-archivio").first()!!.attr("href")
            val cover = it.select("img.locandina-archivio[src]").first()!!.attr("src")
            ShowResponse(title, link, cover)
        }
    }
    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        val response = search(mediaObj.nameRomaji) + search(mediaObj.name!!)
        if (response.isNotEmpty()) {
            val media = response.first {
                getId(client.get(it.link).document) == mediaObj.id
            }
            saveShowResponse(mediaObj.id, media, true)
        }
        return loadSavedShowResponse(mediaObj.id)
    }
    private fun getId(document: Document): Int? {
        var aniListId : Int? = null
        document.select("[rel=\"noopener noreferrer\"]").forEach {
            if(it.attr("href").contains("anilist")) aniListId = it.attr("href").removeSuffix("/").split('/').last().toIntOrNull()
        }
        return aniListId
    }
}