package com.jamsmendez.cieldroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jamsmendez.cieldroid.ui.CielDroidApp
import com.jamsmendez.cieldroid.ui.theme.CielDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CielDroidTheme {
                CielDroidApp()
            }
        }
    }
}
