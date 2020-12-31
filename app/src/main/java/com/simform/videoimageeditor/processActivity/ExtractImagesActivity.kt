package com.simform.videoimageeditor.processActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.jaiselrahman.filepicker.activity.FilePickerActivity
import com.jaiselrahman.filepicker.config.Configurations
import com.jaiselrahman.filepicker.model.MediaFile
import com.simform.videoimageeditor.BaseActivity
import com.simform.videoimageeditor.R
import com.simform.videoimageeditor.utils.Common
import com.simform.videoimageeditor.utils.Extension
import java.io.File
import java.util.concurrent.CyclicBarrier
import kotlinx.android.synthetic.main.activity_extract_images.btnExtract
import kotlinx.android.synthetic.main.activity_extract_images.btnVideoPath
import kotlinx.android.synthetic.main.activity_extract_images.mProgressView
import kotlinx.android.synthetic.main.activity_extract_images.tvInputPathVideo
import kotlinx.android.synthetic.main.activity_extract_images.tvOutputPath

class ExtractImagesActivity : BaseActivity(R.layout.activity_extract_images) {
    private var isInputVideoSelected: Boolean = false
    override fun initialization() {
        btnVideoPath.setOnClickListener(this)
        btnExtract.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnVideoPath -> {
                Common.selectFile(this, maxSelection = 1, isImageSelection = false)
            }
            R.id.btnExtract -> {
                when {
                    !isInputVideoSelected -> {
                        Toast.makeText(
                            this,
                            getString(R.string.input_video_validate_message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        processStart()
                        val gate = CyclicBarrier(2)
                        val imageToVideo = object : Thread() {
                            override fun run() {
                                gate.await()
                                extractProcess()
                            }
                        }
                        imageToVideo.start()
                        gate.await()
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    override fun selectedFiles(mediaFiles: List<MediaFile>?, requestCode: Int) {
        when (requestCode) {
            Common.VIDEO_FILE_REQUEST_CODE -> {
                if (mediaFiles != null && mediaFiles.isNotEmpty()) {
                    tvInputPathVideo.text = mediaFiles[0].path
                    isInputVideoSelected = true
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.video_not_selected_toast_message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun extractProcess() {
        val dir = File(getExternalFilesDir(Common.OUT_PUT_DIR).toString())
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val dest = File(dir.path + File.separator + Common.OUT_PUT_DIR + System.currentTimeMillis().div(1000L) + "%03d.jpg")
        val outputPath = dest.absolutePath

        val query = Extension.extractImages(tvInputPathVideo.text.toString(), outputPath, spaceOfFrame = 4f)

        var totalFramesExtracted = 0
        Config.enableStatisticsCallback { log ->
            log?.videoFrameNumber?.let {
                totalFramesExtracted = it
            }
            tvOutputPath.text = "Frames : ${log?.videoFrameNumber}"
        }
        when (FFmpeg.execute(query)) {
            Config.RETURN_CODE_SUCCESS -> {
                runOnUiThread {
                    tvOutputPath.text = "Output Directory : \n${File(dir.path + File.separator + Common.OUT_PUT_DIR).absolutePath} \n\nTotal Frames Extracted: $totalFramesExtracted"
                    processStop()
                }
            }
            Config.RETURN_CODE_CANCEL -> {
                processStop()
                FFmpeg.cancel()
            }
            else -> {
                processStop()
                Config.printLastCommandOutput(Log.INFO)
            }
        }
    }

    private fun processStop() {
        runOnUiThread {
            btnVideoPath.isEnabled = true
            btnExtract.isEnabled = true
            mProgressView.visibility = View.GONE
        }
    }

    private fun processStart() {
        runOnUiThread {
            btnVideoPath.isEnabled = false
            btnExtract.isEnabled = false
            mProgressView.visibility = View.VISIBLE
        }
    }
}