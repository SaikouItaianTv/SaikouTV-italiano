package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.*

class Doodstream(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        val response = client.get(server.embed.url.replace("/d/","/e/")).text // html of DoodStream page to look for /pass_md5/...
        val hostUrl = server.embed.url.replace("/d/","/e/").substringBefore("/e/")
        val md5 = hostUrl + (Regex("/pass_md5/[^']*").find(response)?.value)  // get https://dood.ws/pass_md5/...
        val link = client.get(md5, referer = server.embed.url).text + "6bD48KyCHM?token=" + md5.substringAfterLast("/")   //direct link to extract  (6bD48KyCHM is random)

        if (link.contains("not found")) return VideoContainer(listOf())
        return VideoContainer(
                listOf( Video(
                    null,
                    VideoType.CONTAINER,
                    FileUrl(link, mapOf("Referer" to hostUrl)),
                    getSize(link)
                )))
    }
}