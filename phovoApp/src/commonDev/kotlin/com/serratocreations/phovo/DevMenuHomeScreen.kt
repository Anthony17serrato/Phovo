package com.serratocreations.phovo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.serratocreations.phovo.core.designsystem.constants.CommonDimensions.paddingSmall

@Composable
fun DevMenuHomeScreen(
    devMenuItems: List<NavKey>,
    onClickMenuItem: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = devMenuItems
        ) { item ->
            Card(
                modifier = Modifier
                    .padding(paddingSmall)
                    .fillMaxWidth(),
                onClick = { onClickMenuItem(item) }
            ) {
                Text(
                    modifier = Modifier.padding(paddingSmall),
                    text = item.toString()
                )
            }
        }
    }
}