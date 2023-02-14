package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.RowPresenter
import ani.saikou.databinding.TvProgressRowHeaderBinding
import ani.saikou.tv.components.HeaderOnlyRow

class HeaderRowPresenter: RowPresenter() {
    override fun createRowViewHolder(parent: ViewGroup?): ViewHolder {
        return HeaderRowViewholder(TvProgressRowHeaderBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun isUsingDefaultSelectEffect(): Boolean {
        return false
    }

    override fun onBindRowViewHolder(vh: ViewHolder?, item: Any?) {
        (vh as HeaderRowViewholder)?.let { holder ->
            (item as HeaderOnlyRow)?.title?.let { item ->
                holder.binding.title.text = item
                holder.binding.progress.visibility = View.GONE
            } ?: run {
            holder.binding.title.visibility = View.GONE
            holder.binding.progress.visibility = View.VISIBLE
            }
        }
    }

    inner class HeaderRowViewholder(val binding: TvProgressRowHeaderBinding) : ViewHolder(binding.root) {}
}