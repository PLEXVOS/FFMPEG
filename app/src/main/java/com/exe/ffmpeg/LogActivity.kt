package com.exe.ffmpeg

import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class LogActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val tvLog = findViewById<TextView>(R.id.tvLog)
        tvLog.text = Utils.getLog(this)

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            Utils.clearLog(this)
            tvLog.text = "Log pulito."
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
