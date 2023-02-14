package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.ItemChipBinding
import ani.saikou.databinding.TvItemChipBinding
import ani.saikou.loadImage

class TagsPresenter(): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return GenreViewHolder(TvItemChipBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val tag = item as String
        val holder = viewHolder as GenreViewHolder
        val binding = holder.binding
        binding.title.text = tag
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class GenreViewHolder(val binding: TvItemChipBinding) : Presenter.ViewHolder(binding.root) {}
}