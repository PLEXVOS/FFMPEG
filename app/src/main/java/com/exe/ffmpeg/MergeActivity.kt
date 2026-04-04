package com.exe.ffmpeg

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MergeActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvFile2: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvFormat: TextView
    private lateinit var etRename: EditText
    private lateinit var switchProcess: Switch

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permesso necessario per scrivere i file", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
        }
    }

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

        checkWritePermission()
    }

    private fun checkWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Permesso gestione file richiesto", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
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

        val outputDir = File(getExternalFilesDir("FFmpegOutput"), "")
        if (!outputDir.exists()) {
            val created = outputDir.mkdirs()
            if (!created) {
                Toast.makeText(this, "Impossibile creare cartella output", Toast.LENGTH_LONG).show()
                switchProcess.isChecked = false
                return
            }
        }

        val outName = Utils.nameWithExt(etRename.text.toString(), selectedFormat, "merged")
        val outFile = File(outputDir, outName)

        val listFile = File(cacheDir, "concat_list.txt")
        try {
            listFile.writeText("file '${f1}'\nfile '${f2}'\n")
        } catch (e: Exception) {
            Toast.makeText(this, "Errore creazione file lista: ${e.message}", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
            return
        }

        val cmd = "-f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

        // Log immediato nella cache
        Utils.appendLog(this, "CMD: $cmd")
        try {
            runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
                if (success) {
                    Utils.appendLog(this, "Output: ${outFile.absolutePath}")
                    Toast.makeText(this, "Merge completato", Toast.LENGTH_LONG).show()
                } else {
                    Utils.appendLog(this, "Merge fallito")
                    Toast.makeText(this, "Merge fallito", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Utils.appendLog(this, "Eccezione FFmpeg: ${e.message}")
            Toast.makeText(this, "Errore esecuzione FFmpeg", Toast.LENGTH_LONG).show()
            switchProcess.isChecked = false
        }
    }
}
