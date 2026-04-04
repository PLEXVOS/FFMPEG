protected fun runFFmpeg(
    cmd: String,
    tvProgress: TextView,
    tvStatus: TextView,
    switch: Switch,
    onDone: (Boolean) -> Unit
) {
    updateProgress(tvProgress, tvStatus, 0, "Elaborazione in corso...")

    Utils.appendLog(this, "CMD: $cmd")

    FFmpegKit.executeAsync(cmd, { session ->
        val success = ReturnCode.isSuccess(session.returnCode)
        val logs = session.allLogsAsString.take(500)

        Utils.appendLog(this, if (success) "SUCCESS" else "FFmpeg ERROR:\n$logs")

        runOnUiThread {
            tvProgress.text = if (success) "100%" else "ERRORE"
            tvStatus.text = if (success) "Completato" else "Errore FFmpeg"
            switch.isChecked = false
            Toast.makeText(this, 
                if (success) "Merge completato!" else "Merge fallito - vedi log", 
                Toast.LENGTH_LONG).show()
        }
        onDone(success)
    }, { log ->
        val line = log.message ?: ""
        if (line.contains("time=")) {
            runOnUiThread { tvStatus.text = line.take(100) }
        }
    }, null)
}
