# BUILD.md — Compilación y diagnóstico

## Requisitos

- JDK 17 (`JAVA_HOME` apuntando al JDK; probado con Microsoft 17.0.13).
- Android SDK con API 36 y build-tools instalados.
- SDK localizado por `local.properties` (`sdk.dir=...`) o, en su defecto, por
  `ANDROID_HOME` / `ANDROID_SDK_ROOT`.
- Windows: `.\gradlew.bat`. Linux/macOS/CI: `./gradlew`.

## Comandos

| Objetivo | Comando |
|---|---|
| Ayuda / validar configuración | `.\gradlew.bat help` |
| APK debug | `.\gradlew.bat assembleDebug` |
| Instalar en dispositivo | `.\gradlew.bat installDebug` |
| Tests unitarios | `.\gradlew.bat test` |
| Lint | `.\gradlew.bat lintDebug` |
| Build completo (lint + tests + release) | `.\gradlew.bat clean build` |
| Forzar recarga de metadatos | `.\gradlew.bat clean build --refresh-dependencies` |

## Firma release

La configuración de firma se aplica solo si existe el keystore:

- Local sin keystore: `app-release-unsigned.apk`, sin fallo de build.
- CI: definir `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
  Si `KEYSTORE_PATH` está definido y no existe, el build falla a propósito.

## Diagnóstico

### 1. `Could not open ... kotlin-dsl\scripts\<hash>\metadata.bin`

Síntoma típico:

```
Could not open cp_settings generic class cache for settings file ... metadata.bin
```

Causa: archivo de caché corrompido (ceros), no un problema de red ni de versión
de Kotlin. Ocurre por apagones/antivirus/FS en Windows.

```bat
.\gradlew.bat --stop
rmdir /s /q "%USERPROFILE%\.gradle\caches\8.14\kotlin-dsl"
.\gradlew.bat help
```

La carpeta se regenera. **No** borrar `modules-2`.

### 2. `Could not find room-compiler-2.6.1.jar` (u otro artefacto que sí existe)

Caché negativa de artefactos. Verificar la URL y forzar recarga:

```bat
.\gradlew.bat clean build --refresh-dependencies
```

### 3. KSP no resuelve (`Plugin [id: 'com.google.devtools.ksp'] was not found`)

Un mirror de Aliyun colocado antes de los repos oficiales devuelve 502 en la
ruta del plugin marker y Gradle no continúa. La configuración válida está en
`settings.gradle.kts`: mirror `google` filtrado por grupo, luego
`gradlePluginPortal()`, `mavenCentral()` y `google()`.

### 4. `SDK XML versions up to 3 but version 4 was encountered`

Aviso de mezcla de versiones entre Android Studio y command-line-tools. No
bloquea el build; actualizar `cmdline-tools` si molesta.

### 5. `ANDROID_HOME` apunta a una ruta inexistente

En esta máquina `ANDROID_HOME` global apunta a `C:\Users\Christian\.android\Sdk`
(no existe); el SDK real es `D:\Desarrollo\Sdk` y lo resuelve
`local.properties`. Para un clon limpio o CI, exportar:

```bat
set ANDROID_HOME=D:\Desarrollo\Sdk
set ANDROID_SDK_ROOT=D:\Desarrollo\Sdk
```

### 6. Warnings de deprecación

No bloquean. Pendientes de limpieza (sin cambiar lógica):

- `MainActivity.kt`: constructor `Locale(String)` y `updateConfiguration`.
- `SavedSumsScreen.kt`: `Icons.Filled.ArrowBack`, `SearchBar` antiguo.
- `ManageDenominationsScreen.kt`, `SettingsDialog.kt`: iconos `Filled` con
  variante `AutoMirrored`.
- `SumadorScreen.kt`: `Divider` → `HorizontalDivider`.
- `UpdateManager.kt`: `GsonBuilder.setLenient`.

## Prueba de clon limpio

Valida que un checkout nuevo resuelve todo desde cero:

```bat
robocopy "D:\1 - Proyectos\Sumador" "%TEMP%\sumador-clean-clone" /E /XD .gradle build .git .idea /XF local.properties *.log
set GRADLE_USER_HOME=%TEMP%\sumador-fresh-gradle
set ANDROID_HOME=D:\Desarrollo\Sdk
set ANDROID_SDK_ROOT=D:\Desarrollo\Sdk
cd /d "%TEMP%\sumador-clean-clone"
.\gradlew.bat clean assembleDebug
```

## Verificación registrada (2026-11-13)

| Prueba | Resultado | Tiempo |
|---|---|---|
| `help --refresh-dependencies` con mirrors nuevos | BUILD SUCCESSFUL | 1m 15s |
| `:app:assembleRelease` sin keystore | BUILD SUCCESSFUL (unsigned) | 19m 48s |
| `clean assembleDebug` | BUILD SUCCESSFUL | 4m 28s |
| `clean build --refresh-dependencies` | BUILD SUCCESSFUL (116 tareas) | 12m 01s |
| Clon limpio + `GRADLE_USER_HOME` nuevo | BUILD SUCCESSFUL, APK 24.7 MB | 15m 04s |

Artefactos: `app/build/outputs/apk/debug/app-debug.apk` y
`app/build/outputs/apk/release/app-release-unsigned.apk` (local sin keystore).
