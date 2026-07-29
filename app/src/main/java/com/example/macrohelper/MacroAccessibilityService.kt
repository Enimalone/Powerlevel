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

    // ---------- Overlay: burbuja flotante ----------

    private fun showBubble() {
        if (bubbleView != null) return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_button, null)
        bubbleView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 15 || kotlin.math.abs(dy) > 15) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
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
        recordBtn.text = if (isRecording) "■ Detener grabación" else "● Grabar nueva macro"
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
            empty.text = "No hay reglas manuales. Creá una en la app."
            empty.setPadding(24, 24, 24, 24)
            container.addView(empty)
        } else {
            manualRules.forEach { rule ->
                val item = TextView(this)
                item.text = rule.name
                item.textSize = 16f
                item.setPadding(32, 28, 32, 28)
                item.setOnClickListener {
                    Toast.makeText(this, "Ejecutando: ${rule.name}", Toast.LENGTH_SHORT).show()
                    executeRule(rule, 0)
                    hidePanel()
                }
                container.addView(item)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABL
