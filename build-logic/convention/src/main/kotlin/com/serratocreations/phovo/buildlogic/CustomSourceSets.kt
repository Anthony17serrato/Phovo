package com.serratocreations.phovo.buildlogic

enum class CustomSourceSets(
    val declarationOrder: Int,
    val sourceSetName: String
) {
    AndroidIosWeb(3, "commonAndroidIosWeb"),
    DesktopIosAndroid(2, "commonDesktopIosAndroid"),
    IosAndroid(1, "commonIosAndroid")
}