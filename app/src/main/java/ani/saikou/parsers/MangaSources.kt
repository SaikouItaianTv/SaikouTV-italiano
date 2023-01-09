package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.manga.*

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "MangaWorld" to ::MangaWorld,
        "INGLESE: MangaKakalot" to ::MangaKakalot,
        "INGLESE: MangaBuddy" to ::MangaBuddy,
        "INGLESE: MangaPill" to ::MangaPill,
        "INGLESE: MangaDex" to ::MangaDex,
        "INGLESE: MangaReaderTo" to ::MangaReaderTo,
        "INGLESE: AllAnime" to ::AllAnime,
        "INGLESE: Toonily" to ::Toonily,
        "INGLESE: MangaHub" to ::MangaHub,
        "INGLESE: MangaKatana" to ::MangaKatana,
        "INGLESE: Manga4Life" to ::Manga4Life,
        "INGLESE: MangaRead" to ::MangaRead,
        "INGLESE: ComickFun" to ::ComickFun,

    )
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList(
        "MangaWorldAdult" to ::MangaWorldAdult,
        "INGLESE: NineHentai" to ::NineHentai,
        "INGLESE: Manhwa18" to ::Manhwa18,
        "INGLESE: NHentai" to ::NHentai,

    )
    override val list = listOf(aList,MangaSources.list).flatten()
}
