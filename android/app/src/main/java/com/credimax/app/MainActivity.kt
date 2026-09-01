package com.credimax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.credimax.app.ui.navigation.CredimaxNav
import com.credimax.app.ui.theme.CredimaxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CredimaxTheme {
                CredimaxNav()
            }
        }
    }
}
