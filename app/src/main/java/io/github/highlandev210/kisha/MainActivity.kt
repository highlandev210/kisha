package io.github.highlandev210.kisha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.highlandev210.kisha.ui.KishaAppUi
import io.github.highlandev210.kisha.ui.theme.KishaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KishaApp).container
        setContent {
            KishaTheme { KishaAppUi(container) }
        }
    }
}
