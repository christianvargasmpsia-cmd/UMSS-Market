# E3 — Resultados finales: monolito modular frente a servicio de reseñas extraído

**Proyecto:** UMSS-Market  
**Rama experimental:** `feature/e3-extraccion-resenas`  
**Commit base de evidencia preliminar:** `bf6cb83` *(verificar que corresponda al commit evaluado)*  
**Experimento:** E3 — Monolito modular vs. extracción del servicio de reseñas  
**Estado:** H1, H2 y H3 cumplen sus criterios; H4 no cumple. Se incorpora una medición complementaria del build Docker.

---

## 1. Pregunta y alternativas arquitectónicas

El objetivo del experimento es comparar dos despliegues de la funcionalidad de reseñas de UMSS-Market y evaluar el beneficio del aislamiento frente a los costos de latencia, arranque y complejidad operativa.

### A — Monolito modular

La API principal y el módulo de reseñas se ejecutan dentro del mismo proceso Spring Boot. El acceso a reseñas se realiza localmente mediante `ReviewsPort`.

### B — Servicio de reseñas extraído

La API principal accede al servicio de reseñas mediante un adaptador HTTP:

API principal → adaptador HTTP → Toxiproxy → servicio de reseñas.

En esta arquitectura se ejecutan dos procesos Java de aplicación.

Ambas alternativas utilizan el mismo código e imagen de backend con parámetros de ejecución diferentes y la misma instancia PostgreSQL de E3. Por tanto, la extracción no proporciona aislamiento independiente de base de datos.

### Rutas evaluadas

- Dependiente de reseñas: `GET /api/publications/{id}/reviews?limit=20`
- Independiente de reseñas: `GET /api/publications/{id}`

ID de publicación utilizado:

`22222222-2222-4222-8222-222222222222`

---

## 2. Condiciones experimentales y controles

| Elemento | Condición |
|---|---|
| Entorno | Misma máquina local para A y B |
| Runtime | Java 21 / Spring Boot |
| Ejecución | Contenedores Docker |
| Base de datos | PostgreSQL 16 E3 |
| Dataset | 1 usuario, 1 publicación y 100 reseñas |
| Generador de carga | k6 1.8.2 |
| Tráfico | 5 solicitudes/s por ruta; 10 solicitudes/s totales programadas |
| Ventana | 30 segundos medidos por corrida |
| Calentamiento | Separado y excluido de las mediciones |
| Repeticiones H1 | 5 por arquitectura |
| Repeticiones H2/H3 | 5 escenarios |
| Repeticiones H4 | 5 parejas A/B |
| Falla inducida | Desactivación y restauración del proxy `reviews` de Toxiproxy |
| Latencia | Percentil 95 por ruta y corrida |
| Resumen | Mediana y rango |
| Seguridad | Archivo `.env` ignorado por Git; no se publican secretos |

Los lotes de validación de una ronda y los pilotos iniciales no se mezclan con los cinco ensayos formales.

Las condiciones de ejecución se mantuvieron en una misma máquina. Sin embargo, el experimento no constituye una evaluación de producción ni demuestra escalabilidad máxima.

---

## 3. H1 — Sobrecosto de latencia

### Criterio

La arquitectura B no debe añadir más de 15 ms al p95 de la ruta de reseñas respecto a A, durante funcionamiento sin falla.

### Evidencia

Lote formal:

`evidencias/formales/20261103-015431-209Z/`

| Ronda | p95 publicaciones A (ms) | B (ms) | p95 reseñas A (ms) | B (ms) | Δ reseñas B−A (ms) |
|---|---:|---:|---:|---:|---:|
| 1 | 7.746 | 7.833 | 11.275 | 15.738 | 4.463 |
| 2 | 7.531 | 8.051 | 10.725 | 16.534 | 5.809 |
| 3 | 8.099 | 8.519 | 11.174 | 21.318 | 10.144 |
| 4 | 7.449 | 7.109 | 10.656 | 15.886 | 5.230 |
| 5 | 7.647 | 6.705 | 10.358 | 15.532 | 5.174 |

