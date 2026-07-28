package com.example.macrohelper

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView

class ActionAdapter(
    private val actions: MutableList<RuleAction>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<ActionAdapter.ActionViewHolder>() {

    private val typeLabels = listOf(
        "Tocar texto",
        "Tocar por ID",
        "Escribir texto",
        "Esperar (ms)",
        "Botón Atrás",
        "Botón Inicio",
        "Abrir app (paquete)"
    )
    private val typeValues = ActionType.values()

    class ActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val spinner: Spinner = view.findViewById(R.id.actionTypeSpinner)
        val selector: EditText = view.findViewById(R.id.actionSelector)
        val value: EditText = view.findViewById(R.id.actionValue)
        val remove: ImageButton = view.findViewById(R.id.actionRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action, parent, false)
        return ActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActionViewHolder, position: Int) {
        val action = actions[position]
        val context = holder.itemView.context

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, typeLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.spinner.adapter = spinnerAdapter
        holder.spinner.setSelection(typeValues.indexOf(action.type))

        // Evitar listeners duplicados al reciclar la vista
        holder.selector.tag?.let { (it as? TextWatcher)?.let { tw -> holder.selector.removeTextChangedListener(tw) } }
        holder.value.tag?.let { (it as? TextWatcher)?.let { tw -> holder.value.removeTextChangedListener(tw) } }

        holder.selector.setText(action.selector)
        holder.value.setText(action.value)

        updateFieldVisibility(holder, action.type)

        holder.spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                action.type = typeValues[pos]
                updateFieldVisibility(holder, action.type)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val selectorWatcher = simpleWatcher { action.selector = it }
        holder.selector.addTextChangedListener(selectorWatcher)
        holder.selector.tag = selectorWatcher

        val valueWatcher = simpleWatcher { action.value = it }
        holder.value.addTextChangedListener(valueWatcher)
        holder.value.tag = valueWatcher

        holder.remove.setOnClickListener { onRemove(holder.adapterPosition) }
    }

    private fun updateFieldVisibility(holder: ActionViewHolder, type: ActionType) {
        when (type) {
            ActionType.PRESS_BACK, ActionType.PRESS_HOME -> {
                holder.selector.visibility = View.GONE
                holder.value.visibility = View.GONE
            }
            ActionType.WAIT -> {
                holder.selector.visibility = View.GONE
                holder.value.visibility = View.VISIBLE
                holder.value.hint = "Milisegundos, ej: 1000"
            }
            ActionType.OPEN_APP -> {
                holder.selector.visibility = View.VISIBLE
                holder.selector.hint = "Paquete, ej: com.whatsapp"
                holder.value.visibility = View.GONE
            }
            ActionType.CLICK_TEXT -> {
                holder.selector.visibility = View.VISIBLE
                holder.selector.hint = "Texto visible a tocar"
                holder.value.visibility = View.GONE
            }
            ActionType.CLICK_ID -> {
                holder.selector.visibility = View.VISIBLE
                holder.selector.hint = "resource-id, ej: com.whatsapp:id/send"
                holder.value.visibility = View.GONE
            }
            ActionType.INPUT_TEXT -> {
                holder.selector.visibility = View.VISIBLE
                holder.selector.hint = "Texto/ID del campo"
                holder.value.visibility = View.VISIBLE
                holder.value.hint = "Texto a escribir"
            }
        }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { onChanged(s?.toString() ?: "") }
        }
    }

    override fun getItemCount(): Int = actions.size
}
