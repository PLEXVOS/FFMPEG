package com.exe.ffmpeg

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MergeActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvFile2: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvFormat: TextView
    private lateinit var etRename: EditText
    private lateinit var switchProcess: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge)

        tvFile1 = findViewById(R.id.tvFile1)
        tvFile2 = findViewById(R.id.tvFile2)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        tvFormat = findViewById(R.id.tvFormat)
        etRename = findViewById(R.id.etRename)
        switchProcess = findViewById(R.id.switchProcess)

        selectedFormat = ".mp4"

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnFile2).setOnClickListener { pickFile(REQUEST_FILE2) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        setupFormatDial(
            findViewById(R.id.dialFormat), tvFormat,
            resources.getStringArray(R.array.video_formats)
        )

        setupSwitch(switchProcess) { startMerge() }
    }

    override fun onFile1Selected(name: String, path: String?) {
        tvFile1.text = name
    }

    override fun onFile2Selected(name: String, path: String?) {
        tvFile2.text = name
    }

    private fun startMerge() {
        val f1 = selectedFile1
        val f2 = selectedFile2

        if (f1.isNullOrEmpty() || f2.isNullOrEmpty()) {
            Toast.makeText(this, "Seleziona entrambi i file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        val outputDir = Utils.getOutputDir()
        if (!outputDir.exists()) outputDir.mkdirs()

        val outName = Utils.nameWithExt(etRename.text.toString(), selectedFormat, "merged")
        val outFile = File(outputDir, outName)

        // File temporaneo con lista per FFmpeg
        val listFile = File(cacheDir, "concat_list.txt")
        listFile.writeText("file '${f1}'\nfile '${f2}'\n")

        val cmd = "-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

        // Log su file nella cartella di output
        val logFile = File(outputDir, "merge_log_${timestamp()}.txt")
        logFile.writeText("CMD: $cmd\n\n")

        runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
            val msg = if (success) {
                "Merge completato: ${outFile.absolutePath}"
            } else {
                "Errore durante il merge"
            }

            Utils.appendLog(this, msg)
            logFile.appendText("$msg\n")
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}
