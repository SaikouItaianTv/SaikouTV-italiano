package ani.saikou.tv.presenters

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvAnimeDetailBinding
import ani.saikou.media.Media
import ani.saikou.parsers.BaseParser

class DetailsWatchPresenter(): Presenter() {

    var userText: String? = null

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return AnimeDetailViewHolder(TvAnimeDetailBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as AnimeDetailViewHolder)?.let { vh ->
            (item as? Media)?.let { media ->
                vh.binding.title.text = media.mainName()
                vh.binding.altTitle.text = userText
                vh.binding.status.text = media.status
                val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(media.description?: "", Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(media.description?:"")
                }
                vh.binding.overview.text = desc
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class AnimeDetailViewHolder(val binding: TvAnimeDetailBinding) : Presenter.ViewHolder(binding.root) {}
}