package com.serratocreations.phovo.buildlogic.ffmpeg

/**
 * Archive layouts the provisioning task knows how to unpack.
 */
enum class FfmpegArchiveFormat {
    ZIP,
    TAR_XZ
}

/**
 * A single pinned FFmpeg build: where to fetch it, what it must hash to, and the executable to pull
 * out of the archive.
 *
 * The checksum is not a nicety. [FfmpegBinaries.MAC_ARM] is served from a host that sits behind a
 * proof of work bot wall which intermittently answers with an HTML challenge page instead of the
 * zip, and a build that trusted the response blindly would happily write that page out and mark it
 * executable. Verifying the archive is what turns that into a loud failure.
 */
data class FfmpegDistribution(
    val platform: String,
    val url: String,
    val sha256: String,
    val format: FfmpegArchiveFormat,
    val executableName: String
)

/**
 * The pinned set of FFmpeg builds, one per supported desktop target.
 *
 * These used to live in the repository under Git LFS, which put roughly half a gigabyte of binaries
 * against the LFS quota. They are downloaded and verified at build time instead.
 *
 * ## Updating
 *
 * Every entry is pinned by exact URL plus SHA-256, so a bump means changing both. Sources, all
 * reachable from https://ffmpeg.org/download.html#build:
 *
 *  - Windows and Linux come from https://github.com/BtbN/FFmpeg-Builds. Pin to a **month end**
 *    autobuild tag: BtbN prunes daily tags after roughly two weeks, but keeps the month end ones
 *    (they currently go back to 2024-09), so a month end tag stays resolvable. Checksums for a
 *    release are published as its `checksums.sha256` asset, so a bump does not require downloading
 *    the archives.
 *  - macOS x86_64 comes from https://evermeet.cx/ffmpeg/ as a version pinned zip. They publish only
 *    a GPG signature, so compute the SHA-256 after downloading.
 *  - macOS arm64 comes from https://www.osxexperts.net/. Version named zips stay available after
 *    newer ones ship. Current checksums are listed on the page; older ones must be computed.
 *
 * ## Why 8.1 and not 9.0
 *
 * FFmpeg 9.0 is released and the two macOS sources ship it, but BtbN's build matrix only covers
 * `master`, `n7.1` and `n8.1`, so there is no 9.0 release branch build for Windows or Linux. Taking
 * 9.0 would mean either running a different major version on macOS than everywhere else, or moving
 * Windows and Linux onto rolling `master` dev builds. 8.1 is the newest version available as a
 * stable release build on all six targets. Once BtbN adds an `n9.0` branch, bumping is a matter of
 * editing the URLs and checksums below.
 */
object FfmpegBinaries {

    /**
     * Reported by `ffmpeg -version`. The BtbN builds are 8.1.2, evermeet is 8.1.2 and osxexperts is
     * 8.1, so this is the shared release branch rather than an exact patch level.
     */
    const val VERSION_BRANCH: String = "8.1"

    private const val BTBN_RELEASE =
        "https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2026-07-31-14-10"

    private const val BTBN_BUILD = "ffmpeg-n8.1.2-34-g9b6c8969e0"

    val MAC_ARM = FfmpegDistribution(
        platform = "mac-arm",
        url = "https://www.osxexperts.net/ffmpeg81arm.zip",
        sha256 = "ebb82529562b71170807bbc6b0e7eb4f0b13af8cbb0e085bb9e8f6fe709598ad",
        format = FfmpegArchiveFormat.ZIP,
        executableName = "ffmpeg"
    )

    val MAC_X64 = FfmpegDistribution(
        platform = "mac-x64",
        url = "https://evermeet.cx/ffmpeg/ffmpeg-8.1.2.zip",
        sha256 = "e91df72a1ee7c26606f90dd2dd4dcccc6a75140ff9ea6fdd50faae828b82ba69",
        format = FfmpegArchiveFormat.ZIP,
        executableName = "ffmpeg"
    )

    val WIN_X64 = FfmpegDistribution(
        platform = "win-x64",
        url = "$BTBN_RELEASE/$BTBN_BUILD-win64-gpl-8.1.zip",
        sha256 = "cc4156d51387566ea8ba653fc3a04897bdf812fddf652428d9030bbf7ae24835",
        format = FfmpegArchiveFormat.ZIP,
        executableName = "ffmpeg.exe"
    )

    val WIN_ARM = FfmpegDistribution(
        platform = "win-arm",
        url = "$BTBN_RELEASE/$BTBN_BUILD-winarm64-gpl-8.1.zip",
        sha256 = "abf3b41c200ce5346b9bb5be6fe634c4720d891778d8921f7b36b76d002b3c96",
        format = FfmpegArchiveFormat.ZIP,
        executableName = "ffmpeg.exe"
    )

    val LINUX_X64 = FfmpegDistribution(
        platform = "linux-x64",
        url = "$BTBN_RELEASE/$BTBN_BUILD-linux64-gpl-8.1.tar.xz",
        sha256 = "09fc77be269c7053e438b7e96548e4af97604faf96a42c4a3c56a1ad74c22c0a",
        format = FfmpegArchiveFormat.TAR_XZ,
        executableName = "ffmpeg"
    )

    val LINUX_ARM = FfmpegDistribution(
        platform = "linux-arm",
        url = "$BTBN_RELEASE/$BTBN_BUILD-linuxarm64-gpl-8.1.tar.xz",
        sha256 = "177e40c91564dec3840096f3bf1ffe696b94330585972462cfc739fa29fe0e1a",
        format = FfmpegArchiveFormat.TAR_XZ,
        executableName = "ffmpeg"
    )

    /**
     * Picks the build matching the machine running the build. Only the host binary is provisioned,
     * matching how the LFS based setup behaved: the desktop app embeds FFmpeg as a Compose resource,
     * so a build produces an artifact for the platform it ran on.
     */
    fun forHost(
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch")
    ): FfmpegDistribution {
        val os = osName.lowercase()
        val arch = osArch.lowercase()
        val isArm = arch.contains("aarch64") || arch.contains("arm")

        return when {
            os.contains("mac") && isArm -> MAC_ARM
            os.contains("mac") -> MAC_X64
            // Win32 support is possible by manually building the binary
            // https://github.com/BtbN/FFmpeg-Builds/tree/latest
            os.contains("win") && isArm -> WIN_ARM
            os.contains("win") -> WIN_X64
            os.contains("linux") && isArm -> LINUX_ARM
            os.contains("linux") -> LINUX_X64
            else -> error("Unsupported OS for FFmpeg provisioning: $osName $osArch")
        }
    }
}
