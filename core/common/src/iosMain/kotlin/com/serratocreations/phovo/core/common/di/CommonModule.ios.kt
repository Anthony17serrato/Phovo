package com.serratocreations.phovo.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.IO