### Resumen de latencia

| Ruta | Mediana p95 A | Mediana p95 B | Rango A | Rango B |
|---|---:|---:|---:|---:|
| Publicaciones | 7.65 ms | 7.83 ms | 7.45–8.10 ms | 6.71–8.52 ms |
| Reseñas | 10.73 ms | 15.89 ms | 10.36–11.28 ms | 15.53–21.32 ms |

Sobrecosto por diferencia de medianas:

`15.886 − 10.725 = 5.161 ms`

Resultado redondeado: **+5.16 ms**.

La mediana de las diferencias entre rondas emparejadas es **5.23 ms**.

Las cinco parejas se mantuvieron dentro del umbral de 15 ms.

### Resultado H1: CUMPLE

La arquitectura B cumple el presupuesto de latencia establecido para esta carga y entorno.

Sin embargo, la latencia de reseñas sí aumentó. Cumplir el presupuesto no significa que B sea más rápida que A.

---

## 4. H2 — Aislamiento durante falla del servicio de reseñas

### Criterio

La ruta independiente de publicaciones debe mantener menos del 1% de errores durante una falla del servicio de reseñas.

### Evidencia

Lote formal:

`evidencias/fallas/20261103-025043-820Z/`

Durante cinco ventanas de falla, con el proxy `reviews` deshabilitado:

| Ruta | Resultado |
|---|---|
| Publicaciones | 0 respuestas inesperadas de 754 solicitudes |
| Reseñas | HTTP 503 previstos por el escenario de falla |

Tasa de errores inesperados en publicaciones:

`0 / 754 = 0.00%`

La tasa global `http_req_failed` de k6 puede indicar aproximadamente 50% en la fase de falla porque considera fallidos los HTTP 503 deliberados de reseñas.

Esa tasa global no debe presentarse como la tasa de errores de publicaciones.

### Resultado H2: CUMPLE

La evidencia confirma aislamiento funcional de la ruta de publicaciones frente a la indisponibilidad de la comunicación con reseñas.

Este resultado no demuestra aislamiento frente a una caída de PostgreSQL compartido ni compara un escenario de falla equivalente en A.

---

## 5. H3 — Latencia después de restaurar la comunicación

### Criterio

Después de retirar la falla, el p95 de B debe ser menor o igual al 120% del p95 previo.

En cada una de las cinco rondas se midieron las fases antes, falla y después. Cada fase tuvo una ventana de 30 segundos y se ejecutó en una sesión k6 separada.

### Evidencia

| Ronda | p95 publicaciones después/antes | p95 reseñas después/antes |
|---|---:|---:|
| 1 | 87.98% | 58.84% |
| 2 | 92.87% | 69.69% |
| 3 | 92.37% | 95.46% |
| 4 | 107.41% | 90.37% |
| 5 | 77.29% | 73.43% |
| Mediana | 92.37% | 73.43% |

- Publicaciones: 5/5 rondas dentro del umbral.
- Reseñas: 5/5 rondas dentro del umbral.
- Después de la restauración se observaron HTTP 200 en ambas rutas.
- El proxy quedó habilitado.

### Resultado H3: CUMPLE

La arquitectura B cumple el criterio de latencia posterior a la restauración bajo la definición por ventanas utilizada.

Esta prueba no mide tiempo de recuperación instantáneo (RTO) ni continuidad durante una única sesión de carga, porque las fases se ejecutaron sucesivamente.

Los ratios inferiores al 100% no demuestran que la falla mejore el rendimiento. Reflejan variabilidad de ejecución, calentamiento y condiciones locales.

---

## 6. H4 — Tiempo de arranque funcional

### Criterio

El arranque de B no debe exceder el de A en más de 30%.

Criterio numérico:

`B / A ≤ 1.30`

### Evidencia

Lote formal:

`evidencias/arranque/20261103-031456-419Z-f60cfb/`

