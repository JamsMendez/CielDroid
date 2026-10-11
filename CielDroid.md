# CielDroid — Especificación para agentes de programación

> Documento de arranque para Codex, OpenCode, Pi y otros agentes. Implementar el proyecto, no solo explicar cómo hacerlo.

## Identidad y objetivo

- **Nombre:** CielDroid (no CieAndroid).
- **Repositorio de referencia:** https://github.com/JamsMendez/CieLinux
- **Sistema:** Android 15 en adelante, `minSdk = 35`.
- **Stack:** Kotlin, Jetpack Compose, Material 3, Coroutines/StateFlow, DataStore, Gradle Kotlin DSL.
- **SDK:** preferir `compileSdk` y `targetSdk` 36 o superior si el entorno y las políticas actuales lo requieren; verificar versiones estables compatibles. No bajar `minSdk` de 35.
- **Paquete provisional:** `com.cieldroid` (confirmar antes de publicar).

Crear una aplicación Android nativa con tres modos independientes y configurables: **Live Wallpaper animado**, **Scene Mini Widget** en el launcher y **Scene Mini Overlay** flotante sobre otras apps. Reutilizar con máxima fidelidad las escenas `processing`, `explorer`, `idle` y `raphael` del proyecto fuente. Cada modo puede seleccionar una escena diferente y funcionar independientemente de los otros.

## Instrucciones obligatorias para el agente

1. Inspeccionar primero el código REAL de CieLinux: `README.md`, `scenes/`, `src/`, `assets/`, `tests/`, contratos de eventos, variantes mini/wallpaper, recursos y dependencias. Crear `docs/source-audit.md` con rutas y hallazgos; no inferir implementación desde el README solamente.
2. Crear **un proyecto separado** CielDroid. No modificar ni romper CieLinux. Copiar solo recursos reutilizables, revisando licencia, atribución y dependencias.
3. Implementar por fases, con código compilable, pruebas y commits pequeños cuando corresponda. No llamar funcional a un placeholder. No afirmar pruebas o builds que no se ejecutaron.
4. Si falta SDK, emulador o dispositivo, dejar comandos de instalación/verificación y reportar bloqueos, sin inventar resultados.
5. Crear `README.md`, `AGENTS.md`, `docs/architecture.md`, `docs/renderer-decision.md`, `docs/test-plan.md` y registrar decisiones relevantes.
6. No añadir permisos ni servicios persistentes innecesarios. Pedir permisos especiales en contexto y ofrecer desactivación visible.

## Alcance y criterios funcionales

### A. Live Wallpaper

- Implementar `WallpaperService` y `WallpaperService.Engine`, declarados correctamente en manifest y XML de wallpaper.
- Animar una escena real detrás de los iconos; permitir seleccionar escena, FPS 30 por defecto/60 opcional, escala y vista previa del sistema.
- Gestionar `onVisibilityChanged`, superficie creada/redimensionada/destruida, rotación, preview, instancias múltiples y liberación de recursos.
- **Riesgo crítico:** una WebView normal NO se conecta directamente a `WallpaperService.SurfaceHolder`. Antes de elegir renderer, realizar un prototipo y documentar comparación entre Canvas/OpenGL ES, port parcial del compositor y técnicas offscreen verificadas. No usar capturas bitmap a 60 FPS sin medir consumo y latencia. Mantener la lógica y visuales JS cuando sea técnicamente viable.
- Criterio de aceptación: wallpaper animado seleccionable desde Android, que pausa al no estar visible y sobrevive a recreación del engine. Una imagen estática no cumple.

### B. Scene Mini Overlay

- Implementar `WindowManager` + `TYPE_APPLICATION_OVERLAY` y WebView transparente si la auditoría valida compatibilidad HTML/JS.
- Solicitar `SYSTEM_ALERT_WINDOW` solo al activar la función; verificar `Settings.canDrawOverlays` al volver de Ajustes.
- Iniciar/detener desde acción explícita del usuario, con servicio y notificación adecuados a Android 15+; investigar tipos válidos de foreground service, restricciones de inicio en background y políticas de Play. No declarar `specialUse` sin justificación comprobable.
- Escena independiente, 30/60 FPS, tamaño en dp, opacidad, posición persistente, arrastrar/mover y opción de detener.
- Posiciones predefinidas: `top-left`, `top-center`, `top-right`, `right-center`, `bottom-right`, `bottom-center`, `bottom-left`, `left-center`.
- Modo interactivo para arrastrar; paso de toques solo cuando sea permitido por reglas Android de ventanas no confiables y opacidad. No prometer click-through universal.
- Manejar revocación del permiso, proceso terminado, orientación, insets y cierre sin fugas.
- Criterio: overlay animado sobre otra app, movible y totalmente desactivable.

### C. Scene Mini Widget

- Usar `AppWidgetProvider`/`RemoteViews` o Jetpack Glance. Los widgets Android **no pueden contener WebView arbitraria** ni garantizar 30/60 FPS.
- Mostrar imagen/frame representativo de la escena, actualizado por cambios de configuración, tamaño, interacción o eventos relevantes; no simular animación con actualizaciones excesivas.
- Cada instancia se configura por `appWidgetId`: escena, apariencia, escala. Soportar múltiples widgets diferentes, redimensionado, reinicio y eliminación.
- Generar/cachar imágenes fuera del hilo principal, con límites de memoria y frecuencia.
- Criterio: widget agregable desde launcher, redimensionable y persistente, con dos instancias independientes.

## Arquitectura

```text
CielDroid/
├── CielDroid.md
├── AGENTS.md
├── README.md
├── docs/
│   ├── source-audit.md
│   ├── architecture.md
│   ├── renderer-decision.md
│   ├── permissions-and-lifecycle.md
│   └── test-plan.md
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── assets/scenes/           # submódulo git CielScenes (escenas compartidas)
│   ├── java/com/cieldroid/
│   │   ├── MainActivity.kt
│   │   ├── ui/                  # Compose, pantallas, previews
│   │   ├── domain/              # modelos y comandos
│   │   ├── data/                # DataStore, repositorios
│   │   ├── scenes/              # catálogo, runtime, puente JS
│   │   ├── wallpaper/           # WallpaperService + renderer
│   │   ├── overlay/             # Service, WindowManager, gestos
│   │   ├── widget/              # Receiver/Glance, snapshots
│   │   └── alerts/              # eventos tipados
│   └── res/
├── app/src/test/
├── app/src/androidTest/
└── gradle/, settings.gradle.kts, build.gradle.kts
```

Separar `SceneManager` y `SettingsRepository` de los adaptadores por superficie. No compartir un WebView singleton entre servicios con distintos ciclos de vida. Usar StateFlow para estado observable y DataStore para preferencias. WorkManager solo para trabajo diferido adecuado, nunca para bucles de renderizado.

### Contrato propuesto de escenas (adaptar tras auditoría)

```typescript
type SceneId = 'processing' | 'explorer' | 'idle' | 'raphael';
type SceneCommand =
  | { type: 'selectScene'; scene: SceneId }
  | { type: 'setFpsCap'; fps: 30 | 60 }
  | { type: 'pause' }
  | { type: 'resume' }
  | { type: 'setViewport'; width: number; height: number; density: number }
  | { type: 'alert'; level: 'warning' | 'failed'; durationMs: number };
```

Para HTML local usar orígenes controlados y `WebViewAssetLoader` cuando aplique. No habilitar puentes JavaScript para contenido remoto/no confiable. Validar mensajes, evitar navegación externa y liberar el runtime al destruirse.

## UI y configuración

Pantallas mínimas: Inicio/estado de los tres modos; Catálogo de escenas y preview; Ajustes Wallpaper; Ajustes Overlay; Configuración Widget por instancia; Acerca de/diagnóstico. Configuraciones independientes por superficie: escena, FPS (solo superficies animadas), tamaño/escala, opacidad y posición donde apliquen. Defaults: 30 FPS, overlay apagado, sin permisos concedidos por defecto. Wallpaper se establece mediante acción explícita y UI del sistema.

## Eventos y alertas

CieLinux documenta alertas y sonidos. Auditar su implementación y crear un bus local de eventos tipados; implementar primero eventos internos. Servidor HTTP local compatible con CieLinux es **opcional y posterior**, desactivado por defecto; si se hace, usar solo loopback, autenticación robusta y ninguna exposición LAN. No copiar tokens ni rutas de Linux. No solicitar acceso a notificaciones ajenas sin un caso de uso específico.

