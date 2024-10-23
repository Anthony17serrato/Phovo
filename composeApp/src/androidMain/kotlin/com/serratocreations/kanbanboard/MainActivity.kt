package com.serratocreations.kanbanboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.serratocreations.kanbanboard.ui.PhovoApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhovoApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    PhovoApp()
}