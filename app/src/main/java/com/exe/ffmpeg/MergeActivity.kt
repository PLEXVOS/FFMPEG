package com.exe.ffmpeg

import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

class MergeActivity : BaseActivity() {

    companion object {
        private const val REQUEST_MULTI_FILE = 201
        private const val REQUEST_FOLDER = 202
    }

    private lateinit var tvFile1: TextView
    private lateinit var tvFile2: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etRename: EditText
    private lateinit var switchProcess: Switch

    // File già copiati in cache, ordinati per nome
    private val selectedFiles = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merge)

        tvFile1 = findViewById(R.id.tvFile1)
        tvFile2 = findViewById(R.id.tvFile2)
        tvProgress = findViewById(R.id.tvProgress)
        tvStatus = findViewById(R.id.tvStatus)
        etRename = findViewById(R.id.etRename)
        switchProcess = findViewById(R.id.switchProcess)

        findViewById<Button>(R.id.btnFile1).apply {
            text = "Seleziona File"
            setOnClickListener { pickMultipleFiles() }
        }

        findViewById<Button>(R.id.btnFile2).apply {
            text = "Seleziona Cartella"
            setOnClickListener { pickFolder() }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        setupSwitch(switchProcess) { startMerge() }
        aggiornaConto()
    }

    // ── Selezione file multipli ────────────────────────────────────────────────
    private fun pickMultipleFiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Seleziona File"), REQUEST_MULTI_FILE)
    }

    // ── Selezione cartella intera ──────────────────────────────────────────────
    private fun pickFolder() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_FOLDER)
    }

    // ── Gestione risultati ────────────────────────────────────────────────────
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK || data == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        when (requestCode) {
            REQUEST_MULTI_FILE -> gestisciFileMultipli(data)
            REQUEST_FOLDER -> gestisciCartella(data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun gestisciFileMultipli(data: Intent) {
        tvStatus.text = "Copia file in corso..."
        selectedFiles.clear()

        Thread {
            val uris = mutableListOf<Uri>()
            if (data.clipData != null) {
                val clip = data.clipData!!
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } else if (data.data != null) {
                uris.add(data.data!!)
            }

            for ((i, uri) in uris.withIndex()) {
                val nome = Utils.getFileNameFromUri(this, uri)
                val dest = File(cacheDir, "mg_${i}_$nome")
                if (copiaUri(uri, dest)) selectedFiles.add(dest.absolutePath)
                runOnUiThread { tvStatus.text = "Copiato ${i + 1}/${uris.size}..." }
            }

            // Ordine naturale per nome (segmenti sequenziali)
            selectedFiles.sortWith { a, b ->
                File(a).name.compareTo(File(b).name, ignoreCase = true)
            }

            runOnUiThread {
                aggiornaConto()
                tvStatus.text = "${selectedFiles.size} file pronti"
            }
        }.start()
    }

    private fun gestisciCartella(data: Intent) {
        val treeUri = data.data ?: return
        tvStatus.text = "Lettura cartella..."
        selectedFiles.clear()

        Thread {
            val docFolder = DocumentFile.fromTreeUri(this, treeUri)
            val files = docFolder?.listFiles()
                ?.filter { it.isFile && it.canRead() }
                ?.sortedBy { it.name ?: "" }
                ?: emptyList()

            for ((i, docFile) in files.withIndex()) {
                val nome = docFile.name ?: "file_$i"
                val dest = File(cacheDir, "mgf_${i}_$nome")
                if (copiaUri(docFile.uri, dest)) selectedFiles.add(dest.absolutePath)
                runOnUiThread { tvStatus.text = "Copia ${i + 1}/${files.size}..." }
            }

            runOnUiThread {
                aggiornaConto()
                tvStatus.text = "${selectedFiles.size} file pronti da cartella"
            }
        }.start()
    }

    private fun copiaUri(uri: Uri, dest: File): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            Utils.appendLog(this, "Errore copia: ${e.message}")
            false
        }
    }

    private fun aggiornaConto() {
        if (selectedFiles.isEmpty()) {
            tvFile1.text = "Nessun file selezionato"
            tvFile2.text = ""
        } else {
            tvFile1.text = "${selectedFiles.size} file selezionati"
            tvFile2.text = selectedFiles.take(4).joinToString("\n") { File(it).name } +
                if (selectedFiles.size > 4) "\n...e altri ${selectedFiles.size - 4}" else ""
        }
    }

    // ── Avvio merge ───────────────────────────────────────────────────────────
    private fun startMerge() {
        if (selectedFiles.size < 2) {
            Toast.makeText(this, "Seleziona almeno 2 file", Toast.LENGTH_SHORT).show()
            switchProcess.isChecked = false
            return
        }

        try {
            val outputDir = Utils.getOutputDir(this, "Uniti")
            val outName = Utils.nameWithExt(
                etRename.text.toString(), ".mp4",
                "merged_${System.currentTimeMillis()}"
            )
            val outFile = File(outputDir, outName)

            val listFile = File(cacheDir, "list_${System.currentTimeMillis()}.txt")
            listFile.writeText(selectedFiles.joinToString("\n") { "file '$it'" })

            Utils.appendLog(this, "Merge ${selectedFiles.size} file → ${outFile.name}")

            val cmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" -c copy \"${outFile.absolutePath}\""

            runFFmpeg(cmd, tvProgress, tvStatus, switchProcess) { success ->
                listFile.delete()
                if (success) {
                    MediaScannerConnection.scanFile(this, arrayOf(outFile.absolutePath), null, null)
                    runOnUiThread {
                        Toast.makeText(this, "Fatto! ${outFile.name}", Toast.LENGTH_LONG).show()
                    }
                }
            }

        } catch (t: Throwable) {
            Utils.appendLog(this, "CRASH merge: ${t.message}")
            switchProcess.isChecked = false
            Toast.makeText(this, "Errore: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }
}
