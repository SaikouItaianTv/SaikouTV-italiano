package ani.saikou.tv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.ProgressBarManager
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.databinding.ItemUrlBinding
import ani.saikou.databinding.TvItemUrlBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class TVSelectorFragment(var media: Media): VerticalGridSupportFragment() {

    lateinit var links: MutableMap<String, Episode.StreamLinks?>
    private var selected:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selected = it.getString("server")
        }

        title = "Select quality"

        progressBarManager.initialDelay = 0
        progressBarManager.show()
        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 1
        gridPresenter = presenter

        if(this::links.isInitialized) {
            setStreamLinks(links)
        }
    }

    fun setStreamLinks(streamLinks: MutableMap<String, Episode.StreamLinks?>) {
        links = streamLinks
        if(gridPresenter == null)
            return

        val linkList = mutableListOf<Episode.StreamLinks>()

        links.keys.toList().forEach { key ->
            links[key]?.let { links ->
                links.quality.forEach {
                    linkList.add(Episode.StreamLinks(links.server, listOf(it), links.headers, links.subtitles))
                }
            }
        }

        val arrayAdapter = ArrayObjectAdapter(StreamAdapter())
        arrayAdapter.addAll(0, linkList)
        adapter = arrayAdapter
        progressBarManager.hide()
    }

    fun startExoplayer(media: Media){
        requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.main_tv_fragment, TVMediaPlayer(media)).commit()
    }

    fun cancel() {
        media!!.selected!!.stream = null
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        fun newInstance(media: Media, server:String?=null, la:Boolean=true, prev:Episode?=null): TVSelectorFragment =
            TVSelectorFragment(media).apply {
                arguments = Bundle().apply {
                    putString("server",server)
                    putBoolean("launch",la)
                    putSerializable("prev",prev)
                }
            }
    }

    private inner class StreamAdapter : Presenter() {

        private inner class UrlViewHolder(val binding: TvItemUrlBinding) : Presenter.ViewHolder(binding.root) {}

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder = UrlViewHolder(TvItemUrlBinding.inflate(LayoutInflater.from(parent?.context), parent, false))


        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val stream = item as Episode.StreamLinks
            val server = stream.server
            val quality = stream.quality.first()
            val qualityPos = links.values.find { it?.server == server }?.quality?.indexOfFirst { it.quality == quality.quality }
            val holder = viewHolder as? UrlViewHolder
            if(server!=null && holder != null && qualityPos != null) {
                holder.view.setOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedStream = server
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedQuality = qualityPos
                    startExoplayer(media!!)
                }

                val binding = holder.binding
                val url = quality
                binding.serverName.text = stream.server
                binding.urlQuality.text = url.quality
                binding.urlNote.text = url.note?:""
                binding.urlNote.visibility = if(url.note!=null) View.VISIBLE else View.GONE
                if(url.quality!="Multi Quality") {
                    binding.urlSize.visibility = if(url.size!=null) View.VISIBLE else View.GONE
                    binding.urlSize.text = (if (url.note!=null) " : " else "")+ DecimalFormat("#.##").format(url.size?:0).toString()+" MB"

                    //TODO Download? on TV?
                    /*binding.urlDownload.visibility = View.VISIBLE
                    binding.urlDownload.setOnClickListener {
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedStream = server
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedQuality = qualityPos
                        download(requireActivity(),media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,media!!.userPreferredName)
                    }*/
                }

            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
    }
}