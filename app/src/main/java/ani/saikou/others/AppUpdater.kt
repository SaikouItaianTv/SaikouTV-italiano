package ani.saikou.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import ani.saikou.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object AppUpdater {
    fun check(activity: Activity, post:Boolean=false){
        try{
            val version =
                if(!BuildConfig.DEBUG)
                    OkHttpClient().newCall(Request.Builder().url("https://raw.githubusercontent.com/Nanoc6/SaikouTV/main/stable.txt").build()).execute().body?.string()?.replace("\n","")?:return
                else {
                    OkHttpClient().newCall(
                        Request.Builder()
                            .url("https://raw.githubusercontent.com/Nanoc6/SaikouTV/main/app/build.gradle")
                            .build()
                    ).execute().body?.string()?.substringAfter("versionName \"")?.substringBefore('"') ?: return
                }
            val dontShow = loadData("dont_ask_for_update_$version")?:false
            if(compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                AlertDialog.Builder(activity, if(isOnTV(activity)) R.style.TVDialogTheme else R.style.DialogTheme)
                    .setTitle("A new update is available, do you want to check it out?").apply {
                        setMultiChoiceItems(
                            arrayOf("Don't show again for version $version"),
                            booleanArrayOf(false)
                        ) { _, _, isChecked ->
                            if (isChecked) {
                                saveData("dont_ask_for_update_$version", isChecked)
                            }
                        }
                        setPositiveButton("Let's Go") { _: DialogInterface, _: Int ->
                            if(!BuildConfig.DEBUG) {
                                MainScope().launch(Dispatchers.IO){

                                    try{
                                        OkHttpClient().newCall(Request.Builder().url("https://api.github.com/repos/Nanoc6/SaikouTV/releases/tags/v$version"+"-tv").build()).execute().body?.string()?.apply {
                                            substringAfter("\"browser_download_url\":\"").substringBefore('"').apply {
                                                if (endsWith("apk")) activity.downloadUpdate(this)
                                                else openLinkInBrowser("https://github.com/Nanoc6/SaikouTV/releases/")
                                            }
                                        }
                                    }catch (e:Exception){
                                        toastString(e.toString())
                                    }
                                }
                            }
                            else openLinkInBrowser( "https://discord.com/channels/902174389351620629/946852010198728704")
                        }
                        setNegativeButton("Cope") { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                    }.show()
            }
            else{
                if(post) toastString("No Update Found")
            }
        }
        catch (e:Exception){
            toastString(e.toString())
        }
    }

    private fun compareVersion(version: String): Boolean {

        fun toDouble(list: List<String>): Double {
            return list.mapIndexed { i: Int, s: String ->
                when (i) {
                    0 -> s.toDouble() * 100
                    1 -> s.toDouble() * 10
                    2 -> s.toDouble()
                    3 -> "0.$s".toDouble()
                    4 -> "0.0$s".toDouble()
                    else -> s.toDouble()
                }
            }.sum()
        }

        val new = toDouble(version.split("."))
        val curr = toDouble(BuildConfig.VERSION_NAME.split("."))
        return new > curr
    }


    //Blatantly kanged from https://github.com/LagradOst/CloudStream-3/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/InAppUpdater.kt
    private fun Activity.downloadUpdate(version: String, url: String): Boolean {

        toast("Scaricando L'aggiornamento $version")

        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Scaricando Saikou $version")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Saikou.apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            logError(e)
            -1
        }
        if (id == -1L) return true
        registerReceiver(
            object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val downloadId = intent?.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, id
                        ) ?: id

                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val c = downloadManager.query(query)

                        if (c.moveToFirst()) {
                            val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (DownloadManager.STATUS_SUCCESSFUL == c
                                    .getInt(columnIndex)
                            ) {
                                c.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)
                                val uri = Uri.parse(
                                    c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                )
                                openApk(this@downloadUpdate, uri)
                            }
                        }
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        return true
    }

    fun openApk(context: Context, uri: Uri) {
        try{
            uri.path?.let {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(it)
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    data = contentUri
                }
                context.startActivity(installIntent)
            }
        }catch (e:Exception){
            toastString(e.toString())
        }
    }
}
