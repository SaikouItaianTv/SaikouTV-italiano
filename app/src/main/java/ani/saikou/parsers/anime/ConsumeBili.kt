package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ConsumeBili : AnimeParser() {
    override val name = "Consume Bili"
    override val saveName = "consume-bili"
    override val isDubAvailableSeparately = false
    override val hostUrl = "https://api-vn.kaguya.app/server"

    override suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse {
        return ShowResponse(
            link = mediaId.toString(),
            name = "",
            coverUrl = ""
        )
    }

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val episodes = client.get("${hostUrl}/anime/episodes?id=${animeLink}&source_id=bilibili").parsed<EpisodesResponse>()
        if (!episodes.success)
            return emptyList()

        return episodes.episodes!!.map {
            val extraData =
                mapOf("sourceEpisodeId" to it.sourceEpisodeId, "sourceMediaId" to it.sourceMediaId, "sourceId" to it.sourceId)
            val title = "Episode ${it.name}"
            Episode(
                number = it.name,
                title = title,
                link = "${it.sourceEpisodeId}-${it.sourceMediaId}-${it.sourceId}",
                extra = extraData
            )
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String,String>?): List<VideoServer> {
        val extraData = extra as Map<*, *>
        val sourceEpisodeId = extraData["sourceEpisodeId"]
        val sourceMediaId = extraData["sourceMediaId"]
        val sourceId = extraData["sourceId"]
        val sources =
            client.get("${hostUrl}/source?episode_id=${sourceEpisodeId}&source_media_id=${sourceMediaId}&source_id=${sourceId}")
                .parsed<SourcesResponse>()

        if (!sources.success)
            return emptyList()

        val modifiedSources = sources.sources.map {
            val source = it
            val file = FileUrl(url = source.file)
            Video(null, VideoType.DASH, file, null)
        }
        val modifiedSubtitles = sources.subtitles?.map {
            Subtitle(
                language = it.lang,
                url = it.file,
                type = SubtitleType.VTT
            )
        } ?: listOf()

        return listOf(
            VideoServer(
                name = "Server",
                embed = FileUrl(url = ""),
                extraData = VideoContainer(modifiedSources,modifiedSubtitles)
            )
        )
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor {
        return BilibiliExtractor(server)
    }

    class BilibiliExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            return server.extraData as VideoContainer
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        return emptyList()
    }

    @Serializable
    data class EpisodesResponse(
        @SerialName("success") val success: Boolean,
        @SerialName("episodes") val episodes: List<SourceEpisode>? = null
    )

    @Serializable
    data class SourcesResponse(
        @SerialName("success") val success: Boolean,
        @SerialName("sources") val sources: List<VideoSource>,
        @SerialName("subtitles") val subtitles: List<VideoSubtitle>?
    )

    @Serializable
    data class VideoSource(
        @SerialName("file") val file: String,
        @SerialName("label") val label: String?,
        @SerialName("useProxy") val useProxy: Boolean?,
        @SerialName("proxy") val proxy: Proxy?,
        @SerialName("type") val type: String?
    )

    @Serializable
    data class Proxy(
        @SerialName("appendReqHeaders") val appendReqHeaders: Map<String, String>?
    )

    @Serializable
    data class VideoSubtitle(
        @SerialName("file") val file: String,
        @SerialName("lang") val lang: String,
        @SerialName("language") val language: String,
    )

    @Serializable
    data class SourceEpisode(
        @SerialName("name") val name: String,
        @SerialName("sourceId") val sourceId: String,
        @SerialName("sourceEpisodeId") val sourceEpisodeId: String,
        @SerialName("sourceMediaId") val sourceMediaId: String,
        @SerialName("slug") val slug: String,
        @SerialName("sourceConnectionId") val sourceConnectionId: String,
        @SerialName("section") val section: String
    )
}
