package ani.saikou.tv

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.R
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.GenresViewModel
import ani.saikou.media.Media
import ani.saikou.settings.UserInterfaceSettings
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.components.DetailsOverviewPresenter
import ani.saikou.tv.components.HeaderOnlyRow
import ani.saikou.tv.presenters.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.Serializable

class TVAnimeDetailInfoFragment() : DetailsSupportFragment() {

    private val scope = lifecycleScope
    private val genresModel: GenresViewModel by activityViewModels()
    val actions = ArrayObjectAdapter(DetailInfoActionPresenter())
    lateinit var uiSettings: UserInterfaceSettings
    var loaded = false
    lateinit var media: Media

    private var descriptionPresenter: DetailsInfoPresenter? = null

    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController

    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var detailsOverview: DetailsOverviewRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        media = requireActivity().intent.getSerializableExtra("media") as Media

        detailsBackground = DetailsSupportFragmentBackgroundController(this)
        uiSettings = loadData("ui_settings", toast = false)
            ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
    }

    override fun onResume() {
        super.onResume()
        progressBarManager.setRootView(this.view as ViewGroup)
        progressBarManager.initialDelay =0
        progressBarManager.show()
        buildDetails()
    }

    override fun onPause() {
        super.onPause()
        loaded = false
    }

    private fun buildDetails() {
        val selector = ClassPresenterSelector().apply {
            descriptionPresenter = DetailsInfoPresenter()
            DetailsOverviewPresenter(descriptionPresenter).also {
                it.backgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
                it.actionsBackgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
                actions.add(DetailInfoActionPresenter.TitleAction(1, media.mainName()))
                addClassPresenter(DetailsOverviewRow::class.java, it)
            }
            val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
            presenter.shadowEnabled = false
            addClassPresenter(ListRow::class.java, presenter)
        }

        rowsAdapter = ArrayObjectAdapter(selector)

        detailsOverview = DetailsOverviewRow(media)
        detailsOverview.actionsAdapter = actions
        rowsAdapter.add(detailsOverview)

        adapter = rowsAdapter

        initializeBackground()
        initializeCover()
        buildRows()
        setClickListener()
    }

    private fun setClickListener() {
        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            if (item is Media) {
                item.anime?.let {
                    ContextCompat.startActivity(
                        requireActivity(),
                        Intent(requireActivity(), TVDetailActivity::class.java).putExtra(
                            "media",
                            item as Serializable
                        ),null
                    )
                    requireActivity().finish()
                }
            } else if (item is Pair<*,*>) {
                val fragment = TVSearchFragment()
                val genre = item.first as String
                fragment.setArgs("ANIME", genre, "Trending", genre.lowercase() == "hentai")
                parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                parentFragmentManager.beginTransaction().replace(R.id.main_detail_fragment, fragment).commit()
            }
        }
    }

    private fun buildRows() {

        if(!media.tags.isNullOrEmpty()) {
            val tagsAdapter = ArrayObjectAdapter(TagsPresenter())
            media.tags.forEach {
                tagsAdapter.add(it)
            }
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.tags)), tagsAdapter))
        }

        if(!media.genres.isNullOrEmpty()) {
            val genresAdapter = ArrayObjectAdapter(GenresPresenter(false))
            if (genresModel.genres.isNullOrEmpty()) {
                loadGenres(genresAdapter)
            } else {
                genresModel.genres?.forEach {
                    genresAdapter.add(it.toPair())
                }
                progressBarManager.hide()
            }
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.genres)), genresAdapter))
        }

        if(!media.characters.isNullOrEmpty()) {
            val characters = ArrayObjectAdapter(CharacterPresenter())
            media.characters?.forEach() {
                characters.add(it)
            }
            rowsAdapter.add(ListRow(HeaderItem(1, getString(R.string.characters)), characters))
        }

        if(!media.relations.isNullOrEmpty()) {
            val relations = ArrayObjectAdapter(AnimePresenter(1,requireActivity()))
            media.relations?.forEach() {
                relations.add(it)
            }
            rowsAdapter.add(ListRow(HeaderItem(2, getString(R.string.relations)), relations))
        }

        if(!media.recommendations.isNullOrEmpty()) {
            val recommendations = ArrayObjectAdapter(AnimePresenter(0,requireActivity()))
            media.recommendations?.forEach() {
                recommendations.add(it)
            }
            rowsAdapter.add(ListRow(HeaderItem(3, getString(R.string.recommended)), recommendations))
        }
    }

    private fun loadGenres(adapter: ArrayObjectAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            genresModel.loadGenres(Anilist.genres ?: loadData("genres_list") ?: arrayListOf()) {
                MainScope().launch {
                    val genre = it.first as String
                    if (media.genres.contains(genre))
                        adapter.add(it)
                    progressBarManager.hide()
                }
            }
        }
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

}