package com.serratocreations.phovo.data.permissions.di

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * In addition to common definitions for IOS, & Android
 * this API provides modules that are specific to each individual platform
 */
internal expect fun getAndroidIosModules(): Module

fun getPermissionsDataModule(): Module = module {
    includes(getAndroidIosModules())
}