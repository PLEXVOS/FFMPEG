package com.exe.ffmpeg

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class LogActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val tvLog = findViewById<TextView>(R.id.tvLog)
        tvLog.text = Utils.getLog(this)

        // Funzione per copiare il log cliccando sul testo
        tvLog.setOnClickListener {
            val logText = tvLog.text.toString()
            if (logText.isNotEmpty() && logText != "Log pulito.") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FFmpeg Log", logText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Log copiato negli appunti!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            Utils.clearLog(this)
            tvLog.text = "Log pulito."
        }
        
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
