![phovo_banner](https://github.com/user-attachments/assets/eb4b5750-7355-459a-b186-40460d80b61a)

## Self hosted Photo/Video backup
This is a Kotlin Multiplatform project targeting Android, iOS, Desktop and WASM(WEB) clients. Phovo is intended to be a self hosted cloud storage solution that enables seamless access to photos and videos on all compatible target devices. While initially support for 4 clients is planned, if there is strong market demand for this product Wear OS, Android XR, and Android TV may eventually be targeted.

### iOS, Android, Browser and Desktop Applications
All applications are developed within a single codebase using [Kotlin Multiplatform technology](https://kotlinlang.org/docs/multiplatform.html). The UI is implemented using [Compose Multiplatform UI framework](https://www.jetbrains.com/lp/compose-multiplatform/).

### Screenshots
| ![android_optimized](https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/android_optimized.gif?raw=true) | <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot_20261014-204941.png" width="900" /> | <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot%202026-10-14%20at%208.27.57%E2%80%AFPM.png" width="900" /> |
| --- | --- | --- |
| <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot%202026-10-14%20at%208.51.58%E2%80%AFPM.png" width="900" /> | <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot%202026-10-14%20at%208.34.43%E2%80%AFPM.png" width="900" /> | <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot_20261014-204740.png" width="900" /> |

#### Desktop
<img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Screenshot%202026-10-14%20at%208.56.34%E2%80%AFPM.png" style="max-width: 100%; height: auto;" />

## 🗓️ Phovo Release Schedule

Phovo is currently undergoing active development. Core logic, architecture, and UI foundations are still being refined. The following is the planned release timeline:

---

### 🚩 October 31, 2026 — Alpha Release
**Availability:** Internal  
**Focus:**  
- Early testing of core features  
- Rapid iteration  
- Expect bugs, instability, and breaking changes without backward compatibility  
- Continued development of initial feature set

---

### 🚩 February 28, 2027 — Beta Release
**Availability:** Public  
**Focus:**  
- Initial feature development complete  
- No further breaking changes  
- Begin stabilization efforts  
- Resolve remaining functional issues

---

### 🏁 May 31, 2027 — Stable Release
**Availability:** Android, iOS & Desktop  
**Focus:**  
- Production-ready release  
- All critical and high-priority bugs resolved  
- Full cross-platform support

# Project Structure
## How to build and run
The project can be assembled in an environment which has been configured for Kotlin Multiplatform development, please follow the official guide:
https://kotlinlang.org/docs/multiplatform/quickstart.html#set-up-the-environment

The desktop build embeds an FFmpeg binary, used for thumbnail generation. It is downloaded and
verified against a pinned SHA-256 on the first build, so that build needs network access. The
archive is cached under `~/.gradle/caches/phovo-ffmpeg`, which survives `clean`, so later builds
work offline. Only the binary for the platform running the build is fetched. The pinned builds, and
notes on updating them, live in `build-logic`'s `FfmpegDistribution.kt`.

### Windows: x64 only
Phovo publishes Windows builds for x64 only. Windows on ARM devices run the x64 build through
Windows' built-in x64 emulation, so there is no separate ARM64 release.

The reason is SQLite. Room uses `BundledSQLiteDriver` from `androidx.sqlite:sqlite-bundled`, and its
JVM artifact ships a native library for `windows_x64` but none for `windows_arm64` (checked on 2.6.2
and 2.7.0). A native ARM64 build compiles and launches, but every database call then fails with
`Cannot find a suitable SQLite binary for windows 11 | aarch64`.

#### Building the x64 release on a Windows on ARM machine
The packaged app bundles the JVM that runs Gradle, and the Skiko and FFmpeg binaries are picked by
that JVM's architecture. To get an x64 app on an ARM machine, Gradle has to run on an x64 JVM:

1. Set `JAVA_HOME` to an **x64 JetBrains Runtime 17**. `gradle/gradle-daemon-jvm.properties`
   requires a JetBrains 17 daemon, so with any other JDK Gradle falls back to an ARM64 JetBrains
   Runtime and the result is ARM64 again.
2. Stop any running daemons so an ARM64 daemon is not reused.
3. Force `createRuntimeImage` to run. It does not notice an architecture change when the JDK
   version is the same, and would otherwise keep a stale ARM64 runtime next to an x64 launcher,
   which fails to start.

```
./gradlew --stop
./gradlew :applicationContainers:desktopApp:createRuntimeImage --rerun :applicationContainers:desktopApp:createDistributable --no-configuration-cache
```

The same `--rerun` applies when switching back to an ARM64 JVM on the same checkout, or run `clean`
first.

## Source Sets
The Phovo project makes use of custom source sets to maximize code re-usability when certain features
are supported only by a subset of platforms. To learn more about custom source sets visit:
https://kotlinlang.org/docs/multiplatform/multiplatform-advanced-project-structure.html#declaring-custom-source-sets

## Troubleshooting WASM issues
When adding new dependencies WASM may require a yarn lock upgrade

`./gradlew kotlinUpgradeYarnLock`
