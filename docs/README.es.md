[English](README.md) | [简体中文](../README.md) | [Tiếng Việt](README.vi.md) | [हिन्दी](README.hi.md) | [Português (BR)](README.pt-BR.md) | **Español**

---

# LowFace - Demo de Reconocimiento Facial Ligero

> Una aplicación de reconocimiento facial diseñada específicamente para **dispositivos Android de gama baja**, implementada con XML/View nativo para verificar la viabilidad en hardware con recursos limitados.

## Sobre LowFace
* LowFace: Reconocimiento Facial Eficiente en Dispositivos de Gama Baja

---

**📝 Esta traducción está en progreso.**

Si domina el idioma español, agradecemos su contribución para completar esta documentación. Por favor, consulte la [versión en inglés](README.md) o la [versión en chino](../README.md) para el contenido completo.

---

## Funciones Principales

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

## Tecnología

- **UI**: XML/View nativo (sin Compose)
- **Cámara**: CameraX + PreviewView
- **Detección Facial**: Google ML Kit
- **Extracción de Features**: Modelo EdgeFace TFLite

## Compilación

```powershell
cd lowFace
.\gradlew.bat assembleDebug
```

---

## Valor del Proyecto

LowFace tiene como objetivo explorar la capacidad de **lograr un reconocimiento facial utilizable en dispositivos Android de gama baja**, permitiendo que más dispositivos existentes tengan capacidades de autenticación digital.

Para muchos países en desarrollo, áreas remotas y empresas sensibles a costos, muchos escenarios de verificación de identidad no requieren sistemas de reconocimiento facial de nivel financiero o de seguridad, sino que necesitan una solución ligera:

- Bajo costo
- Puede funcionar sin conexión
- Baja dependencia de red
- Puede implementarse en dispositivos existentes

Ejemplos incluyen:
- Control de asistencia y registro de empleados
- Gestión de personal en pequeñas organizaciones
- Confirmación de identidad en escenarios de capacitación
- Control de acceso básico y autorización de dispositivos
- Verificación de identidad en servicios comunitarios

---

## Contribuciones

¡Agradecemos las contribuciones de traducción! Por favor, cree un Pull Request para mejorar esta traducción.

## Licencia

El SDK principal (`simface`, `simq`) sigue la licencia del proyecto original.

El código de la capa de aplicación está licenciado bajo la Licencia MIT.
