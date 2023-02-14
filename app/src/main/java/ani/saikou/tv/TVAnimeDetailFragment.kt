package ani.saikou.tv

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.R
import ani.saikou.anime.Episode
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.parsers.AnimeSources
import ani.saikou.parsers.HAnimeSources
import ani.saikou.settings.UserInterfaceSettings
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.components.HeaderOnlyRow
import ani.saikou.tv.presenters.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.*
import kotlin.math.ceil
import kotlin.math.min

class TVAnimeDetailFragment() : DetailsSupportFragment() {

    private val model: MediaDetailsViewModel by activityViewModels()
    private val scope = lifecycleScope
    val actions = ArrayObjectAdapter(DetailActionsPresenter())
    lateinit var uiSettings: UserInterfaceSettings
    var loaded = false
    lateinit var media: Media

    private var linkSelector: TVSelectorFragment? = null
    private var descriptionPresenter: DetailsWatchPresenter? = null

    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController

    private lateinit var rowsAdapter: ArrayObjectAdapter
    var episodePresenters = mutableListOf<ArrayObjectAdapter>()
    private lateinit var detailsOverview: DetailsOverviewRow

    private var episodeObserver = Observer<Episode?> {
        if (it != null){
            MainScope().launch {
                it.extractors
                linkSelector?.setStreamLinks(it.extractors)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        media = requireActivity().intent.getSerializableExtra("media") as Media

        detailsBackground = DetailsSupportFragmentBackgroundController(this)
        uiSettings = loadData("ui_settings", toast = false)
            ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBarManager.setRootView(this.view as ViewGroup)
        progressBarManager.initialDelay =0
        progressBarManager.show()
        buildDetails()
        observeData()
    }

    override fun onPause() {
        super.onPause()
        loaded = false
    }

    private fun buildDetails() {
        model.watchSources = if (media.isAdult) HAnimeSources else AnimeSources
        val selected = model.loadSelected(media)
        media.selected = selected

        val selector = ClassPresenterSelector().apply {
            descriptionPresenter = DetailsWatchPresenter()
            FullWidthDetailsOverviewRowPresenter(descriptionPresenter).also {
                it.backgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
                it.actionsBackgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
                it.setOnActionClickedListener { action ->
                    processAction(action)
                }
                addClassPresenter(DetailsOverviewRow::class.java, it)
            }
            val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
            presenter.shadowEnabled = false
            addClassPresenter(ListRow::class.java, presenter)
            addClassPresenter(HeaderOnlyRow::class.java, HeaderRowPresenter())
        }

        rowsAdapter = ArrayObjectAdapter(selector)

        detailsOverview = DetailsOverviewRow(media)
        detailsOverview.actionsAdapter = actions

        adapter = rowsAdapter

        initializeBackground()
        initializeCover()
    }

    fun observeData() {

        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null && media.id == it.id) {
                media = it
                media.selected = model.loadSelected(media)

                if (!loaded) {
                    media.selected?.let { sel ->
                        model.watchSources?.get(sel.source)?.let {parser ->
                            descriptionPresenter?.userText = parser.showUserText
                            parser.showUserTextListener = {
                                MainScope().launch {
                                    descriptionPresenter?.userText = it
                                    adapter.notifyItemRangeChanged(0, 1)
                                }
                            }
                        }
                    }

                    setupActions()

                    lifecycleScope.launch(Dispatchers.IO) {
                        awaitAll(
                            async { model.loadKitsuEpisodes(media) },
                            async { model.loadFillerEpisodes(media) }
                        )

                        model.loadEpisodes(media, media.selected!!.source)
                    }

                    finishLoadingRows()
                } else {
                    adapter.notifyItemRangeChanged(0, adapter.size())
                }
            }
        }

        model.getEpisodes().observe(viewLifecycleOwner) { loadedEpisodes ->
            loadedEpisodes?.let { epMap ->
                val episodes = loadedEpisodes[media.selected!!.source]

                if (episodes != null) {
                    episodes.forEach { (i, episode) ->
                        if (media.anime?.fillerEpisodes != null) {
                            if (media.anime!!.fillerEpisodes!!.containsKey(i)) {
                                episode.title = episode.title ?: media.anime!!.fillerEpisodes!![i]?.title
                                episode.filler = media.anime!!.fillerEpisodes!![i]?.filler ?: false
                            }
                        }
                        if (media.anime?.kitsuEpisodes != null) {
                            if (media.anime!!.kitsuEpisodes!!.containsKey(i)) {
                                episode.desc = episode.desc ?: media.anime!!.kitsuEpisodes!![i]?.desc
                                episode.title = episode.title ?: media.anime!!.kitsuEpisodes!![i]?.title
                                episode.thumb = episode.thumb?: media.anime!!.kitsuEpisodes!![i]?.thumb ?: FileUrl[media.cover]
                            }
                        }
                    }
                    media.anime?.episodes = episodes

                    //CHIP GROUP
                    val total = episodes.size
                    val divisions = total.toDouble() / 10
                    val limit = when {
                        (divisions < 25) -> 25
                        (divisions < 50) -> 50
                        else -> 100
                    }

                    clearEpisodes()
                    if (total == 0) {
                        rowsAdapter.add(HeaderOnlyRow(getString(R.string.source_not_found)))
                    } else if (total > limit) {
                        val arr = episodes.keys.toList()
                        val numberOfChips = ceil((total).toDouble() / limit).toInt()

                        for (index in 0..numberOfChips - 1) {
                            val last =
                                if (index + 1 == numberOfChips) total else (limit * (index + 1))
                            val startIndex = limit * (index)
                            val start = arr[startIndex]
                            val end = arr[last - 1]
                            createEpisodePresenter(getString(R.string.eps) + " ${start} - ${end}").addAll(
                                0,
                                episodes.values.toList().subList(
                                    startIndex,
                                    min(startIndex + limit, episodes.values.size)
                                )
                            )
                        }
                        focusLastViewedEpisode(episodes)
                    } else {
                        createEpisodePresenter(getString(R.string.eps)).addAll(
                            0,
                            episodes.values.toList()
                        )
                        focusLastViewedEpisode(episodes)
                    }
                }
            }
        }

        model.getEpisode().observeForever(episodeObserver)


        model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.kitsuEpisodes = i
        }

