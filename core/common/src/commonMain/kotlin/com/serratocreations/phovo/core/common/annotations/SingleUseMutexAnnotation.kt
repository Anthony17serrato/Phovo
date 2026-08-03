package com.serratocreations.phovo.core.common.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This mutex property is meant to be used at a single call site. Calling it somewhere else is most likely a mistake."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
annotation class SingleUseMutex