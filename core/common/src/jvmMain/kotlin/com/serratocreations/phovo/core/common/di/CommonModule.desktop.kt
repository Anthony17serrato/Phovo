package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.IO