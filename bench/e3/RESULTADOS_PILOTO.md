# E3 - Resultados preliminares A frente a B

## Configuracion

- Version Java: 21
- Framework: Spring Boot 3.5.15
- Base de datos: PostgreSQL 16
- Generador: k6 1.8.2
- Duracion por piloto: 30 segundos
- Tasa programada: 5 solicitudes/s por ruta
- Seed: 1 usuario, 1 publicacion y 100 resenas
- Rutas evaluadas: GET publicacion y GET resenas
- Sin inyeccion de fallas durante los pilotos

## Pilotos registrados

| Metrica | A | B |
|---|---:|---:|
| Solicitudes HTTP | 301 | 301 |
| Comprobaciones exitosas | 602/602 | 602/602 |
| Errores HTTP | 0% | 0% |
| p95 publicaciones (ms) | 9.12 | 9.24 |
| p95 resenas (ms) | 12.23 | 20.37 |

## Hipotesis H1

Sobrecosto observado en resenas:

20.37 - 12.23 = 8.14 ms

Umbral predefinido: incremento p95 no superior a 15 ms.

Interpretacion provisional:
El piloto cumple el umbral H1, pero faltan las
cinco corridas formales por arquitectura.

## Aislamiento funcional

Antes de la falla:
- Publicaciones HTTP 200
- Resenas HTTP 200

Durante la desactivacion de Toxiproxy:
- Publicaciones HTTP 200
- Resenas HTTP 503

Tras la restauracion:
- Publicaciones HTTP 200
- Resenas HTTP 200

Esto prueba aislamiento funcional en una comprobacion
controlada, pero no valida aun el umbral estadistico H2.

## Limitaciones

- Los pilotos no son mediciones definitivas.
- Existe PostgreSQL compartido entre procesos.
- La API B sigue cargando repositorios JPA.
- No se midio aun la recuperacion p95 bajo carga.
- No se midio sistematicamente el arranque.
- Los resultados pueden variar segun la carga de la maquina.
- El benchmark no ejecuta inferencia de Ollama.

## Evidencias

- evidencias/piloto-a.log
- evidencias/piloto-b.log
- evidencias/piloto-b.json
- evidencias/piloto-b.raw.json

## Estado

Pendientes cinco corridas formales por arquitectura,
pruebas de fallas bajo carga y evaluacion H1-H4.
