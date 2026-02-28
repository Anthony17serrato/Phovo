package com.serratocreations.phovo

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DevMenuResetOptionsScreen(
    onClickResetAppState: () -> Unit
) {
    Column {
        Button(
            onClick = onClickResetAppState
        ) {
            Text("Clear App State")
        }
    }
}