package com.serratocreations.phovo.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.module.Module as KoinModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.*

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(AppModule().module, platformModule())
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}

@Module
@ComponentScan("com.serratocreations.phovo")
class AppModule

expect fun platformModule(): KoinModule