package ani.saikou.settings

import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build.*
import android.os.Build.VERSION.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.databinding.ActivitySettingsBinding
import ani.saikou.others.AppUpdater
import ani.saikou.others.CustomBottomDialog
import ani.saikou.parsers.AnimeSources
import ani.saikou.parsers.MangaSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)
        binding.settingsVersion.setOnLongClickListener {
            fun getArch(): String {
                SUPPORTED_ABIS.forEach {
                    when (it) {
                        "arm64-v8a"   -> return "aarch64"
                        "armeabi-v7a" -> return "arm"
                        "x86_64"      -> return "x86_64"
                        "x86"         -> return "i686"
                    }
                }
                return System.getProperty("os.arch") ?: System.getProperty("os.product.cpu.abi") ?: "Unknown Architecture"
            }

            val info = """
Saikou Version: ${BuildConfig.VERSION_NAME}
Device: $BRAND $DEVICE
Architecture: ${getArch()}
OS Version: $CODENAME $RELEASE ($SDK_INT)
            """.trimIndent()
            copyToClipboard(info, false)
            toast("info dispositivo copiati")
            return@setOnLongClickListener true
        }

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        binding.settingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val animeSource = loadData<Int>("settings_def_anime_source")?.let{ if(it>=AnimeSources.names.size) 0 else it} ?: 0
        binding.animeSource.setText(AnimeSources.names[animeSource], false)
        binding.animeSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, AnimeSources.names))
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_def_anime_source", i)
            binding.animeSource.clearFocus()
        }

        binding.settingsPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }

        binding.settingsDownloadInSd.isChecked = loadData("sd_dl") ?: false
        binding.settingsDownloadInSd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(this, null)
                if (arrayOfFiles.size > 1 && arrayOfFiles[1] != null) {
                    saveData("sd_dl", true)
                } else {
                    binding.settingsDownloadInSd.isChecked = false
                    saveData("sd_dl", false)
                    toastString(getString(R.string.noSdFound))
                }
            } else saveData("sd_dl", false)
        }

        binding.settingsContinueMedia.isChecked = loadData("continue_media") ?: true
        binding.settingsContinueMedia.setOnCheckedChangeListener { _, isChecked ->
            saveData("continue_media", isChecked)
        }

        binding.settingsRecentlyListOnly.isChecked = loadData("recently_list_only") ?: false
        binding.settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
            saveData("recently_list_only", isChecked)
        }

        val dns = listOf("None", "Google", "Cloudflare", "AdGuard")
        binding.settingsDns.setText(dns[loadData("settings_dns") ?: 0], false)
        binding.settingsDns.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, dns))
        binding.settingsDns.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_dns", i)
            initializeNetwork(this)
            binding.settingsDns.clearFocus()
        }

        binding.settingsPreferDub.isChecked = loadData("settings_prefer_dub") ?: false
        binding.settingsPreferDub.setOnCheckedChangeListener { _, isChecked ->
            saveData("settings_prefer_dub", isChecked)
        }

        val mangaSource = loadData<Int>("settings_def_manga_source")?.let{ if(it>=MangaSources.names.size) 0 else it} ?: 0
        binding.mangaSource.setText(MangaSources.names[mangaSource], false)
        binding.mangaSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, MangaSources.names))
        binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_def_manga_source", i)
            binding.mangaSource.clearFocus()
        }

        binding.settingsReader.setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        val uiSettings: UserInterfaceSettings =
            loadData("ui_settings", toast = false) ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
        var previous: View = when (uiSettings.darkMode) {
            null  -> binding.settingsUiAuto
            true  -> binding.settingsUiDark
            false -> binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode: Boolean?, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            uiSettings.darkMode = mode
            saveData("ui_settings", uiSettings)
            Refresh.all()
            finish()
            startActivity(Intent(this, SettingsActivity::class.java))
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(null, it)
        }

        binding.settingsUiLight.setOnClickListener {
            uiTheme(false, it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(true, it)
        }

        var previousStart: View = when (uiSettings.defaultStartUpTab) {
            0    -> binding.uiSettingsAnime
            1    -> binding.uiSettingsHome
            2    -> binding.uiSettingsManga
            else -> binding.uiSettingsHome
        }
        previousStart.alpha = 1f
        fun uiTheme(mode: Int, current: View) {
            previousStart.alpha = 0.33f
            previousStart = current
            current.alpha = 1f
            uiSettings.defaultStartUpTab = mode
            saveData("ui_settings", uiSettings)
            initActivity(this)
        }

        binding.uiSettingsAnime.setOnClickListener {
            uiTheme(0, it)
        }

        binding.uiSettingsHome.setOnClickListener {
            uiTheme(1, it)
        }

        binding.uiSettingsManga.setOnClickListener {
            uiTheme(2, it)
        }

        binding.settingsShowYt.isChecked = uiSettings.showYtButton
        binding.settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
            uiSettings.showYtButton = isChecked
            saveData("ui_settings", uiSettings)
        }

        var previousEp: View = when (uiSettings.animeDefaultView) {
            0    -> binding.settingsEpList
            1    -> binding.settingsEpGrid
            2    -> binding.settingsEpCompact
            else -> binding.settingsEpList
        }
        previousEp.alpha = 1f
        fun uiEp(mode: Int, current: View) {
            previousEp.alpha = 0.33f
            previousEp = current
            current.alpha = 1f
            uiSettings.animeDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsEpList.setOnClickListener {
            uiEp(0, it)
        }

        binding.settingsEpGrid.setOnClickListener {
            uiEp(1, it)
        }

        binding.settingsEpCompact.setOnClickListener {
            uiEp(2, it)
        }

        var previousChp: View = when (uiSettings.mangaDefaultView) {
            0    -> binding.settingsChpList
            1    -> binding.settingsChpCompact
            else -> binding.settingsChpList
        }
        previousChp.alpha = 1f
        fun uiChp(mode: Int, current: View) {
            previousChp.alpha = 0.33f
            previousChp = current
            current.alpha = 1f
            uiSettings.mangaDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsChpList.setOnClickListener {
            uiChp(0, it)
        }

        binding.settingsChpCompact.setOnClickListener {
            uiChp(1, it)
        }

        binding.settingBuyMeCoffee.setOnClickListener {
            openLinkInBrowser("https://www.buymeacoffee.com/brahmkshatriya")
        }

        binding.settingUPI.visibility = if (checkCountry(this)) View.VISIBLE else View.GONE

        binding.settingUPI.setOnClickListener {
            val upi = "upi://pay?pa=brahmkshatriya@apl&pn=Saikou"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upi)
            }
            startActivity(Intent.createChooser(intent, "Donate with..."))
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginTelegram.setOnClickListener {
            openLinkInBrowser(getString(R.string.telegram))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
        }

        binding.settingsUi.setOnClickListener {
            startActivity(Intent(this, UserInterfaceSettingsActivity::class.java))
        }

        binding.settingsFAQ.setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }

        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            (binding.settingsLogo.drawable as Animatable).start()
            toastString(array[(Math.random() * array.size).toInt()], this)
        }

        binding.settingsDev.setOnClickListener {
            DevelopersDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsForks.setOnClickListener {
            ForksDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsDisclaimer.setOnClickListener {
            val title = getString(R.string.disclaimer)
            val text = TextView(this)
            text.setText(R.string.full_disclaimer)

            CustomBottomDialog.newInstance().apply {
                setTitleText(title)
                addView(text)
                setNegativeButton("Close") {
                    dismiss()
                }
                show(supportFragmentManager, "dialog")
            }
        }

        binding.settingsCheckUpdate.isChecked = loadData("check_update") ?: true
        binding.settingsCheckUpdate.setOnCheckedChangeListener { _, isChecked ->
            saveData("check_update", isChecked)
            if (!isChecked) {
                toastString("You Long Click the button to check for App Update")
            }
        }

        binding.settingsLogo.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
        }

        binding.settingsCheckUpdate.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppUpdater.check(this@SettingsActivity, true)
            }
            true
        }
    }
}