package com.example.macrohelper

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson

class MacroAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var rules: MutableList<Rule> = mutableListOf()

    private var isRecording = false
    private var recordedActions: MutableList<RuleAction> = mutableListOf()
    private var lastRecordedEventTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        rules = RuleRepository(this).getRules()
        showBubble()
    }

    private fun showBubble() {
        if (bubbleView != null) return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_button, null)
        bubbleView = view

        val bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        bubbleParams.gravity = Gravity.TOP or Gravity.START
        bubbleParams.x = 0
        bubbleParams.y = 300

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 15 || kotlin.math.abs(dy) > 15) moved = true
                    bubbleParams.x = initialX + dx
                    bubbleParams.y = initialY + dy
                    windowManager.updateViewLayout(view, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, bubbleParams)
    }

    private fun togglePanel() {
        if (panelView != null) {
            hidePanel()
            return
        }
        rules = RuleRepository(this).getRules()
        val manualRules = rules.filter { it.triggerType == TriggerType.MANUAL }

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_panel, null)
        val container = view.findViewById<LinearLayout>(R.id.panelRulesContainer)
        val closeBtn = view.findViewById<TextView>(R.id.panelClose)
        closeBtn.setOnClickListener { hidePanel() }

        val recordBtn = TextView(this)
        recordBtn.textSize = 16f
        recordBtn.setPadding(32, 28, 32, 28)
        recordBtn.setTextColor(if (isRecording) 0xFFD32F2F.toInt() else 0xFF3F51B5.toInt())
        recordBtn.text = if (isRecording) "Detener grabacion" else "Grabar nueva macro"
        recordBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
            hidePanel()
        }
        container.addView(recordBtn)

        if (manualRules.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No hay reglas manuales. Crea una en la app."
            empty.setPadding(24, 24, 24, 24)
            container.addView(empty)
        } else {
            manualRules.forEach { rule ->
                val item = TextView(this)
                item.text = rule.name
                item.textSize = 16f
                item.setPadding(32, 28, 32, 28)
                item.setOnClickListener {
                    Toast.makeText(this, "Ejecutando: " + rule.name, Toast.LENGTH_SHORT).show()
                    executeRule(rule, 0)
                    hidePanel()
                }
                container.addView(item)
            }
        }

        val panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        panelParams.gravity = Gravity.TOP or Gravity.START
        panelParams.x = 0
        panelParams.y = 400

        panelView = view
        windowManager.addView(view, panelParams)
    }

    private fun hidePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    private fun startRecording() {
        isRecording = true
        recordedActions = mutableListOf()
        lastRecordedEventTime = System.currentTimeMillis()
        updateBubbleAppearance()
        Toast.makeText(
            this,
            "Grabando. Toca y escribi normalmente en la otra app. Volve a abrir la burbuja para detener.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun stopRecording() {
        isRecording = false
        updateBubbleAppearance()

        if (recordedActions.isEmpty()) {
            Toast.makeText(this, "No se registro ninguna accion.", Toast.LENGTH_SHORT).show()
            return
        }

        val json = Gson().toJson(recordedActions)
        val intent = android.content.Intent(this, RuleEditActivity::class.java)
        intent.putExtra("recorded_actions_json", json)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Toast.makeText(this, "Grabacion lista: " + recordedActions.size + " paso(s). Pone un nombre y guarda.", Toast.LENGTH_LONG).show()
    }

    private fun updateBubbleAppearance() {
        val bubble = bubbleView as? TextView ?: return
        if (isRecording) {
            bubble.text = "REC"
            bubble.setBackgroundResource(R.drawable.bubble_background_recording)
        } else {
            bubble.text = "M"
            bubble.setBackgroundResource(R.drawable.bubble_background)
        }
    }

    private fun recordClick(node: AccessibilityNodeInfo?, eventText: String?) {
        val text = node?.text?.toString()?.takeIf { it.isNotBlank() } ?: eventText?.takeIf { it.isNotBlank() }
        val id = node?.viewIdResourceName

        maybeRecordWait()

        val action = when {
            !text.isNullOrBlank() -> RuleAction(ActionType.CLICK_TEXT, selector = text)
            !id.isNullOrBlank() -> RuleAction(ActionType.CLICK_ID, selector = id)
            else -> return
        }
        recordedActions.add(action)
        lastRecordedEventTime = System.currentTimeMillis()
    }

    private fun recordTextChanged(node: AccessibilityNodeInfo?, newText: String) {
        val id = node?.viewIdResourceName
        val selector = id ?: node?.className?.toString() ?: "campo_de_texto"

        maybeRecordWait()

        val last = recordedActions.lastOrNull()
        if (last != null && last.type == ActionType.INPUT_TEXT && last.selector == selector) {
            last.value = newText
        } else {
            recordedActions.add(RuleAction(ActionType.INPUT_TEXT, selector = selector, value = newText))
        }
        lastRecordedEventTime = System.currentTimeMillis()
    }

    private fun maybeRecordWait() {
        val now = System.currentTimeMillis()
        val gap = now - lastRecordedEventTime
        if (gap > 600 && recordedActions.isNotEmpty()) {
            recordedActions.add(RuleAction(ActionType.WAIT, value = gap.coerceAtMost(5000).toString()))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        if (isRecording) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    recordClick(event.source, event.text?.joinToString(" "))
                }
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    val newText = event.text?.joinToString(" ") ?: ""
                    if (newText.isNotBlank()) recordTextChanged(event.source, newText)
                }
            }
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            rules.filter { it.triggerType == TriggerType.APP_OPEN && it.targetPackage == pkg }
                .forEach { executeRule(it, 0) }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            rules.filter { it.triggerType == TriggerType.TEXT_APPEARS && it.targetPackage == pkg }
                .forEach { rule ->
                    if (screenContainsText(rule.triggerValue)) {
                        executeRule(rule, 0)
                    }
                }
        }
    }

    override fun onInterrupt() {}

    private fun executeRule(rule: Rule, stepIndex: Int) {
        if (stepIndex >= rule.actions.size) return
        val action = rule.actions[stepIndex]

        when (action.type) {
            ActionType.WAIT -> {
                val ms = action.value.toLongOrNull() ?: 500L
                handler.postDelayed({ executeRule(rule, stepIndex + 1) }, ms)
                return
            }
            ActionType.CLICK_TEXT -> {
                findNodeByText(action.selector)?.let { clickNode(it) }
            }
            ActionType.CLICK_ID -> {
                findNodeById(action.selector)?.let { clickNode(it) }
            }
            ActionType.INPUT_TEXT -> {
                val node = findNodeByText(action.selector) ?: findNodeById(action.selector)
                node?.let { setTextOnNode(it, action.value) }
            }
            ActionType.PRESS_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            ActionType.PRESS_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            ActionType.OPEN_APP -> {
                packageManager.getLaunchIntentForPackage(action.selector)?.let {
                    it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
            }
        }

        handler.postDelayed({ executeRule(rule, stepIndex + 1) }, 350)
    }

    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        if (text.isBlank()) return null
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    private fun findNodeById(id: String): AccessibilityNodeInfo? {
        if (id.isBlank()) return null
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(id)
        return nodes?.firstOrNull()
    }

    private fun screenContainsText(text: String): Boolean {
        if (text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return !nodes.isNullOrEmpty()
    }

    private fun clickNode(node: AccessibilityNodeInfo) {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            current = current.parent
        }
    }

    private fun setTextOnNode(node: AccessibilityNodeInfo, text: String) {
        val args = android.os.Bundle()
        args.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
