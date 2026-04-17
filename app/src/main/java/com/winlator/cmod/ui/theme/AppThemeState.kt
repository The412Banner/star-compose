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
    private lateinit var themePrefs: SharedPreferences

    private val _presetIndex = MutableStateFlow(0)
    val presetIndex: StateFlow<Int> = _presetIndex

    private val _customAccent = MutableStateFlow(Color(0xFF8B6BE0))
    val customAccent: StateFlow<Color> = _customAccent

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    val colorScheme: kotlinx.coroutines.flow.Flow<ColorScheme> =
        combine(_presetIndex, _customAccent, _isDarkMode) { index, accent, dark ->
            val preset = themePresets.getOrElse(index) { themePresets.first() }
            val override = if (index == CUSTOM_PRESET_INDEX) accent else null
            if (dark) preset.toColorScheme(accentOverride = override)
            else      preset.toLightColorScheme(accentOverride = override)
        }

    fun init(context: Context) {
        themePrefs = context.getSharedPreferences("winlator_theme", Context.MODE_PRIVATE)

        _presetIndex.value = themePrefs.getInt("preset_index", 0).coerceIn(0, themePresets.size - 1)
        val savedAccent = themePrefs.getInt("custom_accent", Color(0xFF8B6BE0).toArgb())
        _customAccent.value = Color(savedAccent)
        _isDarkMode.value = true
    }

    fun setPreset(index: Int) {
        _presetIndex.value = index.coerceIn(0, themePresets.size - 1)
        themePrefs.edit().putInt("preset_index", _presetIndex.value).apply()
    }

    fun setCustomAccent(color: Color) {
        _customAccent.value = color
        _presetIndex.value = CUSTOM_PRESET_INDEX
        themePrefs.edit()
            .putInt("custom_accent", color.toArgb())
            .putInt("preset_index", CUSTOM_PRESET_INDEX)
            .apply()
    }

    fun currentColorSchemeSnapshot(): ColorScheme {
        val index = _presetIndex.value
        val preset = themePresets.getOrElse(index) { themePresets.first() }
        val override = if (index == CUSTOM_PRESET_INDEX) _customAccent.value else null
        return if (_isDarkMode.value) preset.toColorScheme(accentOverride = override)
               else                   preset.toLightColorScheme(accentOverride = override)
    }
}