        model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.fillerEpisodes = i
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch(Dispatchers.IO) {
                    model.loadMedia(media)
                    live.postValue(false)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        media.selected?.let { sel ->
            model.watchSources?.get(sel.source)?.showUserTextListener = null
        }
    }

    fun focusLastViewedEpisode(episodes: MutableMap<String, Episode>?) {
        Handler(Looper.getMainLooper()).post (java.lang.Runnable {
            try {
                media.userProgress?.let { progress ->
                    val episode =
                        episodes?.values?.firstOrNull() { it.number.toFloatOrNull() ?: 9999f >= progress.toFloat() }
                    episode.let {
                        episodePresenters.forEachIndexed { row, arrayObjectAdapter ->
                            val index = arrayObjectAdapter.indexOf(episode)
                            if (index != -1) {
                                rowsSupportFragment.setSelectedPosition(
                                    row + 1,
                                    false,
                                    ListRowPresenter.SelectItemViewHolderTask(index + 1)
                                )
                                rowsSupportFragment.setSelectedPosition(
                                    0,
                                    false,
                                    ListRowPresenter.SelectItemViewHolderTask(0)
                                )
                                return@Runnable
                            }
                        }
                    }
                }
            }catch (e: Exception){

            }
        })
    }

    fun onEpisodeClick(i: String) {
        model.continueMedia = false

        val episode = media.anime?.episodes?.get(i)
        if (episode != null) {
            media.anime!!.selectedEpisode = i
        } else {
            return
        }

        media.selected = model.loadSelected(media)
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            media.selected?.let {
                model.loadEpisodeVideos(episode, it.source)
            }
        }
        linkSelector = TVSelectorFragment.newInstance(media)
        parentFragmentManager.beginTransaction().addToBackStack("detail")
            .replace(R.id.main_detail_fragment, linkSelector!!)
            .commit()
    }

    private fun createEpisodePresenter(title: String): ArrayObjectAdapter {
        val adapter = ArrayObjectAdapter(EpisodePresenter(1, media, this))
        episodePresenters.add(adapter)
        rowsAdapter.add(ListRow(HeaderItem(1, title), adapter))
        return adapter
    }

    private fun clearEpisodes() {
        rowsAdapter.removeItems(1, rowsAdapter.size() - 1)
        episodePresenters.clear()
    }

    private fun finishLoadingRows() {
        rowsAdapter.add(detailsOverview)
        rowsAdapter.add(HeaderOnlyRow(null))
        progressBarManager.hide()
        loaded = true
    }

    override fun onDestroy() {
        model.getEpisode().removeObserver(episodeObserver)
        super.onDestroy()
    }

    private fun initializeBackground() {
        detailsBackground.solidColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
        detailsBackground.enableParallax()
        Glide.with(this)
            .asBitmap()
            .centerInside()
            .load(media.banner)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    detailsBackground.coverBitmap = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun initializeCover() {
        Glide.with(this)
            .asBitmap()
            .load(media.cover)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    detailsOverview.apply {
                        imageDrawable = resource.toDrawable(requireActivity().resources)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setupActions() {
        actions.clear()

        val selected = model.loadSelected(media)
        media.selected = selected

        selected.source.let {
            val source = model.watchSources?.get(selected.source)
            actions.add(DetailActionsPresenter.SourceAction(0, getString(R.string.source) +": " + source?.name))
            if(source?.isDubAvailableSeparately == true){
                actions.add(DetailActionsPresenter.SwitchAction(3,  getString(R.string.dubbed), selected.preferDub))
            }
        }

        actions.add(DetailActionsPresenter.InfoAction(1, "+Info"))

        actions.add(DetailActionsPresenter.ChangeAction(2, getString(R.string.wrong)))
    }

    private fun processAction(action: Action){
        if (action.id.toInt() == 0) {
            val sourceSelector = TVSourceSelectorFragment()
            sourceSelector.media = media
            parentFragmentManager.beginTransaction().addToBackStack(null)
                .replace(
                    R.id.main_detail_fragment,
                    sourceSelector
                ).commit()
        } else
        if (action.id.toInt() == 1) {
            val infoFragment = TVAnimeDetailInfoFragment()
            infoFragment.media = media
            parentFragmentManager.beginTransaction().addToBackStack(null)
                .replace(
                    R.id.main_detail_fragment,
                    infoFragment
                ).commit()
        } else
        if (action.id.toInt() == 2) {
            val gridSelector = TVGridSelectorFragment()
            gridSelector.sourceId = media.selected!!.source
            gridSelector.mediaId = media.id
            parentFragmentManager.beginTransaction().addToBackStack(null)
                .replace(
                    R.id.main_detail_fragment,
                    gridSelector
                ).commit()
        } else
        if (action.id.toInt() == 3){
            val checked = !media.selected!!.preferDub
            val selected = model.loadSelected(media)
            model.watchSources?.get(selected.source)?.selectDub = checked
            selected.preferDub = checked
            model.saveSelected(media.id, selected, requireActivity())
            media.selected = selected
            lifecycleScope.launch(Dispatchers.IO) { model.forceLoadEpisode(media, selected.source) }
            setupActions()
        }
    }

}
