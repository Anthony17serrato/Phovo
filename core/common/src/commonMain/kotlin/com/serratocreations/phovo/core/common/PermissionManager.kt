package com.serratocreations.phovo.core.common

import androidx.compose.runtime.Composable

enum class PermissionState {
    Granted,
    Limited,
    Denied,
    NotDetermined
}

interface PermissionManager {
    fun getPermissionState(activity: Any? = null): PermissionState
    fun openSettings()
}

interface PermissionRequester {
    fun requestPermission()
}

@Composable
expect fun rememberPermissionRequester(onResult: (Boolean) -> Unit): PermissionRequester

@Composable
expect fun getActivity(): Any?
