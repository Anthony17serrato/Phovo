package com.serratocreations.phovo

/**
 * Runs app wide setup once at startup, through [initialize].
 *
 * Initializers take their dependencies as [Lazy] and only read them in [initialize] or later.
 * Creating an initializer then builds nothing, so [initialize] can set up what the rest of the app
 * relies on, such as FileKit, before any of it is created.
 */
abstract class AndroidDesktopIosAppInitializer {

    open fun initialize() {
        // Initialization that is common for Android, Desktop, Ios & Wasm
    }
}