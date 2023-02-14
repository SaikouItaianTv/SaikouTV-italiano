package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import ani.saikou.R
import ani.saikou.databinding.TvAnimeDetailInfoBinding
import ani.saikou.media.Media

class DetailsInfoPresenter: Presenter() {

    var userText: String? = null

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return AnimeDetailViewHolder(TvAnimeDetailInfoBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as AnimeDetailViewHolder)?.let { vh ->
            (item as? Media)?.let { media ->
                vh.binding.mediaInfoName.text = "\t\t\t" + (media.name?:media.nameRomaji)
                if (media.name != null) vh.binding.mediaInfoNameRomajiContainer.visibility = View.VISIBLE
                vh.binding.mediaInfoNameRomaji.text = "\t\t\t" + media.nameRomaji
                vh.binding.mediaInfoMeanScore.text = if (media.meanScore != null) (media.meanScore / 10.0).toString() else "??"
                vh.binding.mediaInfoStatus.text = media.status
                vh.binding.mediaInfoFormat.text = media.format
                vh.binding.mediaInfoSource.text = media.source
                vh.binding.mediaInfoStart.text = media.startDate?.toString() ?: "??"
                vh.binding.mediaInfoEnd.text =media.endDate?.toString() ?: "??"
                if (media.anime != null) {
                    vh.binding.mediaInfoDuration.text =
                        if (media.anime.episodeDuration != null) media.anime.episodeDuration.toString() else "??"
                    vh.binding.mediaInfoDurationContainer.visibility = View.VISIBLE
                    vh.binding.mediaInfoSeasonContainer.visibility = View.VISIBLE
                    vh.binding.mediaInfoSeason.text =
                        (media.anime.season ?: "??")+ " " + (media.anime.seasonYear ?: "??")
                    if (media.anime.mainStudio != null) {
                        vh.binding.mediaInfoStudioContainer.visibility = View.VISIBLE
                        vh.binding.mediaInfoStudio.text = media.anime.mainStudio!!.name
                        vh.binding.mediaInfoStudioContainer.setOnClickListener {
                            /*ContextCompat.startActivity(
                                requireActivity(),
                                Intent(activity, StudioActivity::class.java).putExtra(
                                    "studio",
                                    media.anime.mainStudio!! as Serializable
                                ),
                                null
                            )
                        }*/
                        }
                        vh.binding.mediaInfoTotalTitle.setText(R.string.total_eps)
                        vh.binding.mediaInfoTotal.text =
                            if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes
                                ?: "~").toString()) else (media.anime.totalEpisodes
                                ?: "~").toString()
                    }
                }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class AnimeDetailViewHolder(val binding: TvAnimeDetailInfoBinding) : Presenter.ViewHolder(binding.root) {}
}