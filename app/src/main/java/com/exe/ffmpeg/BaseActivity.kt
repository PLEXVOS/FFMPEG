// In BaseActivity.kt — sostituisci la funzione runFFmpegWithArgs con questa:

protected fun runFFmpegWithArgs(
    args: Array<String>,
    tvProgress: TextView,
    tvStatus: TextView,
    switch: Switch,
    onDone: (Boolean) -> Unit
) {
    val cmd = args.joinToString(" ")   // converte array in stringa
    updateProgress(tvProgress, tvStatus, 0, "Elaborazione in corso...")

    Utils.appendLog(this, "CMD: $cmd")

    FFmpegKit.executeAsync(cmd, { session ->
        val success = ReturnCode.isSuccess(session.returnCode)
        val errorMsg = session.allLogsAsString.take(400)

        Utils.appendLog(this, if (success) "SUCCESS" else "FFmpeg ERROR: $errorMsg")

        runOnUiThread {
            tvProgress.text = if (success) "100%" else "ERRORE"
            tvStatus.text = if (success) "Completato" else "Errore FFmpeg"
            switch.isChecked = false

            Toast.makeText(this,
                if (success) "Merge completato!" else "Errore durante il merge",
                Toast.LENGTH_LONG
            ).show()
        }
        onDone(success)
    }, { log ->
        val line = log.message ?: ""
        if (line.contains("time=")) {
            runOnUiThread { tvStatus.text = line.take(90) }
        }
    }, null)
}
