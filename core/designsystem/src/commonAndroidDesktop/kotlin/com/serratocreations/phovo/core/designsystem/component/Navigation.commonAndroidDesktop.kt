package com.serratocreations.phovo.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal actual val platformNavigationBar:
        (@Composable (items: List<PhovoNavigationSuiteItem>, modifier: Modifier) -> Unit)? = null