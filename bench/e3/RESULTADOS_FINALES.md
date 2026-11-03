# E3 — Resultados finales: monolito modular frente a servicio de reseñas extraído

**Proyecto:** UMSS-Market
**Rama experimental:** `feature/e3-extraccion-resenas`
**Commit base de evidencia preliminar:** `bf6cb83`
**Objeto:** comparar dos despliegues de la misma funcionalidad de reseñas y valorar el beneficio de aislamiento frente a los costos de latencia y arranque.
**Estado de medición:** H1–H4 ejecutadas; tres hipótesis cumplen su criterio y H4 no cumple.

## 1. Pregunta y alternativas

- **A — Monolito modular:** API principal y módulo de reseñas en el mismo proceso Spring Boot; acceso local mediante `ReviewsPort`.
- **B — Servicio extraído:** API principal → adaptador HTTP → Toxiproxy → servicio de reseñas separado. En B se ejecutan dos procesos Java de aplicación.
- Las formas usan el mismo código/imágen de backend con parámetros de ejecución diferentes y **la misma instancia PostgreSQL de E3**. No existe aislamiento independiente de base de datos.

Se miden dos rutas públicas:

- Dependiente de reseñas: `GET /api/publications/{id}/reviews?limit=20`.
- Independiente de reseñas: `GET /api/publications/{id}`.

El ID de publicación usado fue `22222222-2222-4222-8222-222222222222`.

## 2. Condiciones y controles

| Elemento | Condición aplicada |
|---|---|
| Máquina | Misma máquina local para A y B |
| Runtime | Java 21 / Spring Boot; contenedores Docker |
| Base de datos | PostgreSQL 16 E3; `1` usuario, `1` publicación, `100` reseñas |
| Generador de carga | k6 `1.8.2` |
| Tráfico | `5` solicitudes/s por ruta; `10` solicitudes/s totales programadas |
| Ventana | `30 s` medidos por corrida; calentamiento separado y excluido |
| Repeticiones | `5` por arquitectura para H1; `5` por escenario para H2/H3; `5` parejas para H4 |
| Cambios de topología | Se alternó A/B evitando que ambas API estuviesen activas al medir; la DB conservó el seed |
| Fallas | Desactivación/restauración del proxy `reviews` de Toxiproxy en B |
| Métrica de latencia | Percentil 95 por ruta y por corrida, a partir de muestras `http_req_duration`; resumen mediante mediana y rango |
| Seguridad del entorno | Archivo `.env` ignorado por Git; sin publicar valores de secretos |

Los lotes de validación de **una** ronda y los pilotos iniciales **no** se mezclan con los cinco ensayos formales.

## 3. H1 — Sobrecosto de latencia

**Criterio predefinido:** B no añade más de **15 ms** al p95 de la ruta de reseñas respecto a A, en funcionamiento sin falla.

**Lote formal:** `evidencias/formales/20261103-015431-209Z/`.

| Ronda | p95 publicaciones A (ms) | B (ms) | p95 reseñas A (ms) | B (ms) | Δ reseñas B−A (ms) |
|---|---:|---:|---:|---:|---:|
| 1 | 7.746 | 7.833 | 11.275 | 15.738 | 4.463 |
| 2 | 7.531 | 8.051 | 10.725 | 16.534 | 5.809 |
| 3 | 8.099 | 8.519 | 11.174 | 21.318 | 10.144 |
| 4 | 7.449 | 7.109 | 10.656 | 15.886 | 5.230 |
| 5 | 7.647 | 6.705 | 10.358 | 15.532 | 5.174 |

| Ruta | Mediana p95 A | Mediana p95 B | Rango A | Rango B |
|---|---:|---:|---:|---:|
| Publicaciones | 7.65 ms | 7.83 ms | 7.45–8.10 ms | 6.71–8.52 ms |
| Reseñas | 10.73 ms | 15.89 ms | 10.36–11.28 ms | 15.53–21.32 ms |

- **Sobrecosto por diferencia de medianas:** `15.886 − 10.725 = 5.161 ms` (redondeado, **5.16 ms**).
- **Mediana de diferencias entre rondas emparejadas:** **5.23 ms**.
- **Parejas dentro del umbral de 15 ms:** **5/5**.
- **Resultado H1: CUMPLE** en esta carga y entorno. La latencia de reseñas **sí aumentó**; cumplir el presupuesto no significa que B sea más rápida.

