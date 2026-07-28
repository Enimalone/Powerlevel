package com.example.macrohelper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RuleAdapter(
    private var rules: MutableList<Rule>,
    private val onClick: (Rule) -> Unit,
    private val onLongClick: (Rule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    class RuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemRuleTitle)
        val subtitle: TextView = view.findViewById(R.id.itemRuleSubtitle)
    }

    fun updateData(newRules: List<Rule>) {
        rules = newRules.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.title.text = rule.name.ifBlank { "(sin nombre)" }
        val triggerLabel = when (rule.triggerType) {
            TriggerType.MANUAL -> "Manual (desde la burbuja flotante)"
            TriggerType.APP_OPEN -> "Al abrir ${rule.targetPackage}"
            TriggerType.TEXT_APPEARS -> "Cuando aparece \"${rule.triggerValue}\" en ${rule.targetPackage}"
        }
        holder.subtitle.text = "$triggerLabel · ${rule.actions.size} paso(s)"
        holder.itemView.setOnClickListener { onClick(rule) }
        holder.itemView.setOnLongClickListener { onLongClick(rule); true }
    }

    override fun getItemCount(): Int = rules.size
}
