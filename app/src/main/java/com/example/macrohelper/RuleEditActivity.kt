package com.example.macrohelper

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RuleEditActivity : AppCompatActivity() {

    private lateinit var repository: RuleRepository
    private lateinit var rule: Rule

    private lateinit var nameField: EditText
    private lateinit var packageField: EditText
    private lateinit var triggerSpinner: Spinner
    private lateinit var triggerValueField: EditText
    private lateinit var actionsRecycler: RecyclerView
    private lateinit var actionAdapter: ActionAdapter

    private val triggerLabels = listOf(
        "Manual (botón en la burbuja)",
        "Al abrir la app",
        "Cuando aparece un texto en pantalla"
    )
    private val triggerValues = TriggerType.values()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_edit)

        repository = RuleRepository(this)
        val ruleId = intent.getStringExtra("rule_id")
        rule = if (ruleId != null) {
            repository.getRules().firstOrNull { it.id == ruleId } ?: Rule()
        } else {
            Rule()
        }

        val recordedJson = intent.getStringExtra("recorded_actions_json")
        if (ruleId == null && recordedJson != null) {
            val type = object : TypeToken<MutableList<RuleAction>>() {}.type
            val recorded: MutableList<RuleAction>? = try {
                Gson().fromJson(recordedJson, type)
            } catch (e: Exception) {
                null
            }
            if (recorded != null) {
                rule.actions = recorded
                rule.name = "Macro grabada"
            }
        }

        nameField = findViewById(R.id.fieldRuleName)
        packageField = findViewById(R.id.fieldRulePackage)
        triggerSpinner = findViewById(R.id.fieldTriggerType)
        triggerValueField = findViewById(R.id.fieldTriggerValue)
        actionsRecycler = findViewById(R.id.actionsRecycler)

        nameField.setText(rule.name)
        packageField.setText(rule.targetPackage)
        triggerValueField.setText(rule.triggerValue)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, triggerLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        triggerSpinner.adapter = spinnerAdapter
        triggerSpinner.setSelection(triggerValues.indexOf(rule.triggerType))
        updateTriggerValueVisibility(rule.triggerType)

        triggerSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                updateTriggerValueVisibility(triggerValues[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        actionsRecycler.layoutManager = LinearLayoutManager(this)
        actionAdapter = ActionAdapter(rule.actions) { position ->
            rule.actions.removeAt(position)
            actionAdapter.notifyDataSetChanged()
        }
        actionsRecycler.adapter = actionAdapter

        findViewById<Button>(R.id.btnAddStep).setOnClickListener {
            rule.actions.add(RuleAction())
            actionAdapter.notifyItemInserted(rule.actions.size - 1)
        }

        findViewById<Button>(R.id.btnSaveRule).setOnClickListener {
            saveAndFinish()
        }

        findViewById<Button>(R.id.btnDeleteRule).setOnClickListener {
            repository.deleteRule(rule.id)
            finish()
        }
    }

    private fun updateTriggerValueVisibility(type: TriggerType) {
        triggerValueField.visibility = if (type == TriggerType.TEXT_APPEARS) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        triggerValueField.hint = "Texto que debe aparecer en pantalla"
    }

    private fun saveAndFinish() {
        rule.name = nameField.text.toString()
        rule.targetPackage = packageField.text.toString().trim()
        rule.triggerType = triggerValues[triggerSpinner.selectedItemPosition]
        rule.triggerValue = triggerValueField.text.toString()
        repository.upsertRule(rule)
        finish()
    }
}
