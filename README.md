# Phovo
## Self hosted Photo/Video backup
This is a Kotlin Multiplatform project targeting Android, iOS, Desktop and WASM(WEB) clients. Phovo is intended to be a self hosted cloud storage client that enables seamless access to photos and videos on all compatible target devices. While initially support for 4 clients is planned, if there is strong market demand for this product Wear OS, Android XR, and Android TV may eventually be targeted.

### iOS, Android, Browser and Desktop Applications
All applications are developed within a single codebase using [Kotlin Multiplatform technology](https://kotlinlang.org/docs/multiplatform.html). The UI is implemented using [Compose Multiplatform UI framework](https://www.jetbrains.com/lp/compose-multiplatform/).

### Screenshots
| Android | iOS |
| --- | --- |
| ![android_optimized](https://github.com/user-attachments/assets/48a38135-7c10-48be-bac6-afd085440c0a) | <img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/Simulator%20Screenshot%20-%20iPhone%2016%20-%202024-12-02%20at%2013.43.23.png?raw=true" width="840" /> |

#### Desktop
<img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/desktop_screenshot.png" style="max-width: 100%; height: auto;" />

## How to build and run

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

# Modular Design Strategy
The Phovo project follows the modular design guidelines published by Google for mobile development:

https://developer.android.com/topic/modularization/patterns

As mentioned in the guidelines modularization can be done by feature or by layer. Due to the flexibility of the gradle build system, projects can also adopt a flexible modular design where modularization is done by both feature and layer. A flexible modular design is required for the Phovo project as there are currently 4 target platforms(Android, IOS, Web, Desktop). 
- For cases where a feature is small and all platforms may adopt the same UI, the module in the `feature` directory will contain [UI, Domain, and Data layers](https://developer.android.com/topic/architecture)
- For cases where a feature is large and/or specific targets require different UI layer implementations, modularization will be done by both feature and layer.

<img src="https://github.com/Anthony17serrato/Phovo/blob/main/docs/images/PhovoModularization.png" />

## Troubleshooting WASM issues
When adding new dependencies WASM may require a yarn lock upgrade

`./gradlew kotlinUpgradeYarnLock`
