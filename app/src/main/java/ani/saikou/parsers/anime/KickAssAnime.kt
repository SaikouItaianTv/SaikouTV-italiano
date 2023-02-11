package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.Mapper
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.*
import kotlinx.serialization.Serializable
import java.net.URLDecoder

class KickAssAnime : AnimeParser() {

    override val name: String = "KickAssAnime"
    override val saveName: String = "kick_ass_anime"
    override val hostUrl: String = "https://www2.kickassanime.ro"
    override val isDubAvailableSeparately: Boolean = true

    override suspend fun loadEpisodes(
        animeLink: String,
        extra: Map<String, String>?
    ): List<Episode> {
        val tag = client.get(animeLink).document.getElementsByTag("script")[5].toString()

        val jsonSlice =
            "{\"episodes\":".plus(
                tag.substringAfter("appData = ")
                    .substringAfter("episodes\":")
                    .substringBefore(",\"types\"")
                    .plus("}")
            )

        val json = Mapper.parse<SearchResponseEpisodes>(jsonSlice)

        return json.episodes
            .mapNotNull { episode ->
                Episode(
                    number = episode.num,
                    link = hostUrl + episode.slug,
                    title = episode?.name ?: episode.epnum
                )
            }
            .asReversed()
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val encoded = encode(query + if (selectDub) " (Dub)" else "")
        val tag =
            client
                .get("$hostUrl/search?q=$encoded")
                .document
                .getElementsByTag("script")[5]
                .toString()

        // The JSON is contained within a script tag which defines some "appData"
        // We first slice off all of this and then the contained "animes" field within
        // The JSON object then ends with "|| {}," which we have as a definite ending
        // so we can slice between the animes and "|| {},", which will contain an array
        // of anime objects
        val jsonSlice =
            "{\"animes\":".plus(
                tag.substringAfter("appData = ")
                    .substringAfter("animes\":")
                    .substringBefore(",\"query\"")
                    .plus("}")
            )
        val json = Mapper.parse<SearchResponse>(jsonSlice)

        return json.animes.mapNotNull { anime ->
            // KA seems to have no way to differentiate between Dub and Sub
            // so we simply remove those with "(Dub)" in the title.
            if (!selectDub && anime.name.contains("(Dub)")) return@mapNotNull null
            ShowResponse(
                name = anime.name,
                link = hostUrl + anime.slug,
                coverUrl = FileUrl(anime.image + anime.poster, mapOf(
                    "User-Agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:101.0) Gecko/20100101 Firefox/101.0"
                )),
            )
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String,String>?): List<VideoServer> {

        val (kaastLinks, externalServers) = this.getVideoPlayerLink(episodeLink)

        val serverLinks = mutableListOf<VideoServer>()
        kaastLinks.forEach { pageLink ->
            if (pageLink == null || pageLink.isBlank()) return@forEach

            // These are not yet implemented
            if (pageLink.contains("mobile")) return@forEach

            val tag =
                client.get(pageLink).document.getElementsByTag("script").reversed()[7].toString()
            val jsonSlice =
                "{\"sources\":"
                    .plus(tag.substringAfter("var sources = ").substringBefore("}];"))
                    .plus("}]}")

            val json = Mapper.parse<EpisodePage>(jsonSlice)

            json.sources.forEach { server ->
                serverLinks.add(videoServerTemplate(server.name, server.src))
            }
        }

        externalServers.forEach { server ->
            serverLinks.add(videoServerTemplate(server.name, server.link))
        }
        return serverLinks
    }

    suspend fun getVideoPlayerLink(
        episodeLink: String
    ): Pair<KAASTLink.KAASTObject, MutableList<KAASTLink.ExternalServer>> {
        val tag =
            client
                .get(episodeLink)
                .document
                .getElementsByTag("script")[6]
                .toString()
                .substringAfter("appData = {")
                .substringAfter("\"episode\":")
                .substringBefore(",\"episodes\"")

        val slice =
            "{ \"episode\": ${
                tag.substringBefore(",\"ext_servers\"")
            },\"ext_servers\"${
                tag.substringAfter(",\"ext_servers\"")
            }}"

        val json = Mapper.parse<KAASTLink>(slice)

        listOf(
            json.episode.link1,
            json.episode.link2,
            json.episode.link3,
            json.episode.link4
        )
            .forEachIndexed { index, link ->
                // We can't use listOfNotNull because that will mess up
                // the indexes
                if (link == null) return@forEachIndexed

                if (link.contains("addkaa")) {
                    when (index + 1) {
                        1 -> json.episode.link1 = ""
                        2 -> json.episode.link2 = ""
                        3 -> json.episode.link3 = ""
                        4 -> json.episode.link4 = ""
                    }
                    json.ext_servers.add(KAASTLink.ExternalServer(name = "VidCDN", link = link))
                }
            }
        return Pair(json.episode, json.ext_servers)
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null

        if (server.extraData !is Map<*, *>) return null

        val referralServer = {
            var url = server.extraData.get("data_segment").toString()
            url = if (!url.startsWith("https:")) {
                "https:$url"
            } else {
                url
            }

            val cleanedURL =
                when (server.extraData.get("id")) {
                    "streamsb" -> {
                        val regex = "(?<=embed-).+(?=\\.html)".toRegex()
                        val id = regex.find(url)
                        FileUrl(
                            "https://sbplay.org/e/${id?.value}.html?referer=&",
                            server.embed.headers
                        )
                    }
                    "kickassanimev2" ->
                        FileUrl(
                            server.embed.url.replace("embed.php", "pref.php"),
                            server.embed.headers
                        )
                    "betaplayer" ->
                        server.embed
                    "vidstreaming" ->
                        FileUrl(
                            url,
                            server.embed.headers
                        )
                    "vidcdn",
                    "addkaa" -> {
                        FileUrl(
                            url.replace("load.php", "streaming.php"),
                            server.embed.headers
                        )
                    }
                    "pink-bird" ->
                        FileUrl(
                            server.embed.url.replace(Regex("player[0-9]?.php"), "pref.php"),
                            server.embed.headers
                        )
                    "xstreamcdn" ->
                        server.embed
                    else ->
                        FileUrl(
                            url,
                            server.embed.headers
                        )
                }

            VideoServer(server.name, cleanedURL)
        }
        val extractor: VideoExtractor? =
            when (server.extraData.get("id")) {
                "pink-bird"      -> PinkBird(referralServer())
                "streamsb"       -> StreamSB(referralServer())
                "maverickki"     -> Maverickki(server)
                "vidstreaming"   -> VidStreaming(referralServer())
                "betaplayer"     -> BetaPlayer(referralServer())
                "gogo",
                "vidcdn",
                "addkaa"         -> GogoCDN(referralServer())
                "kickassanimev2" -> KickAssAnimeV2(referralServer())
                "streamtape"     -> StreamTape(referralServer())
                "xstreamcdn" -> FPlayer(referralServer())
                else             -> null
            }

        return extractor
    }

    private fun videoServerTemplate(name: String, src: String): VideoServer {
        return VideoServer(
            name = name,
            embed =
            FileUrl(
                url = URLDecoder.decode(src, "utf-8"),
                headers =
                mapOf(
                    "Accept" to
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Referer" to "https://kaast1.com/",
                    "Sec-Fetch-Dest" to "iframe"
                )
            ),
            extraData =
            mapOf(
                "id" to name.lowercase(),
                "data_segment" to
                        URLDecoder.decode(src.split("&data=")?.elementAtOrNull(1) ?: "", "utf-8")
            )
        )
    }

    @Serializable
    data class SearchResponseEpisodes(val episodes: List<EpisodeSearchResult>) {
        @Serializable
        data class EpisodeSearchResult(
            val epnum: String,
            val name: String?,
            val slug: String,
            val createddate: String,
            val num: String,
        )
    }

    @Serializable
    data class SearchResponse(val animes: List<AnimeSearchResult>) {
        @Serializable
        data class AnimeSearchResult(
            val name: String,
            val slug: String, // NOTE: This is just the SLUG, not the entire absolute url
            val poster: String, // The actual posterID (e.g 40319.jpg)
            val image:
            String, // The directory which contains poster images (e.g
            // www2.kickassanime.ro/uploads)
        )
    }

    @Serializable
    data class KAASTLink(val episode: KAASTObject, val ext_servers: MutableList<ExternalServer>) {

        @Serializable
        data class KAASTObject(

            // NOTE: name, title, slug, dub are also available from this object
            var link1: String?,
            var link2: String?,
            var link3: String?,
            var link4: String?,
        ) {

            // This allows us to do `episode.forEach { link -> ... }`
            suspend fun forEach(action: suspend (String) -> Unit) {
                listOfNotNull(link1, link2, link3, link4).forEach { action(it) }
            }
        }

        @Serializable
        data class ExternalServer(
            val name: String,
            val link: String,
        )
    }

    @Serializable
    data class EpisodePage(val sources: List<EpisodePageJSON>) {
        @Serializable
        data class EpisodePageJSON(
            val name: String,
            val src: String,
            val rawSrc: String,
        )
    }
}