| Ronda | A (ms) | B (ms) | B/A |
|---|---:|---:|---:|
| 1 | 6,839.305 | 8,617.666 | 126.00% |
| 2 | 7,114.531 | 9,592.011 | 134.82% |
| 3 | 6,917.535 | 9,743.113 | 140.85% |
| 4 | 6,896.212 | 9,429.478 | 136.73% |
| 5 | 6,968.885 | 9,256.888 | 132.83% |

Mediana A:

`6,917.535 ms ≈ 6.92 s`

Rango A:

`6.839–7.115 s`

Mediana B:

`9,429.478 ms ≈ 9.43 s`

Rango B:

`8.618–9.743 s`

Sobrecosto relativo de medianas:

`(9429.478 / 6917.535 − 1) × 100 = 36.31%`

Cuatro de las cinco parejas superaron el umbral del 30%.

### Resultado H4: NO CUMPLE

La arquitectura B excede el presupuesto de overhead de arranque establecido.

### Alcance de la medición

Se cronometró el inicio mediante `docker start` de contenedores de aplicación existentes hasta que las dos rutas devolvieron HTTP 200.

Para B se iniciaron secuencialmente el servicio de reseñas y la API.

PostgreSQL y Toxiproxy permanecieron iniciados.

No se cronometraron recreación completa, arranque de base de datos, build ni despliegue de producción.

---

## 7. Medición complementaria — Construcción de imagen Docker

### Objetivo

Complementar la evaluación E3 mediante la medición del tiempo necesario para construir la imagen Docker utilizada por las arquitecturas A y B.

Esta medición es complementaria y no reemplaza H4, que evalúa el tiempo de arranque funcional.

### Metodología

Se realizaron cinco construcciones de la imagen compartida utilizando Docker Engine 29.9.1 y la opción `--no-cache`.

Dockerfile:

`backend/umss-market-api/Dockerfile`

Contexto:

`backend/umss-market-api`

Imagen:

`umss-e3-build:local`

La imagen fue construida a partir del JAR existente del backend.

Ambas arquitecturas utilizan la misma imagen Docker y el mismo Dockerfile. Por lo tanto, esta medición representa un build compartido, no dos builds independientes de A y B.

### Resultados

| Ronda | Tiempo de build (s) |
|---|---:|
| 1 | 7.365 |
| 2 | 7.026 |
| 3 | 6.962 |
| 4 | 7.251 |
| 5 | 6.733 |

### Resumen estadístico

| Métrica | Resultado |
|---|---:|
| Rondas | 5 |
| Promedio | 7.067 s |
| Mediana | 7.026 s |
| Mínimo | 6.733 s |
| Máximo | 7.365 s |
| Rango | 0.632 s |

La mediana corresponde al valor central de las cinco mediciones ordenadas.

### Interpretación

La construcción de la imagen compartida se mantuvo relativamente estable durante las cinco ejecuciones realizadas en el entorno local.

Esta medición aporta información complementaria sobre el proceso de construcción de la aplicación, pero no permite concluir que la extracción del servicio de reseñas incremente o reduzca el tiempo de build respecto al monolito modular.

La imagen se construyó con `--no-cache` para reducir la reutilización de capas de builds anteriores. Los tiempos pueden variar según CPU, disco, red y disponibilidad de dependencias.

La primera construcción manual realizada durante la preparación del experimento tardó 80.3 segundos e incluyó la descarga de la imagen base. Ese resultado no se mezcla con las cinco mediciones formales del script.

### Limitaciones de la medición

- Se construyó una única imagen compartida.
- No se compararon builds independientes de A y B.
- No se midió despliegue completo.
- No se midió arranque de PostgreSQL.
- No se midió tiempo de disponibilidad funcional.
- No se comparó el build en diferentes máquinas.

### Evidencias

Directorio:

`bench/e3/evidencias/build/20261104-002014-959Z/`

Contenido relevante:

- `build.csv`: mediciones por ronda.
- `logs/`: salida de Docker build por ronda.
- `RESUMEN-BUILD.md`: resumen metodológico y estadístico.

### Resultado de build: MEDICIÓN COMPLEMENTARIA COMPLETADA

