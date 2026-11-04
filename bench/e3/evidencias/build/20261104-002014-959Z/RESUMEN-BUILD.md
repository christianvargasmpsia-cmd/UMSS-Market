# E3 - Build de imagen compartida

Fecha UTC: 2026-11-04T00:20:50.6654230Z

## Resultado

- Imagen: `umss-e3-build:local`
- Dockerfile: `backend/umss-market-api/Dockerfile`
- Contexto: `backend/umss-market-api`
- Rondas: 5
- Build con `--no-cache`: mediana **7.026 s**, rango **6.733–7.365 s**.
- A y B reutilizan la misma imagen Docker y el mismo Dockerfile; por tanto, este resultado es **un build compartido**, no dos builds arquitectónicos diferentes.
- No se midió despliegue completo, arranque de PostgreSQL ni tiempo de disponibilidad funcional. H4 permanece separado.

## Evidencia

- `build.csv` contiene los tiempos por ronda.
- `logs/` contiene salida de Docker build por ronda.

## Interpretación

El build se repite sin caché de capas para reducir el efecto de builds incrementales. Los tiempos pueden variar por CPU, disco, red y dependencias descargadas. No comparar con otra máquina como si fuera una prueba controlada.
