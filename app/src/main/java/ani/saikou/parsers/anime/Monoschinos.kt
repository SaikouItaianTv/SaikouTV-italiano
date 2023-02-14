package ani.saikou.parsers.anime

import android.util.Base64
import ani.saikou.*
import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.OK
import ani.saikou.parsers.anime.extractors.StreamSB


class Monoschinos : AnimeParser() {
    override val name = "Monoschinos"
    override val saveName = "monos_chinos"
    override val hostUrl = "https://monoschinos2.com"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        return pageBody.select("div.heroarea2 div.heromain2 div.allanimes div.row.jpage.row-cols-md-6 div.col-item").map {
            val epNum = it.attr("data-episode")
            logger("Episode-$epNum")
            val url = it.select("a").attr("href")
            val thumb1 = it.select("a div.animedtlsmain div.animeimgdiv img.animeimghv").attr("data-src")
            Episode(epNum,url,thumbnail = thumb1)
            }
        }


    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        return client.get(episodeLink).document.select("ul.dropcaps li").map{
            val server = it.select("a").text()
            val urlBase64 = it.select("a").attr("data-player")
            val url1 = Base64.decode(urlBase64, Base64.DEFAULT)
            val url = String(url1).replace("https://monoschinos2.com/reproductor?url=", "")
            val embed = FileUrl(url,mapOf("referer" to hostUrl))
            VideoServer(server,embed)

        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "gogo" in domain    -> GogoCDN(server)
            "goload" in domain  -> GogoCDN(server)
            "sb" in domain      -> StreamSB(server)
            "fplayer" in domain -> FPlayer(server)
            "fembed" in domain  -> FPlayer(server)
            "ok" in domain      -> OK(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        return client.get("$hostUrl/buscar?q=$encoded").document.select("div.heromain div.row div.col-md-4").map {
                val link = it.select("a").attr("href")
                val title = it.select("a div.series div.seriesdetails h3").text()
                val cover = it.select("a div.series div.seriesimg img").attr("src")
                ShowResponse(title, link, cover)
            }
    }
}