## Seguridad y compatibilidad Android 15+

- `BIND_WALLPAPER` es un permiso protegido declarado en el servicio para que el sistema se enlace; no se solicita como permiso runtime.
- `SYSTEM_ALERT_WINDOW` requiere autorización especial del usuario.
- `POST_NOTIFICATIONS` si se muestran notificaciones y corresponde; la denegación no debe producir un fallo.
- Foreground services: declarar solo tipos/permisos legítimos y compatibles; cumplir restricciones de Android 15+, de arranque en segundo plano y de publicación.
- Sin servicios innecesarios al arranque; sin overlays invisibles que intercepten toques; sin tráfico de red no requerido.
- Probar con Android 15 (API 35) y al menos una versión posterior disponible.

## Fases de ejecución

### Fase 0 — Investigación y bootstrap

- Clonar/leer CieLinux y producir auditoría concreta.
- Crear proyecto Android compilable con Compose, CI básica, README y AGENTS.md.
- Identificar licencias y dependencias de assets.
- **Entrega:** app abre y muestra pantalla inicial, `assembleDebug` funciona.

### Fase 1 — Motor y previews

- Portar assets, adaptar rutas y construir Scene Runtime para Android.
- Renderizar las cuatro escenas en pantalla de preview WebView, probar JS/CSS/Canvas/WebGL según aplique.
- Implementar comandos de escena, FPS, pausa/reanudación y persistencia.
- **Entrega:** preview funcional y comparativa visual documentada.

### Fase 2 — Overlay

- Permisos, servicio, ventana transparente, posición/escala, Start/Stop, recuperación.
- **Entrega:** overlay funcional sobre otras aplicaciones, sin fugas.

### Fase 3 — Live Wallpaper

- Prototipo y decisión de renderer basada en evidencia; implementar servicio, preview, render, lifecycle y control FPS.
- **Entrega:** wallpaper realmente animado y estable. Si el renderer no es viable, reportar bloqueo y alternativa, no marcar fase terminada.

### Fase 4 — Widget

- Widget redimensionable, snapshots, actualización controlada y preferencias por instancia.
- **Entrega:** múltiples widgets configurables, restaurables y estables.

### Fase 5 — Calidad y distribución

- Pruebas unitarias/instrumentadas, rotación, reinicio, permisos revocados, cambios de launcher, estrés, consumo CPU/GPU/batería y accesibilidad.
- APK debug, build release reproducible y documentación de firma/distribución.

## Validación y definición de terminado

Ejecutar cuando el entorno lo permita:

```bash
./gradlew --version
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
# Si hay dispositivo/emulador:
./gradlew :app:connectedDebugAndroidTest
adb devices
```

Registrar resultados reales, errores y capturas. Validar por separado: preview de las cuatro escenas, overlay sobre otra app, wallpaper animado en launcher, widget redimensionable y dos instancias con distinta escena; pausas al ocultar, revocación de permisos, recreación del proceso, rotación y memoria estable. No afirmar compatibilidad con todos los launchers o fabricantes sin pruebas.

## Primera tarea para el agente

**Comienza por Fase 0 y Fase 1.** Inspecciona https://github.com/JamsMendez/CieLinux, escribe `docs/source-audit.md`, crea el proyecto Android 15+ compilable y logra mostrar las escenas originales en una pantalla de vista previa. Después informa exactamente qué archivos y APIs de CieLinux pueden reutilizarse, qué partes requieren port y qué estrategia de Live Wallpaper resulta técnicamente viable. Continúa con las fases posteriores solo cuando las dependencias estén validadas; pregunta únicamente si hay una decisión que no puedas resolver de forma segura.

## Fuentes de referencia

- Código fuente: https://github.com/JamsMendez/CieLinux
- Android WallpaperService: https://developer.android.com/reference/android/service/wallpaper/WallpaperService
- Android app widgets: https://developer.android.com/develop/ui/views/appwidgets
- Android overlays: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- Android foreground services: https://developer.android.com/develop/background-work/services/fgs
- WebViewAssetLoader: https://developer.android.com/reference/androidx/webkit/WebViewAssetLoader