## 4. H2 — Aislamiento durante falla de reseñas

**Criterio predefinido:** la ruta independiente de publicaciones mantiene **menos del 1%** de errores durante una falla del servicio de reseñas.

**Lote formal:** `evidencias/fallas/20261103-025043-820Z/`.

Durante cinco ventanas de falla, con el proxy `reviews` deshabilitado:

| Ruta | Resultado observado |
|---|---|
| Publicaciones | **0 respuestas inesperadas de 754** solicitudes, tasa **0.00%** |
| Reseñas | Respuestas **HTTP 503** previstas por el escenario de falla |

La tasa global `http_req_failed` de k6 puede indicar aproximadamente 50% en la fase de falla porque considera fallidos los HTTP 503 deliberados de reseñas; **no** se debe presentar esa tasa como fallas de publicaciones.

**Resultado H2: CUMPLE** bajo la falla de comunicación inducida. La evidencia confirma aislamiento funcional de la ruta de publicaciones frente a la indisponibilidad de la comunicación con reseñas. **No** demuestra aislamiento frente a una caída de PostgreSQL compartido ni compara un escenario de falla equivalente en A.

## 5. H3 — Latencia después de restaurar la comunicación

**Criterio predefinido:** después de quitar la falla, el p95 de B debe ser **≤120%** del p95 previo. En cada una de cinco rondas se midieron las fases **antes → falla → después**, cada una de 30 segundos y en una sesión k6 separada.

| Ronda | p95 publicaciones después/antes | p95 reseñas después/antes |
|---|---:|---:|
| 1 | 87.98% | 58.84% |
| 2 | 92.87% | 69.69% |
| 3 | 92.37% | 95.46% |
| 4 | 107.41% | 90.37% |
| 5 | 77.29% | 73.43% |
| **Mediana** | **92.37%** | **73.43%** |

- **5/5** rondas de publicaciones y **5/5** de reseñas dentro del umbral.
- Tras la restauración se observaron HTTP 200 en ambas rutas; el proxy quedó habilitado.
- **Resultado H3: CUMPLE** con esta definición por ventanas. **No** mide tiempo de recuperación instantáneo (RTO) ni continuidad durante una única sesión de carga: las fases son ejecuciones sucesivas.
- Los ratios menores al 100% no prueban que la falla mejore el rendimiento; reflejan variabilidad de ejecución, calentamiento y condiciones locales.

## 6. H4 — Tiempo de arranque funcional

**Criterio predefinido:** el arranque de B no debe exceder el de A en **más de 30%** (B/A ≤1.30).

**Lote formal:** `evidencias/arranque/20261103-031456-419Z-f60cfb/`.

| Ronda | A (ms) | B (ms) | B/A |
|---|---:|---:|---:|
| 1 | 6,839.305 | 8,617.666 | 126.00% |
| 2 | 7,114.531 | 9,592.011 | 134.82% |
| 3 | 6,917.535 | 9,743.113 | 140.85% |
| 4 | 6,896.212 | 9,429.478 | 136.73% |
| 5 | 6,968.885 | 9,256.888 | 132.83% |

- **Mediana A:** `6,917.535 ms` ≈ **6.92 s** (rango **6.839–7.115 s**).
- **Mediana B:** `9,429.478 ms` ≈ **9.43 s** (rango **8.618–9.743 s**).
- **Sobrecosto relativo de medianas:** `(9429.478 / 6917.535 − 1) × 100 = 36.31%`.
- **4/5 parejas** superan el umbral del 30%; la pareja 1 no.
- **Resultado H4: NO CUMPLE**.

**Qué se cronometró:** inicio con `docker start` de contenedores de aplicación existentes hasta que las dos rutas devuelven HTTP 200. Para B se inician secuencialmente el servicio de reseñas y la API. PostgreSQL y Toxiproxy permanecen iniciados; **no** se cronometraron recreación completa, arranque de DB, build ni despliegue de producción.

## 7. Costos, riesgos y limitaciones

**Costos observados cuantitativamente:** aumento mediano de p95 en reseñas (**+5.16 ms**), arranque funcional más lento (**+36.31%**).

**Costos operativos cualitativos de B:** dos procesos Java, tráfico HTTP interno, token de comunicación, proxy para prueba de fallas, configuración y monitoreo adicionales. Estos costos existen como complejidad de arquitectura, pero **no** se midieron consumo de CPU/RAM, dinero por infraestructura, tiempo de build ni horas de mantenimiento; no inventar cifras.

