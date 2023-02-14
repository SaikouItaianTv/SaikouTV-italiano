package ani.saikou.parsers.anime

import android.util.Base64
import ani.saikou.*
import android.net.Uri
import android.util.Log
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.OK
import ani.saikou.parsers.anime.extractors.StreamSB




class Animefenix : AnimeParser() {
    override val name = "Animefenix"
    override val saveName = "animefenix"
    override val hostUrl = "https://www.animefenix.com"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"


       class amazonExtractor(override val server: VideoServer): VideoExtractor() {

           override suspend fun extract(): VideoContainer {
               val url = server.embed.url
               val videos = mutableListOf<Video>()
               val jsoup = client.get(url).document
               val videoUrl = jsoup.select("body script").toString()
                   .substringAfter("[{\"file\":\"")
                   .substringBefore("\",").replace("\\", "")
               videos.add(Video(null, VideoType.CONTAINER, videoUrl))

               return VideoContainer(videos)
           }
       }






    private suspend fun thumbResolver(url:String): String{
       val script =  client.get(url).toString()
        val thumbUrl = script
            .substringAfter("&amp;thumbnail=")
            .substringBefore("'")
            .replace("%2F","/")
            .replace("%3A",":")

        return thumbUrl

    }




    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        val thumbUrl = thumbResolver(pageBody.select("ul.anime-page__episode-list.is-size-6 li a").first()?.attr("href").toString())

        return pageBody.select("ul.anime-page__episode-list.is-size-6 li").reversed().map { it ->
            val epNum = it.select("a span").text().replace("Episodio", "")
            val url = it.select("a").attr("href")

            Episode(number = epNum,link = url,thumbnail = thumbUrl)
        }
    }


    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val document = client.get(episodeLink).document
        return document.select("ul.is-borderless.episode-page__servers-list li").map {
            val serverName = it.select("a").attr("title")
            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
            val serverCode =
                document.select("div.player-container script").toString()
                    .substringAfter("tabsArray['$serverId'] =")
                    .substringBefore("&amp;thumbnail")
                    .substringAfter("code=")
                    .substringBefore("&amp")

            val url = when(serverName.lowercase()){
               "fembed" -> "https://www.fembed.com/v/$serverCode"
               "ru" -> "https://ok.ru/videoembed/$serverCode"
               "amazon" -> "https://www.animefenix.com/stream/amz.php?v=$serverCode"
               "amazones" -> "https://www.animefenix.com/stream/amz.php?v=$serverCode&ext=es"
               else -> ""
            }
            
            val embed = FileUrl(url,mapOf("referer" to hostUrl))
            VideoServer(serverName,embed)

        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "fembed" in domain     -> FPlayer(server)
            "ok" in domain         -> OK(server)
            "animefenix" in domain -> amazonExtractor(server)
             else  -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        return client.get("$hostUrl/animes?q=$encoded").document
               .select("div.container div.container div.list-series article.serie-card").map {
                val link = it.select("figure.image a").attr("href")
                val title = it.select("div.title h3 a").text()
                val cover = it.select("figure.image a img").attr("src")
                ShowResponse(title, link, cover)
            }
    }
}
