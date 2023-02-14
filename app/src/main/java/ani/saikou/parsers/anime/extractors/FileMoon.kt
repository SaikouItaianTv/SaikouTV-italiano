package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.others.JsUnpacker
import ani.saikou.parsers.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
fun getPacked(string: String): String? {
    return packedRegex.find(string)?.value
}

fun getAndUnpack(string: String): String {
    val packedText = getPacked(string)
    return JsUnpacker(packedText).unpack() ?: string
}
class FileMoon(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val prova = client.get(server.embed.url)
        val unpacked = getAndUnpack(prova.text)
        val link = Regex("file:\"(.+?)\"").find(unpacked)?.groupValues?.last()

        link?.let {
            return VideoContainer(
                listOf(
                    Video(
                        null,
                        VideoType.M3U8,
                        FileUrl(link, mapOf("Referer" to server.embed.url)),
                        getSize(link)
                    )
                )
            )
        }
        return VideoContainer(listOf())

    }
    @Serializable
    private data class DataLink(
        @SerialName("status") val status: String? = null,
        @SerialName("message") val message: String? = null,
        @SerialName("type") val type: String? = null,
        @SerialName("token") val token: String? = null,
        @SerialName("result") val result: Result? = null
    )

    @Serializable
    private data class Result(
        @SerialName("Original") val original: Original? = null
    )

    @Serializable
    private data class Original(
        @SerialName("label") val label: String? = null,
        @SerialName("file") val file: String? = null,
        @SerialName("type") val type: String? = null
    )
}