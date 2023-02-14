package ani.saikou.tv.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import ani.saikou.R
import ani.saikou.databinding.TvItemSourceBinding

class TVLoginFragment: VerticalGridSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val items = ArrayObjectAdapter(LoginMethodPresenter())
        items.add(LoginMethod(0, "Phone Login"))
        items.add(LoginMethod(1, "WebBrowser Login"))

        title = "Select login method"

        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 1
        gridPresenter = presenter

        adapter = items
    }

    fun onMethodSelected(method: LoginMethod) {
        val key = 6818
        if(method.id == 0){
            requireActivity().supportFragmentManager.beginTransaction().addToBackStack(null)
                .replace(R.id.main_tv_fragment, TVNetworkLoginFragment())
                .commit()
        } else {
            openBrowser("https://anilist.co/api/v2/oauth/authorize?client_id=$key&response_type=token")
        }
    }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ContextCompat.startActivity( requireContext(), intent, null)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG)
        }
    }

    private inner class LoginMethodPresenter : Presenter() {

        inner class LoginMethodViewHolder(val binding: TvItemSourceBinding) : Presenter.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            return LoginMethodViewHolder(TvItemSourceBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val holder = viewHolder as LoginMethodViewHolder

            holder.binding.sourceName.text = (item as LoginMethod).text
            holder.binding.root.setOnClickListener {
                onMethodSelected(item)
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
    }

    data class LoginMethod(val id: Int, val text: String)
}