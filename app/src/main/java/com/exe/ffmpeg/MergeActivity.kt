package com.exe.ffmpeg

import android.os.Bundle
import android.widget.*
import java.io.File

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

        setupFormatDial(findViewById(R.id.dialFormat), tvFormat, resources.getStringArray(R.array.video_formats))
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

        val outName = Utils.nameWithExt(etRename.text.toString(), selectedFormat, "merged")
        val outputDir = File("/storage/emulated/0/FFmpegOutput")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outFile = File(outputDir, outName)

        val listFile = File(cacheDir, "concat_list.txt")
        try {
            listFile.writeText("file '${f1.absolutePath}'\nfile '${f2.absolutePath}'\n")
        } catch (e: Exception) {
            Toast.makeText(this, "Errore scrittura lista file: ${e.message}", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        // Verifica percorsi
        if (!f1.exists() || !f2.exists()) {
            Toast.makeText(this, "Uno dei file selezionati non esiste", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        if (!listFile.exists() || !outputDir.exists()) {
            Toast.makeText(this, "Errore di accesso directory cache o output", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        val cmd = "-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

        // Controllo finale prima di eseguire
        if (cmd.contains("  ") || cmd.contains("\n")) {
            Toast.makeText(this, "Comando FFmpeg contiene errori di formattazione", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
            if (success) {
                Utils.appendLog(this, "Output creato: ${outFile.absolutePath}")
                Toast.makeText(this, "File unito creato in: ${outFile.absolutePath}", Toast.LENGTH_LONG).show()
            } else {
                Utils.appendLog(this, "Errore durante il merge")
                Toast.makeText(this, "Errore durante il merge, controlla il log", Toast.LENGTH_LONG).show()
            }
        }
    }
}
