package com.serratocreations.phovo.core.common.util
object DesktopOsUtil {
    const val OS_NAME = "os.name"
    const val OS_ARCH = "os.arch"
    const val MAC = "mac"
    const val LINUX = "linux"
    const val WINDOWS = "win"

    val desktopOs: DesktopOs by lazy {
        val osName = System.getProperty(OS_NAME).lowercase()
        val arch = System.getProperty(OS_ARCH).lowercase()
        val isArm64 = arch.contains("aarch64") || arch.contains("arm64")
        val isArm32 = arch.startsWith("arm") && !isArm64
        val isX86_64 = arch.contains("x86_64") || arch.contains("amd64")
        val isX86_32 = arch == "x86" || arch == "i386" || arch == "i686"

        return@lazy when {
            osName.contains(MAC) && isArm64 -> DesktopOs.Mac(Architecture.ARM64)
            osName.contains(MAC) && isX86_64 -> DesktopOs.Mac(Architecture.X64)

            osName.contains(WINDOWS) && isArm64 -> DesktopOs.Windows(Architecture.ARM64)
            osName.contains(WINDOWS) && isArm32 -> DesktopOs.Windows(Architecture.ARM32)
            osName.contains(WINDOWS) && isX86_32 -> DesktopOs.Windows(Architecture.X32)
            osName.contains(WINDOWS) && isX86_64 -> DesktopOs.Windows(Architecture.X64)

            osName.contains(LINUX) && isArm64 -> DesktopOs.Linux(Architecture.ARM64)
            osName.contains(LINUX) && isArm32 -> DesktopOs.Linux(Architecture.ARM32)
            osName.contains(LINUX) && isX86_32 -> DesktopOs.Linux(Architecture.X32)
            osName.contains(LINUX) && isX86_64 -> DesktopOs.Linux(Architecture.X64)
            // TODO: Identify any remaining os
            else -> error("Unsupported OS: $osName $arch")
        }
    }
}
sealed interface DesktopOs {
    val architecture: Architecture

    data class Windows(override val architecture: Architecture) : DesktopOs
    data class Mac(override val architecture: Architecture) : DesktopOs
    data class Linux(override val architecture: Architecture) : DesktopOs
}

enum class Architecture {
    X32, X64, ARM64, ARM32
}