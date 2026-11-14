package com.gideon.buecherwelt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gideon.buecherwelt.ui.navigation.AppNavHost
import com.gideon.buecherwelt.ui.theme.BuecherweltTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BuecherweltApp
        lifecycleScope.launch {
            app.container.tropeRepository.ensureSeeded()
        }
        setContent {
            val themeMode = app.container.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = "system").value
            BuecherweltTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(container = app.container)
                }
            }
        }
    }
}
