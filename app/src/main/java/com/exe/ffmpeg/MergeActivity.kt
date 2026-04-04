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

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnFile2).setOnClickListener { pickFile(REQUEST_FILE2) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // Usiamo direttamente il metodo della BaseActivity senza ridefinirlo
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

        if (f1 == null || f2 == null) {
            Toast.makeText(this, "Seleziona entrambi i file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        val outputDir = Utils.getOutputDir()
        val outName = Utils.nameWithExt(etRename.text.toString(), ".mp4", "merged_${System.currentTimeMillis()}")
        val outFile = File(outputDir, outName)

        // Creazione lista per FFmpeg
        val listFile = File(cacheDir, "list.txt")
        try {
            listFile.writeText("file '$f1'\nfile '$f2'")
        } catch (e: Exception) {
            Utils.appendLog(this, "Errore scrittura lista: ${e.message}")
            switchProcess.isChecked = false
            return
        }

        val cmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

        Utils.appendLog(this, "Esecuzione Merge: $cmd")

        runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
            if (success) {
                MediaScannerConnection.scanFile(this, arrayOf(outFile.absolutePath), null, null)
                runOnUiThread {
                    Toast.makeText(this, "Successo! Salvato in Movies/FFmpegOutput", Toast.LENGTH_LONG).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Errore durante l'unione - Controlla i log", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
