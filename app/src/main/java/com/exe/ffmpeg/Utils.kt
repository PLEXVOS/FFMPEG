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

    // subfolder vuoto = Movies/FFmpegOutput/
    // subfolder "Uniti" = Movies/FFmpegOutput/Uniti/
    fun getOutputDir(context: Context, subfolder: String = ""): File {
        val base = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "FFmpegOutput"
        )
        val dir = if (subfolder.isNotBlank()) File(base, subfolder) else base
        if (!dir.exists()) {
            val success = dir.mkdirs()
            Log.d("FFmpegLog", "Cartella: ${dir.absolutePath} created=$success")
        }
        // Fallback cartella privata se storage pubblico non accessibile
        if (!dir.exists()) {
            val fallback = File(
                context.getExternalFilesDir(null),
                if (subfolder.isNotBlank()) "FFmpegOutput/$subfolder" else "FFmpegOutput"
            )
            fallback.mkdirs()
            return fallback
        }
        return dir
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx)
                    else "file_${System.currentTimeMillis()}"
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
        prefs.edit().putString(KEY_LOG, updated.take(15000)).commit()
        Log.d("FFmpegLog", "[$ts] $msg")
    }

    fun getLog(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOG, "") ?: ""

    fun clearLog(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LOG).apply()

    fun nameWithExt(name: String, ext: String, fallback: String): String {
        val base = if (name.isBlank()) fallback else name.trim()
        return if (base.contains('.')) base else "$base$ext"
    }
}
