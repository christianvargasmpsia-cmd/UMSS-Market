# ADR-0007 — Mantener el monolito modular para reseñas tras el experimento E3

- **Estado:** Aceptado para el alcance experimental de E3; revisión condicionada.
- **Fecha:** 2026-11-03 (fecha de los lotes experimentales proporcionados).
- **Proyecto:** UMSS-Market.
- **Ámbito:** módulo de reseñas de publicaciones.
- **Evidencia:** [`../../bench/e3/RESULTADOS_FINALES.md`](../../bench/e3/RESULTADOS_FINALES.md).

## Contexto

UMSS-Market incluye reseñas en un monolito modular Spring Boot. La propuesta E3 estudió si extraer las operaciones de reseñas a un servicio HTTP permite aislar fallas sin penalizaciones desproporcionadas de rendimiento y operación.

Se compararon dos alternativas que conservan el contrato HTTP externo y la misma instancia PostgreSQL:

- **A:** `ReviewsPort` con adaptador local dentro del proceso de la API.
- **B:** `ReviewsPort` con adaptador HTTP; API principal, Toxiproxy y servicio de reseñas en procesos separados. La API principal y el servicio de reseñas siguen compartiendo PostgreSQL E3.

Se definieron por adelantado cuatro criterios en `bench/e3/HIPOTESIS.md`: H1 +15 ms de p95, H2 <1% de errores de publicaciones durante falla, H3 p95 posterior ≤120% del previo y H4 arranque de B ≤130% del de A.

## Decisión

**Mantener A (monolito modular) como arquitectura por defecto, conservando B como opción experimental de extracción condicionada.** No retirar el puerto `ReviewsPort` ni sus adaptadores: permiten reconsiderar la decisión sin reescribir la lógica de negocio.

Motivos:

1. **El aislamiento de B sí aportó una ventaja en la falla ensayada:** se deshabilitó la comunicación con reseñas y las publicaciones siguieron disponibles (0/754 fallas, H2 cumple). Después de restaurar el proxy se verificó recuperación funcional (H3 cumple en 5/5 rondas).
2. **El sobrecosto de latencia de B cabe en el umbral predefinido, pero existe:** mediana del p95 de reseñas de 10.73 ms (A) frente a 15.89 ms (B), diferencia +5.16 ms (H1 cumple).
3. **El arranque funcional no cabe en el presupuesto declarado:** 6.92 s (A) frente a 9.43 s (B), sobrecosto +36.31% (H4 no cumple frente a +30%). En 4/5 parejas el incremento supera 30%.
4. **La independencia conseguida es parcial:** A y B comparten PostgreSQL; el servicio B agrega otro proceso, comunicación y configuración. No se han cuantificado costos monetarios ni de recursos.

La extracción **no se rechaza por principio**: su valor puede ser mayor donde la disponibilidad independiente de reseñas sea un requisito real y se justifiquen los costos adicionales.

## Alternativas consideradas

| Opción | Ventajas | Desventajas | Estado |
|---|---|---|---|
| A — Monolito modular | Menor latencia de reseñas en el ensayo, un proceso de aplicación, arranque más corto | Una falla interna del mismo proceso puede afectar otros módulos; sin despliegue aislado de reseñas | **Seleccionada por defecto** |
| B — Servicio de reseñas extraído | Falla de comunicación ensayada no afectó publicaciones; separación de proceso y despliegue | Mayor latencia, arranque +36.31%, tráfico HTTP y complejidad operativa; base de datos compartida | Conservada como alternativa |

## Consecuencias de la decisión

**Positivas:** se mantienen las propiedades modulares y el costo operativo más simple de A; la implementación de B y las evidencias quedan disponibles para una migración posterior; la decisión es cuantificable y reversible.

**Negativas:** no se obtiene por defecto el aislamiento de proceso de reseñas observado en B; una necesidad futura de desplegar reseñas de forma independiente exigirá reabrir el ADR.

**Riesgo residual:** una falla en PostgreSQL puede afectar a ambas variantes; el experimento no comparó escenarios de saturación, disponibilidad de host, infraestructura productiva ni pruebas de carga a tasas superiores.

## Condiciones de reconsideración y reversión

Se reconsiderará cambiar de A a B **si se justifica por requisitos concretos de disponibilidad** y, bajo un nuevo lote comparable de **al menos 5 rondas**, B logra simultáneamente:

1. **H1:** diferencia de medianas del p95 de reseñas B−A **≤15 ms**.
2. **H2:** errores de publicaciones durante la falla de comunicación con reseñas **<1%**.
3. **H3:** p95 después/antes de la falla de B **≤120%** en cada ronda para ambas rutas, con HTTP 200 restablecido.
4. **H4:** razón de medianas de arranque funcional **B/A ≤1.30**.
5. Una revisión operativa que explicite **uso de CPU, RAM y costo de despliegue** para ambos escenarios (todavía no medidos), así como un plan de reducción de dependencia del PostgreSQL compartido.

**Condición numérica de reversión propuesta:** si después de habilitar B un lote comparable de cinco rondas deja de cumplir cualquiera de los umbrales H1–H4, revertir la selección de adaptador a **A**, investigar y repetir la medición. Esta regla es una recomendación posterior al experimento, **no** una hipótesis preinscrita adicional. La conmutación debe realizarse con control operacional y verificando compatibilidad de datos, sin alterar ni borrar el volumen de PostgreSQL.

## Trazabilidad de resultados

| Hipótesis | Evidencia resumida | Resultado |
|---|---|---|
| H1 | +5.16 ms de diferencia de medianas p95; 5/5 parejas dentro de +15 ms | Cumple |
| H2 | 0/754 errores de publicaciones durante fallas inducidas | Cumple |
| H3 | Ratios p95 después/antes de ambas rutas dentro de 120% en 5/5 rondas | Cumple |
| H4 | Arranque A 6.92 s, B 9.43 s, B/A 136.31% | **No cumple** |

Trazas en `bench/e3/evidencias/formales/20261103-015431-209Z/`, `bench/e3/evidencias/fallas/20261103-025043-820Z/` y `bench/e3/evidencias/arranque/20261103-031456-419Z-f60cfb/`.

## Limitaciones de la evidencia

Se usó una sola máquina local, el mismo PostgreSQL, un seed de 100 reseñas y una carga nominal total de 10 solicitudes/s, con ventanas cortas. El p95 fue resumido como mediana de cinco p95 por ruta. La falla de H2 fue inducida mediante Toxiproxy, no simulando la caída del host ni de la base de datos. H3 usa ventanas sucesivas de k6, no un tiempo exacto de recuperación. H4 mide un reinicio de contenedores existentes y **arranque secuencial** de los dos procesos de B; excluye build, DB y despliegue completo. El protocolo se desvió del `down -v` previsto en `HIPOTESIS.md` para conservar los datos aislados y comparables.

Por estas limitaciones, la decisión aplica al **alcance de E3**; no generaliza automáticamente a producción.

## Revisión futura

Reabrir este ADR cuando existan nuevos requisitos cuantificados de aislamiento o cuando una optimización verificada reduzca el arranque de B por debajo del presupuesto H4, con pruebas repetibles de los demás umbrales y una evaluación de recursos.
