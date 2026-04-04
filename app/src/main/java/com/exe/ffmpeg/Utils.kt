package com.exe.ffmpeg

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
        val dir = File(android.os.Environment.getExternalStorageDirectory(), "FFmpegOutput")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // === VERSIONE SUPER ROBUSTA DEL LOG ===
    fun appendLog(context: Context, msg: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val existing = prefs.getString(KEY_LOG, "") ?: ""
            val updated = "[$ts] $msg\n$existing"
            prefs.edit().putString(KEY_LOG, updated.take(20000)).apply()
        } catch (e: Exception) {
            // Fallback su Logcat (lo vedi con Android Studio / Logcat)
            Log.e("FFmpegDebug", "appendLog fallito: $msg | Errore: ${e.message}")
        }
    }

    fun getLog(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOG, "Nessun log disponibile.") ?: ""
    }

    fun clearLog(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply()
    }

    // le altre funzioni rimangono uguali...
    fun getFileNameFromUri(context: Context, uri: Uri): String { ... } // copia le tue
    fun getRealPath(context: Context, uri: Uri): String? { ... }
    fun nameWithExt(name: String, ext: String, fallback: String): String { ... }
}
