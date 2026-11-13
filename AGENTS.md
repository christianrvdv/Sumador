# AGENTS.md

Single-module Android app (`:app`). Kotlin + Jetpack Compose (Material 3), Hilt, Room, DataStore, WorkManager. App id/package: `cu.christianrvdv.sumador`.

## Commands

Windows: `.\gradlew.bat`; Linux/macOS/CI: `./gradlew`.

- Build debug: `.\gradlew.bat assembleDebug`
- Install on device/emulator: `.\gradlew.bat installDebug`
- Unit tests: `.\gradlew.bat test` — only the template `ExampleUnitTest` exists; no real coverage.
- Instrumented tests: `.\gradlew.bat connectedAndroidTest` (requires a device).
- Lint: `.\gradlew.bat lintDebug` — no ktlint/detekt configured.
- Pre-commit gate per `CONSTRAINTS.md`: `.\gradlew.bat clean assembleDebug` then `.\gradlew.bat clean build --refresh-dependencies`, both must succeed.
- Release locally: `.\gradlew.bat assembleRelease` works without a keystore and produces `app-release-unsigned.apk` (signing config is only applied when the keystore file exists). If `KEYSTORE_PATH` is set but missing, the build fails on purpose.

JDK 17 required (Gradle 8.14, AGP 8.13.2, Kotlin 2.0.21, KSP 2.0.21-1.0.25). Room and Hilt use KSP, not kapt.

## Build constraints (see `CONSTRAINTS.md`)

- `settings.gradle.kts` has a group-filtered Aliyun mirror before official repos. Do not add the Aliyun `gradle-plugin`/`repository/public` mirrors before the official ones; the plugin-marker 502 aborts resolution. Keep the `content { includeGroupByRegex(...) }` filters.
- Do not bump Gradle/AGP alone; it is already outside the official KGP 2.0.21 matrix and only works because Gradle 8.14 embeds Kotlin 2.0.21. Bump Kotlin/KSP/Room/Hilt together and re-run the full verification.
- Do not "fix" resolution by deleting `~/.gradle/caches/modules-2`; use `--refresh-dependencies`.
- Build environment diagnostics (corrupt `metadata.bin`, `ANDROID_HOME` pointing to a nonexistent path, clean-clone test) are documented in `BUILD.md`.

## Versioning and releases

- `app/build.gradle.kts` is the source of truth: currently `versionCode = 5`, `versionName = "1.8.3"`. README is in sync; update both when bumping.
- `.github/workflows/release.yml` runs only on `v*` tags (no branch/PR CI), builds `assembleRelease`, and uploads `app/build/outputs/apk/release/app-release.apk`. Tag must be `v<versionName>`.
- The in-app updater compares the GitHub latest-release tag (`v` stripped) with `versionName`, so tag and `versionName` must match and the release must carry an `.apk` asset.
- CI decodes `KEYSTORE_BASE64` into `app/keystore.jks`; never commit keystores (`*.jks` is gitignored).

## Architecture

- `MainActivity`: NavHost routes `sumador`, `history`, `manage_denominations/{currency}`; applies locale via `resources.updateConfiguration` + `recreate()`, re-keys composition on language change, and drives update checks.
- `SumadorApplication`: `@HiltAndroidApp`; holds `pendingUpdate` and creates the `update_channel` notification channel.
- UI convention: `ui/<feature>/{Screen,ViewModel,State}.kt`; ViewModels are `@HiltViewModel`, expose `StateFlow`, screens use `hiltViewModel()`.
- Room `AppDatabase` is version 3 with a hand-written `MIGRATION_2_3`, built by its own `getInstance()` and provided via `di/DatabaseModule`. `exportSchema = false` and `app/schemas/` is empty even though KSP declares `room.schemaLocation` — when changing entities, bump the version and add a `Migration`; there is no destructive fallback.
- Two preference DataStores with distinct names: `settings` (in `SettingsViewModel`) and `sumador_state` (in `SumadorViewModel`; keys are `${CURRENCY}_$denomination`). Do not reuse those names.
- Default denominations are hardcoded in `SumadorViewModel.getDefaultDenominationsWithType` per currency; non-empty Room rows for a currency replace them entirely.
- Update path: GitHub latest-release API → `DownloadWorker` (`filesDir/updates`, `.tmp` then rename) → install via FileProvider authority `${applicationId}.fileprovider`. The manifest declares the nested receiver as `.utils.DownloadWorker$InstallRetryReceiver`.

## Backup format (do not break)

- `.sumadorbak` files are AES/CBC/PKCS5 with a 16-byte random IV prefix and the fixed 16-byte ASCII key `SumadorBackupKey`, implemented in `SettingsViewModel`.
- `BackupData` is Gson-serialized (`@SerializedName`, `@Keep`); import rejects any version other than `1`. Renaming/removing fields or serialized names breaks existing backups and the minified release build.
- Import replaces saved sums and custom denominations inside a Room transaction, then restores DataStore settings.

## Conventions and gotchas

- Default resources are English (`res/values/strings.xml`); Spanish is `res/values-es/strings.xml`. Add new user-facing strings to both.
- Code comments, docs, and commit messages are Spanish; commits use conventional prefixes (`feat:`, `fix:`, `chore:`).
- Release has `minifyEnabled = true`; keep the `proguard-rules.pro` keeps for backup/serialized classes in mind when adding Gson models.
- Firebase Messaging/Analytics dependencies are declared but unused: no `google-services` plugin or `google-services.json`; do not assume FCM works.
- `local.properties` (SDK path) is gitignored and must not be committed.
