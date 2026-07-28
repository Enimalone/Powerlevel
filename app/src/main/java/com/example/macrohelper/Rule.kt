package com.example.macrohelper

import java.util.UUID

enum class TriggerType {
    MANUAL,        // Se ejecuta al tocarla en el panel flotante
    APP_OPEN,      // Se ejecuta cuando se abre targetPackage
    TEXT_APPEARS   // Se ejecuta cuando aparece triggerValue en pantalla dentro de targetPackage
}

enum class ActionType {
    CLICK_TEXT,   // selector = texto visible a tocar
    CLICK_ID,     // selector = resource-id completo, ej: com.whatsapp:id/send
    INPUT_TEXT,   // selector = texto o id del campo, value = texto a escribir
    WAIT,         // value = milisegundos
    PRESS_BACK,
    PRESS_HOME,
    OPEN_APP      // selector = paquete a abrir
}

data class RuleAction(
    var type: ActionType = ActionType.CLICK_TEXT,
    var selector: String = "",
    var value: String = ""
)

data class Rule(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var targetPackage: String = "",
    var triggerType: TriggerType = TriggerType.MANUAL,
    var triggerValue: String = "",
    var actions: MutableList<RuleAction> = mutableListOf()
)
