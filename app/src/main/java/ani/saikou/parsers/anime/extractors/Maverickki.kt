package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.Serializable

class Maverickki(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val host = "https://" + Uri.parse(server.embed.url).host
        val res =
            client
                .get(server.embed.url.replace("/embed/", "/api/source/"), server.embed.headers)
                .body
                .string()
        val json = Mapper.parse<MaverickkiResponse>(res)

        val videos = listOf(Video(null, VideoType.M3U8, host + json.hls))

        val subtitles =
            json.subtitles?.map { sub ->
                Subtitle(
                    sub.name,
                    host + sub.src,
                    // SubtitleType.VTT // Let this be auto-filled
                )
            }
                ?: listOf()

        return VideoContainer(videos, subtitles)
    }

    @Serializable
    data class MaverickkiResponse(val hls: String, val subtitles: List<MaverickkiSubtitles>?) {
        @Serializable
        data class MaverickkiSubtitles(
            val name: String,
            val src: String,
        )
    }
}
