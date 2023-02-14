package ani.saikou.parsers.anime.extractors

import android.util.Log
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.*


class OK(override val server: VideoServer) : VideoExtractor() {

    override suspend fun extract(): VideoContainer {
        val url = server.embed.url
        val document = client.get(url).document
        val videos = mutableListOf<Video>()
        val videosString = document.select("div[data-options]").attr("data-options")
            .substringAfter("\\\"videos\\\":[{\\\"name\\\":\\\"")
            .substringBefore("]")
        videosString.split("{\\\"name\\\":\\\"").reversed().forEach {
            val videoUrl = it.substringAfter("url\\\":\\\"")
                .substringBefore("\\\"")
                .replace("\\\\u0026", "&")
            var extraNote = ""
            var videoQuality = /*"Okru: " + */it.substringBefore("\\\"")


            if(videoQuality == "lowest") {
                extraNote = "Lowest"
                videoQuality = 240.toString()}
            if(videoQuality == "low") {
                extraNote = "Low"
                videoQuality = 360.toString()}
            if(videoQuality == "mobile") {
                extraNote = "Mobile"
                videoQuality = 140.toString()}
            if(videoQuality == "sd") {
                extraNote = "SD"
                videoQuality = 480.toString()}
            if(videoQuality == "hd") {
                extraNote = "HD"
                videoQuality = 720.toString()}
            if(videoQuality == "full") {
                extraNote = "FULL HD"
                videoQuality = 1080.toString()} //why the quality can not be a string ???




            if (videoUrl.startsWith("https://")) {
                videos.add(Video(videoQuality.toInt(),VideoType.CONTAINER, videoUrl,size = getSize(videoUrl),extraNote = extraNote))
            }
        }
        return VideoContainer(videos)
    }
}