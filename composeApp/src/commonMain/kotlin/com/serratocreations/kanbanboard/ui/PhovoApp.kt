package com.serratocreations.kanbanboard.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serratocreations.phovo.core.designsystem.component.PhovoBackground
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun PhovoApp(
    appState: PhovoAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    kanbanViewModel: KanbanViewModel = viewModel { KanbanViewModel() }
) {
    PhovoBackground(modifier = modifier) {
        val snackbarHostState = remember { SnackbarHostState() }
        PhovoApp(
            appState = appState,
            snackbarHostState = snackbarHostState,
            windowAdaptiveInfo = windowAdaptiveInfo,
        )
    }
//    MaterialTheme {
//        val kanbanUiState = kanbanViewModel.kanbanUiState.collectAsState()
//        var showContent by remember { mutableStateOf(false) }
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Text(kanbanUiState.value.joinToString())
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                val greeting = remember { Greeting().greet() }
//                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }
//    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun PhovoApp(
    appState: PhovoAppState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {

}