La mediana de construcción de la imagen compartida fue de 7.026 segundos.

Este resultado se incorpora como evidencia complementaria del experimento E3 y no modifica el veredicto de H4.

---

## 8. Costos, riesgos y limitaciones

### Costos observados cuantitativamente

| Métrica | Resultado |
|---|---:|
| Aumento mediano de p95 en reseñas | +5.16 ms |
| Overhead relativo de arranque funcional | +36.31% |
| Mediana de build de imagen compartida | 7.026 s |

El tiempo de build es una medición complementaria y no un costo diferencial entre A y B.

### Costos operativos cualitativos de B

La arquitectura B introduce:

- Dos procesos Java.
- Comunicación HTTP interna.
- Token de comunicación.
- Proxy utilizado para la prueba de fallas.
- Configuración y monitoreo adicionales.

Estos costos representan complejidad arquitectónica, pero no se midieron consumo de CPU/RAM, dinero de infraestructura ni horas de mantenimiento. No se atribuyen cifras no observadas.

### Limitaciones y validez

1. PostgreSQL es compartido. Su indisponibilidad podría afectar tanto a la API como al servicio de reseñas, por lo que el aislamiento es parcial.

2. En B, la API principal conserva dependencias de persistencia. No se afirma que exista autonomía completa de datos.

3. H2 deshabilita la comunicación por Toxiproxy. No representa todos los modos de falla posibles, como caída de base de datos, host, saturación o errores lógicos.

4. Se ensayó una sola tasa de tráfico, durante ventanas de 30 segundos, con un dataset pequeño y una máquina local. No se demuestra escalabilidad máxima ni validez productiva.

5. El protocolo original en `HIPOTESIS.md` proponía `docker compose down -v` entre corridas. El procedimiento se desvió para evitar destruir el volumen y mantener el seed; se reiniciaron los procesos de aplicación. Esta desviación se documenta y no se oculta.

6. No se midió inferencia de Ollama ni generación de resúmenes IA. La carga se limita a dos consultas GET.

7. H4 incluye tiempo de comandos Docker, sondeo de respuestas y arranque secuencial de B. Mide disponibilidad funcional del protocolo, no diferencia intrínseca aislada de JVM ni tiempo integral de despliegue.

8. La medición de build corresponde a una imagen compartida. No permite afirmar que A o B tenga un build más rápido.

9. Los análisis representan evidencia generada por scripts locales. No constituyen una auditoría independiente de todos los archivos sin cotejarlos contra los JSON y CSV originales.

10. Los resultados corresponden a un entorno experimental local y no deben generalizarse directamente a producción.

---

## 9. Evaluación consolidada de hipótesis

| Hipótesis | Criterio | Observación | Veredicto |
|---|---|---|---|
| H1 | Δ mediana p95 reseñas ≤15 ms | +5.16 ms | CUMPLE |
| H2 | Errores publicaciones durante falla <1% | 0/754 = 0.00% | CUMPLE |
| H3 | p95 posterior/anterior ≤120% | Todas las parejas dentro del umbral | CUMPLE |
| H4 | Mediana tiempo B/A ≤130% | Overhead relativo 36.31% | NO CUMPLE |
| Build complementario | Construcción de imagen compartida | Mediana 7.026 s | MEDIDO |

La medición complementaria de build no se considera una hipótesis adicional y no cambia la evaluación de H1–H4.

---

## 10. Evaluación arquitectónica y decisión

### Resultado general

El experimento muestra que la extracción del servicio de reseñas proporciona aislamiento funcional frente a una falla de comunicación inducida, pero introduce costos de latencia y arranque.

H1 cumple el presupuesto de latencia de 15 ms.

H2 demuestra que la ruta independiente de publicaciones mantuvo 0% de errores inesperados durante la falla inducida.

H3 cumple el criterio de latencia posterior a la restauración bajo la metodología por ventanas utilizada.

H4 no cumple el presupuesto de overhead de arranque del 30%.

