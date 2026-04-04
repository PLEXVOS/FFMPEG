package com.exe.ffmpeg

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

abstract class BaseActivity : AppCompatActivity() {
    protected var selectedFile1: String? = null
    protected var selectedFile2: String? = null
    protected var selectedFormat: String = ".mp4"
    protected val REQUEST_FILE1 = 101
    protected val REQUEST_FILE2 = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkStoragePermissions()
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    // Metodo mancante ripristinato
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

    // Metodo mancante ripristinato
    protected fun setupSwitch(switch: Switch, onStart: () -> Unit) {
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onStart()
        }
    }

    protected fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Seleziona File"), requestCode)
    }

    protected fun runFFmpeg(cmd: String, tvProgress: TextView, tvStatus: TextView, switch: Switch, onDone: (Boolean) -> Unit) {
        runOnUiThread {
            tvProgress.text = "0%"
            tvStatus.text = "In corso..."
        }

        FFmpegKit.executeAsync(cmd, { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            val logs = session.allLogsAsString.take(500)
            
            Utils.appendLog(this, if (success) "SUCCESS" else "ERROR: $logs")

            runOnUiThread {
                switch.isChecked = false
                tvProgress.text = if (success) "100%" else "ERRORE"
                tvStatus.text = if (success) "Terminato" else "Fallito"
            }
            onDone(success)
        }, { log ->
            if (log.message.contains("time=")) {
                runOnUiThread { tvStatus.text = "Elaborazione in corso..." }
            }
        }, null)
    }

    private fun copyUriToInternal(uri: Uri): String {
        val file = File(cacheDir, "input_${System.currentTimeMillis()}.tmp")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            val path = copyUriToInternal(data.data!!)
            val name = Utils.getFileNameFromUri(this, data.data!!)
            if (requestCode == REQUEST_FILE1) {
                selectedFile1 = path
                onFile1Selected(name, path)
            } else {
                selectedFile2 = path
                onFile2Selected(name, path)
            }
        }
    }

    open fun onFile1Selected(name: String, path: String?) {}
    open fun onFile2Selected(name: String, path: String?) {}
}
