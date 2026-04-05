package com.exe.ffmpeg

import android.media.MediaScannerConnection
import android.os.Bundle
import android.widget.*
import java.io.File

class MergeActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvFile2: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etRename: EditText
    private lateinit var switchProcess: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge)

        tvFile1 = findViewById(R.id.tvFile1)
        tvFile2 = findViewById(R.id.tvFile2)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        etRename = findViewById(R.id.etRename)
        switchProcess = findViewById(R.id.switchProcess)

        if (selectedFile1 != null) tvFile1.text = File(selectedFile1!!).name
        if (selectedFile2 != null) tvFile2.text = File(selectedFile2!!).name

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnFile2).setOnClickListener { pickFile(REQUEST_FILE2) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        setupSwitch(switchProcess) { startMerge() }
    }

    override fun onFile1Selected(name: String, path: String?) { tvFile1.text = name }
    override fun onFile2Selected(name: String, path: String?) { tvFile2.text = name }

    private fun step(msg: String) {
        Utils.appendLog(this, msg) // sincrono grazie al .commit() in Utils
        runOnUiThread { tvStatus.text = msg }
    }

    private fun startMerge() {
        step("S1: startMerge avviata")

        val f1 = selectedFile1
        val f2 = selectedFile2

        step("S2: f1=${if(f1==null)"NULL" else "OK"} f2=${if(f2==null)"NULL" else "OK"}")

        if (f1 == null || f2 == null) {
            step("S2-STOP: file null")
            switchProcess.isChecked = false
            return
        }

        try {
            step("S3: getOutputDir...")
            val outputDir = Utils.getOutputDir(this)
            step("S3-OK: ${outputDir.absolutePath}")

            step("S4: controllo file input...")
            val f1ok = File(f1).exists()
            val f2ok = File(f2).exists()
            step("S4: f1.exists=$f1ok f2.exists=$f2ok size1=${File(f1).length()} size2=${File(f2).length()}")

            val outName = Utils.nameWithExt(
                etRename.text.toString(), ".mp4",
                "merged_${System.currentTimeMillis()}"
            )
            val outFile = File(outputDir, outName)

            val listFile = File(cacheDir, "merge_list_${System.currentTimeMillis()}.txt")
            listFile.writeText("file '${f1}'\nfile '${f2}'")
            step("S5: listFile scritto OK")

            val cmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""
            step("S6: avvio FFmpegKit...")

            runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
                if (success) {
                    MediaScannerConnection.scanFile(this, arrayOf(outFile.absolutePath), null, null)
                    step("S7-OK: file creato ${outFile.name}")
                } else {
                    step("S7-FAIL: FFmpeg fallito - vedi log completo")
                }
                listFile.delete()
            }

        } catch (t: Throwable) {
            val msg = "CRASH ${t::class.simpleName}: ${t.message}"
            step(msg)
            switchProcess.isChecked = false
        }
    }
}
