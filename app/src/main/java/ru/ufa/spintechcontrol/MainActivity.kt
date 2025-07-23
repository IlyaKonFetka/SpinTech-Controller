package ru.ufa.spintechcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import ru.ufa.spintechcontrol.ui.navigation.BottomNavigationBar
import ru.ufa.spintechcontrol.ui.navigation.SpinTechNavigation
import ru.ufa.spintechcontrol.ui.theme.SpinTechControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpinTechControlTheme {
                SpinTechControlApp()
            }
        }
    }
}

@Composable
fun SpinTechControlApp() {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            SpinTechNavigation(
                navController = navController
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpinTechControlAppPreview() {
    SpinTechControlTheme {
        SpinTechControlApp()
    }
}