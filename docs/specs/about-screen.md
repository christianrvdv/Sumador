# Spec: pantalla «Acerca de» y compartir la app

Estado: **aprobada en Fase 1**, con decisiones y refinamientos A–D añadidos antes de la Fase 2. Implementación en curso.

## 1. Contexto verificado

- Módulo único `:app`, package `cu.christianrvdv.sumador`.
- Navegación: `MainActivity.kt` usa `NavHost` (Compose Navigation) con rutas `sumador`, `history`, `manage_denominations/{currency}`.
- «Ajustes» no es una pantalla: es `SettingsBottomSheet` en `ui/settings/SettingsDialog.kt:23`, abierto desde un `IconButton` del TopAppBar de `SumadorScreen.kt:219`.
- «Acerca de» hoy es **otro** `ModalBottomSheet`: `ui/settings/AboutBottomSheet.kt:24` (título, versión vía `packageManager` con fallback `1.8.2`, descripción, 9 características, GitHub, portafolio, cerrar). Sus enlaces hacen `startActivity` sin protección.
- Tema MD3: `ui/theme/Theme.kt:63` (`SumadorTheme`, esquemas claros/oscuros + dynamic color). Compose BOM `2024.09.00`, Material3, navigation-compose `2.7.7`.
- Gradle: `buildConfiguration`/`buildFeatures.buildConfig = true`, `versionCode 5`, `versionName "1.8.3"`, minSdk 24. Ya hay `ui-test-junit4` y `ui-test-manifest` en `androidTest`; no hay espresso-intents ni Robolectric. No se toca Gradle.
- Recursos: `res/values/strings.xml` = inglés (default), `res/values-es/strings.xml` = español. Decisión: **mantener esta convención**, no invertir.
- `material-icons-extended` **sí está** declarado en `app/build.gradle.kts:106` (`implementation("androidx.compose.material:material-icons-extended")`), así que se pueden usar `Icons.Default.Language` e `Icons.Default.OpenInBrowser`. `Icons.Default.Share` está en el set core. Verificado antes de escribir código (refinamiento B).
- La landing oficial ya existe en `docs/index.html` y declara `og:url = https://christianrvdv.github.io/Sumador/`.

## 2. Decisión A/B

| Opción | Evaluación |
|---|---|
| **A. Pantalla dedicada «Acerca de / Info» enlazada desde Ajustes** | **Aprobada.** About ya es un sheet dentro de otro sheet; una ruta propia permite back real, `TopAppBar`, scroll, targets táctiles amplios y semántica de navegación. Además da un único sitio para el enlace oficial y el share. |
| B. Ítem directo en Ajustes | Rechazada: el sheet de Ajustes ya es largo y añadir share + enlace lo recargaría y duplicaría el sheet de About existente. |

## 3. Contrato de UI y navegación

- Nuevo archivo: `ui/info/InfoScreen.kt` (convención `ui/<feature>/Screen.kt`).
- Composable: `InfoScreen(onBack: () -> Unit, versionName: String = BuildConfig.VERSION_NAME, onOpenOfficialPage: () -> Unit, onShareApp: () -> Unit)`.
- Ruta: `"about"`, registrada en el `NavHost` de `MainActivity.kt` con `InfoScreen(onBack = { navController.popBackStack() }, ...)`.
- Entradas:
  1. Ítem en `SettingsBottomSheet`: fila con `Icons.Default.Info`, label `settings_about_button`, `contentDescription` en el icono (`content_description_about`), `testTag("settings_about_item")`. El propio `SettingsBottomSheet` llama a `onDismiss()` **antes** de `onAboutClick()` para garantizar que no queden dos overlays apilados (refinamiento D). `SumadorScreen` solo pasa `onNavigateToAbout`.
  2. Icono Info del TopAppBar de `SumadorScreen`: pasar de `showAboutDialog = true` a la misma navegación, para no dejar dos «Acerca de» distintos.
