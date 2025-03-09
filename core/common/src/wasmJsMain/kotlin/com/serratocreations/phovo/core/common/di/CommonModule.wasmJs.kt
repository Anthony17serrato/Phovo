package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// No concept of IO dispatcher in WASM
actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.Default