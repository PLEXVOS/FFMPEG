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

        // Ripristina i nomi file se l'Activity era stata ricreata
        if (selectedFile1 != null) tvFile1.text = File(selectedFile1!!).name
        if (selectedFile2 != null) tvFile2.text = File(selectedFile2!!).name

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnFile2).setOnClickListener { pickFile(REQUEST_FILE2) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        setupSwitch(switchProcess) { startMerge() }
    }

    override fun onFile1Selected(name: String, path: String?) { tvFile1.text = name }
    override fun onFile2Selected(name: String, path: String?) { tvFile2.text = name }

    private fun startMerge() {
        val f1 = selectedFile1
        val f2 = selectedFile2

        if (f1 == null || f2 == null) {
            Toast.makeText(this, "Seleziona entrambi i file prima di procedere", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        // AGGIUNTO: try-catch per esporre qualsiasi eccezione nascosta
        try {
            // FIX: passa context a getOutputDir
            val outputDir = Utils.getOutputDir(this)
            val outName = Utils.nameWithExt(
                etRename.text.toString(), ".mp4",
                "merged_${System.currentTimeMillis()}"
            )
            val outFile = File(outputDir, outName)

            val listFile = File(cacheDir, "merge_list_${System.currentTimeMillis()}.txt")
            listFile.writeText("file '${f1}'\nfile '${f2}'")

            val cmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

            Utils.appendLog(this, "Merge avviato: $cmd")
            Utils.appendLog(this, "Output dir esiste: ${outputDir.exists()} | path: ${outputDir.absolutePath}")
            Utils.appendLog(this, "File1 esiste: ${File(f1).exists()} size: ${File(f1).length()}")
            Utils.appendLog(this, "File2 esiste: ${File(f2).exists()} size: ${File(f2).length()}")

            runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
                if (success) {
                    MediaScannerConnection.scanFile(
                        this, arrayOf(outFile.absolutePath), null, null
                    )
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Fatto! File: ${outFile.name}\nCartella: ${outputDir.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                // Pulizia file temporaneo lista
                listFile.delete()
            }

        } catch (e: Exception) {
            Utils.appendLog(this, "ECCEZIONE in startMerge: ${e::class.simpleName}: ${e.message}")
            switchProcess.isChecked = false
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
