# E3 - Hipotesis del experimento

## Pregunta
¿Conviene extraer el modulo de resenas de UMSS Market
a un servicio independiente?

## Forma A
Monolito modular Spring Boot.

## Forma B
Servicio de resenas extraido mediante HTTP y Toxiproxy.

## Hipotesis H1 - Latencia
La forma B aumentara el p95 de la ruta dependiente
en no mas de 15 ms respecto a la forma A,
durante funcionamiento normal.

## Hipotesis H2 - Aislamiento
Durante la falla de B, la ruta independiente
mantendra menos del 1% de errores.

## Hipotesis H3 - Recuperacion
Despues de retirar la falla, el p95 de B
no superara el 120% del p95 previo a la falla.

## Hipotesis H4 - Arranque
El tiempo de arranque de B no superara
en mas del 30% el tiempo de arranque de A.

## Metodologia
- k6 con ritmo fijo.
- Dos rutas GET.
- Fases antes, durante y despues.
- Falla mediante Toxiproxy en B.
- Cinco corridas como minimo por forma.
- Descartar calentamiento.
- Mismo seed y UUID existentes.
- Docker Compose down -v entre corridas.
- Medir build y arranque por forma.
- Reportar mediana y rango de cada metrica.

## Criterio de decision
Se considerara favorable la extraccion solamente
si ofrece una ventaja comprobable de aislamiento
o resiliencia que justifique sus costos adicionales.

La decision definitiva y su condicion numerica
de reversion se estableceran en el ADR,
utilizando resultados reales.

## Estado
Hipotesis previas a las mediciones.
Sin resultados experimentales.
