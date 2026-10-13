package cz.financer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.financer.ui.navigation.FinancerNavHost
import cz.financer.ui.theme.FinancerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinancerTheme {
                FinancerNavHost()
            }
        }
    }
}
