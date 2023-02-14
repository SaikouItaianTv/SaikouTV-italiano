package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvGenreCardBinding
import ani.saikou.databinding.TvItemCharacterBinding
import ani.saikou.loadImage
import ani.saikou.media.Character
import ani.saikou.px

class CharacterPresenter: Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return CharacterViewHolder(TvItemCharacterBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val character = item as Character
        val holder = viewHolder as CharacterViewHolder
        val binding = holder.binding
        binding.itemCompactRelation.text = character.role + "  "
        binding.itemCompactImage.loadImage(character.image)
        binding.itemCompactTitle.text = character.name
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class CharacterViewHolder(val binding: TvItemCharacterBinding) : Presenter.ViewHolder(binding.root) {}
}