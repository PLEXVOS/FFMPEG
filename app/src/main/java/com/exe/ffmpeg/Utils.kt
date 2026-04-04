package com.exe.ffmpeg

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    private const val PREFS = "ffmpeg_log"
    private const val KEY_LOG = "log_content"

    fun getOutputDir(): File {
        // Proviamo a forzare la cartella nella root come richiesto
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, "FFmpegOutput")
        if (!dir.exists()) {
            val success = dir.mkdirs()
            Log.d("FFmpegLog", "Creazione cartella dedicata: $success")
        }
        return dir
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "file_${System.currentTimeMillis()}"
                } ?: "file"
            }
            else -> File(uri.path ?: "file").name
        }
    }

    fun appendLog(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val existing = prefs.getString(KEY_LOG, "") ?: ""
        val updated = "[$ts] $msg\n$existing"
        prefs.edit().putString(KEY_LOG, updated.take(15000)).apply()
    }

    fun getLog(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOG, "") ?: ""
    fun clearLog(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply()

    fun nameWithExt(name: String, ext: String, fallback: String): String {
        val base = if (name.isBlank()) fallback else name.trim()
        return if (base.contains('.')) base else "$base$ext"
    }
}
