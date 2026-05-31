package com.photo.plan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.photo.plan.ui.navigation.AppNavigation
import com.photo.plan.ui.theme.PhotoPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoPlanTheme {
                AppNavigation()
            }
        }
    }
}
