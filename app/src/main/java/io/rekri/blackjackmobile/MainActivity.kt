package io.rekri.blackjackmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.rekri.blackjackmobile.ui.MainWidget
import io.rekri.blackjackmobile.ui.theme.BlackJackMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlackJackMobileTheme {
                MainWidget()
            }
        }
    }
}