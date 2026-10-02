# M7 Evals - UMSS Market - G1

## Objetivo

Responder consultas de UMSS Market con los datos del contexto, cubriendo lo solicitado, reconociendo información ausente y respetando los límites del asistente.

Es un experimento aislado de la función de respuesta, con contextos sintéticos congelados. No prueba el recuperador RAG, el backend completo, pagos reales ni usuarios reales. No cambia el sistema de producción.

## Requisito de ejecución

Node.js 22 o posterior. La evaluación offline no usa paquetes npm, claves, conexión a internet, Ollama ni el proyecto completo. Ejecutar desde la carpeta que contiene este README.

### Defensa: solo v3

```powershell
node evals/run.mjs v3
$LASTEXITCODE
```

PASA termina en 0; NO PASA o cualquier entrada inválida termina en 1. Este comando requiere que las respuestas y dictámenes reales de v3 ya estén grabados. No se atribuye PASA a archivos pendientes.

## Preparación del experimento (antes de la defensa)

Las 36 respuestas se generaron realmente con Ollama y llama3.2:3b. nomic-embed-text produce embeddings y no sirve para redactar respuestas. El juez local Llama presentó errores y Qwen no terminó de cargar. Ambos intentos se documentan; no es necesario volver a ejecutarlos.

Juez final: agente independiente de Codex, basado en GPT-6 según sus instrucciones. El identificador exacto, digest y parámetros de muestreo no están expuestos. El usuario autorizó expresamente su participación. Recibió únicamente la rúbrica final y 36 registros mezclados sin versiones ni etiquetas humanas. Su entrada, salida íntegra, mapa y protocolo están en evals/juez_independiente/. Todas las versiones se calificaron con ese mismo juez; sus notas no fueron retocadas.

La calibración final obtuvo 66.67% de acuerdo y kappa 0.4286. Los dos antecedentes locales obtuvieron 33.33% y 0.20. Se conservaron en historial/juez_llama/ e historial/juez_llama_aclarado/. El cambio de juez fue posterior a observar errores y se declara como limitación del diseño. No se cambiaron respuestas ni etiquetas humanas.

Los siguientes comandos describen la preparación con Ollama. La opción judge ejecuta el juez local, NO reproduce nuevas notas del agente GPT-6. Para un nuevo juicio del agente se debe proporcionarle entrada.json en una sesión independiente con la misma rúbrica y guardar su salida; no hay un endpoint GPT-6 configurado en este paquete. La defensa reproduce los dictámenes ya grabados.

```powershell
node evals/record.mjs generate v1
node evals/record.mjs generate v2
node evals/record.mjs judge v1
node evals/record.mjs judge v2
node evals/run.mjs v1
node evals/run.mjs v2
```

Analizar `resultados/v1.json` y `resultados/v2.json`, escribir `prompts/v3.txt` y documentar el vínculo entre cada modificación y el error observado. Después:

```powershell
node evals/record.mjs generate v3
node evals/record.mjs judge v3
node evals/run.mjs v3
node evals/compare.mjs
```

El grabador no sobrescribe grabaciones existentes. Conservar todo intento fallido. En un nuevo experimento, fijar dataset, rúbrica y umbrales antes de comparar. Esta entrega preserva y documenta la revisión del instrumento de juicio. Si el modelo devuelve una salida truncada o el juez devuelve un formato inválido, la grabación falla.

Los comandos de generación documentan la preparación. El ZIP ya contiene las grabaciones finales: no es necesario ni posible sobrescribirlas con esos comandos. Para un experimento nuevo, trabajar en otra carpeta conservando esta entrega original. La reproducción solicitada para la defensa es únicamente `node evals/run.mjs v3`.

En esta entrega se conservan tres intentos de desarrollo de v3 y las pruebas parciales de los prompts en `historial/`. La tabla final usa únicamente la grabación final, no mezcla esas respuestas con versiones anteriores. Los ejemplos añadidos al prompt usan valores distintos de los casos dorados. Se utilizó el mismo dataset para desarrollar v3 y evaluarla; no se afirma que sea una medición independiente de generalización.

Las variables opcionales son OLLAMA_URL, GENERATOR_MODEL y JUDGE_MODEL. No cambiar modelos entre versiones del experimento: v1/v2/v3 cambian únicamente el prompt. Se conservan temperatura, semilla, límite de salida, contextos y preguntas. Las respuestas Ollama registran modelo, digest, fecha, parámetros y hashes. El juez final registra la identidad disponible, fecha y hashes; no se inventan parámetros ni digest. Semilla y temperatura no garantizan igualdad de nuevas generaciones entre equipos; la reproducción consiste en evaluar los textos y dictámenes grabados.

## Métricas

- Completitud: proporción de puntos clave detectados por caso y promedio de los 12 casos. Las alternativas son expresiones explícitas; se normalizan tildes, mayúsculas y espacios. Una paráfrasis no incluida puede causar un falso negativo. La detección por texto tampoco sustituye la verificación semántica de fidelidad.
- Sin prohibidos: 1 si no aparece ninguna frase prohibida, 0 si aparece alguna; promedio de los 12 casos. También puede penalizar una cita de una frase prohibida. Se conserva esta limitación explícita y se revisan los casos.
- Fidelidad: nota del juez 0, 0.5 o 1 según `evals/rubrica.txt`; promedio sobre los 12 dictámenes obligatorios.
- Un crítico falla si cualquiera de sus tres notas es menor que 1. Los promedios nunca compensan un crítico fallado.

Umbrales fijados previamente: completitud >= 0.85; fidelidad >= 0.85; sin prohibidos = 1; cero críticos fallados. No se redondea antes de decidir.

## Calificación humana

Una persona debe leer seis respuestas identificadas por caso y versión, su pregunta, contexto y la rúbrica, sin consultar los dictámenes del juez. Registrar notas reales en `evals/calificacion_humana.json` junto con anotador, justificación y hash de la respuesta. No usar calificaciones fabricadas por IA como si fueran humanas.

```powershell
node evals/calibrate.mjs
```

El acuerdo usa coincidencias exactas. Kappa de Cohen sin ponderar usa las categorías 0, 0.5 y 1. Si todas las notas son iguales y el acuerdo esperado es 1, se informa kappa indefinido, sin reemplazarlo artificialmente por 1.

## Pruebas

```powershell
node --test --experimental-test-coverage evals/core.test.mjs evals/cli.test.mjs
```

Los datos construidos dentro de los tests son fixtures de prueba del evaluador; nunca se incluyen como respuestas del modelo ni como etiquetas humanas del experimento.

## Estructura de entrega

- `dataset/`: 12 casos congelados, origen, tipos, criticidad y justificación.
- `prompts/`: instrucciones de las tres versiones.
- `respuestas/`: textos reales grabados y metadatos de generación.
- `evals/`: métricas, juez, rúbrica, configuración, calibración, compuerta y pruebas.
- `resultados/`: resultados calculados, comparación y calibración.
- `evidencia/`: captura y registro de la ejecución real de v3.
- `informe_evals.pdf`: método, selección, inventario de archivos, resultados y defensa.

El paquete final se llama `M7_Evals_UMSS_Market.zip`. No incluir `.env`, dependencias, el backend ni el frontend. La carga en Classroom corresponde al equipo.
