# Build environment (recorded from an actual local build)

Recorded 11 September 2026 on the development machine after `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed.

| Component | Pinned / resolved | Where |
| --- | --- | --- |
| Gradle | 9.3.1 (wrapper, SHA-256 verified) | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.1.1 (built-in Kotlin) | `gradle/libs.versions.toml` |
| Kotlin compiler used by AGP | 2.2.10 (AGP 9.1.1 declares `kotlin-gradle-plugin:2.2.10`) | resolved transitively |
| Compose compiler Gradle plugin | 2.2.10 | `gradle/libs.versions.toml` |
| Compose BOM | 2025.08.01 | `gradle/libs.versions.toml` |
| compileSdk / targetSdk / minSdk | 37 / 37 / 37 | `app/build.gradle.kts` |
| Build Tools | 36.0.0 (installed; AGP 9.1.1 default) | SDK |
| Platform | `platforms;android-37.0` | SDK |
| JDK running Gradle | Temurin 17.0.20.1 (`C:\HA\HEALTH APP\.local\toolchains\jdk-17.0.20.1+1`) | `scripts/build-and-test.ps1` picks it up when `JAVA_HOME` is unset |
| SDK location | `C:\Users\abdul\AppData\Local\Android\Sdk` | `local.properties` (not committed) |

## Observations

- D8 in AGP 9.1.1 prints `An API level of 37 is not supported by this compiler` for every dex task. The build still succeeds; the warning means the dexer has no API-37 specific knowledge yet. Revisit when a newer AGP lists API 37 explicitly.
- Lint (`lintDebug`, `abortOnError = true`): 0 errors, 16 warnings (dependency-version notices, `ClickableViewAccessibility` on the trigger view, `ViewConstructor`, `DataExtractionRules`).
- Unit tests: `DomainTest`, 26 tests, 0 failures.
- The Shizuku API/provider libraries are on the classpath but unused in this build; their manifest merge adds `moe.shizuku.manager.permission.API_V23` to the APK. No Shizuku code runs.
- Debug APK: ~60 MB (unminified, includes `material-icons-extended`).

## Commands

```powershell
.\scripts\build-and-test.ps1
.\scripts\install-debug.ps1 -Serial <adb serial>
```
