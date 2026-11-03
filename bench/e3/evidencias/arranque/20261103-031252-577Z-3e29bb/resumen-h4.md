# E3 - H4: tiempo de arranque funcional

Lote: `20261103-031252-577Z-3e29bb`; 1 rondas A/B.

| Ronda | A (ms) | B (ms) | B/A |
|---|---:|---:|---:|
| 1 | 6,318.56 | 8,530.84 | 135.01% |

- Mediana de arranque A: **6,318.56 ms**.
- Mediana de arranque B: **8,530.84 ms**.
- Sobrecosto relativo de medianas: **35.01%**.
- H4: **NO CUMPLE** usando medianas (criterio B/A <= 130%).

- A, rango min-max: 6,318.56-6,318.56 ms.
- B, rango min-max: 8,530.84-8,530.84 ms.

## Alcance y limitaciones
- El reloj comienza justo antes de docker start y termina cuando ambas rutas responden HTTP 200.
- Medicion funcional de un reinicio de los procesos de aplicacion en contenedores existentes.
- PostgreSQL y Toxiproxy permanecen activos para ambas arquitecturas; no se cronometra su arranque.
- Para B se inician secuencialmente reviews-b y api-b, respetando la misma operacion en cada ronda.
- Se incluye el overhead de Docker CLI y del sondeo HTTP. La resolucion efectiva no es de microsegundos.
- No equivale al tiempo de despliegue completo en produccion ni al tiempo de build.
- HIPOTESIS.md proponia down -v entre corridas, pero aqui se preserva el volumen existente; documentar la desviacion.
- Una diferencia desfavorable para B no invalida la ganancia demostrada en aislamiento H2; cambia la decision de trade-offs.
