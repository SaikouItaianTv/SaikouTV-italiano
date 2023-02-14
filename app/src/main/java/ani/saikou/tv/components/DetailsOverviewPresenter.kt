package ani.saikou.tv.components

import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.Presenter
import ani.saikou.R


class DetailsOverviewPresenter(detailsPresenter: Presenter?): FullWidthDetailsOverviewRowPresenter(detailsPresenter) {

    override fun getLayoutResourceId(): Int {
        return R.layout.tv_anime_detail_info_overview
    }
}