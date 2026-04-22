package com.winlator.cmod.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object XServerDrawerState {

    private val _isPaused                = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean>     = _isPaused

    private val _isRelativeMouseMovement = MutableStateFlow(false)
    val isRelativeMouseMovement: StateFlow<Boolean> = _isRelativeMouseMovement

    private val _isMouseDisabled         = MutableStateFlow(false)
    val isMouseDisabled: StateFlow<Boolean> = _isMouseDisabled

    private val _moveCursorToTouchpoint  = MutableStateFlow(false)
    val moveCursorToTouchpoint: StateFlow<Boolean> = _moveCursorToTouchpoint

    private val _showLogs                = MutableStateFlow(false)
    val showLogs: StateFlow<Boolean>     = _showLogs

    private val _showMagnifier           = MutableStateFlow(true)
    val showMagnifier: StateFlow<Boolean> = _showMagnifier

    private val _cursorExpanded          = MutableStateFlow(false)
    val cursorExpanded: StateFlow<Boolean> = _cursorExpanded

    // Callbacks wired by XServerDisplayActivity
    var onClose:                   (() -> Unit)? = null
    var onKeyboard:                (() -> Unit)? = null
    var onInputControls:           (() -> Unit)? = null
    var onScreenEffects:           (() -> Unit)? = null
    var onGraphicEngine:           (() -> Unit)? = null
    var onVibration:               (() -> Unit)? = null
    var onToggleFullscreen:        (() -> Unit)? = null
    var onPauseResume:             (() -> Unit)? = null
    var onPipMode:                 (() -> Unit)? = null
    var onActiveWindows:           (() -> Unit)? = null
    var onTaskManager:             (() -> Unit)? = null
    var onMagnifier:               (() -> Unit)? = null
    var onLogs:                    (() -> Unit)? = null
    var onExit:                    (() -> Unit)? = null
    var onMoveCursorToTouchpoint:  (() -> Unit)? = null
    var onRelativeMouseMovement:   (() -> Unit)? = null
    var onDisableMouse:            (() -> Unit)? = null
    var onCursorExpandedChanged:   ((Boolean) -> Unit)? = null

    // Setters called from Java (Activity keeps Java fields as source of truth,
    // updates state here so Compose re-renders correctly)
    fun setIsPaused(v: Boolean)                { _isPaused.value = v }
    fun setIsRelativeMouseMovement(v: Boolean) { _isRelativeMouseMovement.value = v }
    fun setIsMouseDisabled(v: Boolean)         { _isMouseDisabled.value = v }
    fun setMoveCursorToTouchpoint(v: Boolean)  { _moveCursorToTouchpoint.value = v }
    fun setShowLogs(v: Boolean)                { _showLogs.value = v }
    fun setShowMagnifier(v: Boolean)           { _showMagnifier.value = v }
    fun setCursorExpanded(v: Boolean)          { _cursorExpanded.value = v }

    fun toggleCursorExpanded() {
        val next = !_cursorExpanded.value
        _cursorExpanded.value = next
        onCursorExpandedChanged?.invoke(next)
    }

    fun reset() {
        _isPaused.value = false
        _isRelativeMouseMovement.value = false
        _isMouseDisabled.value = false
        _moveCursorToTouchpoint.value = false
        _showLogs.value = false
        _showMagnifier.value = true
        _cursorExpanded.value = false
        onClose = null; onKeyboard = null; onInputControls = null
        onScreenEffects = null; onGraphicEngine = null; onVibration = null
        onToggleFullscreen = null; onPauseResume = null; onPipMode = null
        onActiveWindows = null; onTaskManager = null; onMagnifier = null
        onLogs = null; onExit = null; onMoveCursorToTouchpoint = null
        onRelativeMouseMovement = null; onDisableMouse = null
        onCursorExpandedChanged = null
    }
}
