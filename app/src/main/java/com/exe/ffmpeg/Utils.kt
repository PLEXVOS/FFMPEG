package com.exe.ffmpeg

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    private const val PREFS = "ffmpeg_log"
    private const val KEY_LOG = "log_content"

    fun getOutputDir(): File {
        val dir = File(android.os.Environment.getExternalStorageDirectory(), "FFmpegOutput")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "file"
                } ?: "file"
            }
            "file" -> File(uri.path ?: "file").name
            else -> "file"
        }
    }

    fun getRealPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        val temp = File(context.cacheDir, getFileNameFromUri(context, uri))
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            temp.absolutePath
        } catch (e: Exception) { null }
    }

    fun appendLog(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val existing = prefs.getString(KEY_LOG, "") ?: ""
        val updated = "[$ts] $msg\n$existing"
        prefs.edit().putString(KEY_LOG, updated.take(20000)).apply()
    }

    fun getLog(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOG, "Nessun log disponibile.") ?: ""
    }

    fun clearLog(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply()
    }

    fun nameWithExt(name: String, ext: String, fallback: String): String {
        val base = if (name.isBlank()) fallback else name.trim()
        return if (base.contains('.')) base else "$base$ext"
    }
}
