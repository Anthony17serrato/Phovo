package com.serratocreations.phovo.core.common.util

import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatformTools

inline fun <reified T: Any> getKoinInstance(qualifier: Qualifier? = null,): T {
    return KoinPlatformTools.defaultContext().get().get<T>(qualifier)
}