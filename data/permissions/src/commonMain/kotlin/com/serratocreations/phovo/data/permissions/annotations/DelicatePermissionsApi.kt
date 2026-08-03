package com.serratocreations.phovo.data.permissions.annotations

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is a delicate synchronous API. It must only be called after PermissionRepository state " +
            "has been populated; otherwise, it may throw runtime exceptions. Prefer using the coroutine-based API instead."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class DelicatePermissionsApi