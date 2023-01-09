package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "AnimeWorld" to ::AnimeWorld,
        "AnyPlay" to ::AniPlay,
        "AnimeSaturn" to ::AnimeSaturn,
        "Kamyroll" to ::Kamyroll,

        "AllAnime" to ::AllAnime,
        "Gogo" to ::Gogo,
        "Zoro" to ::Zoro,
        "Tenshi" to ::Tenshi,
        "9Anime" to ::NineAnime,
        "9Anime Backup" to ::AniWatchPro,
        "AnimePahe" to ::AnimePahe,
        "ConsumeBili" to ::ConsumeBili

    )
}

object HAnimeSources : WatchSources() {
    val aList: List<Lazier<BaseParser>>  = lazyList(
        "HentaiWorld" to ::HentaiWorld,
        "HentaiSaturn" to ::HentaiSaturn,

        "HentaiMama" to ::HentaiMama,
        "Haho" to ::Haho,
        "HentaiStream" to ::HentaiStream,
        "HentaiFF" to ::HentaiFF,

    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
