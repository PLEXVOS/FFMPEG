package com.exe.ffmpeg

import android.os.Bundle
import android.widget.*
import java.io.File

class ExtractAudioActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvFormat: TextView
    private lateinit var etRename: EditText
    private lateinit var switchProcess: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extract_audio)

        tvFile1 = findViewById(R.id.tvFile1)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        tvFormat = findViewById(R.id.tvFormat)
        etRename = findViewById(R.id.etRename)
        switchProcess = findViewById(R.id.switchProcess)

        selectedFormat = ".mp3"

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        setupFormatDial(
            findViewById(R.id.dialFormat), tvFormat,
            resources.getStringArray(R.array.audio_formats)
        )
        setupSwitch(switchProcess) { startExtract() }
    }

    override fun onFile1Selected(name: String, path: String?) { tvFile1.text = name }

    private fun startExtract() {
        val f1 = selectedFile1
        if (f1 == null) {
            Toast.makeText(this, "Seleziona un file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }
        val outName = Utils.nameWithExt(etRename.text.toString(), selectedFormat, "audio")
        val outFile = File(Utils.getOutputDir(this, "Audio"), outName)
        val cmd = "-i \"$f1\" -vn -acodec libmp3lame -q:a 2 \"${outFile.absolutePath}\""
        runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) {}
    }
}
