import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../m7-evals');
const purposes={
 'README.md':'Requisitos, preparación, ejecución offline, limitaciones y comandos de verificación.',
 'dataset/golden.json':'12 preguntas y contextos ficticios congelados; puntos clave, alternativas, prohibidos, tipos, criticidad y motivos de selección.',
 'evals/config.json':'Parámetros fijos de generación y juez; umbrales fijados antes de observar resultados y su justificación.',
 'evals/core.mjs':'Validación, métricas, compuerta, integridad SHA-256, acuerdo y kappa.',
 'evals/io.mjs':'Lectura y escritura de archivos relativas a la raíz del paquete; grabaciones sin sobrescritura.',
 'evals/run.mjs':'Evaluación offline de una versión; muestra métricas y devuelve 0 o 1.',
 'evals/record.mjs':'Llamadas reales a Ollama para generar respuestas o registrar dictámenes del juez. Solo preparación.',
 'evals/rubrica.txt':'Instrucciones completas del juez, escala 0/0.5/1 y defensa contra instrucciones insertadas en los datos.',
 'evals/calibrate.mjs':'Compara etiquetas humanas con el juez sobre las mismas respuestas y calcula acuerdo y kappa.',
 'evals/compare.mjs':'Verifica que solo varíe el prompt del generador y recalcula la comparación de las tres versiones.',
 'evals/core.test.mjs':'Pruebas unitarias de métricas, criticidad, integridad, errores y calibración.',
 'evals/cli.test.mjs':'Prueba integral de comandos con servidor local ficticio y archivos temporales; sus respuestas son solo datos de test.',
 'evals/calificacion_humana.json':'Seis notas reales del usuario, identificadas por caso, versión y hash; registra que no proporcionó justificaciones individuales.',
 'resultados/calibracion.json':'Pares humano-juez, porcentaje de acuerdo, kappa y justificaciones.',
 'resultados/comparacion.json':'Resultados completos de v1/v2/v3 recalculados con criterios idénticos.',
 'resultados/analisis.json':'Errores observados, diferencias exactas entre prompts y explicación de correcciones de v3.',
 'resultados/inventario.json':'Descripción de cada archivo entregado; fuente del inventario del PDF.',
 'evidencia/ejecucion_v3.json':'Comando, salida textual, código del proceso real, fecha y duración de la evaluación de v3.',
 'evidencia/compuerta_v3.png':'Captura visual de la salida real y el código observado del proceso, sin simular una consola.',
 'evidencia/tests.txt':'Salida real de las pruebas y cobertura del evaluador.',
 'informe_evals.pdf':'Documento final con método, selección de casos, comparación, calibración, correcciones, inventario y defensa.',
 'historial/v3_intento1/prompt.txt':'Primer prompt de desarrollo de v3, conservado para transparencia.',
 'historial/v3_intento1/respuestas.json':'12 respuestas reales del primer intento de v3; no se sobrescribieron.',
 'historial/v3_intento1/revision.json':'Errores detectados por inspección y métricas literales del intento descartado. No afirma una evaluación completa del juez.'
};
for(const v of ['v1','v2','v3']){
 purposes[`prompts/${v}.txt`]=`Prompt íntegro del generador para ${v}; única variable del experimento.`;
 purposes[`respuestas/${v}.json`]=`12 respuestas reales de ${v}, modelo y digest, fecha, parámetros y hashes.`;
 purposes[`evals/dictamenes/${v}.json`]=`12 calificaciones del agente independiente para ${v}, justificación, ID ciego y hashes; sin edición de sus notas.`;
 purposes[`resultados/${v}.json`]=`Métricas por caso y globales de ${v}, umbrales y decisión de compuerta.`;
}
fs.mkdirSync(path.join(root,'resultados'),{recursive:true});
const historyPurposes={
 'historial/juez_llama/rubrica.txt':'Rúbrica original del juez antes de aclarar los errores observados en la calibración.',
 'historial/juez_llama/calibracion.json':'Calibración inicial: 33.33% de acuerdo y kappa 0.20. No se mezcla con la calibración final.',
 'historial/juez_llama/resultado_v1.json':'Evaluación inicial de v1 con el juez anterior; se conserva como antecedente, no como resultado final.',
 'historial/juez_llama/dictamenes_v1.json':'Dictámenes originales de Llama para v1 antes de aclarar la rúbrica.',
 'historial/juez_llama/dictamenes_v2.json':'Dictámenes originales de Llama para v2 antes de aclarar la rúbrica.'
};
Object.assign(purposes,historyPurposes);
for(const v of ['v1','v2','v3']){
 purposes[`historial/juez_llama_aclarado/dictamenes_${v}.json`]='Juicios locales de Llama con rúbrica aclarada, conservados íntegros antes del cambio de juez.';
 purposes[`historial/juez_llama_aclarado/resultado_${v}.json`]='Resultado previo con juez local; permite auditar por qué se revisó el instrumento de evaluación.';
}
for(const f of ['calibracion.json','comparacion.json','rubrica.txt'])purposes[`historial/juez_llama_aclarado/${f}`]='Antecedente del juez local con rúbrica aclarada, separado de los resultados finales.';
Object.assign(purposes,{
 'evals/juez_independiente/entrada.json':'Rúbrica y 36 registros mezclados que recibió el agente, sin versiones ni etiquetas humanas.',
 'evals/juez_independiente/mapa.json':'Correspondencia de IDs ciegos con caso, versión y hash; no fue mostrada al juez.',
 'evals/juez_independiente/dictamenes.json':'Salida íntegra del agente independiente con las 36 notas y su identidad declarada.',
 'evals/juez_independiente/protocolo.json':'Autorización, identidad del juez, límites de identificación, procedimiento y hashes de entrada/salida.'
});
for(const n of [2,3]){
 purposes[`historial/v3_intento${n}/prompt.txt`]=`Prompt del intento de desarrollo ${n} de v3, conservado para trazabilidad.`;
 purposes[`historial/v3_intento${n}/respuestas.json`]=`Respuestas reales del intento ${n}, descartado antes de una evaluación completa del juez.`;
 purposes[`historial/v3_intento${n}/revision.json`]=`Errores observados y métricas literales del intento ${n}; no es un reporte de compuerta completo.`;
}
for(const file of fs.readdirSync(path.join(root,'historial/v3_prueba_prompt'))){
 purposes[`historial/v3_prueba_prompt/${file}`]='Prueba parcial de desarrollo con prompt completo y respuestas reales; conserva los intentos de corrección y no se usa como conjunto de prueba independiente.';
}
fs.writeFileSync(path.join(root,'resultados/inventario.json'),JSON.stringify(Object.entries(purposes).sort(([a],[b])=>a.localeCompare(b)).map(([path,purpose])=>({path,purpose})),null,2)+'\n');
