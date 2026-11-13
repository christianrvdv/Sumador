# CONSTRAINTS.md — Restricciones y contrato de calidad del build

Este archivo fija las reglas del proyecto para que un cambio futuro no vuelva a
romper la resolución de dependencias ni la compilación. Si una regla debe
cambiar, se actualiza este documento en el mismo commit.

## Versiones fijadas

| Componente | Versión | Fuente |
|---|---|---|
| Gradle wrapper | 8.14 | `gradle/wrapper/gradle-wrapper.properties` |
| AGP | 8.13.2 | `gradle/libs.versions.toml` |
| Kotlin / KGP | 2.0.21 | `gradle/libs.versions.toml` |
| KSP | 2.0.21-1.0.25 | `gradle/libs.versions.toml` |
| Room | 2.6.1 | `gradle/libs.versions.toml` |
| Hilt | 2.51.1 | `gradle/libs.versions.toml` |
| compileSdk / targetSdk | 36 / 36 | `app/build.gradle.kts` |
| minSdk | 24 | `app/build.gradle.kts` |
| JDK | 17 | Requisito de AGP 8.13 |

## Matriz oficial y desviación conocida

- AGP 8.13 exige Gradle ≥ 8.13, JDK 17 y admite hasta API 36.1. **Cumple.**
- KGP 2.0.20–2.0.21 figura soportado oficialmente hasta Gradle 8.8 y AGP 8.5.
  El proyecto usa Gradle 8.14 + AGP 8.13.2: **fuera de matriz, pero verificado**.
  Mitigante: Gradle 8.14 embebe Kotlin 2.0.21 (misma versión del DSL), así que no
  hay desalineación de Kotlin DSL.
- Regla: **no subir Gradle ni AGP** sin actualizar antes Kotlin/KSP/Room/Hilt y
  sin volver a ejecutar la verificación completa de este documento.
- Actualización propuesta (no aplicada): Kotlin 2.3.21 + KSP 2.3.x + Room 2.8.x
  + Hilt actualizado. Requiere revisar código y no debe mezclarse con fixes.

## Repositorios (mirrors)

`settings.gradle.kts` usa un mirror de Google Maven con filtro de grupos:

- `https://maven.aliyun.com/repository/google` limitado a `com.android.*`,
  `androidx.*`, `com.google.android.*` y `com.google.firebase.*`.
- Después, repos oficiales: `gradlePluginPortal()`, `mavenCentral()`, `google()`.

Reglas:

- **No** añadir `https://maven.aliyun.com/repository/gradle-plugin` ni
  `repository/public` delante de los oficiales para plugins. Verificado: el
  mirror devuelve 502 en la ruta del plugin marker de KSP y Gradle aborta la
  resolución en vez de continuar al repositorio siguiente.
- Mantener los filtros `content { includeGroupByRegex(...) }`: sin ellos el
  mirror intercepta grupos que no aloja y rompe la resolución.
- Si se añade un grupo nuevo de Google Maven, ampliar el filtro del mirror.

## Firma release

- `signingConfigs.release` solo se asigna si el keystore existe.
- Sin keystore local, `assembleRelease` genera `app-release-unsigned.apk` y
  `build` no falla.
- Si `KEYSTORE_PATH` está definido pero el archivo no existe, el build **falla**
  con `GradleException` (no se ignora silenciosamente).
- En CI, las variables `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` y
  `KEY_PASSWORD` deben estar definidas; el workflow espera
  `app/build/outputs/apk/release/app-release.apk` (firmado).

## Prohibiciones

- No borrar `~/.gradle/caches/modules-2` como "solución". Para metadatos
  negativos usar `--refresh-dependencies`.
- No usar `@Suppress`, `--continue`, `-x` ni desactivar lint para hacer pasar
  el build.
- No cambiar lógica de negocio ni el formato de backup (`.sumadorbak`) al
  tocar configuración de build.
- No commitear `build/`, `.gradle/`, `local.properties`, `*.jks` ni logs.
- No bajar versiones de AGP/Gradle/Kotlin sin justificarlo con la matriz oficial.

## Verificación obligatoria antes de commit

```bat
.\gradlew.bat clean assembleDebug
.\gradlew.bat clean build --refresh-dependencies
```

Ambos deben terminar en `BUILD SUCCESSFUL`. Para cambios de repositorios o
versiones, repetir además la prueba de clon limpio con `GRADLE_USER_HOME` nuevo
(ver `BUILD.md`).

## Entorno Windows

- Si aparece `Could not open ... kotlin-dsl\scripts\...\metadata.bin`, el archivo
  suele estar corrompido con ceros (fallo de caché, no de red). Solución:
  detener daemons y borrar `~/.gradle/caches/<version>/kotlin-dsl`.
- `ANDROID_HOME` global puede apuntar a una ruta inexistente; `local.properties`
  manda. En CI o clon limpio, definir `ANDROID_HOME`/`ANDROID_SDK_ROOT` a un SDK
  válido.
