package ani.saikou.parsers.anime

import android.util.Base64
import ani.saikou.FileUrl
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class PinkBird(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val slice = client.get(server.embed.url, server.embed.headers).body.string()
        val json = Mapper.parse<PinkDuckJSON>(slice)

        return VideoContainer(
            videos =
            json.sources.map { source ->
                return@map Video(
                    null,
                    VideoType.M3U8,
                    FileUrl(
                        "https://pb.kaast1.com/manifest/${Base64.decode(source.encodedBase64, Base64.NO_WRAP).decodeToString()}/master.m3u8",
                        server.embed.headers
                    ),
                )
            }
        )
    }

    @Serializable
    data class PinkDuckJSON(
        @SerialName("data") val sources: List<PinkDuckSource>,
    ) {
        @Serializable
        data class PinkDuckSource(
            @SerialName("eid") val encodedBase64: String,
        )
    }
}