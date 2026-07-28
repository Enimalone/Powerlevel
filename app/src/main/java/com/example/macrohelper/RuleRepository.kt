package com.example.macrohelper

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RuleRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("macro_rules_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "rules_json"

    fun getRules(): MutableList<Rule> {
        val json = prefs.getString(key, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Rule>>() {}.type
        return try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveRules(rules: List<Rule>) {
        prefs.edit().putString(key, gson.toJson(rules)).apply()
    }

    fun upsertRule(rule: Rule) {
        val rules = getRules()
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) rules[index] = rule else rules.add(rule)
        saveRules(rules)
    }

    fun deleteRule(ruleId: String) {
        val rules = getRules()
        rules.removeAll { it.id == ruleId }
        saveRules(rules)
    }
}
