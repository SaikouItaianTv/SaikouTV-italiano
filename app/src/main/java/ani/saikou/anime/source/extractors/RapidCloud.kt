package ani.saikou.anime.source.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

class RapidCloud : Extractor() {
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val qualities = arrayListOf<Episode.Quality>()
        val subtitle = mutableMapOf<String,String>()

        val soup = Jsoup.connect(url).referrer("https://zoro.to/").get().toString().replace("\n","")
        val key = soup.findBetween("var recaptchaSiteKey = '","',")
        val number = soup.findBetween("recaptchaNumber = '","';")
        if(key!=null && number!=null){
            captcha(url,key).apply {
                val jsonLink = "https://rapid-cloud.ru/ajax/embed-6/getSources?id=${url.findBetween("/embed-6/", "?z=")!!}&_token=${this?:return@apply}&_number=$number"
                val json = Json.decodeFromString<JsonObject>(Jsoup.connect(jsonLink).ignoreContentType(true).execute().body())
                val m3u8 = json["sources"]!!.jsonArray[0].jsonObject["file"].toString().trim('"')

                json["tracks"]!!.jsonArray.forEach {
                    if (it.jsonObject["kind"].toString().trim('"') == "captions")
                        subtitle[it.jsonObject["label"].toString().trim('"')] = it.jsonObject["file"].toString().trim('"')
                }
                qualities.add(Episode.Quality(m3u8, "Multi Quality", null))
            }
        }

        return Episode.StreamLinks(
            name,
            qualities,
            null,
            subtitle
        )
    }

    private fun captcha(url: String, key:String):String?{
        val uri = Uri.parse(url)
        val domain = (Base64.encodeToString((uri.scheme + "://" + uri.host + ":443").encodeToByteArray(), Base64.NO_PADDING)+".").replace("\n","")
        val vToken=Jsoup.connect("https://www.google.com/recaptcha/api.js?render=$key").referrer(uri.scheme + "://" + uri.host ).get().toString().replace("\n","").findBetween("/releases/","/recaptcha")?:return null
        val recapToken = Jsoup.connect("https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=kr60249sk&k=$key&co=$domain&v=$vToken").get().selectFirst("#recaptcha-token")?.attr("value")?:return null
        return Jsoup.connect("https://www.google.com/recaptcha/api2/reload?k=$key").ignoreContentType(true)
                .data(mutableMapOf("v" to vToken,"k" to key,"c" to recapToken,"co" to domain,"sa" to "","reason" to "q"))
                .post().toString().replace("\n","").findBetween("rresp\",\"","\",null")
    }
}