package com.serratocreations.phovo.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.defaultModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(defaultModule)
}

// called by IOS in iOSApp.swift
fun initKoin() = initKoin {}