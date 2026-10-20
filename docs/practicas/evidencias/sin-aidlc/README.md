# Evidencia de implementación sin AWS AI-DLC — UMSS Market

## Responsable
- **Integrante:** Abad Melani Rodriguez Gonzales
- **Rama:** `feature-sin-aidlc`
- **Herramienta:** ChatGPT (asistencia directa, sin AWS AI-DLC)
- **Commit inicial:** `11e6f6c`
- **Primer commit de implementación:** `544a848`

## Funcionalidad desarrollada
Se implementó el endpoint REST `GET /api/health`, que permite consultar el estado básico del servicio UMSS Market.

**Respuesta esperada:**
```json
{
  "status": "UP",
  "service": "umss-market-api"
}
```

El endpoint devuelve HTTP 200 y no requiere autenticación. La petición POST al mismo endpoint debe ser rechazada mediante HTTP 405.

## Archivos implementados o modificados
- `HealthController.java`: implementación del endpoint.
- `HealthControllerTest.java`: pruebas automatizadas del controlador.
- `GlobalExceptionHandler.java`: corrección del manejo de métodos HTTP no permitidos.

## Pruebas realizadas

| Prueba | Resultado |
|---|---|
| GET /api/health | HTTP 200 — aprobado |
| POST /api/health | HTTP 405 — aprobado |
| HealthControllerTest | 2 pruebas aprobadas |
| GlobalExceptionHandlerTest | 4 pruebas aprobadas |

**Resultado total:** 6 pruebas aprobadas, 0 fallidas.

Se ejecutaron las pruebas indicadas, no la suite completa del proyecto.

## Intervenciones y correcciones
1. Se corrigió la ubicación inicial de `HealthControllerTest.java`.
2. Se utilizó Maven Wrapper porque `mvn` no estaba disponible globalmente.
3. Se resolvió un problema de autenticación local con PostgreSQL para arrancar el servidor.
4. Se detectó HTTP 500 al enviar POST. Se reprodujo mediante una prueba que incluía el manejador global de excepciones y se corrigió para devolver HTTP 405.

## Tiempos registrados

- **Inicio:** 20/10/2026 — 14:46:06.
- **Primer commit:** 20/10/2026 — 15:04:16.
- **Validación automatizada final:** 20/10/2026 — 15:41:28.
- **Cierre funcional y publicación:** 20/10/2026 — 15:51:42.
- **Tiempo total registrado:** 1 hora, 5 minutos y 36 segundos.

El tiempo incluye implementación, pruebas, correcciones, documentación y publicación de evidencias.

## Evaluación
**Aspectos positivos:** implementación sencilla, pruebas rápidas, identificación y corrección de un problema real del manejo HTTP.

**Aspectos negativos:** las pruebas iniciales no incluían el manejador global de excepciones, por lo que no detectaron el problema observado en Postman.

## Evidencias
- `postman-get-200.png`
- `postman-post-405.png`
- `health-tests.log`

## Conclusión
El desarrollo con asistencia directa de IA permitió implementar rápidamente el endpoint. Sin embargo, fue necesaria la intervención humana para validar el comportamiento real y corregir problemas que inicialmente no estaban cubiertos por las pruebas.