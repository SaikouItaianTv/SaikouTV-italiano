package ani.saikou.parsers.anime;

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.OK
import kotlinx.serialization.Serializable


class Jkanime : AnimeParser() {
    override val name = "Jkanime"
    override val saveName = "jkanime"
    override val hostUrl = "https://jkanime.net"
    override val isDubAvailableSeparately = false
    override val language = "Spanish"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        val animeId =
            pageBody.select("div.anime__details__text div.anime__details__title div#guardar-anime.btn.btn-light.btn-sm.ml-2")
                .attr("data-anime")
        return episodesParser(animeLink,animeId)
        }

    private suspend fun episodesParser(animeLink: String, animeId:String):List<Episode>{
        val episodes = mutableListOf<Episode>()
        val lastEp = client.get("https://jkanime.net/ajax/last_episode/$animeId/").document.body().toString().findBetween("{\"number\":\"","\",")
            ?.toInt()
        Log.i("bruh",lastEp.toString())

        for (i in 0 until lastEp!!) {
                Log.i("bruh",(i+1).toString())
                episodes.add(
                    Episode(
                        (i+1).toString(),
                        "$animeLink/${(i+1)}"
                    )
                )
        }

        return episodes
        }


    @SuppressLint("SuspiciousIndentation")
    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val videos = mutableListOf<VideoServer>()
          client.get(episodeLink).document.select("div.col-lg-12.rounded.bg-servers.text-white.p-3.mt-2 a").forEach{ it ->
            val server = it.text()
            val serverId = it.attr("data-id")
             client.get(episodeLink).document.select("script").forEach{script ->
                if(script.data().contains("var video = [];")){
                    val url1 = hostUrl + script.data().substringAfter("video[$serverId] = '<iframe class=\"player_conte\" src=\"")
                        .substringBefore("\"").replace("$hostUrl/jkfembed.php?u=","https://fembed.com/v/")
                        .replace("$hostUrl/jkokru.php?u=","http://ok.ru/videoembed/")
                        .replace("$hostUrl/jkvmixdrop.php?u=","https://mixdrop.co/e/")
                    val url = url1.replace("$hostUrl/jkokru.php?u=","http://ok.ru/videoembed/")
                        .replace("$hostUrl/jkvmixdrop.php?u=","https://mixdrop.co/e/")
                        .replace("$hostUrl/jkfembed.php?u=","https://embedsito.com/v/")


                    if(url.contains("um2")){
                        val doc = client.get(url, referer = episodeLink).document
                        val dataKey = doc.select("form input[value]").attr("value")
                        Log.i("bruh","Data: $dataKey")
                        client.post("$hostUrl/gsplay/redirect_post.php",
                        headers = mapOf(
                            "Host" to "jkanime.net",
                            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                            "Accept-Language" to "en-US,en;q=0.5",
                            "Referer" to episodeLink,
                            "Content-Type" to "application/x-www-form-urlencoded",
                            "Origin" to "https://jkanime.net",
                            "DNT" to "1",
                            "Connection" to "keep-alive",
                            "Upgrade-Insecure-Requests" to "1",
                            "Sec-Fetch-Dest" to "iframe",
                            "Sec-Fetch-Mode" to "navigate",
                            "Sec-Fetch-Site" to "same-origin",
                            "TE" to "trailers",
                            "Pragma" to "no-cache",
                            "Cache-Control" to "no-cache",),
                        data = mapOf(Pair("data",dataKey)),
                        allowRedirects = false).okhttpResponse.headers.values("location").forEach(){loc ->
                            val postkey = loc.replace("/gsplay/player.html#","")
                            val nozomiText = client.post("$hostUrl/gsplay/api.php",
                                headers = mapOf(
                                    "Host" to "jkanime.net",
                                    "Accept" to "application/json, text/javascript, */*; q=0.01",
                                    "Accept-Language" to "en-US,en;q=0.5",
                                    "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                                    "X-Requested-With" to "XMLHttpRequest",
                                    "Origin" to "https://jkanime.net",
                                    "DNT" to "1",
                                    "Connection" to "keep-alive",
                                    "Sec-Fetch-Dest" to "empty",
                                    "Sec-Fetch-Mode" to "cors",
                                    "Sec-Fetch-Site" to "same-origin",),
                                data = mapOf(Pair("v",postkey)),
                                allowRedirects = false
                            ).parsed<Nozomi>()
                            Log.i("bruh",nozomiText.file.toString())
                            videos.add(VideoServer("Nozomi",nozomiText.file.toString(),null))
                        }
                    }
                    Log.i("bruh",url)
                    videos.add(VideoServer(server,url))
                }
            }
        }
        return videos
    }
    @Serializable
    data class Nozomi (
        val file: String?
    )




    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "fembed" in domain  -> FPlayer(server)
            "embedsito" in domain  -> FPlayer(server)
            "ok" in domain      -> OK(server)
            "jkanime" in domain -> JkanimeExtractor(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if(selectDub) " (Sub)" else "")
        return client.get("$hostUrl/buscar/$encoded/1/?filtro=nombre&tipo=none&estado=none&orden=desc").document.select("div.anime__page__content div.row div.col-lg-2").map {
            val link = it.select("div.anime__item a").attr("href")
            val title = it.select("div.anime__item div#ainfo div.title").text()
            val cover = it.select("div.anime__item a div.anime__item__pic.set-bg").attr("data-setbg")
            ShowResponse(title, link, cover)
        }
    }
}


class JkanimeExtractor(override val server: VideoServer): VideoExtractor() {
    @SuppressLint("SuspiciousIndentation")
    override suspend fun extract(): VideoContainer {
        val videos = mutableListOf<Video>()
        val url = server.embed.url.replace("um2","um")

        if(url.contains("jk.php")){
            return VideoContainer(listOf(Video(null,VideoType.CONTAINER,url.replace("jk.php?u=",""))))
        }
        client.get(url).document.select("script").forEach{script ->
            if(script.data().contains("var parts = {")){
                val data = script.data().substringAfter("customType")
                    .findBetween("video: ","})")

                val videoUrl = data.toString().substringAfter("url: '").substringBefore("'")
                val type = data.toString().substringAfter("type: '").substringBefore("'")

                    if(type == "hls" || type == "custom"){
                        videos.add( Video(null,VideoType.M3U8,videoUrl))
                    }else{
                        videos.add( Video(null,VideoType.CONTAINER,videoUrl))
                    }


            }

        }
        return VideoContainer(
            videos
        )

    }
}

