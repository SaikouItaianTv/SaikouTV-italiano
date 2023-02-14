package ani.saikou

import android.content.Context
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import com.lagradost.nicehttp.addGenericDns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.io.PrintWriter
import java.io.Serializable
import java.io.StringWriter
import java.util.concurrent.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

val defaultHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"
)
lateinit var cache: Cache

lateinit var okHttpClient: OkHttpClient
lateinit var client: Requests

fun initializeNetwork(context: Context) {
    val dns = loadData<Int>("settings_dns", context)
    cache = Cache(
        File(context.cacheDir, "http_cache"),
        5 * 1024L * 1024L // 5 MiB
    )
    okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(cache)
        .apply {
            when (dns) {
                1 -> addGoogleDns()
                2 -> addCloudFlareDns()
                3 -> addAdGuardDns()
            }
        }
        .build()
    client = Requests(
        okHttpClient,
        defaultHeaders,
        defaultCacheTime = 6,
        defaultCacheTimeUnit = TimeUnit.HOURS,
        responseParser = Mapper
    )
}

object Mapper : ResponseParser {

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(InternalSerializationApi::class)
    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return json.decodeFromString(kClass.serializer(),text)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            parse(text, kClass)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return json.encodeToString(serializer(),obj)
    }

    inline fun <reified T> parse(text: String): T {
        return json.decodeFromString(text)
    }
}

fun <A, B> Collection<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}

fun <A, B> Collection<A>.asyncMapNotNull(f: suspend (A) -> B?): List<B> = runBlocking {
    map { async { f(it) } }.mapNotNull { it.await() }
}

fun logError(e: Exception) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    val stackTrace: String = sw.toString()

    toastString(e.localizedMessage, null , stackTrace)
    e.printStackTrace()
}

fun <T> tryWith(call: () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    }
}

suspend fun <T> tryWithSuspend(call: suspend () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    } catch (e: CancellationException) {
        null
    }
}

/**
 * A url, which can also have headers
 * **/
data class FileUrl(
    val url: String,
    val headers: Map<String, String> = mapOf()
) : Serializable {
    companion object {
        operator fun get(url: String?, headers: Map<String, String> = mapOf()): FileUrl? {
            return FileUrl(url ?: return null, headers)
        }
    }
}

//Credits to leg
data class Lazier<T>(
    val lClass: KFunction<T>,
    val name: String
) {
    val get = lazy { lClass.call() }
}

fun <T> lazyList(vararg objects: Pair<String, KFunction<T>>): List<Lazier<T>> {
    return objects.map {
        Lazier(it.second, it.first)
    }
}


fun OkHttpClient.Builder.addGoogleDns() = (
        addGenericDns(
            "https://dns.google/dns-query",
            listOf(
                "8.8.4.4",
                "8.8.8.8"
            )
        ))

fun OkHttpClient.Builder.addCloudFlareDns() = (
        addGenericDns(
            "https://cloudflare-dns.com/dns-query",
            listOf(
                "1.1.1.1",
                "1.0.0.1",
                "2606:4700:4700::1111",
                "2606:4700:4700::1001"
            )
        ))

fun OkHttpClient.Builder.addAdGuardDns() = (
        addGenericDns(
            "https://dns.adguard.com/dns-query",
            listOf(
                // "Non-filtering"
                "94.140.14.140",
                "94.140.14.141",
            )
        ))