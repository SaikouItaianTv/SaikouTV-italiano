package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.StreamSB
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.DecimalFormat

class AllAnime : AnimeParser() {
    override val name = "AllAnime"
    override val saveName = "all_anime"
    override val hostUrl = "https://allanime.site"
    override val isDubAvailableSeparately = true

    private val apiHost = "https://blog.allanimenews.com/"
    private val ytAnimeCoversHost = "https://wp.youtube-anime.com/aln.youtube-anime.com"
    private val idRegex = Regex("${hostUrl}/anime/(\\w+)")
    private val epNumRegex = Regex("/[sd]ub/(\\d+)")


    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val responseArray = mutableListOf<Episode>()

        val showId = idRegex.find(animeLink)?.groupValues?.get(1)
        if (showId != null) {
            val episodeInfos = getEpisodeInfos(showId)
            val format = DecimalFormat("#####.#####")
            episodeInfos?.sortedBy { it.episodeIdNum }?.forEach { epInfo ->
                val link = """${hostUrl}/anime/$showId/episodes/${if (selectDub) "dub" else "sub"}/${epInfo.episodeIdNum}"""
                val epNum = format.format(epInfo.episodeIdNum).toString()
                val thumbnail = epInfo.thumbnails?.let {

                    if (it.isNotEmpty()) {
                        var url = it[0]
                        if (!url.startsWith("https")) {
                            url = "$ytAnimeCoversHost$url"
                        }
                        FileUrl(url)
                    } else {
                        null
                    }
                }
                responseArray.add(Episode(epNum, link = link, epInfo.notes, thumbnail))
            }
        }
        return responseArray
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val showId = idRegex.find(episodeLink)?.groupValues?.get(1)
        val videoServers = mutableListOf<VideoServer>()
        val episodeNum = epNumRegex.find(episodeLink)?.groupValues?.get(1)
        if (showId != null && episodeNum != null) {

            val variables =
                """{"showId":"$showId","translationType":"${if (selectDub) "dub" else "sub"}","episodeString":"$episodeNum"}"""
            graphqlQuery(
                variables,
                "3933a4a68bc80c46e25b7b8b3f563df1416b7b583595e5e5bfc67c01bd791df8"
            ).data?.episode?.sourceUrls?.forEach { source ->
                // It can be that two different actual sources share the same sourceName
                var serverName = source.sourceName
                var sourceNum = 2
                // Sometimes provides relative links just because ¯\_(ツ)_/¯
                while (videoServers.any { it.name == serverName }) {
                    serverName = "${source.sourceName} ($sourceNum)"
                    sourceNum++
                }

                if (source.sourceUrl.toHttpUrlOrNull() == null) {
                    val jsonUrl = """${apiHost}${source.sourceUrl.replace("clock", "clock.json").substring(1)}"""
                    videoServers.add(VideoServer(serverName, jsonUrl))
                } else {
                    videoServers.add(VideoServer(serverName, source.sourceUrl))
                }
            }

        }
        return videoServers
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val serverUrl = Uri.parse(server.embed.url)
        val domain = serverUrl.host ?: return null
        val path = serverUrl.path ?: return null
        val extractor: VideoExtractor? = when {
            "gogo" in domain    -> GogoCDN(server)
            "goload" in domain  -> GogoCDN(server)
            "sb" in domain      -> StreamSB(server)
            "sss" in domain     -> StreamSB(server)
            "fplayer" in domain -> FPlayer(server)
            "fembed" in domain  -> FPlayer(server)
            "apivtwo" in path   -> AllAnimeExtractor(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArray = arrayListOf<ShowResponse>()

        val variables =
            """{"search":{"allowAdult":${Anilist.adult},"query":"$query"},"translationType":"${if (selectDub) "dub" else "sub"}"}"""
        val edges =
            graphqlQuery(variables, "9c7a8bc1e095a34f2972699e8105f7aaf9082c6e1ccd56eab99c2f1a971152c6").data?.shows?.edges
        if (!edges.isNullOrEmpty()) {
            for (show in edges) {
                val link = """${hostUrl}/anime/${show.id}"""
                val otherNames = mutableListOf<String>()
                show.englishName?.let { otherNames.add(it) }
                show.nativeName?.let { otherNames.add(it) }
                show.altNames?.forEach { otherNames.add(it) }
                if (show.thumbnail == null) {
                    toastString(""""Impossibile ottenere il poster per ${show.id}""")
                    continue
                }
                responseArray.add(
                    ShowResponse(
                        show.name,
                        link,
                        show.thumbnail,
                        otherNames,
                        show.availableEpisodes.let { if (selectDub) it.dub else it.sub })
                )
            }

        }

        return responseArray
    }

    private suspend fun graphqlQuery(variables: String, persistHash: String): Query {
        val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"$persistHash"}}"""
        val graphqlUrl = ("$hostUrl/allanimeapi").toHttpUrl().newBuilder()
            .addQueryParameter("variables", variables)
            .addQueryParameter("extensions", extensions)
            .build().toString()
        println(variables)
        return client.get(
            graphqlUrl,
            mapOf("Host" to "allanime.site")
        ).also { println("res : ${it.text}") }.parsed()
    }

    private suspend fun getEpisodeInfos(showId: String): List<EpisodeInfo>? {
        val variables = """{"_id": "$showId"}"""
        val show = graphqlQuery(variables, "afcdaedfd46f36448916b5f7db84d2bdbb72fded428ad8755179a03845c57b96").data?.show
        if (show != null) {
            val epCount = if (selectDub) show.availableEpisodes.dub else show.availableEpisodes.sub
            val epVariables = """{"showId":"$showId","episodeNumStart":0,"episodeNumEnd":${epCount}}"""
            return graphqlQuery(
                epVariables,
                "73d998d209d6d8de325db91ed8f65716dce2a1c5f4df7d304d952fa3f223c9e8"
            ).data?.episodeInfos
        }
        return null
    }

    private class AllAnimeExtractor(override val server: VideoServer) : VideoExtractor() {
        private val languageRegex = Regex("vo_a_hls_(\\w+-\\w+)")

        override suspend fun extract(): VideoContainer {
            val url = server.embed.url
            val rawResponse = client.get(url)
            val linkList = mutableListOf<String>()
            if (rawResponse.code < 400) {
                val response = rawResponse.text
                Mapper.parse<ApiSourceResponse>(response).links.forEach {
                    // Avoid languages other than english when multiple urls are provided
                    val matchesLanguagePattern = languageRegex.find(it.resolutionStr)
                    val language = matchesLanguagePattern?.groupValues?.get(1)
                    if (matchesLanguagePattern == null || language?.contains("en") == true) {
                        (it.src ?: it.link)?.let { fileUrl ->
                            linkList.add(fileUrl)
                        }
                    }
                }
            }

            return VideoContainer(toVideoList(linkList))
        }

        private suspend fun toVideoList(
            links: List<String>
        ): List<Video> {
            val videos = mutableListOf<Video>()
            val headers = mutableMapOf<String, String>()
            links.forEach {
                val fileUrl = FileUrl(it, headers)
                val urlPath = Uri.parse(it).path
                if (urlPath != null) {
                    if (urlPath.endsWith(".m3u8")) {
                        videos.add(Video(null, VideoType.M3U8, fileUrl))
                    }
                    if (urlPath.endsWith(".mp4")) {
                        if ("king.stronganime" in it) {
                            headers["Referer"] = "https://allanime.site"
                        }
                        videos.add(Video(null, VideoType.CONTAINER, fileUrl, getSize(fileUrl)))
                    }
                }
            }
            return videos
        }
    }

    override suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse? {
        return loadData("${saveName}_$mediaId")
    }

    override fun saveShowResponse(mediaId: Int, response: ShowResponse?, selected: Boolean) {
        if (response != null) {
            setUserText("${if (selected) "Selected" else "Found"} : ${response.name}")
            saveData("${saveName}_$mediaId", response)
        }
    }

    @Serializable
    private data class Query(@SerialName("data") var data: Data?) {

        @Serializable
        data class Data(
            @SerialName("shows") val shows: ShowsConnection?,
            @SerialName("show") val show: Show?,
            @SerialName("episodeInfos") val episodeInfos: List<EpisodeInfo>?,
            @SerialName("episode") val episode: AllAnimeEpisode?,
        )

        @Serializable
        data class ShowsConnection(
            @SerialName("edges") val edges: List<Show>
        )

        @Serializable
        data class Show(
            @SerialName("_id") val id: String,
            @SerialName("name") val name: String,
            @SerialName("englishName") val englishName: String?,
            @SerialName("nativeName") val nativeName: String?,
            @SerialName("thumbnail") val thumbnail: String?,
            @SerialName("availableEpisodes") val availableEpisodes: AvailableEpisodes,
            @SerialName("altNames") val altNames: List<String>?
        )

        @Serializable
        data class AvailableEpisodes(
            @SerialName("sub") val sub: Int,
            @SerialName("dub") val dub: Int,
            // @SerialName("raw") val raw: Int,
        )

        //        data class LastEpisodeInfos(
        //            @SerialName("sub") val sub: LastEpisodeInfo?,
        //            @SerialName("dub") val dub: LastEpisodeInfo?,
        //        )
        //
        //        data class LastEpisodeInfo(
        //            @SerialName("episodeString") val episodeString: String?,
        //            @SerialName("notes") val notes: String?
        //        )

        @Serializable
        data class AllAnimeEpisode(
            @SerialName("sourceUrls") var sourceUrls: List<SourceUrl>
        )

        @Serializable
        data class SourceUrl(
            @SerialName("sourceUrl") val sourceUrl: String,
            @SerialName("sourceName") val sourceName: String,
            @SerialName("priority") val priority: String
        )
    }

    @Serializable
    private data class EpisodeInfo(
        // Episode "numbers" can have decimal values, hence float
        @SerialName("episodeIdNum") val episodeIdNum: Float,
        @SerialName("notes") val notes: String?,
        @SerialName("thumbnails") val thumbnails: List<String>?,
        @SerialName("vidInforssub") val vidInforssub: VidInfo?,
        @SerialName("vidInforsdub") val vidInforsdub: VidInfo?,
    ) {
        @Serializable
        data class VidInfo(
            // @SerialName("vidPath") val vidPath
            @SerialName("vidResolution") val vidResolution: Int?,
            @SerialName("vidSize") val vidSize: Double?,
        )
    }

    @Serializable
    private data class ApiSourceResponse(@SerialName("links") val links: List<ApiLink>) {

        @Serializable
        data class ApiLink(
            @SerialName("link") val link: String?,
            @SerialName("src") val src: String?,
            @SerialName("hls") val hls: Boolean?,
            @SerialName("mp4") val mp4: Boolean?,
            @SerialName("resolutionStr") val resolutionStr: String,
        )
    }

}


