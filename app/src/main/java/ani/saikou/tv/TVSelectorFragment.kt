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
import ani.saikou.anime.ExoplayerView
import ani.saikou.databinding.ItemUrlBinding
import ani.saikou.databinding.TvItemUrlBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class TVSelectorFragment(): VerticalGridSupportFragment() {

    lateinit var links: MutableList<VideoExtractor>
    private var selected:String?=null
    lateinit var media: Media
    var fromPlayer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selected = it.getString("server")
        }

        title = "Select quality"

        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 1
        gridPresenter = presenter

        if(this::links.isInitialized) {
            setStreamLinks(links)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(!this::links.isInitialized) {
            progressBarManager.setRootView(this.view as ViewGroup)
            progressBarManager.initialDelay = 0
            progressBarManager.show()
        }
    }

    fun setStreamLinks(streamLinks: MutableList<VideoExtractor>?) {
        streamLinks?.let {
            links = it
        }
        if(gridPresenter == null)
            return

        val linkList = mutableListOf<StreamItem>()

        links.forEach { extractor ->
            extractor.videos.forEachIndexed { index, video ->
                linkList.add(StreamItem(extractor.server.name, video, index)) //Episode.StreamLinks(links.server, listOf(it), links.headers, links.subtitles)
            }
        }

        val arrayAdapter = ArrayObjectAdapter(StreamAdapter())
        arrayAdapter.addAll(0, linkList)
        adapter = arrayAdapter
        progressBarManager.hide()
    }

    fun startExoplayer(media: Media){
        val player = TVMediaPlayer()
        player.media = media
        requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null).replace(R.id.main_detail_fragment, player).commit()
    }

    fun cancel() {
        media!!.selected?.server = null
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        fun newInstance(media: Media, fromPlayer: Boolean = false): TVSelectorFragment
        {
            val fragment = TVSelectorFragment()
            fragment.media = media
            fragment.fromPlayer = fromPlayer
            return fragment
        }
    }

    private data class StreamItem(val server: String, val video: Video, val videoIndex: Int)

    private inner class StreamAdapter : Presenter() {

        private inner class UrlViewHolder(val binding: TvItemUrlBinding) : Presenter.ViewHolder(binding.root) {}

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder = UrlViewHolder(TvItemUrlBinding.inflate(LayoutInflater.from(parent?.context), parent, false))


        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val stream = item as StreamItem
            val server = stream.server
            val quality = if(stream.video.quality!=null) "${stream.video.quality}p" else "Default Quality"
            //val qualityPos = links.values.find { it?.server == server }?.quality?.indexOfFirst { it.quality == quality.quality }
            val holder = viewHolder as? UrlViewHolder
            if(server!=null && holder != null ) { //&& qualityPos != null) {
                holder.view.setOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedExtractor = server
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedVideo = stream.videoIndex
                    if (fromPlayer)
                        parentFragmentManager.popBackStack()
                    else
                        startExoplayer(media!!)
                }

                val binding = holder.binding
                binding.serverName.text = stream.server
                binding.urlQuality.text = quality
                binding.urlNote.text = stream.video.extraNote?:""
                binding.urlNote.visibility = if(stream.video.extraNote!=null) View.VISIBLE else View.GONE
                /*if(url.quality!="Multi Quality") {
                    binding.urlSize.visibility = if(url.size!=null) View.VISIBLE else View.GONE
                    binding.urlSize.text = (if (url.note!=null) " : " else "")+ DecimalFormat("#.##").format(url.size?:0).toString()+" MB"

                    //TODO Download? on TV?
                    /*binding.urlDownload.visibility = View.VISIBLE
                    binding.urlDownload.setOnClickListener {
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedStream = server
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedQuality = qualityPos
                        download(requireActivity(),media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,media!!.userPreferredName)
                    }*/
                }*/
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
    }
}