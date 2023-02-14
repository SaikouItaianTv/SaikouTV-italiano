package ani.saikou

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistHomeViewModel
import ani.saikou.databinding.ActivityMainBinding
import ani.saikou.databinding.SplashScreenBinding
import ani.saikou.media.MediaDetailsActivity
import ani.saikou.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.Serializable


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val scope = lifecycleScope
    private var load = false

    private var uiSettings = UserInterfaceSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(isOnTV(this))
            startMainActivity(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this){
            if (doubleBackToExitPressedOnce) {
                finish()
            }
            doubleBackToExitPressedOnce = true
            toastString("BACK Ancora una volta per uscire")
            Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }

        binding.root.isMotionEventSplittingEnabled = false

        lifecycleScope.launchWhenStarted {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val splash = SplashScreenBinding.inflate(layoutInflater)
                binding.root.addView(splash.root)
                (splash.splashImage.drawable as Animatable).start()
                launch {
                    delay(2000)
                    ObjectAnimator.ofFloat(
                        splash.root,
                        View.TRANSLATION_Y,
                        0f,
                        -splash.root.height.toFloat()
                    ).apply {
                        interpolator = AnticipateInterpolator()
                        duration = 200L
                        doOnEnd { binding.root.removeView(splash.root) }
                        start()
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.TRANSLATION_Y,
                    0f,
                    -splashScreenView.height.toFloat()
                ).apply {
                    interpolator = AnticipateInterpolator()
                    duration = 200L
                    doOnEnd { splashScreenView.remove() }
                    start()
                }
            }
        }

        binding.root.doOnAttach {
            initActivity(this)
            uiSettings = loadData("ui_settings") ?: uiSettings
            selectedOption = uiSettings.defaultStartUpTab
            binding.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        }

        if (!isOnline(this)) {
            toastString("Connessione internet Assente")
            startActivity(Intent(this, NoInternet::class.java))
        } else {
            val model: AnilistHomeViewModel by viewModels()
            model.genres.observe(this) {
                if (it != null) {
                    if (it) {
                        val navbar = binding.navbar
                        bottomBar = navbar
                        navbar.visibility = View.VISIBLE
                        binding.mainProgressBar.visibility = View.GONE
                        val mainViewPager = binding.viewpager
                        mainViewPager.isUserInputEnabled = false
                        mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
                        mainViewPager.setPageTransformer(ZoomOutPageTransformer(uiSettings))
                        navbar.setOnTabSelectListener(object :
                            AnimatedBottomBar.OnTabSelectListener {
                            override fun onTabSelected(
                                lastIndex: Int,
                                lastTab: AnimatedBottomBar.Tab?,
                                newIndex: Int,
                                newTab: AnimatedBottomBar.Tab
                            ) {
                                navbar.animate().translationZ(12f).setDuration(200).start()
                                selectedOption = newIndex
                                mainViewPager.setCurrentItem(newIndex, false)
                            }
                        })
                        navbar.selectTabAt(selectedOption)
                        mainViewPager.post { mainViewPager.setCurrentItem(selectedOption, false) }

                        if (loadMedia != null) {
                            scope.launch {
                                val media = withContext(Dispatchers.IO) {
                                    Anilist.query.getMedia(
                                        loadMedia!!,
                                        loadIsMAL
                                    )
                                }
                                if (media != null) {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            MediaDetailsActivity::class.java
                                        ).putExtra("media", media as Serializable)
                                    )
                                } else {
                                    toastString("Non trovato su Anilist.")
                                }
                            }
                        }
                    } else {
                        binding.mainProgressBar.visibility = View.GONE
                        //                        toastString("Error Loading Tags & Genres.")
                    }
                }
            }
            //Load Data
            if (!load) {
                scope.launch(Dispatchers.IO) {
                    model.loadMain(this@MainActivity)
                }
                load = true
            }
        }
    }




    //ViewPager
    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return AnimeFragment()
                1 -> return if (Anilist.token != null) HomeFragment() else LoginFragment()
                2 -> return MangaFragment()
            }
            return LoginFragment()
        }
    }

}