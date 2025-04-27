/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.serratocreations.phovo.buildlogic"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    //implementation(libs.truth)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "phovo.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpApplicationKoin") {
            id = "phovo.kmp.application.koin"
            implementationClass = "KmpApplicationKoinConventionPlugin"
        }
        register("kmpLibraryKoin") {
            id = "phovo.kmp.library.koin"
            implementationClass = "KmpLibraryKoinConventionPlugin"
        }
        register("kmpLibraryCompose") {
            id = "phovo.kmp.library.compose"
            implementationClass = "KmpLibraryComposeConventionPlugin"
        }
        register("kmpApplicationCompose") {
            id = "phovo.kmp.application.compose"
            implementationClass = "KmpApplicationComposeConventionPlugin"
        }
        register("kmpApplication") {
            id = "phovo.kmp.application"
            implementationClass = "KmpApplicationConventionPlugin"
        }
        register("kmpLibraryRoom") {
            id = "phovo.kmp.library.room"
            implementationClass = "KmpLibraryRoomConventionPlugin"
        }
    }
}
