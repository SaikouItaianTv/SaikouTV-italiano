package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.parsers.*
import ani.saikou.client
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.OK

class TioAnime:AnimeParser() {
    override val name = "AnimeFLV"
    override val saveName = "tioanime"
    override val hostUrl = "https://tioanime.com/"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"


    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val document = client.get(animeLink).document
        val epInfoScript = document.selectFirst("script:containsData(var episodes = )")!!.data()

        val epNumList = epInfoScript.substringAfter("episodes = [").substringBefore("];").split(",")
        val epSlug = epInfoScript.substringAfter("anime_info = [").substringBefore("];").replace("\"", "").split(",")[1]

        return epNumList.map{
            Episode(
                number = it,
                link = "$hostUrl/ver/$epSlug-$it",
                title = "Episodio $it"
            )
        }

    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val document = client.get(episodeLink).document
        val serverList = document.selectFirst("script:containsData(var videos =)")!!.data()
            .substringAfter("var videos = [[").substringBefore("]];")
            .replace("\"", "").split("],[")

        return serverList.map {
            val servers = it.split(",")
            val serverName = servers[0]
            val serverUrl = servers[1].replace("\\/", "/")

            VideoServer(
                serverName,
                serverUrl
            )
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
       return when{
           "fembed" in domain -> FPlayer(server)
           "okru" in domain -> OK(server)
           else -> null
       }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val document = client.get("$hostUrl/directorio?q=$query").document
        return document.select("ul.animes.list-unstyled.row li.col-6.col-sm-4.col-md-3.col-xl-2").map {element ->
            val url = hostUrl + element.select("article a").attr("href")
            val title = element.select("article a h3").text()
            val thumbnailUrl = hostUrl + element.select("article a div figure img").attr("src")
            ShowResponse(title, url, thumbnailUrl)
        }
    }

}