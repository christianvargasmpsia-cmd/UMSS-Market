# E3 - Aislamiento bajo carga (H2) y recuperacion (H3)

Lote: `20261103-025043-820Z`. Arquitectura B, tasa 5 solicitudes/s por ruta, 30 s por ventana.

En la ventana de falla el HTTP 503 de resenas es intencional y se comprueba como comportamiento esperado.

| Ronda | Falla: publicaciones incorrectas / total | Antes p95 pub. | Despues p95 pub. | Ratio pub. | Antes p95 resenas | Despues p95 resenas | Ratio resenas |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 0 / 150 | 4.98 | 4.38 | 87.98% | 20.31 | 11.95 | 58.84% |
| 2 | 0 / 151 | 4.15 | 3.86 | 92.87% | 12.94 | 9.02 | 69.69% |
| 3 | 0 / 151 | 4.57 | 4.22 | 92.37% | 11.07 | 10.57 | 95.46% |
| 4 | 0 / 151 | 4.01 | 4.31 | 107.41% | 10.19 | 9.21 | 90.37% |
| 5 | 0 / 151 | 4.20 | 3.24 | 77.29% | 10.59 | 7.78 | 73.43% |

## H2: publicaciones durante la falla
- Errores inesperados en publicaciones: 0 de 754 solicitudes, **0.00%**.
- Umbral: porcentaje de errores en publicaciones menor al 1%.
- Resultado para el lote: CUMPLE.
- El total HTTP de k6 considera fallidos los 503 de resenas: son esperados, no deben contabilizarse como fallas de publicaciones.

## H3: p95 posterior frente al anterior
- Mediana de ratios posteriores/anteriores de publicaciones: **92.37%**.
- Mediana de ratios posteriores/anteriores de resenas: **73.43%**.
- Umbral de referencia: p95 posterior no superior a 120% del previo. Revisar cada ronda, no solo la mediana.
- Las ventanas son sucesivas pero cada fase se ejecuta como una instancia nueva de k6; no se ha medido una transicion en una sola sesion HTTP.
- Las mediciones usan PostgreSQL E3 compartido y el mismo equipo local; no extrapolar a produccion.
