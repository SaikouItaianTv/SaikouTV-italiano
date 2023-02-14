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
import org.jsoup.Jsoup


//not used because the servers is too slow 
class Henaojara : AnimeParser() {
    override val name = "Henaojara(Experimental)"
    override val saveName = "henaojara"
    override val hostUrl = "https://henaojara.com"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"
    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document

        return  pageBody.select("div.TPTblCn.AA-cont table tbody tr").map{
            val episodeLink = it.select("td.MvTbImg.B a").attr("href")
            val episodeNumber = it.select("td span.num").text()
            val episodeCover = it.select("td.MvTbImg.B a img").attr("src")
            Episode(episodeNumber, episodeLink, thumbnail = episodeCover)
        }
        }


    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        logger("Loading video servers for $episodeLink")
        val servers = client.get(episodeLink).document.select("div.TPlayer div").map{
            val embedUrl = it.toString().substringAfter("src=\"").substringBefore("\"").replace("amp;", "")
            val serverUrl = client.get(Uri.decode(embedUrl)).document.selectFirst("iframe")?.attr("src")!!
            val serverName = when{
                serverUrl.contains("ok.ru") -> "okru"
                serverUrl.contains("streamsb") -> "streamsb"
                serverUrl.contains("fembed") -> "fembed"
                serverUrl.contains("dood")   -> "dood"
                else -> "unknown"
            }
            VideoServer(serverName,serverUrl)

        }
        return servers
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
        return client.get("$hostUrl/?s=$encoded").document.select(" li.TPostMv").map {
                val link = it.select("article a").attr("href")
                val title = it.select("article a h3").text()
                val cover = it.select("article a div figure img").attr("src")
                ShowResponse(title, link,cover)
            }
    }
}