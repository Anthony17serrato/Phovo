package com.serratocreations.phovo

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DevMenuResetOptionsScreen(
    onClickResetAppState: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Button(
            onClick = onClickResetAppState
        ) {
            Text("Clear App State")
        }
    }
}