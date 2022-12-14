package com.udacity

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber
import com.udacity.DownloadStatus.*
import com.udacity.ButtonState.*
import kotlinx.android.synthetic.main.content_main.loading_button
import kotlinx.android.synthetic.main.content_main.view.*

class MainActivity : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private lateinit var viewBinding: ActivityMainBinding

    private var downloadFileName = ""
    private var downloadID: Long = NO_DOWNLOAD
    private var downloadContentObserver: ContentObserver? = null
    private var downloadNotificator: DownloadNotificator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(toolbar)

        viewBinding.apply {
            viewBinding = this
            setSupportActionBar(toolbar)
            registerReceiver(
                onDownloadCompletedReceiver,
                IntentFilter(ACTION_DOWNLOAD_COMPLETE)
            )
            onLoadingButtonClicked()
        }

      //  registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

       // loading_button.setOnClickListener { download() }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        }
    }


    private fun ActivityMainBinding.onLoadingButtonClicked() {
        with(contentMain) {
            loading_button.setOnClickListener {
                when (groupradio.checkedRadioButtonId) {
                    View.NO_ID ->
                        Toast.makeText(
                            this@MainActivity,
                            "Please select the file to download",
                            Toast.LENGTH_SHORT
                        ).show()
                    else -> {
                        downloadFileName =
                            findViewById<RadioButton>(groupradio.checkedRadioButtonId)
                                .text.toString()
                        requestDownload()
                    }
                }
            }
        }
    }

    private fun requestDownload() {
        with(getDownloadManager()) {
            downloadID.takeIf { it != NO_DOWNLOAD }?.run {
                val downloadsCancelled = remove(downloadID)
                unregisterDownloadContentObserver()
                downloadID = NO_DOWNLOAD
                Timber.d("Number of downloads cancelled: $downloadsCancelled")
            }

            val request = Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadID = enqueue(request)

            createAndRegisterDownloadContentObserver()
        }
    }


    private val onDownloadCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
            id?.let {
                val downloadStatus = getDownloadManager().queryStatus(it)
                Timber.d("Download $it completed with status: ${downloadStatus.statusText}")
                unregisterDownloadContentObserver()
                downloadStatus.takeIf { status -> status != UNKNOWN }?.run {
                    getDownloadNotificator().notify(downloadFileName, downloadStatus)
                }
            }
        }
    }


    private fun getDownloadNotificator(): DownloadNotificator = when (downloadNotificator) {
        null -> DownloadNotificator(this, lifecycle).also { downloadNotificator = it }
        else -> downloadNotificator!!
    }

    private fun DownloadManager.queryStatus(id: Long): DownloadStatus {
        query(Query().setFilterById(id)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    return when (getInt(getColumnIndex(COLUMN_STATUS))) {
                        STATUS_SUCCESSFUL -> SUCCESSFUL
                        STATUS_FAILED -> FAILED
                        else -> UNKNOWN
                    }
                }
                return UNKNOWN
            }
        }
    }

    private fun download() {
        val request =
            DownloadManager.Request(Uri.parse(URL))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    private fun DownloadManager.createAndRegisterDownloadContentObserver() {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                downloadContentObserver?.run { queryProgress() }
            }
        }.also {
            downloadContentObserver = it
            contentResolver.registerContentObserver(
                "content://downloads/my_downloads".toUri(),
                true,
                downloadContentObserver!!
            )
        }
    }

    private fun DownloadManager.queryProgress() {
        query(Query().setFilterById(downloadID)).use {
            with(it) {
                if (this != null && moveToFirst()) {
                    val id = getInt(getColumnIndex(COLUMN_ID))
                    when (getInt(getColumnIndex(COLUMN_STATUS))) {
                        STATUS_FAILED -> {
                            Timber.d("Download $id: failed")
                            viewBinding.contentMain.loading_button.changeButtonState(Completed)
                        }
                        STATUS_PAUSED -> {
                            Timber.d("Download $id: paused")
                        }
                        STATUS_PENDING -> {
                            Timber.d("Download $id: pending")
                        }
                        STATUS_RUNNING -> {
                            Timber.d("Download $id: running")
                            viewBinding.contentMain.loading_button.changeButtonState(Loading)
                        }
                        STATUS_SUCCESSFUL -> {
                            Timber.d("Download $id: successful")
                            viewBinding.contentMain.loading_button.changeButtonState(Completed)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadCompletedReceiver)
        unregisterDownloadContentObserver()
        downloadNotificator = null
    }

    private fun unregisterDownloadContentObserver() {
        downloadContentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            downloadContentObserver = null
        }
    }
    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val CHANNEL_ID = "channelId"
        private const val NO_DOWNLOAD = 0L
    }

}
