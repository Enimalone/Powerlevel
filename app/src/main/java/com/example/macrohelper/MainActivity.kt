package com.example.macrohelper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var repository: RuleRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RuleAdapter
    private lateinit var statusOverlay: TextView
    private lateinit var statusAccessibility: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = RuleRepository(this)

        statusOverlay = findViewById(R.id.statusOverlay)
        statusAccessibility = findViewById(R.id.statusAccessibility)

        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAccessibilityPermission).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        recyclerView = findViewById(R.id.recyclerRules)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RuleAdapter(
            mutableListOf(),
            onClick = { rule ->
                val intent = Intent(this, RuleEditActivity::class.java)
                intent.putExtra("rule_id", rule.id)
                startActivity(intent)
            },
            onLongClick = { rule -> confirmDelete(rule) }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddRule).setOnClickListener {
            startActivity(Intent(this, RuleEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        adapter.updateData(repository.getRules())
    }

    private fun refreshStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        statusOverlay.text = if (overlayOk) "Permiso de superposición: activo"
        else "Permiso de superposición: falta activarlo"

        statusAccessibility.text = if (isAccessibilityServiceEnabled()) "Servicio de accesibilidad: activo"
        else "Servicio de accesibilidad: falta activarlo"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "$packageName/${MacroAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
        }
        return false
    }

    private fun confirmDelete(rule: Rule) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar regla")
            .setMessage("¿Eliminar \"${rule.name}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                repository.deleteRule(rule.id)
                adapter.updateData(repository.getRules())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
