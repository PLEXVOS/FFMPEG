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
        // Su Android 16, scrivere nella Root è bloccato. Usiamo la cartella Movies.
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val dir = File(publicDir, "FFmpegOutput")
        if (!dir.exists()) {
            dir.mkdirs()
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
            "file" -> File(uri.path ?: "file").name
            else -> "file"
        }
    }

    fun appendLog(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val existing = prefs.getString(KEY_LOG, "") ?: ""
        val updated = "[$ts] $msg\n$existing"
        prefs.edit().putString(KEY_LOG, updated.take(10000)).apply()
        Log.d("FFmpegLog", msg)
    }

    fun nameWithExt(name: String, ext: String, fallback: String): String {
        val base = if (name.isBlank()) fallback else name.trim()
        return if (base.endsWith(ext)) base else "$base$ext"
    }
}
