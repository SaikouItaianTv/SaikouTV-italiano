package ani.saikou.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.R
import ani.saikou.databinding.ActivityFaqBinding
import ani.saikou.initActivity

class FAQActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaqBinding

    private val faqs = listOf(
        Triple(R.drawable.ic_round_help_24,"What is Saikou?\nWhy should you use Saikou?", "Saikou is crafted based on simplistic yet state-of-the-art elegance. It is an Anilist only client, which also lets you stream-download Anime & Manga. We plan on adding MyAnimeList support very soon.\nSaikou (最高; Sai-kō) literally means the best in japanese. Well, we would like to say this is the best open source app for anime and manga on Android, but hey, Try it out yourself & judge! "),
        Triple(R.drawable.ic_round_auto_awesome_24,"What are some features of Saikou?","Some mentionable features of Saikou are\n\n- Easy and functional way to both, watch anime and read manga, Ad Free.\n- A completely open source app with a nice UI & Animations\n- The most Efficient scraping for anime and manga from multiple sources. (Spoiler Alert : No web-views were harmed)\n- Synchronize anime and manga real-time with AniList. Easily categorise anime and manga based on your current status. (Powered by AniList)\n- Find all shows using thoroughly and frequently updated list of all trending, popular and ongoing anime based on scores.\n- View extensive details about anime shows, movies and manga titles. It also features ability to countdown to the next episode of airing anime. (Powered by AniList & MyAnimeList)"),
        Triple(R.drawable.ic_round_download_24,"What are Artifacts?","Whenever a developer commits or pull requests a feature or fix, github automatically makes an APK file for you to use. This APK is called an Artifact. Artifacts through pull requests may or may not be added to the main release of the app. Artifacts have a higher chance of having bugs and glitches. To know if new artifacts are available, star the saikou repository and turn on notifications\n\nTo download an Artifact:\n1) Sign In/Up in GitHub\n2) Go to saikou\n3) Go to actions\n4) Press on the workflow run you want to download the artifact of.\n5) Press on artifact\n6) Extract the file using a zip extractor\n7) Install and enjoy.\n\nYou can get a video tutorial, press [here](https://cdn.discordapp.com/attachments/930496165512896543/1050799830714298418/Video.Guru_20221204_114907988.mp4)"),
        Triple(R.drawable.ic_round_dns_24,"Is Saikou available for PC?","Currently no(for both Windows and Linux). There isn't any estimation when it will be available. But you can download any android emulator and run saikou on it. For Windows 11 users, they can use the Windows Subsystem for Android(a.k.a WSA) to run Saikou in Windows."),
        Triple(R.drawable.ic_baseline_screen_lock_portrait_24,"Is Saikou available for IOS?","NO!"),
        Triple(R.drawable.ic_anilist,"Why are my stats not updating?","This is because it updates every 48 hours automatically (by anilist), If you really need to update your stats, you can force update your stats after going to this [link](https://anilist.co/settings/lists)"),
        Triple(R.drawable.ic_round_movie_filter_24,"How to download Episodes?","Please follow this link for a video tutorial\n[link](https://cdn.discordapp.com/attachments/930496165512896543/1021728565097664582/Saikou_ep_download_tutorial_Full_HD_1080p_HIGH_FR30.mp4)\n\nPlease remember that there is a method 2 after 50 seconds into the video. Please don't leave after method 1 and complain that all sources don't have the download button.\nNOTE: IF ADM DOESN'T WORK FOR METHOD 2 THEN USE 1DM"),
        Triple(R.drawable.ic_round_menu_book_24,"How to download Manga Chapters?","It is yet not possible to download chapters in Saikou but this feature will be implemented soon."),
        Triple(R.drawable.ic_round_lock_open_24,"How to enable NSFW content?","You can enable nsfw content by enabling 18+ contents form this [link](https://anilist.co/settings/media)"),
        Triple(R.drawable.ic_round_smart_button_24,"How to import my anime/manga list from Mal/Kitsu to Anilist?","Here is how you do it,\n\nExport: Go to this [link](https://malscraper.azurewebsites.net/) . Then give your Kitsu/MAL username and download both anime and manga list.(they will be in xml format)\nNote: You have to write the username of the tracker you selected\n\nImport:\nAfter exporting your anime and manga list from kitsu/MAL, now go [here](https://anilist.co/settings/import)\nSelect/drop the anime xml file on the box above.\nSelect/drop the manga xml file on the box below."),
        Triple(R.drawable.ic_round_info_24,"Why can't I find a specific anime/manga title?","Let's say you are looking for Castlevania in Saikou. But Anilist doesn't have it, so saikou doesn't too.\nA solution to the above problem is as follows-\n1) Go to any anime that's not in your list.\n2) Go to watch section.\n3) Select any source and press on wrong title.\n4) Now search for Castlevania (The anime you were looking for) and select it.\n5) ENJOY!\n\nIf you can't find the anime even through these steps, then that's bad luck for you bud. Even that source doesn't have it. Try a different source."),
        Triple(R.drawable.ic_round_help_24,"How to fix sources selecting a completely wrong title?","If your app selects the wrong title, then you can correct it by pressing on \"Wrong Title?\" above the layouts and selecting the correct title"),
        Triple(R.drawable.ic_round_art_track_24,"How to read coloured mangas?","Are you in search of coloured manga? Sorry to break it to you but an extremely small amount of mangas have coloured version. Those which has a coloured version is also available in Saikou. Let's say you want to read the coloured version of chainsaw man. Then follow the below steps ↓\n\n1) Go to Chainsaw Man\n2) Press on 'Read'\n3) Select any working source\n4) Press on 'Wrong Title'\n5) Select the colored version chainsaw man\n6) Enjoy\n\nNote: Many sources don't have the coloured version available even if it's available somewhere in the internet. So try a different source. If non of the sources have it, then a coloured version of your desired manga simply doesn't exist. If you can find it in any manga site in the internet, you can suggest that site through the discord server."),
        Triple(R.drawable.ic_round_video_settings_24,"Handshake fails? Why are no timestamps not loading?","You can fix this issue by enabling `Proxy` from \n`settings > anime > player settings > timestamps > proxy`\nIf still timestamps are not loading but handshake failed popping up is fixed, then the episode you are watching just doesn't have timestamps yet for it."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.devsTitle2.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.devsRecyclerView.adapter = FAQAdapter(faqs, supportFragmentManager)
        binding.devsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}