La medición complementaria de build registra una mediana de 7.026 segundos para la imagen compartida, sin establecer una diferencia entre arquitecturas.

### Recomendación

Mantener **A — Monolito modular** como arquitectura por defecto del proyecto UMSS-Market.

Mantener B como alternativa experimental validada para aislamiento de indisponibilidad de la comunicación con reseñas, sin habilitarla como arquitectura principal por ahora.

La decisión se fundamenta en:

- El cumplimiento del presupuesto de latencia.
- El aislamiento funcional observado durante la falla inducida.
- El cumplimiento del criterio de latencia posterior a la restauración.
- El incumplimiento del presupuesto de arranque.
- La complejidad operativa adicional.
- El aislamiento limitado por la base de datos compartida.

La decisión es reversible. La extracción podrá reconsiderarse cuando existan necesidades concretas de escalabilidad independiente, despliegue autónomo o aislamiento operativo que justifiquen los costos adicionales.

El incumplimiento de H4 no representa un fracaso del experimento. Constituye evidencia para decidir cuándo conviene mantener o reconsiderar la extracción.

---

## 11. Trazabilidad y reproducción

### Diseño y planificación

- Diseño previo: `DISENO.md`
- Hipótesis previas: `HIPOTESIS.md`
- Resultados de pilotos: `RESULTADOS_PILOTO.md`

### Infraestructura

- `compose.yaml`
- `seed.sql`

### Pruebas de carga

- `smoke.js`
- `carga.js`
- `fallas.js`

### Automatización

- `run-formal.ps1`
- `analizar-formales.ps1`
- `run-fallas.ps1`
- `medir-arranque-h4.ps1`
- `medir-build-e3.ps1`

### Evidencias H1

`evidencias/formales/20261103-015431-209Z/`

Archivos relevantes:

- `manifest.txt`
- `p95-por-ruta.csv`
- `resumen-h1.md`
- Archivos JSON originales

### Evidencias H2/H3

`evidencias/fallas/20261103-025043-820Z/`

Archivos relevantes:

- `manifest.txt`
- `resumen-fases.csv`
- `resumen-h2-h3.md`
- Archivos JSON originales

### Evidencias H4

`evidencias/arranque/20261103-031456-419Z-f60cfb/`

Archivos relevantes:

- `manifest.txt`
- `tiempos-arranque.csv`
- `resumen-h4.md`

### Evidencias de build complementario

`bench/e3/evidencias/build/20261104-002014-959Z/`

Archivos relevantes:

- `build.csv`
- `RESUMEN-BUILD.md`
- `logs/`

### ADR

`../../docs/adr/ADR-0007-extraccion-resenas-e3.md`

### Recomendación para una nueva reproducción

1. Levantar solo el perfil A o B.
2. Comprobar el seed `1|1|100`.
3. Calentar por separado.
4. Usar la misma imagen y el mismo ritmo de tráfico.
5. Preservar los registros existentes y evitar sobrescribirlos.
6. No utilizar `docker compose down -v` sin una copia deliberada del volumen aislado.
7. Registrar cambios de configuración, versión de Docker y condiciones del entorno.

---

## 12. Conclusión final

El experimento E3 comparó el monolito modular con la extracción del servicio de reseñas bajo condiciones locales controladas.

Tres hipótesis cumplieron sus criterios y una no cumplió. La arquitectura B mostró aislamiento funcional de la ruta de publicaciones frente a la falla inducida de comunicación con reseñas, pero presentó un incremento de latencia y un overhead de arranque superior al presupuesto definido.

La medición complementaria registró una mediana de 7.026 segundos para la construcción de la imagen Docker compartida. Esta evidencia no representa una comparación independiente de builds entre A y B ni modifica el resultado de H4.

**La recomendación es mantener el monolito modular como arquitectura por defecto y conservar la extracción del servicio de reseñas como alternativa experimental para futuras necesidades de aislamiento o escalabilidad.**

La decisión se basa en los resultados medidos, las limitaciones del entorno y los costos observados, sin afirmar que la extracción sea universalmente mejor o peor.

