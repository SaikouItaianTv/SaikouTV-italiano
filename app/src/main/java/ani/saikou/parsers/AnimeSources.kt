package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "AnimeWorld" to ::AnimeWorld,
        "AnyPlay" to ::AniPlay,
        "AnimeSaturn" to ::AnimeSaturn,
        "MULTILINGUA: Kamyroll" to ::Kamyroll,

        "INGLESE: AllAnime" to ::AllAnime,
        "INGLESE: Gogo" to ::Gogo,
        "INGLESE: Zoro" to ::Zoro,
        "INGLESE: Marin" to ::Marin,
//        "INGLESE: 9Anime" to ::NineAnime,
        "INGLESE: AnimePahe" to ::AnimePahe,
        "INGLESE: ConsumeBili" to ::ConsumeBili

    )
}

object HAnimeSources : WatchSources() {
    val aList: List<Lazier<BaseParser>>  = lazyList(
        "HentaiWorld" to ::HentaiWorld,
        "HentaiSaturn" to ::HentaiSaturn,

        "INGLESE: HentaiMama" to ::HentaiMama,
        "INGLESE: Haho" to ::Haho,
        "INGLESE: HentaiStream" to ::HentaiStream,
        "INGLESE: HentaiFF" to ::HentaiFF,

    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
