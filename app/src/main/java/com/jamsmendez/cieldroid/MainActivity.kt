package com.jamsmendez.cieldroid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamsmendez.cieldroid.ui.CielDroidApp
import com.jamsmendez.cieldroid.ui.theme.CielDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme is always dark, so the system bars keep light icons even on a light system theme.
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            CielDroidTheme {
                CielDroidApp()
            }
        }
    }
}
