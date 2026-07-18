package com.hect0x7.proxy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private lateinit var preferences: ProxyPreferences
    private var settings by mutableStateOf(ProxySettings())
    private var addresses by mutableStateOf(emptyList<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = ProxyPreferences(this)
        settings = preferences.read()
        addresses = localIpv4Addresses()
        requestRuntimePermissions()

        if (settings.httpEnabled || settings.socksEnabled) {
            ProxyService.applySettings(this, settings)
        }

        setContent {
            val runtime by ProxyService.runtime.collectAsState()
            ProxyTheme {
                ProxyScreen(
                    settings = settings,
                    runtime = runtime,
                    addresses = addresses,
                    onSettingsChanged = ::updateSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        addresses = localIpv4Addresses()
    }

    private fun updateSettings(updated: ProxySettings) {
        if (updated == settings) return
        settings = updated
        preferences.write(updated)
        ProxyService.applySettings(this, updated)
    }

    private fun requestRuntimePermissions() {
        val missing = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), REQUEST_PERMISSIONS)
    }

    private companion object {
        const val REQUEST_PERMISSIONS = 100
    }
}
