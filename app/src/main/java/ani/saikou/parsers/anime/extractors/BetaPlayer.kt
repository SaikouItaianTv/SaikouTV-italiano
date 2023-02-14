package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BetaPlayer(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        // This needs to be implemented differently
        // but I need to find an anime which uses https://kaast1.com/betaplayer/ instead of
        // betaplayer.life first
        if (server.embed.url.contains("kaast1")) return VideoContainer(listOf())

        val res =
            client
                .get(server.embed.url, server.embed.headers)
                .text
                .substringAfter("JSON.parse('")
                .substringBefore("')")

        val json = Mapper.parse<BetaPlayerJSON>("{\"files\":$res}")
        return VideoContainer(
            json.files.map() { video ->
                return@map Video(
                    video.quality.removeSuffix("p").toInt(),
                    VideoType.CONTAINER,
                    FileUrl(video.link, server.embed.headers),
                )
            },
        )
    }

    @Serializable
    data class BetaPlayerJSON(val files: List<BetaPlayerFiles>) {
        @Serializable
        data class BetaPlayerFiles(
            @SerialName("label") val quality: String,
            @SerialName("file") val link: String,
            val type: String,
        )
    }
}