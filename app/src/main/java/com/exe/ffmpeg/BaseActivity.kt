package com.exe.ffmpeg

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    companion object {
        private const val KEY_FILE1 = "key_file1"
        private const val KEY_FILE2 = "key_file2"
        private const val KEY_FORMAT = "key_format"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NESSUNA chiamata a checkStoragePermissions qui
        // I permessi vengono gestiti solo da MainActivity
        if (savedInstanceState != null) {
            selectedFile1 = savedInstanceState.getString(KEY_FILE1)
            selectedFile2 = savedInstanceState.getString(KEY_FILE2)
            selectedFormat = savedInstanceState.getString(KEY_FORMAT, ".mp4") ?: ".mp4"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FILE1, selectedFile1)
        outState.putString(KEY_FILE2, selectedFile2)
        outState.putString(KEY_FORMAT, selectedFormat)
    }

    protected fun setupFormatDial(
        dialFormat: FrameLayout,
        tvFormat: TextView,
        formats: Array<String>
    ) {
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
            if (isChecked) onStart()
        }
    }

    protected fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(
            Intent.createChooser(intent, "Seleziona File"),
            requestCode
        )
    }

    protected fun runFFmpeg(
        cmd: String,
        tvProgress: TextView,
        tvStatus: TextView,
        switch: Switch,
        onDone: (Boolean) -> Unit
    ) {
        runOnUiThread {
            tvProgress.text = "0%"
            tvStatus.text = "In corso..."
        }

        FFmpegKit.executeAsync(cmd, { session ->
            val success = ReturnCode.isSuccess(session.returnCode)
            val logs = session.allLogsAsString?.take(500) ?: "nessun log"
            Utils.appendLog(
                this,
                if (success) "SUCCESS" else "ERROR: $logs"
            )
            runOnUiThread {
                switch.isChecked = false
                tvProgress.text = if (success) "100%" else "ERRORE"
                tvStatus.text = if (success) "Terminato" else "Fallito"
            }
            runOnUiThread { onDone(success) }

        }, { log ->
            if (log.message.contains("time=")) {
                runOnUiThread { tvStatus.text = "Elaborazione..." }
            }
        }, null)
    }

    private fun copyUriToInternal(uri: Uri): String? {
        return try {
            val file = File(cacheDir, "input_${System.currentTimeMillis()}.tmp")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) {
            Utils.appendLog(this, "Errore copia URI: ${e.message}")
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            val path = copyUriToInternal(data.data!!)
            if (path == null) {
                Toast.makeText(this, "Errore lettura file", Toast.LENGTH_SHORT).show()
                return
            }
            val name = Utils.getFileNameFromUri(this, data.data!!)
            if (requestCode == REQUEST_FILE1) {
                selectedFile1 = path
                onFile1Selected(name, path)
            } else if (requestCode == REQUEST_FILE2) {
                selectedFile2 = path
                onFile2Selected(name, path)
            }
        }
    }

    open fun onFile1Selected(name: String, path: String?) {}
    open fun onFile2Selected(name: String, path: String?) {}
}
