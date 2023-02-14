package ani.saikou.tv.presenters

import ani.saikou.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Action
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvDetailInfoTitleBinding

class DetailInfoActionPresenter(): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return DetailActionsViewHolder(TvDetailInfoTitleBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as DetailActionsViewHolder)?.let {
            it.binding.title.text = (item as Action).label1.toString()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class DetailActionsViewHolder(val binding: TvDetailInfoTitleBinding) : Presenter.ViewHolder(binding.root) {}

    class TitleAction(id: Long, label: String): Action(id, label) {}
}

