package com.exe.ffmpeg

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class FunctionItem(val icon: String, val name: String, val desc: String, val activityClass: Class<*>)

class MainActivity : AppCompatActivity() {

    private val functions = listOf(
        FunctionItem("⧉", "UNISCI VIDEO", "Combina piu' video in uno", MergeActivity::class.java),
        FunctionItem("⊢", "DIVIDI VIDEO", "Dividi in N parti uguali", SplitActivity::class.java),
        FunctionItem("✂", "RITAGLIA VIDEO", "Taglia un segmento temporale", TrimVideoActivity::class.java),
        FunctionItem("♪", "ESTRAI AUDIO", "Estrai la traccia audio", ExtractAudioActivity::class.java),
        FunctionItem("✂", "RITAGLIA AUDIO", "Taglia segmento audio", TrimAudioActivity::class.java),
        FunctionItem("⇄", "CONVERTI", "Converti in qualsiasi formato", ConvertActivity::class.java),
        FunctionItem("≡", "LOG", "Cronologia operazioni", LogActivity::class.java)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = FuncAdapter(functions) { item ->
            startActivity(Intent(this, item.activityClass))
        }

        findViewById<TextView>(R.id.tvLog).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}

class FuncAdapter(
    private val items: List<FunctionItem>,
    private val onClick: (FunctionItem) -> Unit
) : RecyclerView.Adapter<FuncAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.tvIcon)
        val name: TextView = v.findViewById(R.id.tvFuncName)
        val desc: TextView = v.findViewById(R.id.tvFuncDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_function_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        h.icon.text = item.icon
        h.name.text = item.name
        h.desc.text = item.desc
        h.itemView.setOnClickListener { onClick(item) }
    }
}
