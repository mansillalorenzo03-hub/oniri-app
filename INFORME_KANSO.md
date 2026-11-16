# Informe Completo de la Aplicación Android **Kanso**

**Kanso** (`com.kanso.focus`) es una aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose** (Material 3) diseñada con dos propósitos principales:
1. **Concentración y Bienestar Digital:** Bloqueo real de aplicaciones distractoras y silenciamiento selectivo de notificaciones organizado por perfiles personalizados.
2. **Accesibilidad Cognitiva y Visual:** Lector y exportador de documentos PDF/TXT adaptado para personas con **Dislexia** y **Poca Visión**, con síntesis de voz multitimbre.

---

## 1. Módulos y Funciones Principales

### 1.1. Sistema de Perfiles Estilo Netflix con Fotos Personalizadas
- **Pantalla de Bienvenida de Perfiles:** Al abrir la aplicación, se presenta una cuadrícula visual de perfiles (*Estudio*, *Trabajo*, *Descanso* o cualquier perfil creado por el usuario).
- **Imagen Personalizada por Perfil:**
  - Cada tarjeta de perfil tiene un botón flotante de **cámara** que permite elegir cualquier foto de la galería del teléfono.
  - Al crear un perfil nuevo, también se puede elegir su foto desde la galería.
  - La imagen se recorta en formato cuadrado centrado (`280x280 px`) y se guarda en el almacenamiento interno privado de la app (`filesDir/profile_avatars/`) para cargar en **0 ms** y mantenerse siempre disponible.
- **Creación y Eliminación Libre:** Desde el botón **"Administrar / Borrar perfiles"**, el usuario puede eliminar cualquier perfil existente (siempre que quede al menos 1 activo) o crear nuevos.
- **Configuración Independiente:** Cada perfil guarda su propia lista de aplicaciones bloqueadas (`restrictedPackages`) y su propia lista de aplicaciones silenciadas (`mutedPackages`).

---

### 1.2. Navegación Fluida con Menú de 3 Barritas (`☰`) y Optimización de Velocidad
- **Menú Hamburguesa (`ModalNavigationDrawer`):** En la esquina superior izquierda se ubica el botón de **3 barritas (`☰`)**, que despliega un panel lateral moderno desde donde se alterna entre:
  - **Modo Enfoque** (Bloqueo de apps y notificaciones)
  - **Lector Adaptado** (Dislexia, Poca Visión y 5 Voces)
  - **Cambiar de Perfil** (acceso directo a la pantalla estilo Netflix)
- **Precarga Asíncrona en Caché (`0 ms` de espera):**
  - Mientras el usuario visualiza la pantalla de perfiles al abrir la app, `FocusSessionManager` precarga en segundo plano (`Dispatchers.IO`) el catálogo de aplicaciones instaladas y convierte sus iconos a bitmaps compactos de `96x96 px` en memoria.
  - Así, al tocar **"Bloquear Apps"** o **"Notificaciones"**, la cuadrícula de 3 columnas se abre instantáneamente sin tirones ni tiempos de carga.

---

### 1.3. Modo Enfoque: Control de Encendido y Selección en Cuadrícula de 3 Columnas
1. **Tarjeta Principal de Encendido/Apagado (*Power Hero Card*):**
   - Incluye un **botón circular central tipo Power (`ON` / `OFF`)** y un interruptor superior (`Switch`).
   - **Modo Libre:** Permite encender y apagar la sesión de concentración en cualquier momento con un toque.
   - **Modo con Temporizador Estricto (*Sin salida*):** Permite definir los minutos de estudio/trabajo. Una vez activado, bloquea la cancelación hasta que la cuenta regresiva llegue a `00:00`.
2. **Dos Cuadrados Separados para Apps y Notificaciones:**
   - **Cuadrado "Bloquear Apps":** Abre un selector exclusivo con todas las aplicaciones en **cuadrados distribuidos en 3 columnas**, mostrando el **icono real** de cada aplicación arriba y su **nombre** abajo. Tocar un cuadrado activa o desactiva el bloqueo al instante.
   - **Cuadrado "Notificaciones":** Abre un selector independiente en **3 columnas** para elegir qué aplicaciones tendrán sus notificaciones silenciadas durante la sesión.
3. **Programador Diario Automático:**
   - Permite programar una hora fija del día mediante `AlarmManager.setExactAndAllowWhileIdle` y `ScheduleReceiver` para que el enfoque arranque automáticamente todos los días.

---

### 1.4. Motor Dual de Bloqueo Real y Silenciador de Notificaciones
- **Asistente de Permisos Integrado:** Si el usuario intenta iniciar el bloqueo sin haber concedido los permisos especiales de Android, Kanso abre un panel guiado con estado en vivo para activar:
  1. **Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`):** Permite superponer la pantalla de bloqueo.
  2. **Acceso de Uso (`PACKAGE_USAGE_STATS`):** Detecta en tiempo real qué aplicación está en primer plano cada `450 ms`.
  3. **Servicio de Accesibilidad (`FocusAccessibilityService`):** Detecta cambios de ventana, ejecuta `GLOBAL_ACTION_HOME` para sacar al usuario de la app restringida y lanza `BlockOverlayActivity`.
  4. **Acceso a Notificaciones (`FocusNotificationListener`):** Intercepta y cancela (`cancelNotification`) cualquier notificación entrante de las apps silenciadas.

---

### 1.5. Lector y Exportador Accesible (Dislexia, Poca Visión y 5 Voces)
Ubicado en `AdaptiveReaderScreen.kt` y `PdfExportHelper.kt`:

- **3 Modos de Adaptación:**
  - **Solo Dislexia:** Lectura biónica resaltando el 45% inicial (raíz) de cada palabra en negrita para guiar la fijación ocular.
  - **Poca Visión:** Escala tipográfica ampliada y todos los números resaltados un **15% más grandes en negrita**.
  - **Ambas:** Combina lectura biónica + resaltado numérico + escala ampliada.
- **Importación PDF y TXT:** Carga cualquier archivo `.pdf` o `.txt` del teléfono usando `PDFBox-Android` (`PDFTextStripper`) y divide el texto automáticamente en **micro-bloques cortos numerados** para evitar fatiga visual.
- **5 Perfiles de Voz (`TextToSpeech`):**
  1. **1. Humana (`es-MX`):** Voz natural cálida (Tono `100%`).
  2. **2. Mujer (`es-ES`):** Voz femenina suave y clara (Tono `142%`).
  3. **3. Grave (`es-US`):** Voz profunda y firme (Tono `58%`).
  4. **4. Narrador (`es-AR`):** Voz barítono pausada para lectura guiada (Tono `76%`, cadencia `0.78x`).
  5. **5. Juvenil (`es-ES`):** Voz aguda y dinámica (Tono `166%`, cadencia `1.15x`).
  - Incluye además selector de velocidad (`1.0x`, `1.25x`, `1.5x`, `1.75x`).
- **Controles Visuales:** Botones **`A-` / `A+`** para ajustar el tamaño de fuente en vivo y botón de **Alto Contraste Amarillo Óptico (`#FFD600` sobre fondo negro `#000000`)**.
- **Exportador de PDF Adaptado:** Genera un nuevo archivo PDF con todas las adaptaciones visuales aplicadas y permite activar el interruptor **"Máx. 3 hojas"**, que calcula mediante búsqueda binaria el tamaño de letra óptimo para que todo el documento entre en un máximo de 3 páginas A4.
