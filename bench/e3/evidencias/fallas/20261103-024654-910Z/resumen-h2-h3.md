# E3 - Aislamiento bajo carga (H2) y recuperacion (H3)

Lote: `20261103-024654-910Z`. Arquitectura B, tasa 5 solicitudes/s por ruta, 30 s por ventana.

En la ventana de falla el HTTP 503 de resenas es intencional y se comprueba como comportamiento esperado.

| Ronda | Falla: publicaciones incorrectas / total | Antes p95 pub. | Despues p95 pub. | Ratio pub. | Antes p95 resenas | Despues p95 resenas | Ratio resenas |
|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 0 / 150 | 8.33 | 6.10 | 73.23% | 28.81 | 28.38 | 98.49% |

## H2: publicaciones durante la falla
- Errores inesperados en publicaciones: 0 de 150 solicitudes, **0.00%**.
- Umbral: porcentaje de errores en publicaciones menor al 1%.
- Resultado para el lote: CUMPLE.
- El total HTTP de k6 considera fallidos los 503 de resenas: son esperados, no deben contabilizarse como fallas de publicaciones.

## H3: p95 posterior frente al anterior
- Mediana de ratios posteriores/anteriores de publicaciones: **73.23%**.
- Mediana de ratios posteriores/anteriores de resenas: **98.49%**.
- Umbral de referencia: p95 posterior no superior a 120% del previo. Revisar cada ronda, no solo la mediana.
- Las ventanas son sucesivas pero cada fase se ejecuta como una instancia nueva de k6; no se ha medido una transicion en una sola sesion HTTP.
- Las mediciones usan PostgreSQL E3 compartido y el mismo equipo local; no extrapolar a produccion.
