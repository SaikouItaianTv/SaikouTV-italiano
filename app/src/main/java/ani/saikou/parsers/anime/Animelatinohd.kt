package ani.saikou.parsers.anime

import ani.saikou.*
import android.net.Uri
import android.util.Log
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.OK
import ani.saikou.parsers.anime.extractors.StreamSB
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject


class Animelatinohd : AnimeParser() {
    override val name = "AnimeLatinoHD"
    override val saveName = "animelatinohd"
    override val hostUrl = "https://www.animelatinohd.com"
    override val isDubAvailableSeparately = true
    override val language = "Spanish"

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        return pageBody.select("div.Anime_listEpisodes__1KGdz div.EpisodeCard_container__8iff3").reversed().map {
            val epNum = it.select("div.EpisodeCard_text__nITZ3 a span").text().substringAfter(". ")
            val url = it.select("div.EpisodeCard_holder__2xYAZ div.EpisodeCard_overlay__tqLPT a").attr("href")
            Episode(epNum,hostUrl + url)
            }
        }


    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val animeJson = client.get(episodeLink).document.selectFirst("script#__NEXT_DATA__")!!.data()
        val lang = when(selectDub){
            true -> 1
            else -> 0
        }
        return try {
            //-----   this is deprecated, i need change it   -------
            Mapper.parse<AnimeDetails>(animeJson).props!!.pageProps!!.data!!.players[lang].map {
                val server = it.server!!.title!!
                val embed = it.code!!
                VideoServer(server,embed)
            }
            //-------
        }catch (e: Exception){
            val props = JSONObject(animeJson)["props"]
            val pageprops = JSONObject(props.toString())["pageProps"]
            val data = JSONObject(pageprops.toString())["data"]
            val players = JSONObject(data.toString())["players"]
            return if(lang == 0) {
                val servers = JSONArray(players.toString())[0]
                Json.decodeFromString<JsonArray>(servers.toString()).map {
                    val server =
                        it.jsonObject["server"]?.jsonObject?.get("title")?.jsonPrimitive?.content.toString()
                    val embedId = it.jsonObject["id"]?.jsonPrimitive?.content.toString()
                    val embed = client.get("https://api.animelatinohd.com/stream/$embedId", referer = hostUrl).url
                    VideoServer(server, embed )
                }
            }else{
                val servers = JSONArray(players.toString())[1]
                Json.decodeFromString<JsonArray>(servers.toString()).map {
                    val server =
                        it.jsonObject["server"]?.jsonObject?.get("title")?.jsonPrimitive?.content.toString()
                    val embedId = it.jsonObject["id"]?.jsonPrimitive?.content.toString()
                    val embed = client.get("https://api.animelatinohd.com/stream/$embedId", referer = hostUrl).url
                    VideoServer(server, embed )
                }
            }
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "gogo" in domain    -> GogoCDN(server)
            "goload" in domain  -> GogoCDN(server)
            "sb" in domain      -> StreamSB(server)
            "fembed" in domain || "vanfem" in domain || "fplayer" in domain -> FPlayer(server)
            "ok" in domain      -> OK(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val jsonResponse = client.get("https://api.animelatinohd.com/api/anime/search?search=$query")
        return Mapper.parse<List<SearchResponse>>("$jsonResponse").map {
            ShowResponse(it.name!!,"$hostUrl/anime/${it.slug}","https://image.tmdb.org/t/p/original" + it.poster)}
        }
    }



@Serializable
data class SearchResponse (
    var name   : String? = null,
    var slug   : String? = null,
    var type   : String? = null,
    var poster : String? = null
)

@Serializable
data class AnimeDetails (
    var props      : Props?   = Props(),
    var page       : String?  = null,
    var query      : Query?   = Query(),
    var buildId    : String?  = null,
    var isFallback : Boolean? = null,
    var gssp       : Boolean? = null
)

@Serializable
data class Query (
    var slug   : String? = null,
    var number : String? = null
)

@Serializable
data class Props (
    var pageProps : PageProps? = PageProps(),
    var _NSSP     : Boolean?   = null
)


@Serializable
data class PageProps (
    var data : Data? = Data()
)

@Serializable
data class Data (
    var id        : Int?                          = null,
    var number    : Int?                          = null,
    var views     : Int?                          = null,
    var anime     : Anime?                        = Anime(),
    var anterior  : Anterior?                     = Anterior(),
    var siguiente : Siguiente?                    = Siguiente(),
    var players   : ArrayList<ArrayList<Players>> = arrayListOf()
)

@Serializable
data class Players (
    var id       : Int?    = null,
    var code     : String? = null,
    var languaje : String? = null,
    var serverId : Int?    = null,
    var server   : Server? = Server(),
    var position : Int?    = null
)

@Serializable
data class Server (
    var id       : Int?    = null,
    var title    : String? = null,
    var embed    : String? = null,
    var type     : Int?    = null,
    var status   : Int?    = null,
    var position : Int?    = null
)

@Serializable
data class Siguiente (
    var number : Int? = null
)

@Serializable
data class Anterior (
    var number : Int? = null
)

@Serializable
data class Anime (
    var id     : Int?    = null,
    var name   : String? = null,
    var slug   : String? = null,
    var banner : String? = null,
    var poster : String? = null
)