- Retirada de `AboutBottomSheet.kt`: **eliminar** el archivo y su estado `showAboutDialog` (y sus imports) en `SumadorScreen`. No se deja código muerto.
- `Scaffold` con `TopAppBar(title = { Text(stringResource(R.string.about_title)) }, navigationIcon = IconButton(Icons.Auto.Mirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.info_back)))` y `SnackbarHost` para errores.
- Contenido en `Column` con `verticalScroll(rememberScrollState())`, **en este orden de prioridad** (decisión del usuario):
  1. Identidad: icono de la app + `about_title` + `about_version` (con `versionName` de `BuildConfig`) + descripción `about_description`.
  2. `Card` destacada «Página oficial» con `Icons.Default.Language`, título `info_official_page_title`, soporte `info_official_page_support` y `FilledTonalButton` `info_official_page_button` con `Icons.Default.OpenInBrowser` y label `info_open_page_button`.
  3. `FilledTonalButton` destacado `testTag("info_share_button")` con `Icons.Default.Share` y label `info_share_button`.
  4. Lista de características migrada de `AboutBottomSheet` con el **texto idéntico** al actual (`about_features_title` y los 9 `about_feature_*`), sin reescribir.
  5. Enlaces del desarrollador al final (GitHub con `Icons.Default.Code`, portafolio con `Icons.Default.OpenInBrowser`), conservando los textos actuales.
- `testTag` estables: `info_screen`, `info_share_button`, `info_official_page_button`, `info_version`, `settings_about_item`.

## 4. Contrato de URL / intents (refinamiento A)

- Fuente única: `object AppLinks { const val DOWNLOAD_URL = "https://christianrvdv.github.io/Sumador/" }` en `utils/AppLinks.kt` (testeable sin `Context`). La URL **no** se duplica en `strings.xml`.
- Abstracción inyectable en `ui/info/AppIntents.kt`:

```kotlin
interface AppIntents {
    fun openUrl(url: String): Boolean
    fun share(subject: String, text: String): Boolean
}
```

- Implementación Android (`AndroidAppIntents`):
  - **NO usar `resolveActivity` / `canResolveActivity` ni `<queries>`.** Con API 30+ (targetSdk 36), el package visibility filtra lo que `resolveActivity` puede ver y da falsos negativos incluso cuando el `startActivity` funcionaría.
  - `openUrl`: `try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))); true } catch (e: ActivityNotFoundException) { false }`.
  - `share`: construir `Intent.ACTION_SEND` con `type = "text/plain"`, `EXTRA_SUBJECT`, `EXTRA_TEXT`; `try { context.startActivity(Intent.createChooser(intent, ...)); true } catch (e: ActivityNotFoundException) { false }`. El chooser nativo muestra estado vacío si no hay apps; no hace falta pre-chequeo.
  - Se crea con `LocalContext.current` en el `NavHost` (`AppIntents` no es ViewModel; sin Hilt).
- La pantalla recibe lambdas puras (`onOpenOfficialPage`, `onShareApp`) cableadas en el `NavHost` a `AppLinks.DOWNLOAD_URL` y al texto de `stringResource`; los tests usan fakes.
- Texto del share: `stringResource(R.string.info_share_text, AppLinks.DOWNLOAD_URL)` (un único `%1$s`).

## 5. Microcopy exacto (contrato, sin cambios)

| Clave | Español (`values-es`) | Inglés (`values`, default) |
|---|---|---|
| `about_title` (reutilizar) | Acerca de Sumador | About Sumador |
| `settings_about_button` (reutilizar) | Acerca de | About |
| `content_description_about` (reutilizar) | Acerca de | About |
| `info_back` (nuevo) | Volver | Back |
| `info_official_page_title` (nuevo) | Página oficial | Official website |
| `info_official_page_support` (nuevo) | Novedades, descarga e información sobre la app. | News, download and information about the app. |
| `info_open_page_button` (nuevo) | Abrir página oficial | Open official website |
| `info_share_button` (nuevo) | Compartir aplicación | Share app |
| `info_share_chooser_title` (nuevo) | Compartir Sumador | Share Sumador |
| `info_share_subject` (nuevo) | Descarga Sumador | Download Sumador |
| `info_share_text` (nuevo, `%1$s` = URL) | Sumador te ayuda a contar billetes y monedas de forma rápida y sencilla. Descárgala aquí: %1$s | Sumador helps you count bills and coins quickly and easily. Download it here: %1$s |
| `info_no_browser` (nuevo) | No se encontró un navegador para abrir la página. | No browser was found to open the page. |
| `info_no_share_apps` (nuevo) | No hay aplicaciones disponibles para compartir. | No apps are available to share. |

`EXTRA_SUBJECT` = `Descarga Sumador`. `EXTRA_TEXT` = `info_share_text` con la URL exacta. Sin WebView.

## 6. Accesibilidad (MD3)

