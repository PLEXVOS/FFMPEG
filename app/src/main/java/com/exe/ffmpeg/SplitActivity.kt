package com.exe.ffmpeg

import android.os.Bundle
import android.view.View
import android.widget.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class SplitActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etRename: EditText
    private lateinit var etCustomParts: EditText
    private lateinit var spinnerParts: Spinner
    private lateinit var switchProcess: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split)

        tvFile1 = findViewById(R.id.tvFile1)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        etRename = findViewById(R.id.etRename)
        etCustomParts = findViewById(R.id.etCustomParts)
        spinnerParts = findViewById(R.id.spinnerParts)
        switchProcess = findViewById(R.id.switchProcess)

        val parts = resources.getStringArray(R.array.split_parts)
        spinnerParts.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, parts
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerParts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etCustomParts.visibility =
                    if (parts[pos] == "Personalizzato") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        setupSwitch(switchProcess) { startSplit() }
    }

    override fun onFile1Selected(name: String, path: String?) {
        tvFile1.text = name
        // Pre-compila il prefisso con il nome del file senza estensione
        if (etRename.text.isBlank() && path != null) {
            etRename.setText(File(path).nameWithoutExtension)
        }
    }

    private fun startSplit() {
        val f1 = selectedFile1
        if (f1 == null) {
            Toast.makeText(this, "Seleziona un file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        val partsStr = if (spinnerParts.selectedItem.toString() == "Personalizzato")
            etCustomParts.text.toString()
        else
            spinnerParts.selectedItem.toString()

        val n = partsStr.toIntOrNull()
        if (n == null || n < 2) {
            Toast.makeText(this, "Numero parti non valido (minimo 2)", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        // Prefisso: usa etRename (giÃ  pre-compilato con nome file), o fallback
        val prefisso = etRename.text.toString().ifBlank {
            File(f1).nameWithoutExtension
        }

        tvStatus.text = "Lettura durata..."

        Thread {
            val probe = FFprobeKit.getMediaInformation(f1)
            val duration = probe.mediaInformation?.duration?.toDoubleOrNull() ?: 0.0

            if (duration <= 0.0) {
                runOnUiThread {
                    tvStatus.text = "Impossibile leggere durata"
                    switchProcess.isChecked = false
                }
                return@Thread
            }

            val segDuration = duration / n
            val outDir = Utils.getOutputDir(this, "Divisi")
            val ext = File(f1).extension.let { if (it.isNotBlank()) ".$it" else ".mp4" }

            var completati = 0

            for (i in 0 until n) {
                val start = i * segDuration
                // Es: "Hulk_Parte_1.mp4", "Hulk_Parte_2.mp4"
                val nomeOutput = "${prefisso}_Parte_${i + 1}$ext"
                val output = File(outDir, nomeOutput).absolutePath
                val cmd = "-ss $start -t $segDuration -i \"$f1\" -c copy \"$output\""

                runOnUiThread {
                    tvProgress.text = "${(i * 100 / n)}%"
                    tvStatus.text = "Parte ${i + 1}/$n: $nomeOutput"
                }

                val session = FFmpegKit.execute(cmd)
                if (ReturnCode.isSuccess(session.returnCode)) completati++
            }

            runOnUiThread {
                tvProgress.text = "100%"
                tvStatus.text = "Completato: $completati/$n parti"
                switchProcess.isChecked = false
                Utils.appendLog(
                    this,
                    "Split $n parti '$prefisso' â†’ ${outDir.absolutePath}"
                )
                Toast.makeText(
                    this,
                    "Fatto! $completati parti in Movies/FFmpegOutput/Divisi/",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }
}
