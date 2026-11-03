# E3 - Analisis de cinco corridas formales por arquitectura

Lote: `20261103-015431-209Z`. Se excluye el calentamiento y el lote piloto de una ronda.

| Corrida | p95 pub. A (ms) | p95 pub. B (ms) | p95 resenas A (ms) | p95 resenas B (ms) | Delta resenas B-A (ms) |
|---|---:|---:|---:|---:|---:|
| 1 | 7.75 | 7.83 | 11.28 | 15.74 | 4.46 |
| 2 | 7.53 | 8.05 | 10.73 | 16.53 | 5.81 |
| 3 | 8.10 | 8.52 | 11.17 | 21.32 | 10.14 |
| 4 | 7.45 | 7.11 | 10.66 | 15.89 | 5.23 |
| 5 | 7.65 | 6.71 | 10.36 | 15.53 | 5.17 |

## Resumen de los p95 entre las cinco corridas

| Ruta | Arquitectura | Mediana de p95 (ms) | Rango min-max (ms) |
|---|---|---:|---:|
| Publicaciones | A | 7.65 | 7.45-8.10 |
| Publicaciones | B | 7.83 | 6.71-8.52 |
| Resenas | A | 10.73 | 10.36-11.28 |
| Resenas | B | 15.89 | 15.53-21.32 |

## H1: sobrecosto del p95 de resenas no superior a 15 ms

- Diferencia de medianas de p95 (B - A): **5.16 ms**.
- Mediana de diferencias por parejas A/B: **5.23 ms**.
- Parejas con diferencia <=15 ms: **5 / 5**.
- H1 solo se refiere a condiciones sin fallas, a la tasa probada. No demuestra aislamiento, recuperacion ni tiempos de arranque.
- Cada p95 se calcula de muestras de la misma ruta y misma corrida; luego se resume entre las cinco corridas.
- Metodo para el percentil: interpolacion lineal entre posiciones ordenadas con indice (n-1)*0.95.
- Limitacion: las corridas A/B se ejecutan en la misma maquina y comparten el PostgreSQL E3; no se infiere comportamiento en produccion.
