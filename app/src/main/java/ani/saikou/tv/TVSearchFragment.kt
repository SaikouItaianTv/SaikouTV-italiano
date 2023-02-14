package ani.saikou.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.ObjectAdapter.DataObserver
import androidx.leanback.widget.SearchBar.SearchBarListener
import androidx.leanback.widget.SearchBar.SearchBarPermissionListener
import androidx.lifecycle.lifecycleScope
import ani.saikou.R
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistSearch
import ani.saikou.anilist.SearchResults
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.tv.components.SearchFragment
import ani.saikou.tv.presenters.AnimePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.*

class TVSearchFragment: SearchFragment(), SearchSupportFragment.SearchResultProvider {

    private val PAGING_THRESHOLD = 40

    lateinit var rowAdapter: ArrayObjectAdapter
    private val scope = lifecycleScope

    val model: AnilistSearch by viewModels()
    lateinit var result: SearchResults

    lateinit var type: String
    var genre: String? = null
    var sortBy: String? = null

    var searchText: String? = null
    var _tag: String? = null
    var adult = false
    var listOnly :Boolean?= null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rowAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        super.setSearchResultProvider(this)

        val intent = requireActivity().intent
        //style = loadData<Int>("searchStyle") ?: 0
        //adult = intent.getBooleanExtra("hentai", false)
        listOnly = intent.getBooleanExtra("listOnly",false)

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (requireActivity()::class.java.isAssignableFrom(TVDetailActivity::class.java)) {
                startActivity(
                    Intent(
                        requireContext(),
                        TVDetailActivity::class.java
                    ).putExtra("media", item as Serializable)
                )
                requireActivity().finish()
            } else {
                startActivity(
                    Intent(
                        requireContext(),
                        TVDetailActivity::class.java
                    ).putExtra("media", item as Serializable)
                )
            }
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading && isNearEndOfList(rowAdapter, item)) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    loading=true
                    model.loadNextPage(model.searchResults)
                }
            }
        }

        setObservers()
        search(null,genre,tag,sortBy,adult,listOnly)
        setLoadingVisibility(View.VISIBLE)
    }

    fun setArgs(t: String, g: String? = null, s: String? = null, h: Boolean = false){
        type = t
        genre = g
        sortBy = s
        adult = h
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

    private fun setObservers(){
        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(type,
                isAdult = false,
                onList = null,
                results = arrayListOf(),
                hasNextPage = false)
        }

        model.getSearch().observe(viewLifecycleOwner) {
            if (it != null) {
                model.searchResults.apply {
                    onList = it.onList
                    isAdult = it.isAdult
                    perPage = it.perPage
                    search = it.search
                    sort = it.sort
                    genres = it.genres
                    tags = it.tags
                    format = it.format
                    page = it.page
                    hasNextPage = it.hasNextPage
                }
                loading = false
                setLoadingVisibility(View.GONE)
                setEmptyListText(null)
                if( it.results.size == 0) {
                    setEmptyListText("Your search \"" + searchText + "\" returned no results")
                } else {
                    setEmptyListText(null)
                    model.searchResults.results.addAll(it.results)
                    rowAdapter.addAll(rowAdapter.size(), it.results)
                }
            }
        }
    }

    private var searchTimer = Timer()
    private var loading = false
    fun search(
        search: String? = null,
        genre: String? = null,
        tag: String? = null,
        sort: String? = null,
        adult: Boolean = this.adult,
        listOnly: Boolean? = null
    ) {
        model.searchResults.results.clear()
        rowAdapter.clear()

        this.genre = genre
        this.sortBy = sort
        this.searchText = search
        this.adult = adult
        this._tag = tag
        this.listOnly = listOnly

        searchTimer.cancel()
        searchTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                scope.launch(Dispatchers.IO) {
                    loading = true
                    MainScope().launch {
                        setLoadingVisibility(View.VISIBLE)
                        setEmptyListText(null)
                    }
                    model.searchResults = SearchResults(
                        type,
                        isAdult = adult,
                        onList = listOnly,
                        search = search,
                        genres =  if (genre != null) arrayListOf(genre) else null,
                        sort = sort,
                        results = mutableListOf(),
                        hasNextPage = false
                    )
                    result = model.searchResults

                    model.loadSearch(result)
                }
            }
        }
        searchTimer = Timer()
        searchTimer.schedule(timerTask, 500)
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        if (newQuery != null && newQuery != searchText) {
            if(newQuery.isNotEmpty()) {
                search(newQuery)
            } else {
                search(null,genre,tag,sortBy,adult,listOnly)
            }
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null && query != searchText) {
            search(query)
        }
        return true
    }


}
