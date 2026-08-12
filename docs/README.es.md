[English](README.md) | [简体中文](../README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md) | [Português (BR)](README.pt-BR.md) | **Español**

---

# LowFace - Demo de Reconocimiento Facial Ligero

> Una aplicación de reconocimiento facial diseñada específicamente para **dispositivos Android de gama baja**, implementada con XML/View nativo para verificar la viabilidad en hardware con recursos limitados.

## Sobre LowFace
* LowFace: Reconocimiento Facial Eficiente en Dispositivos de Gama Baja

## Origen del Proyecto

Este proyecto está desarrollado basándose en [Simprints Face Biometrics SDK](https://github.com/Simprints/Biometrics-SimFace), manteniendo las capacidades principales de reconocimiento facial mientras reescribe completamente la capa de UI:

- **Proyecto Original**: Construido con Jetpack Compose para UI moderna
- **Este Proyecto**: Construido con XML/View nativo, optimizado para dispositivos de gama baja

## Funcionalidades

- Entrada de ID de empleado/nombre
- Inscripción facial (captura automática cuando se alcanza el umbral de calidad)
- Reconocimiento facial (coincidencia 1:N)
- Visualización de cuadro delimitador facial en tiempo real
- Indicación de puntuación de calidad

## Parámetros Principales

| Parámetro | Valor | Descripción |
|-----------|-------|-------------|
| Umbral de Calidad | 0.4 | Umbral de juicio de calidad facial |
| Umbral de Coincidencia | 0.85 | Umbral de coincidencia 1:N |
| Dimensión de Features | 512 | Dimensión del embedding de salida EdgeFace |

## Stack Tecnológico

- **UI**: XML/View nativo (sin Compose)
- **Cámara**: CameraX + PreviewView
- **Detección Facial**: Google ML Kit (vía SimFace SDK)
- **Extracción de Features**: Modelo EdgeFace TFLite
- **Lenguajes**: Java + Kotlin (solo capa SDK)

## Estructura del Proyecto

```
lowFace/
├── app/                         # Módulo principal de aplicación
│   └── src/main/java/com/low/face/
│       ├── FaceDemoActivity.java       # Activity principal
│       ├── FaceCameraActivity.java     # Activity de cámara
│       ├── FaceEngineManager.java      # Operaciones principales de rostro
│       ├── FaceEngineSingleton.java    # Gestor singleton
│       ├── FaceStore.java              # Almacenamiento en memoria
│       ├── FaceRecord.java             # Modelo de datos
│       ├── OverlayView.java            # View de overlay facial
│       └── utils/SimFaceWrapper.kt     # Wrapper Kotlin
├── simface/                     # SDK principal de reconocimiento facial
└── simq/                        # Biblioteca de evaluación de calidad facial
```

## Compilar y Ejecutar

### Requisitos

- JDK 17+
- Android SDK 33+
- Gradle 9.6.1+

### Comandos de Compilación

```powershell

# Entrar al directorio del proyecto
cd lowFace

# Verificación de compilación
.\gradlew.bat compileDebugJavaWithJavac

# Compilar Debug APK
.\gradlew.bat assembleDebug
```

### Instalar y Probar

```powershell
# Instalar en dispositivo
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Ver logs
adb logcat -s FaceDemo:* FaceEngine:* FaceCamera:*
```

---

## Adaptación para Dispositivos de Gama Baja (Enfoque Principal)

### Especificaciones del Dispositivo Objetivo

Este proyecto está optimizado para dispositivos de gama baja con las siguientes especificaciones:

| Item | Especificación |
|------|----------------|
| CPU | MediaTek MT6762 (4 núcleos 2.0GHz) |
| RAM | 2GB |
| Almacenamiento | 32GB |
| Versión Android | 10-11 |

### ¿Por qué no Compose?

Jetpack Compose tiene los siguientes problemas en dispositivos de gama baja:

1. **Carga Inicial Lenta**: Inicialización del runtime Compose + primera recomposición toma 200-500ms
2. **Alto Uso de Memoria**: La biblioteca base de Compose es aproximadamente 2-3MB, una carga para dispositivos con 2GB RAM
3. **Latencia de Entrada**: Recomposición compleja puede causar lag en campos de entrada
4. **Cold Start Largo**: Tiempo desde tocar el ícono hasta estado interactivo es mayor

Este proyecto elige **XML/View nativo**:

- Cero sobrecarga de dependencias extras
- Optimización de renderizado a nivel de sistema
- Respuesta de entrada más directa
- Menor huella de memoria

### Optimizaciones de Rendimiento

#### 1. Optimización de Procesamiento de Imagen

| Optimización | Solución | Efecto |
|--------------|----------|--------|
| Conversión Bitmap | Conversión directa RGBA_8888, saltar YUV→JPEG→decode | Ahorrar ~20ms |
| Liberación ImageProxy | Cerrar inmediatamente después de conversión Bitmap, antes de detección | Evitar bloqueo del pipeline de cámara |
| Escalado de Imagen | Imagen de análisis limitada a 480×640 | resizeBitmap toma 0ms |

#### 2. Throttling de Detección

- Intervalo de detección: **800ms**
- Usar `AtomicBoolean` para prevenir detección concurrente
- Frames no detectados se cierran inmediatamente, no consumen CPU

#### 3. Reutilización de Resultados

Reutilizar resultados de detección de frames de preview durante captura automática para evitar detección redundante:

```
Antes: Detección preview → Captura automática → Redetección(400ms) → Extracción de features
Después: Detección preview → Captura automática → Extracción de features directa
```

Ahorra **400-500ms**.

### Datos de Rendimiento en Dispositivo Real

#### Cold Start (Primera Ejecución)

| Etapa | Duración |
|-------|----------|
| setContentView | 217-248ms |
| Inicialización de cámara | 267-278ms |
| bindToLifecycle | 278ms |
| Llegada del primer frame | 1200-1400ms desde onCreate |
| Primera detección facial | 1000-1100ms |
| Primera extracción de features | 900-950ms |

#### Operación Estable (Después de Calentamiento)

| Etapa | Duración |
|-------|----------|
| Detección facial | 400-530ms |
| Alineamiento facial | 100-130ms |
| Extracción de features | 90-100ms |
| Coincidencia 1:N (10 personas) | 10-15ms |
| Procesamiento post captura automática | ~230ms |

---

## Limitaciones Actuales

### 1. Velocidad de Detección Limitada

- **Causa**: Detección facial ML Kit toma 400-500ms/frame en CPU de gama baja
- **Impacto**: No se puede lograr detección fluida frame-a-frame en tiempo real
- **Estado Actual**: Usando solución de throttling 800ms + captura automática

### 2. Cold Start Lento

- **Causa**: Carga de modelo, inicialización OpenCV, escalado de frecuencia CPU
- **Impacto**: Respuesta lenta para primera inscripción/reconocimiento
- **Estado Actual**: Aún no hay solución perfecta, se recomienda calentamiento

### 3. Almacenamiento en Memoria

- **Estado Actual**: Datos inscritos almacenados solo en memoria
- **Impacto**: Necesidad de reinscribir después de reiniciar la app
- **Plan**: Versión futura soportará almacenamiento persistente

### 4. Sin Detección de Vivacidad

- **Estado Actual**: Reconocimiento basado solo en fotos
- **Riesgo**: Puede ser engañado por fotos
- **Plan**: Necesario integrar solución de detección de vivacidad

### 5. Soporte de Una Cámara

- **Estado Actual**: Solo cámara frontal
- **Impacto**: Puede ser inconveniente en algunos escenarios
- **Plan**: Soporte futuro para cambio de cámara

### 6. Experiencia de Entrada No Completamente Verificada

- **Estado Actual**: Si los campos de entrada permanecen fluidos después de inicialización del SDK no completamente verificado
- **Riesgo**: Posible latencia de entrada en dispositivos de gama baja
- **Sugerencia**: Necesario más pruebas para timing "foco ganado → primer carácter ingresado"

---

## Comparación con Versión Original Compose

| Item | Original (Compose) | Este Proyecto (XML/View) |
|------|-------------------|--------------------------|
| Framework UI | Jetpack Compose | XML/View Nativo |
| Carga primera pantalla | Más lento | Más rápido |
| Uso de Memoria | Mayor | Menor |
| Respuesta de Entrada | Puede laggear | Más fluido |
| Eficiencia de Desarrollo | Alta | Media |
| Costo de Mantenimiento | Bajo | Medio |

---

## Direcciones de Optimización Futura

1. **Almacenamiento Persistente**: Usar SQLite o SharedPreferences para guardar rostros inscritos
2. **Detección de Vivacidad**: Integrar detección de parpadeo/apertura de boca
3. **Calentamiento de Cámara**: Pre-calentar cámara y modelo en background en la activity principal
4. **Cámara Trasera**: Soportar cambio de cámara frontal/trasera
5. **Inscripción en Lote**: Soportar inscribir múltiples personas a la vez
6. **Aceleración NPU**: Utilizar NPU para aceleración de inferencia si el dispositivo lo soporta

---

## Licencia

El SDK principal (`simface`, `simq`) sigue la licencia del proyecto original.

El código de la capa de aplicación está licenciado bajo MIT License, libre para usar y modificar.

---

## Agradecimientos

- [Simprints](https://simprints.com/) - Por el SDK de reconocimiento facial open source
- [Google ML Kit](https://developers.google.com/ml-kit) - Capacidad de detección facial
- [EdgeFace](https://github.com/SeetaFace6Open/SeetaFace6Open) - Modelo de extracción de features

---

## Valor y Significado del Proyecto

En la tecnología de reconocimiento facial en rápido desarrollo hoy, muchas soluciones por defecto corren en dispositivos inteligentes de gama media-alta o servidores en la nube. Sin embargo, aún existen muchos escenarios de uso con recursos limitados: dispositivos sensibles al costo, condiciones de red limitadas, recursos de computación insuficientes, pero aún necesitando capacidades básicas de autenticación de identidad.

El objetivo de LowFace no es buscar la más alta precisión de reconocimiento en ambientes de laboratorio, sino explorar **lograr capacidades de reconocimiento facial utilizables en dispositivos Android de gama baja**, permitiendo que más dispositivos existentes tengan capacidades de autenticación digital.

Para muchos países en desarrollo, áreas remotas y empresas sensibles al costo, muchos escenarios de verificación de identidad no requieren sistemas de reconocimiento facial de nivel financiero o de seguridad, sino que necesitan una solución ligera que sea:

- Bajo costo
- Puede correr offline
- Baja dependencia de red
- Puede desplegarse en dispositivos existentes

Ejemplos incluyen:

- Control de asistencia interno empresarial y registro de empleados
- Gestión de personal en pequeñas organizaciones
- Confirmación de identidad en escenarios de capacitación educativa
- Control de acceso básico y autorización de dispositivos
- Verificación de identidad en servicios comunitarios o de base

Estos escenarios se enfocan más en "confiabilidad y facilidad de despliegue" en lugar de buscar métricas máximas de reconocimiento en ambientes extremos.

Al mismo tiempo, LowFace también se enfoca en extender el ciclo de vida de dispositivos electrónicos. Muchos dispositivos Android antiguos no pueden correr aplicaciones modernas debido a rendimiento insuficiente, pero sus cámaras, pantallas y capacidades básicas de computación aún pueden cumplir muchos requisitos de tareas ligeras. A través de optimización para hardware de gama baja, estos dispositivos pueden continuar creando valor y reducir la generación de residuos electrónicos.

Desde una perspectiva ambiental, traer dispositivos antiguos de vuelta a escenarios de producción y servicio es esencialmente una forma de reutilización de recursos:

- Reducir necesidades de compra de nuevo hardware
- Extender ciclo de uso de dispositivos
- Reducir residuos electrónicos
- Bajar costos de construcción de infraestructura digital

LowFace espera explorar un enfoque técnico más inclusivo:

> No actualizar todos los dispositivos a hardware de alto rendimiento, sino permitir que más dispositivos existentes continúen creando valor a través de optimización de software.
> Capacidades avanzadas no deberían pertenecer solo a dispositivos de alto rendimiento, sino servir más escenarios reales a menor costo y más ampliamente.

Este es el significado de optimización de dispositivos de gama baja, reconocimiento facial ligero y tecnología de AI en el borde en el mundo real.
