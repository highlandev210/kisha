package com.example.kisha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kisha.ui.KishaAppUi
import com.example.kisha.ui.theme.KishaTheme

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
