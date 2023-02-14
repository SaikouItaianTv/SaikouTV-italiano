package ani.saikou.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.databinding.TvAnimeCardBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.parsers.*
import ani.saikou.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVGridSelectorFragment: VerticalGridSupportFragment() {

    var sourceId: Int = 0
    var mediaId: Int = 0

    val model: MediaDetailsViewModel by activityViewModels()
    var media: Media? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Similar titles"

        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 5
        gridPresenter = presenter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressBarManager.setRootView(this.view as ViewGroup)
        progressBarManager.initialDelay = 0
        progressBarManager.show()

        observeData()

        super.onViewCreated(view, savedInstanceState)
    }

    fun observeData() {
        model.getMedia().observe(viewLifecycleOwner) {
            media = it
            if (media != null) {
                val source = (if (!media!!.isAdult) AnimeSources else HAnimeSources)[media!!.selected!!.source]

                requireActivity().lifecycleScope.launch {
                    model.responses.postValue(
                        withContext(Dispatchers.IO) {
                            tryWithSuspend {
                                source.search(media!!.mangaName())
                            }
                        }
                    )
                }

                model.responses.observe(viewLifecycleOwner) { list ->
                    if (list != null) {
                        if(list.isNotEmpty()) {
                            val arrayAdapter =
                                ArrayObjectAdapter(AnimeSourcePresenter(requireActivity()))
                            arrayAdapter.addAll(0, list!!)
                            adapter = arrayAdapter
                        } else {
                            Toast.makeText(requireContext(), "Nothing found, try another source.", Toast.LENGTH_LONG).show()
                        }
                        progressBarManager.hide()
                    }
                }
            }
        }
    }


    inner class AnimeSourcePresenter(private val activity: FragmentActivity): Presenter() {

        private val uiSettings =
            loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings()

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            return AnimeSourceViewHolder(
                TvAnimeCardBinding.inflate(
                    LayoutInflater.from(
                        parent?.context ?: activity.applicationContext
                    ), parent, false
                )
            )
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val itemView =
                (viewHolder as AnimeSourceViewHolder).view
            val b =
                (viewHolder as AnimeSourceViewHolder).binding
            setAnimation(activity, b.root, uiSettings)
            val media = item as ShowResponse
            itemView.setSafeOnClickListener { clicked(media) }


            b.itemCompactImage.loadImage(media.coverUrl.url)
            b.itemCompactOngoing.visibility = View.GONE
            b.itemCompactTitle.text = media.name
            b.itemCompactScore.visibility = View.GONE
            b.itemCompactScoreBG.visibility = View.GONE
            b.itemCompactUserProgress.visibility = View.GONE
            b.itemCompactTotal.visibility = View.GONE
        }

        fun clicked(source: ShowResponse){
            requireActivity().lifecycleScope.launch(Dispatchers.IO) { model.overrideEpisodes(sourceId, source, mediaId) }
            requireActivity().supportFragmentManager.popBackStack()
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        }

        inner class AnimeSourceViewHolder(val binding: TvAnimeCardBinding) : Presenter.ViewHolder(binding.root) {}
    }

}