**Limitaciones y validez:**

1. La base PostgreSQL es compartida. Su indisponibilidad podría afectar tanto a la API como al servicio de reseñas, por lo que el aislamiento es **parcial**.
2. En B la API principal conserva dependencias de persistencia; no se afirma que se haya conseguido autonomía completa de datos.
3. La prueba de H2 deshabilita la comunicación por Toxiproxy; no representa todos los modos de falla (BD, host, saturación, errores lógicos).
4. Se ensayó una sola tasa de tráfico, durante 30 s por ventana, con un seed pequeño y una máquina local. No se demuestra escalabilidad máxima ni validez productiva.
5. El protocolo original en `HIPOTESIS.md` proponía `docker compose down -v` entre corridas. **Se desvió el procedimiento** para evitar destruir el volumen y mantener idéntico el seed; se reiniciaron los procesos de aplicación. Esta desviación debe conservarse documentada y no ocultarse.
6. No se midió inferencia de Ollama/resúmenes IA; la carga se limita a dos consultas GET.
7. H4 incluye tiempo de comandos Docker, sondeo de respuestas y arranque secuencial de B; mide disponibilidad funcional de este protocolo, **no** diferencia intrínseca aislada de JVM ni tiempo integral de despliegue.
8. Los análisis representan la evidencia generada por los scripts locales; no constituyen una auditoría independiente de los archivos sin cotejarlos contra los JSON/CSV incluidos.

## 8. Evaluación y decisión

| Hipótesis | Criterio | Observación | Veredicto |
|---|---|---|---|
| H1 | Δ mediana p95 reseñas ≤15 ms | +5.16 ms | **CUMPLE** |
| H2 | Errores publicaciones durante falla <1% | 0/754 = 0% | **CUMPLE** |
| H3 | p95 posterior/anterior ≤120% | Todas las parejas ≤120% | **CUMPLE** |
| H4 | Mediana tiempo B/A ≤130% | 136.31% | **NO CUMPLE** |

**Recomendación:** mantener **A, monolito modular**, como forma por defecto del proyecto. Mantener B como alternativa validada para aislamiento de indisponibilidad de la comunicación con reseñas, sin habilitarla como arquitectura principal por ahora. El costo extra de arranque rebasa el presupuesto declarado; la extracción implica operación más compleja y aislamiento limitado por la DB compartida. Esta elección es **reversible** bajo los criterios numéricos indicados en el ADR.

Esta recomendación pondera el valor de H2 frente a los costos, y no convierte el incumplimiento de H4 en fracaso del experimento.

## 9. Trazabilidad y reproducción

- Diseño previo: `DISENO.md`.
- Hipótesis previas: `HIPOTESIS.md`.
- Pilotos: `RESULTADOS_PILOTO.md`; `evidencias/piloto-a.log`, `evidencias/piloto-b.*`.
- Infraestructura y seed: `compose.yaml`, `seed.sql`.
- Pruebas k6: `smoke.js`, `carga.js`, `fallas.js`.
- Automatización: `run-formal.ps1`, `analizar-formales.ps1`, `run-fallas.ps1`, `medir-arranque-h4.ps1`.
- H1: `evidencias/formales/20261103-015431-209Z/`, especialmente `manifest.txt`, `p95-por-ruta.csv`, `resumen-h1.md` y `*.raw.json`.
- H2/H3: `evidencias/fallas/20261103-025043-820Z/`, especialmente `manifest.txt`, `resumen-fases.csv`, `resumen-h2-h3.md` y `*.raw.json`.
- H4: `evidencias/arranque/20261103-031456-419Z-f60cfb/`, especialmente `manifest.txt`, `tiempos-arranque.csv`, `resumen-h4.md`.
- ADR: [`../../docs/adr/ADR-0007-extraccion-resenas-e3.md`](../../docs/adr/ADR-0007-extraccion-resenas-e3.md).

**Recomendación para una nueva reproducción:** levantar solo el perfil A o B, comprobar el seed `1|1|100`, calentar por separado, usar la misma imagen y el ritmo de tráfico, preservar los registros existentes (nunca sobrescribir) y **no** usar `down -v` sin una copia deliberada del volumen aislado.
