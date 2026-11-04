# Complemento E3: build y reproducción segura

Este complemento cubre dos pendientes metodológicos del experimento. No reemplaza ni modifica los resultados ya obtenidos.

## 1. Build

El Compose E3 usa `umss-e3-benchmark:local` para `api-a`, `api-b` y `reviews-b`; las arquitecturas A y B reutilizan el mismo Dockerfile e imagen. Por eso no sería correcto reportar dos tiempos de build distintos como si fueran dos artefactos de software distintos.

Ejecutar desde la raíz del repositorio:

```powershell
powershell -ExecutionPolicy Bypass -File bench/e3/medir-build-e3.ps1
```

El script realiza cinco builds sin caché de capas, guarda CSV y logs bajo `bench/e3/evidencias/build/<fecha>/`, y calcula mediana y rango. No borra contenedores, volúmenes ni evidencia anterior.

**Interpretación:** reportar como `build de imagen compartida A/B`. H4 continúa siendo el tiempo de arranque funcional medido por el script existente. No confundir build con arranque o despliegue.

Si la consigna requiere build por forma, explicar que ambas formas comparten la imagen; no existe una build distinta por arquitectura en este diseño.

## 2. Reproducción y datos

La consigna original menciona `docker compose down -v` entre corridas. En el experimento previo se preservó el volumen E3. Esa desviación debe declararse.

Para una repetición limpia, usar exclusivamente el volumen experimental `umss-e3_e3_pgdata` (confirmar nombre con `docker volume ls`) y respaldar si se necesita conservarlo. Nunca ejecutar `down -v` en un Compose de desarrollo o producción.

Procedimiento recomendado:
1. Confirmar rama y commit.
2. Confirmar `.env` local, sin compartirlo.
3. Levantar DB E3 aislada.
4. Inicializar el seed fijo y comprobar `1 user / 1 publication / 100 reviews`.
5. Ejecutar warmup descartado.
6. Ejecutar cinco corridas A/B con la misma imagen, tasa y duración.
7. Guardar cada lote en carpeta nueva; no sobrescribir evidencia.
8. Repetir falla y arranque bajo el mismo protocolo si se busca actualizar H2/H3/H4.
9. Registrar cualquier desviación.

## 3. Representatividad del seed

El seed de 100 reseñas es determinista y adecuado para validar que las rutas consultan IDs existentes. Sin embargo, es pequeño y no se debe describir como “tamaño parecido al real” sin justificarlo con datos del proyecto.

Añadir al informe:
- cantidad de usuarios, publicaciones y reseñas;
- si el seed representa o no la distribución real;
- que los resultados son locales y de una sola tasa de llegada;
- que no permiten inferir escalabilidad ni rendimiento en producción.

No ampliar el seed solo para “mejorar” resultados. Si se cambia, mantener A y B idénticos y volver a ejecutar las mediciones.

## 4. Prueba de timeout

Antes de añadir otra prueba, revisar `ReviewsHttpAdapterTest.java`: el proyecto ya tiene pruebas de respuestas 404, 500, JSON inválido y token ausente. Verificar si existe prueba explícita de timeout. Si no existe, añadir una prueba con un servidor HTTP de prueba que demore la respuesta más que `timeout-ms`, y comprobar el status esperado (normalmente 503) y que la prueba no espera indefinidamente.

No cambiar el timeout de producción para que la prueba pase. El test debe usar un valor pequeño y controlado.

## 5. Cierre documental

Actualizar `RESULTADOS_FINALES.md` y ADR únicamente después de generar evidencia nueva. Hasta entonces, agregar una nota:
- build no medido en el lote formal original;
- el complemento de build es una medición posterior;
- H4 sigue siendo 36.31% y NO CUMPLE;
- no se afirma que el volumen se haya eliminado entre las corridas originales;
- mantener la decisión reversible de monolito modular.
