package com.winlator.cmod.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

object AppThemeState {
    private lateinit var prefs: SharedPreferences

    private val _presetIndex = MutableStateFlow(0)
    val presetIndex: StateFlow<Int> = _presetIndex

    private val _customAccent = MutableStateFlow(Color(0xFF8B6BE0))
    val customAccent: StateFlow<Color> = _customAccent

    // Derived: current ColorScheme combining preset + optional custom accent
    val colorScheme: kotlinx.coroutines.flow.Flow<ColorScheme> =
        combine(_presetIndex, _customAccent) { index, accent ->
            val preset = themePresets.getOrElse(index) { themePresets.first() }
            if (index == CUSTOM_PRESET_INDEX) {
                preset.toColorScheme(accentOverride = accent)
            } else {
                preset.toColorScheme()
            }
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("winlator_theme", Context.MODE_PRIVATE)
        _presetIndex.value = prefs.getInt("preset_index", 0).coerceIn(0, themePresets.size - 1)
        val savedAccent = prefs.getInt("custom_accent", Color(0xFF8B6BE0).toArgb())
        _customAccent.value = Color(savedAccent)
    }

    fun setPreset(index: Int) {
        _presetIndex.value = index.coerceIn(0, themePresets.size - 1)
        prefs.edit().putInt("preset_index", _presetIndex.value).apply()
    }

    fun setCustomAccent(color: Color) {
        _customAccent.value = color
        _presetIndex.value = CUSTOM_PRESET_INDEX
        prefs.edit()
            .putInt("custom_accent", color.toArgb())
            .putInt("preset_index", CUSTOM_PRESET_INDEX)
            .apply()
    }

    fun currentColorSchemeSnapshot(): ColorScheme {
        val index = _presetIndex.value
        val preset = themePresets.getOrElse(index) { themePresets.first() }
        return if (index == CUSTOM_PRESET_INDEX) {
            preset.toColorScheme(accentOverride = _customAccent.value)
        } else {
            preset.toColorScheme()
        }
    }
}
