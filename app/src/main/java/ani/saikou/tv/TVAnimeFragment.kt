package ani.saikou.tv

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import ani.saikou.*
import ani.saikou.R
import ani.saikou.anilist.*
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.tv.components.ButtonListRow
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.login.TVLoginFragment
import ani.saikou.tv.login.TVNetworkLoginFragment
import ani.saikou.tv.presenters.AnimePresenter
import ani.saikou.tv.presenters.ButtonListRowPresenter
import ani.saikou.tv.presenters.GenresPresenter
import ani.saikou.tv.presenters.MainHeaderPresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class TVAnimeFragment: BrowseSupportFragment()  {

    companion object {
        var shouldReload: Boolean = false
    }
    private val PAGING_THRESHOLD = 15

    private val homeModel: AnilistHomeViewModel by activityViewModels()
    private val model: AnilistAnimeViewModel by activityViewModels()
    private val genresModel: GenresViewModel by activityViewModels()

    //TODO Sketchy handling here
    var nCallbacks: Int = 0

    lateinit var continueAdapter: ArrayObjectAdapter
    lateinit var recommendedAdapter: ArrayObjectAdapter
    lateinit var plannedAdapter: ArrayObjectAdapter
    lateinit var completedAdapter: ArrayObjectAdapter
    lateinit var genresAdapter: ArrayObjectAdapter
    lateinit var trendingAdapter: ArrayObjectAdapter
    lateinit var updatedAdapter: ArrayObjectAdapter
    lateinit var popularAdapter: ArrayObjectAdapter
    lateinit var rowAdapter: ArrayObjectAdapter

    lateinit var continueRow: ListRow
    lateinit var recommendedRow: ListRow
    lateinit var plannedRow: ListRow
    lateinit var completedRow: ListRow
    lateinit var genresRow: ListRow
    lateinit var trendingRow: ListRow
    lateinit var updatedRow: ListRow
    lateinit var popularRow: ListRow
    var loading: Boolean = false
    var viewLoaded: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUIElements()
        loading = true
        viewLoaded = false
        nCallbacks = 0

        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults("ANIME", isAdult = false, onList = false, results = arrayListOf(), hasNextPage = true, sort = "Popular")
        }

        initAdapters()

        genresAdapter.clear()
        genresModel.genres?.let {
            it.forEach {
                genresAdapter.add(it.toPair())
            }
        }

        observeData()
    }

    private fun initAdapters() {
        val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
        presenter.shadowEnabled = false
        rowAdapter = ArrayObjectAdapter(presenter)
        adapter = rowAdapter

        continueAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        recommendedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        plannedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        completedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        genresAdapter = ArrayObjectAdapter(GenresPresenter(true))
        trendingAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        popularAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        updatedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))

        continueRow = ListRow(HeaderItem(getString(R.string.continue_watching)), continueAdapter)
        recommendedRow = ListRow(HeaderItem(getString(R.string.recommended)), recommendedAdapter)
        plannedRow = ListRow(HeaderItem(getString(R.string.planned)), plannedAdapter)
        completedRow = ListRow(HeaderItem(getString(R.string.completed)), completedAdapter)
        genresRow = ListRow(HeaderItem(getString(R.string.genres)), genresAdapter)
        trendingRow = ListRow(HeaderItem(getString(R.string.trending_anime)), trendingAdapter)
        popularRow = ListRow(HeaderItem(getString(R.string.popular_anime)), popularAdapter)
        updatedRow = ListRow(HeaderItem(getString(R.string.updated)), updatedAdapter)

        progressBarManager.initialDelay = 0
        progressBarManager.show()
    }

    private fun observeData() {
        model.getTrending().observe(viewLifecycleOwner) {
            if (it != null) {
                //TODO Some users reported crashes related with Leanback channels
                //updateHomeTVChannel(it)
                trendingAdapter.clear()
                trendingAdapter.addAll(0, it)
                checkLoadingState()
            }
        }

        model.getUpdated().observe(viewLifecycleOwner) {
            if (it != null) {
                updatedAdapter.clear()
                updatedAdapter.addAll(0, it)
                checkLoadingState()
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                if(!loading)
                    popularAdapter.clear()
                loading = false
                model.searchResults = it
                popularAdapter.addAll(popularAdapter.size(), it.results)
                checkLoadingState()
            }
        }

        homeModel.getAnimeContinue().observe(viewLifecycleOwner) {
            if (it != null) {
                continueAdapter.clear()
                continueAdapter.addAll(0, it)
                if(it.isEmpty()) {
                    rowAdapter.remove(continueRow)
                }
                checkLoadingState()
            }
        }

        homeModel.getRecommendation().observe(viewLifecycleOwner) {
            if (it != null) {
                recommendedAdapter.clear()
                recommendedAdapter.addAll(0, it.filter { it.relation == "ANIME" })
                if(it.isEmpty()) {
                    rowAdapter.remove(recommendedRow)
                }
                checkLoadingState()
            }
        }

        homeModel.getAnimePlanned().observe(viewLifecycleOwner) {
            if (it != null) {
                plannedAdapter.clear()
                plannedAdapter.addAll(0, it.filter { it.userStatus == "PLANNING" })
                if(plannedAdapter.size() == 0) {
                    rowAdapter.remove(plannedRow)
                }

                completedAdapter.clear()
                completedAdapter.addAll(0, it.filter { it.userStatus == "COMPLETED" })
                if(completedAdapter.size() == 0) {
                    rowAdapter.remove(completedRow)
                }
                checkLoadingState()
            }
        }

        val scope = viewLifecycleOwner.lifecycleScope
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loaded = true
                        homeModel.loaded = true
                        loading = false

                        if (Anilist.userid == null) {
                            if (Anilist.query.getUserData())
                                loadUserData()
                        }

                        model.loadTrending(0)
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "Popular")
                        genresModel.loadGenres(Anilist.genres?: loadData("genres_list") ?: arrayListOf()) {
                            MainScope().launch {
                                val genre = it.first as String
                                if (genre.lowercase() != "hentai" || Anilist.adult)
                                    genresAdapter.add(it)
                            }
                        }
                        homeModel.setListImages()
                        homeModel.setAnimeContinue()
                        homeModel.setRecommendation()
                        homeModel.setAnimePlanned()
                    }
                    live.postValue(false)
                }
            }
        }
    }

    fun checkLoadingState(){
        if(nCallbacks == 5 && !viewLoaded) {
            progressBarManager.hide()
            //This determines order in screen
            if (Anilist.userid == null) {
                rowAdapter.add(genresRow)
                rowAdapter.add(trendingRow)
                rowAdapter.add(popularRow)
                rowAdapter.add(updatedRow)
                rowAdapter.add(ButtonListRow(getString(R.string.login), object : ButtonListRow.OnClickListener {
                    override fun onClick() {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.main_tv_fragment, TVLoginFragment()).addToBackStack("home")
                            .commit()
                    }
                }))
            } else {
                if(continueAdapter.size() > 0)
                rowAdapter.add(continueRow)
                if(recommendedAdapter.size() > 0)
                rowAdapter.add(recommendedRow)
                if(plannedAdapter.size() > 0)
                rowAdapter.add(plannedRow)
                if(completedAdapter.size() > 0)
                rowAdapter.add(completedRow)

                rowAdapter.add(genresRow)
                rowAdapter.add(trendingRow)
                rowAdapter.add(popularRow)
                rowAdapter.add(updatedRow)
                rowAdapter.add(ButtonListRow(getString(R.string.logout), object : ButtonListRow.OnClickListener {
                    override fun onClick() {
                        Anilist.removeSavedToken(requireContext())
                        reloadScreen()
                    }
                }))
            }
            viewLoaded = true
        } else {
            nCallbacks++
        }
    }

    fun loadUserData(){
        if(activity!=null)
            lifecycleScope.launch(Dispatchers.Main) {
                title = Anilist.username
                Glide.with(requireContext())
                    .asDrawable()
                    .centerInside()
                    .load(Anilist.avatar)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            badgeDrawable = resource
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        if(shouldReload) {
            reloadScreen()
        } else {
            if (!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        }
    }

    private fun reloadScreen() {
        shouldReload = false
        nCallbacks = 0
        model.loaded = true
        loading = false
        viewLoaded = false
        genresModel.genres = null
        initAdapters()
        Refresh.all()
    }

    private fun setupUIElements() {
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // Set fastLane (or headers) background color
        brandColor = Color.parseColor("#66000000")//ContextCompat.getColor(requireActivity(), R.color.bg_black)
        // Set search icon color.
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.pink_200)

        setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(o: Any): Presenter {
                if(o is ButtonListRow) {
                    return ButtonListRowPresenter()
                } else {
                    return MainHeaderPresenter()
                }
            }
        })

        setOnSearchClickedListener {
            val fragment = TVSearchFragment()
            fragment.setArgs("ANIME", null, null)
           parentFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.main_tv_fragment, fragment).commit()
        }

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

            if (item is Media) {
                startActivity(
                    Intent(
                        requireContext(),
                        TVDetailActivity::class.java
                    ).putExtra("media", item as Serializable))
            } else if (item is Pair<*,*>) {
                val fragment = TVSearchFragment()
                val genre = item.first as String
                fragment.setArgs("ANIME", genre, "Trending", genre.lowercase() == "hentai")
                parentFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.main_tv_fragment, fragment).commit()
            }
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            item?.let {
                if ((row as ListRow).adapter == popularAdapter && model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading && isNearEndOfList(popularAdapter, item)) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        loading=true
                        model.loadNextPage(model.searchResults)
                    }
                }
                //TODO find a way to properly show this image
                /*if (it is Media && !it.banner.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .asDrawable()
                        .centerCrop()
                        .load(it.banner)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                backgroundManager.drawable = resource
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                } else {
                    //backgroundManager.clearDrawable()
                }*/
            }
        }
    }

    private fun isNearEndOfList(adapter: ArrayObjectAdapter, item: Any): Boolean {
        var found = false
        for (i in adapter.size()-PAGING_THRESHOLD until adapter.size()-1) {
            if(adapter.get(i) == item) {
                found = true
                break
            }
        }
        return found
    }

    private fun updateHomeTVChannel(animes: List<Media>) {
        clearHomeTVChannel()
        animes.forEach {
            addMediaToHomeTVChannel(it)
        }
    }

    
    private fun clearHomeTVChannel() {
        requireContext().contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)
    }

    private fun addMediaToHomeTVChannel(media: Media): Long {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return -1
        val channelID = sharedPref.getLong(TVMainActivity.defaultChannelIDKey, -1)
        if (channelID == -1L) return -1

        val intent = Intent(requireContext(), TVMainActivity::class.java)
        intent.putExtra("media", media.id)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        val builder = PreviewProgram.Builder()
        builder.setChannelId(channelID)
            .setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
            .setTitle(media.mainName())
            .setDescription(media.description)
            .setPosterArtUri(Uri.parse(media.banner?:media.cover))
            .setIntent(intent)

        val programURI = requireContext().contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI,
            builder.build().toContentValues())
        programURI?.let {
            return ContentUris.parseId(it)
        } ?: run {
            return -1
        }
    }
}