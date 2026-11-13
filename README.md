# Sumador – Cuenta billetes y monedas rápidamente

[![Versión](https://img.shields.io/badge/version-1.8.3-blue.svg)](https://github.com/Christianrvdv/Sumador/releases/tag/v1.8.3)
[![Licencia: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-7.0%2B-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/Material-3-757575)](https://m3.material.io)

**Sumador** es una aplicación para Android sencilla e intuitiva para contar billetes y monedas.
Está diseñada para comerciantes, cajeros y cualquier persona que maneje efectivo, ayudándote a llevar el control de los totales de forma rápida y precisa.

> ✨ **Última actualización (v1.8.3):** nuevas denominaciones de **10 000 y 20 000 pesos**, además de la **copia de seguridad y restauración manual** completa (cifrada), filtros avanzados en el historial, denominaciones personalizadas por moneda y soporte mejorado para monedas.

---

## 🚀 Características

| Característica | Descripción |
|---------|-------------|
| 💱 **Múltiples monedas** | Peso cubano ($), Dólar estadounidense (USD), Euro (€) – cambia al instante |
| 🪙 **Soporte para monedas y billetes** | Cada denominación se puede marcar como moneda o billete; se muestran con diferente formato |
| 🛠️ **Denominaciones personalizadas** | Añade, edita o elimina denominaciones **por moneda** – la app guarda tus listas personalizadas en Room |
| 🗂️ **Historial con filtros avanzados** | Busca por nombre, filtra por rango de fechas, rango de cantidades y ordena por fecha o nombre (ascendente/descendente) |
| 🔢 **Orden de las denominaciones** | Las denominaciones se pueden ordenar de forma ascendente o descendente (aplica tanto a billetes como a monedas) |
| 💾 **Auto‑guardado** | Las cantidades se guardan y restauran automáticamente al reiniciar la app **por moneda** (mediante DataStore) |
| 🌗 **Temas** | Claro, oscuro y predeterminado del sistema (con **color dinámico** en Android 12+) |
| 🌐 **Idiomas** | Inglés, español y el del sistema – cambia sin necesidad de reiniciar (la actividad se recrea) |
| 📤 **Compartir** | Comparte el total actual o cualquier suma guardada como un desglose detallado en texto |
| 📱 **Mantener pantalla activa** | Opción para evitar que la pantalla se apague mientras usas la app |
| 🧹 **Confirmación al limpiar** | Confirmación opcional antes de borrar todas las cantidades |
| 🔄 **Buscador de actualizaciones** | Buscador integrado que descarga nuevos APKs desde las versiones de GitHub usando WorkManager |
| 💾 **Copia de seguridad y restauración manual** | Exporta/importa todos los datos (ajustes, sumas guardadas, denominaciones personalizadas) mediante archivos **cifrados** `.sumadorbak` (AES‑CBC) |
| 🔒 **Seguridad de datos** | Los archivos de respaldo están cifrados con una clave fija para evitar manipulaciones |
| 📱 **Android 7.0+** | SDK mínimo 24 (Nougat) |

---

## 🛠️ Tecnologías

- **Kotlin** – 100% Kotlin
- **Jetpack Compose** – Kit de herramientas moderno para la interfaz de usuario
- **Material 3** – Último diseño Material con color dinámico
- **Room** – Base de datos local para sumas guardadas y denominaciones personalizadas (migración incluida a la versión 3)
- **DataStore** – Almacenamiento persistente para ajustes de usuario y cantidades auto‑guardadas
- **Hilt** – Inyección de dependencias
- **Coroutines & Flow** – Operaciones asíncronas e interfaz de usuario reactiva
- **WorkManager** – Descarga en segundo plano de actualizaciones (exenta de optimizaciones de batería)
- **OkHttp & Gson** – Solicitudes de red y análisis JSON para las versiones de GitHub
- **Cifrado AES‑CBC** – Utilizado para los archivos de copia de seguridad y restauración

---

## 📥 Instalación y compilación

### Requisitos previos
- Android Studio Narwhal 3 o superior (AGP 8.13)
- JDK 17
- SDK de Android (API mínima 24)

### Clonar el repositorio
```bash
git clone https://github.com/Christianrvdv/Sumador.git
cd Sumador
```

### Compilar y ejecutar
1. Abre el proyecto en Android Studio.
2. Espera a que finalice la sincronización de Gradle.
3. Conecta un dispositivo Android o inicia un emulador.
4. Haz clic en **Ejecutar** (▶) o usa la tarea de Gradle:

```bash
./gradlew installDebug
```

### Generar un APK / AAB firmado
```bash
./gradlew assembleRelease
```
La salida estará en `app/build/outputs/apk/release/`.

---

## 🎮 Uso

1. **Contar** – Pulsa el botón **+** junto a cada denominación para añadir billetes o monedas, o usa el botón **−** para eliminar.
2. **Total** – El total se actualiza en tiempo real en la parte inferior.
3. **Limpiar** – Pulsa el icono de la papelera para reiniciar todos los conteos (con confirmación si está activada).
4. **Guardar** – Guarda la suma actual en el historial con un nombre personalizado.
5. **Compartir** – Comparte la suma actual o cualquier suma guardada como texto.
6. **Historial** – Visualiza, busca, filtra (por fecha/cantidad), ordena, edita o elimina sumas guardadas.
7. **Ajustes** – Accede a temas, monedas, idioma, orden de las denominaciones, auto‑guardado, confirmación, mantener pantalla activa, activar monedas, buscador de actualizaciones y **copia de seguridad/restauración manual**.
8. **Gestionar denominaciones** – Desde Ajustes, pulsa “Gestionar denominaciones” para añadir, editar o eliminar valores de billetes y monedas para la moneda seleccionada. Los cambios se guardan por moneda.
9. **Copia de seguridad** – Usa los botones **Exportar** / **Importar** en Ajustes para crear o restaurar una copia de seguridad cifrada de todos tus datos (ajustes, historial, denominaciones personalizadas). Los archivos de respaldo tienen la extensión `.sumadorbak`.

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si tienes una idea, encuentras un error o quieres mejorar el código:

1. Haz un fork del repositorio.
2. Crea una nueva rama (`git checkout -b feature/tu-caracteristica`).
3. Confirma tus cambios (`git commit -m 'Añadir alguna característica'`).
4. Sube la rama (`git push origin feature/tu-caracteristica`).
5. Abre una solicitud de extracción (Pull Request).

Asegúrate de que tu código siga el estilo del proyecto e incluya las pruebas adecuadas.

---

## 📄 Licencia

Este proyecto está licenciado bajo la **GNU General Public License v3.0** – consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## 👨‍💻 Autor

**Christian R Vazquez**
- [GitHub](https://github.com/Christianrvdv)
- [Portfolio](https://christianrvdv.github.io/christianrvdv)

---

## ⭐ Apoyo

Si encuentras útil esta aplicación, considera darle una estrella ⭐ en GitHub para mostrar tu apoyo.

---

## 📦 Descarga

Puedes descargar el último APK desde la página de [Versiones](https://github.com/Christianrvdv/Sumador/releases).

---

*Hecho con ❤️ en Cuba.*