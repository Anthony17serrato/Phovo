package com.serratocreations.phovo.buildlogic

/**
 * Defines custom source sets that can be used to define common logic
 * amongst a subset of project platforms
 */
enum class CustomSourceSets(
    val declarationOrder: Int,
    val sourceSetName: String
) {
    AndroidIosWeb(4, "commonAndroidIosWeb"),
    DesktopIosAndroid(3, "commonDesktopIosAndroid"),
    AndroidDesktop(2, "commonAndroidDesktop"),
    IosAndroid(1, "commonIosAndroid")
}