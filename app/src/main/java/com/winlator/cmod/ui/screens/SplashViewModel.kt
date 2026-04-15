package com.winlator.cmod.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.winlator.cmod.MainActivity
import com.winlator.cmod.xenvironment.ImageFs
import com.winlator.cmod.xenvironment.ImageFsInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SplashViewModel(app: Application) : AndroidViewModel(app) {

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    /**
     * Check whether the system image needs (re)installation.
     * Returns true if an install was triggered; the caller should show the install overlay.
     */
    fun installIfNeeded(activity: MainActivity): Boolean {
        if (_isInstalling.value) return true   // already running

        val imageFs = ImageFs.find(activity)
        if (imageFs.isValid && imageFs.version >= ImageFsInstaller.LATEST_VERSION) return false

        _isInstalling.value = true
        _progress.value = 0

        ImageFsInstaller.installFromAssetsWithCallback(
            activity,
            { pct ->
                _progress.value = pct
            },
            {
                // Called on UI thread after install completes
                _isInstalling.value = false
            },
        )
        return true
    }
}
