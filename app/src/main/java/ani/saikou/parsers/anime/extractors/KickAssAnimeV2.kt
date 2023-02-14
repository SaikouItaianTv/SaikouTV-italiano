package ani.saikou.parsers.anime

import android.util.Base64
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class KickAssAnimeV2(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val tag =
            client
                .get(server.embed.url, server.embed.headers)
                .document
                .getElementsByTag("script")[4]
                .toString()

        val encoded = tag.substringAfter("atob(\"").substringBefore("\")")
        val decoded = Base64.decode(encoded, Base64.NO_WRAP).decodeToString()
        val slice = "{\"sources\":${decoded.substringAfter("sources:").substringBefore("}],")}}]}"

        val json = Mapper.parse<KickAssAnimeV2JSON>(slice)

        return VideoContainer(
            videos =
            json.sources.map { source ->
                return@map Video(
                    source.quality.removeSuffix("p").toInt(),
                    if (source.type == "video/mp4") VideoType.CONTAINER else VideoType.M3U8,
                    source.link,
                )
            }
        )
    }

    @Serializable
    data class KickAssAnimeV2JSON(val sources: List<KickAssAnimeV2Links>) {
        @Serializable
        data class KickAssAnimeV2Links(
            @SerialName("file") val link: String,
            @SerialName("label") val quality: String,
            val type: String
        )
    }
}