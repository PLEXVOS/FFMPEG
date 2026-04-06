package com.exe.ffmpeg

import android.os.Bundle
import android.view.View
import android.widget.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.Locale

class SplitActivity : BaseActivity() {

    private var tvFile1: TextView? = null
    private var tvProgress: TextView? = null
    private var tvStatus: TextView? = null
    private var etRename: EditText? = null
    private var etCustomParts: EditText? = null
    private var spinnerParts: Spinner? = null
    private var switchProcess: Switch? = null

    // Salva l'estensione originale quando il file viene selezionato
    private var originalExtension: String = ".mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_split)

            tvFile1 = findViewById(R.id.tvFile1)
            tvProgress = findViewById(R.id.tvProgress)
            tvStatus = findViewById(R.id.tvStatus)
            etRename = findViewById(R.id.etRename)
            etCustomParts = findViewById(R.id.etCustomParts)
            spinnerParts = findViewById(R.id.spinnerParts)
            switchProcess = findViewById(R.id.switchProcess)

            Utils.appendLog(this, "SplitActivity: onCreate OK")

            val parts = resources.getStringArray(R.array.split_parts)
            spinnerParts?.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, parts
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            spinnerParts?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    etCustomParts?.visibility =
                        if (parts[pos] == "Personalizzato") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }

            findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
            findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

            val sw = switchProcess
            if (sw != null) setupSwitch(sw) { startSplit() }

        } catch (t: Throwable) {
            Utils.appendLog(this, "CRASH SplitActivity onCreate: ${t::class.simpleName}: ${t.message}")
            Toast.makeText(this, "Errore: ${t.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onFile1Selected(name: String, path: String?) {
        tvFile1?.text = name
        // Salva l'estensione dal nome originale (es. "video.mp4" → ".mp4")
        val ext = File(name).extension
        originalExtension = if (ext.isNotBlank()) ".$ext" else ".mp4"
        if (etRename?.text.isNullOrBlank()) {
            etRename?.setText(File(name).nameWithoutExtension)
        }
    }

    private fun startSplit() {
        val f1 = selectedFile1
        if (f1 == null) {
            Toast.makeText(this, "Seleziona un file", Toast.LENGTH_SHORT).show()
            switchProcess?.isChecked = false
            return
        }

        val selectedItem = spinnerParts?.selectedItem?.toString() ?: ""
        val partsStr = if (selectedItem == "Personalizzato")
            etCustomParts?.text?.toString() ?: ""
        else
            selectedItem

        val n = partsStr.toIntOrNull()
        if (n == null || n < 2) {
            Toast.makeText(this, "Numero parti non valido (minimo 2)", Toast.LENGTH_SHORT).show()
            switchProcess?.isChecked = false
            return
        }

        val prefisso = etRename?.text?.toString()?.ifBlank {
            File(f1).nameWithoutExtension
        } ?: File(f1).nameWithoutExtension

        tvStatus?.text = "Lettura durata..."

        Thread {
            try {
                val probe = FFprobeKit.getMediaInformation(f1)
                val duration = probe.mediaInformation?.duration?.toDoubleOrNull() ?: 0.0

                if (duration <= 0.0) {
                    runOnUiThread {
                        tvStatus?.text = "Impossibile leggere durata"
                        switchProcess?.isChecked = false
                    }
                    return@Thread
                }

                val segDuration = duration / n
                val outDir = Utils.getOutputDir(this, "Divisi")
                var completati = 0

                for (i in 0 until n) {
                    // FIX 1: Locale.US forza il punto decimale
                    val start = String.format(Locale.US, "%.3f", i * segDuration)
                    val dur = String.format(Locale.US, "%.3f", segDuration)
                    // FIX 2: usa estensione originale, non quella del file cache
                    val nomeOutput = "${prefisso}_Parte_${i + 1}$originalExtension"
                    val output = File(outDir, nomeOutput).absolutePath
                    val cmd = "-ss $start -t $dur -i \"$f1\" -c copy \"$output\""

                    runOnUiThread {
                        tvProgress?.text = "${(i * 100 / n)}%"
                        tvStatus?.text = "Parte ${i + 1}/$n"
                    }

                    Utils.appendLog(this, "Split cmd: $cmd")
                    val session = FFmpegKit.execute(cmd)
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        completati++
                    } else {
                        Utils.appendLog(
                            this,
                            "Split parte ${i + 1} fallita: ${session.allLogsAsString?.take(200)}"
                        )
                    }
                }

                runOnUiThread {
                    tvProgress?.text = "100%"
                    tvStatus?.text = "Completato: $completati/$n parti"
                    switchProcess?.isChecked = false
                    Utils.appendLog(this, "Split OK: $completati/$n parti di '$prefisso'")
                    Toast.makeText(
                        this,
                        "Fatto! $completati/$n parti in Movies/FFmpegOutput/Divisi/",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (t: Throwable) {
                Utils.appendLog(this, "CRASH startSplit: ${t::class.simpleName}: ${t.message}")
                runOnUiThread {
                    switchProcess?.isChecked = false
                    Toast.makeText(this, "Errore: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
