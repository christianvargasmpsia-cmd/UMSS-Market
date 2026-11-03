# E3 - H4: tiempo de arranque funcional

Lote: `20261103-031456-419Z-f60cfb`; 5 rondas A/B.

| Ronda | A (ms) | B (ms) | B/A |
|---|---:|---:|---:|
| 1 | 6,839.31 | 8,617.67 | 126.00% |
| 2 | 7,114.53 | 9,592.01 | 134.82% |
| 3 | 6,917.54 | 9,743.11 | 140.85% |
| 4 | 6,896.21 | 9,429.48 | 136.73% |
| 5 | 6,968.89 | 9,256.89 | 132.83% |

- Mediana de arranque A: **6,917.54 ms**.
- Mediana de arranque B: **9,429.48 ms**.
- Sobrecosto relativo de medianas: **36.31%**.
- H4: **NO CUMPLE** usando medianas (criterio B/A <= 130%).

- A, rango min-max: 6,839.31-7,114.53 ms.
- B, rango min-max: 8,617.67-9,743.11 ms.

## Alcance y limitaciones
- El reloj comienza justo antes de docker start y termina cuando ambas rutas responden HTTP 200.
- Medicion funcional de un reinicio de los procesos de aplicacion en contenedores existentes.
- PostgreSQL y Toxiproxy permanecen activos para ambas arquitecturas; no se cronometra su arranque.
- Para B se inician secuencialmente reviews-b y api-b, respetando la misma operacion en cada ronda.
- Se incluye el overhead de Docker CLI y del sondeo HTTP. La resolucion efectiva no es de microsegundos.
- No equivale al tiempo de despliegue completo en produccion ni al tiempo de build.
- HIPOTESIS.md proponia down -v entre corridas, pero aqui se preserva el volumen existente; documentar la desviacion.
- Una diferencia desfavorable para B no invalida la ganancia demostrada en aislamiento H2; cambia la decision de trade-offs.