- Botones con `Modifier.heightIn(min = 48.dp)` (Material3 `Button` mide 40dp por defecto). Iconos de acción en `IconButton` (48dp), fila de ajustes con `minimumInteractiveComponentSize`.
- `contentDescription` en todos los iconos: back, página oficial, compartir, GitHub, portafolio, flecha de la fila de Ajustes. Iconos decorativos llevan `contentDescription = null`.
- Colores `onSurface` / `onSurfaceVariant` / `onPrimary` del `MaterialTheme.colorScheme`.
- Textos en `stringResource`; no hace falta `Role.Button` porque se usan componentes nativos.

## 7. Casos borde (refinamiento A)

1. Ajustes → «Acerca de» con el sheet abierto: el sheet se cierra en el propio `SettingsBottomSheet` antes de navegar (refinamiento D), verificado por test.
2. Back desde Info: `navController.popBackStack()`; no se usa `BackHandler` propio.
3. Sin navegador: `startActivity` lanza `ActivityNotFoundException` → `openUrl` devuelve `false` → Snackbar `info_no_browser`, sin crash. Sin `resolveActivity` (API 30+ package visibility).
4. Sin apps para compartir: el chooser puede quedar vacío o lanzar `ActivityNotFoundException`; el `try/catch` cubre el segundo caso → Snackbar `info_no_share_apps`, sin crash.
5. Versión: `BuildConfig.VERSION_NAME` (1.8.3); nunca el fallback `1.8.2`.
6. Rotación: el estado de la ruta sobrevive; Snackbar solo ante un nuevo fallo.
7. Idioma: textos en ambos `strings.xml`; el cambio de idioma ya recrea la actividad.
8. **Scope +1**: `shareCurrentSum()` en `SumadorScreen.kt:937` recibe el mismo `try/catch ActivityNotFoundException` (sin `resolveActivity`) para no crashear al compartir una suma.

## 8. Criterios de aceptación

1. Ruta `about` accesible desde el ítem de Ajustes y desde el icono Info del TopAppBar.
2. Pantalla muestra nombre, versión de `BuildConfig`, página oficial, compartir y todo el contenido migrado en el orden acordado.
3. «Abrir página oficial» dispara `ACTION_VIEW` con `AppLinks.DOWNLOAD_URL`; excepción → Snackbar, sin crash.
4. «Compartir aplicación» dispara `ACTION_SEND` con `type=text/plain`, `EXTRA_SUBJECT=Descarga Sumador`, `EXTRA_TEXT` con la URL; excepción → Snackbar, sin crash.
5. URL definida una sola vez en `AppLinks`; compartir y abrir usan la misma constante.
6. Touch targets >= 48dp, contentDescriptions y textos MD3.
7. Textos en ambos `strings.xml`; sin WebView; sin cambios en el flujo de conteo.
8. Tests de navegación/ítem (incluido el cierre previo del sheet), render y fakes de share/openUrl.

## 9. Decisiones aprobadas

1. Idioma: mantener `values/` inglés + `values-es/` español. **No invertir**.
2. `AboutBottomSheet`: eliminar archivo y estado; migrar todo su contenido a `InfoScreen`; sin código muerto.
3. Contenido migrado completo, reorganizado por prioridad (identidad → página oficial → compartir → características → enlaces desarrollador).
4. Refinamientos A (try/catch, sin resolveActivity), B (iconos: extended presente), C (fix scope+1 en `shareCurrentSum`), D (test de cierre del sheet antes de navegar).

## 10. Plan de tests

- `src/test/.../AppLinksTest.kt` (JUnit puro): URL exacta.
- `src/androidTest/.../InfoScreenTest.kt` (Compose UI test con fakes): render de título/versión; click share → fake recibe subject y texto con la URL; click página → fake recibe la URL; testTags.
- `src/androidTest/.../SettingsAboutNavigationTest.kt`: el ítem «Acerca de» invoca `onAboutClick` **después** de que el host haya cerrado el sheet (assert dentro del callback), sin overlays apilados.
- `connectedAndroidTest` requiere dispositivo; si no lo hay, se reporta como **no ejecutado**.

## 11. Fuera de alcance

Gradle, WebView, flujo de conteo (salvo el `try/catch` scope+1 de `shareCurrentSum`), backup/import, WorkManager, versionado/README.

## 12. Riesgos

- Al eliminar `AboutBottomSheet` hay que limpiar su estado y sus imports en `SumadorScreen.kt`.
- Tests instrumentados sin emulador: no ejecutables; se reporta honestamente.
- El chooser vacío no lanza excepción en algunos fabricantes; se acepta el estado vacío nativo, sin `<queries>`.
