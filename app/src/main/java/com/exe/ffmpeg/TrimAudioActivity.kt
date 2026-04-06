package com.exe.ffmpeg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.util.Locale

class TrimAudioActivity : BaseActivity() {

    private lateinit var tvFile1: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvFormat: TextView
    private lateinit var etRename: EditText
    private lateinit var etStart: EditText
    private lateinit var etEnd: EditText
    private lateinit var switchProcess: Switch

    private var originalExtension: String = ".mp3"
    private var originalName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim_audio)

        tvFile1 = findViewById(R.id.tvFile1)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        tvFormat = findViewById(R.id.tvFormat)
        etRename = findViewById(R.id.etRename)
        etStart = findViewById(R.id.etStart)
        etEnd = findViewById(R.id.etEnd)
        switchProcess = findViewById(R.id.switchProcess)

        selectedFormat = ".mp3"

        etStart.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val txt = s?.toString()?.trim() ?: ""
                tvStatus.text = when {
                    txt.matches(Regex("\\d+")) -> "Modalità: DIVIDI IN $txt PARTI UGUALI"
                    txt.isNotBlank() -> "Modalità: TAGLIA (inizio=$txt)"
                    else -> "Inserisci orario (es. 00:01:00) o numero parti (es. 4)"
                }
            }
        })

        findViewById<Button>(R.id.btnFile1).setOnClickListener { pickFile(REQUEST_FILE1) }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        setupFormatDial(
            findViewById(R.id.dialFormat), tvFormat,
            resources.getStringArray(R.array.audio_formats)
        )
        setupSwitch(switchProcess) { startTrim() }
    }

    override fun onFile1Selected(name: String, path: String?) {
        tvFile1.text = name
        originalName = File(name).nameWithoutExtension
        val ext = File(name).extension
        originalExtension = if (ext.isNotBlank()) ".$ext" else ".mp3"
        if (etRename.text.isBlank()) {
            etRename.setText(originalName)
        }
    }

    private fun startTrim() {
        val f1 = selectedFile1
        if (f1 == null) {
            Toast.makeText(this, "Seleziona un file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        val startText = etStart.text.toString().trim()

        if (startText.matches(Regex("\\d+")) && startText.toIntOrNull() != null) {
            val n = startText.toInt()
            if (n < 2) {
                Toast.makeText(this, "Numero parti minimo: 2", Toast.LENGTH_SHORT).show()
                switchProcess.isChecked = false
                return
            }
            avviaModalitaDivisione(f1, n)
            return
        }

        val start = if (startText.isBlank()) "00:00:00" else startText
        val end = etEnd.text.toString().ifBlank { "00:01:00" }
        val outName = Utils.nameWithExt(etRename.text.toString(), selectedFormat, "trimmed_audio")
        val outFile = File(Utils.getOutputDir(this, "Audio"), outName)
        val cmd = "-ss $start -to $end -i \"$f1\" -c copy \"${outFile.absolutePath}\""
        Utils.appendLog(this, "Taglia audio: $start -> $end")
        runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) {}
    }

    private fun avviaModalitaDivisione(f1: String, n: Int) {
        val prefisso = etRename.text.toString().ifBlank { originalName.ifBlank { "audio" } }
        tvStatus.text = "Lettura durata audio..."

        Thread {
            try {
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
                val outDir = Utils.getOutputDir(this, "Audio")
                val minPerParte = (segDuration / 60).toInt()
                val secPerParte = (segDuration % 60).toInt()

                runOnUiThread {
                    tvStatus.text = "Ogni parte: ${minPerParte}m ${secPerParte}s"
                }

                var completati = 0

                for (i in 0 until n) {
                    val start = String.format(Locale.US, "%.3f", i * segDuration)
                    val dur = String.format(Locale.US, "%.3f", segDuration)
                    // usa estensione originale, non quella del file cache
                    val nomeOutput = "${prefisso}_Parte_${i + 1}$originalExtension"
                    val output = File(outDir, nomeOutput).absolutePath
                    val cmd = "-ss $start -t $dur -i \"$f1\" -c copy \"$output\""

                    runOnUiThread {
                        tvProgress.text = "${(i * 100 / n)}%"
                        tvStatus.text = "Parte ${i + 1}/$n"
                    }

                    val session = FFmpegKit.execute(cmd)
                    if (ReturnCode.isSuccess(session.returnCode)) completati++
                    else Utils.appendLog(this, "Parte ${i+1} fallita: ${session.allLogsAsString?.take(100)}")
                }

                runOnUiThread {
                    tvProgress.text = "100%"
                    tvStatus.text = "Completato: $completati/$n parti"
                    switchProcess.isChecked = false
                    Utils.appendLog(this, "Split audio $n parti '$prefisso'")
                    Toast.makeText(
                        this,
                        "Fatto! $completati/$n parti in Movies/FFmpegOutput/Audio/",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (t: Throwable) {
                Utils.appendLog(this, "CRASH avviaModalitaDivisione: ${t::class.simpleName}: ${t.message}")
                runOnUiThread {
                    switchProcess.isChecked = false
                    Toast.makeText(this, "Errore: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
