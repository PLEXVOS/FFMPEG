package com.exe.ffmpeg

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode

abstract class BaseActivity : AppCompatActivity() {

    protected var selectedFile1: String? = null
    protected var selectedFile2: String? = null
    protected var selectedFormat: String = ".mp4"
    protected val REQUEST_FILE1 = 101
    protected val REQUEST_FILE2 = 102

    protected fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Seleziona file"), requestCode)
    }

    protected fun setupFormatDial(dialFormat: FrameLayout, tvFormat: TextView, formats: Array<String>) {
        dialFormat.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Seleziona formato")
                .setItems(formats) { _, which ->
                    selectedFormat = formats[which]
                    tvFormat.text = selectedFormat
                }
                .show()
        }
    }

    protected fun setupSwitch(switch: Switch, onStart: () -> Unit) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onStart()
            }
        }
    }

    protected fun updateProgress(tvProgress: TextView, tvStatus: TextView, progress: Int, status: String) {
        runOnUiThread {
            tvProgress.text = "$progress%"
            tvStatus.text = status
        }
    }

    protected fun runFFmpeg(
        cmd: String,
        tvProgress: TextView,
        tvStatus: TextView,
        switch: Switch,
        onDone: (Boolean) -> Unit
    ) {
        updateProgress(tvProgress, tvStatus, 0, "Elaborazione...")
        Utils.appendLog(this, "CMD: $cmd")

        FFmpegKit.executeAsync(cmd, { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            val msg = if (success) "Completato" else "Errore: ${session.failStackTrace?.take(100)}"
            Utils.appendLog(this, msg)
            runOnUiThread {
                tvProgress.text = if (success) "100%" else "ERR"
                tvStatus.text = msg
                switch.isChecked = false
                Toast.makeText(this, if (success) "Completato!" else "Errore", Toast.LENGTH_LONG).show()
            }
            onDone(success)
        }, { log ->
            val line = log.message ?: ""
            // Parse progress from FFmpeg output
            if (line.contains("time=")) {
                runOnUiThread { tvStatus.text = line.take(60) }
            }
        }, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            val path = Utils.getRealPath(this, uri)
            val name = Utils.getFileNameFromUri(this, uri)
            when (requestCode) {
                REQUEST_FILE1 -> {
                    selectedFile1 = path
                    onFile1Selected(name, path)
                }
                REQUEST_FILE2 -> {
                    selectedFile2 = path
                    onFile2Selected(name, path)
                }
            }
        }
    }

    open fun onFile1Selected(name: String, path: String?) {}
    open fun onFile2Selected(name: String, path: String?) {}
}
