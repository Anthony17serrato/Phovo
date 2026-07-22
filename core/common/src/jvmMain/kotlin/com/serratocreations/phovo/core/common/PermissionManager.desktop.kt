package com.serratocreations.phovo.core.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class DesktopPermissionManager : PermissionManager {
    override fun getPermissionState(activity: Any?): PermissionState = PermissionState.Granted
    override fun openSettings() {}
}

@Composable
actual fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester {
    return remember {
        object : PermissionRequester {
            override fun requestPermission() {
                onResult(true)
            }
        }
    }
}

@Composable
actual fun getActivity(): Any? = null

