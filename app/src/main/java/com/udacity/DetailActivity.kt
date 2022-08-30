package com.udacity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_detail.*
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.udacity.BuildConfig
import com.udacity.R
import com.udacity.DownloadStatus
import com.udacity.DownloadStatus.FAILED
import com.udacity.DownloadStatus.SUCCESSFUL
import com.udacity.databinding.ActivityDetailBinding


class DetailActivity : AppCompatActivity() {

    /*private val fileName by lazy {
        intent?.extras?.getString(EXTRA_FILE_NAME, unknownText) ?: unknownText
    }
    private val downloadStatus by lazy {
        intent?.extras?.getString(EXTRA_DOWNLOAD_STATUS, unknownText) ?: unknownText
    }
*/
    private val unknownText by lazy { getString(R.string.unknown) }

    private lateinit var binding : ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)

        binding.apply {
                setSupportActionBar(toolbar)
                content_detail.initializeView()
            }

    }


    private fun DetailContentBinding.initializeView() {
        fileNameText.text = fileName
        downloadStatusText.text = downloadStatus
        okButton.setOnClickListener { finish() }
        changeViewForDownloadStatus()
    }

    private fun DetailContentBinding.changeViewForDownloadStatus() {
        when (downloadStatusText.text) {
            SUCCESSFUL.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_check_circle_outline_24)
                changeDownloadStatusColorTo(R.color.colorPrimaryDark)
            }
            FAILED.statusText -> {
                changeDownloadStatusImageTo(R.drawable.ic_error_24)
                changeDownloadStatusColorTo(R.color.design_default_color_error)
            }
        }
    }

    private fun DetailContentBinding.changeDownloadStatusImageTo(@DrawableRes imageRes: Int) {
        downloadStatusImage.setImageResource(imageRes)
    }

    private fun DetailContentBinding.changeDownloadStatusColorTo(@ColorRes colorRes: Int) {
        ContextCompat.getColor(this@DetailActivity, colorRes)
            .also { color ->
                downloadStatusImage.imageTintList = ColorStateList.valueOf(color)
                downloadStatusText.setTextColor(color)
            }
    }

    companion object {
        private const val EXTRA_FILE_NAME = "${BuildConfig.APPLICATION_ID}.FILE_NAME"
        private const val EXTRA_DOWNLOAD_STATUS = "${BuildConfig.APPLICATION_ID}.DOWNLOAD_STATUS"

        /**
         * Creates a [Bundle] with given parameters and pass as data to [DetailActivity].
         */
        fun bundleExtrasOf(
            fileName: String,
            downloadStatus: DownloadStatus
        ) = bundleOf(
            EXTRA_FILE_NAME to fileName,
            EXTRA_DOWNLOAD_STATUS to downloadStatus.statusText
        )
    }


}
