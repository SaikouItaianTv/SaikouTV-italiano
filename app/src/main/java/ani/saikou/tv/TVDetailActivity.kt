package ani.saikou.tv

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.BackgroundManager
import ani.saikou.R

class TVDetailActivity: FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_detail_activity)
        val backgroundManager = BackgroundManager.getInstance(this)
        backgroundManager.attach(this.window)

        backgroundManager.color = ContextCompat.getColor(this, R.color.bg_black)

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_detail_fragment, TVAnimeDetailFragment(), "Detail")
            .commitNow()
    }
}