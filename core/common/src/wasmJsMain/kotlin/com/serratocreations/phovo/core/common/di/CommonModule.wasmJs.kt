package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher

// No concept of IO dispatcher in WASM
actual fun getIoDispatcher(): CoroutineDispatcher = CommonModule.defaultDispatcher