import fs from 'node:fs';
const root=new URL('../m7-evals/',import.meta.url);
const read=p=>JSON.parse(fs.readFileSync(new URL(p,root),'utf8'));
const write=(p,t)=>fs.writeFileSync(new URL(p,root),t);
const a=read('resultados/analisis.json'),cal=read('resultados/calibracion.json');
a.findings[6]='EV-04/v1 reconoce que desconoce el stock, pero añade atención al cliente y una página de productos no documentadas. En v2 recomienda contactar genéricamente a UMSS Market; el juez lo considera una recomendación sin afirmar un canal específico. La v3 responde únicamente que no puede confirmar stock.';
a.findings[8]='La v3 se desarrolló observando v1/v2 sobre estos mismos casos. No se utilizó un conjunto de prueba separado. Se conservaron respuestas originales y no se cambiaron dataset ni umbrales. La rúbrica se aclaró y el juez se sustituyó por errores observados; las tres versiones finales se evaluaron con el mismo instrumento revisado. Se declara este cambio posterior, sin atribuir toda la diferencia respecto de los reportes locales al prompt.';
a.findings[10]='La primera preparación del juez local con JSON libre se interrumpió al faltar una lista válida de afirmaciones sin respaldo. Se añadió un esquema obligatorio. Finalmente se utilizó un agente independiente autorizado, cuya salida íntegra se conserva en evals/juez_independiente/dictamenes.json. record.mjs conserva la opción de juicio local, pero no produjo las notas finales del agente.';
a.findings[12]='El juez Llama obtuvo 33.33% de acuerdo y kappa 0.20 tanto antes como después de aclarar la rúbrica. Persistieron errores en negaciones y abstenciones. Con autorización del equipo, un agente basado en GPT-6 reevaluó las 36 respuestas mezcladas, sin ver versiones ni notas humanas. Su identificador exacto y parámetros no están expuestos. Se conservaron ambos antecedentes locales y no se editaron notas del agente.';
a.calibration_discussion=[`La calibración final obtuvo ${cal.agreement_percent.toFixed(2)}% de acuerdo (4 de 6) y kappa ${cal.kappa.toFixed(4)}. Es una muestra pequeña: no demuestra que el juez esté validado para producción.`,
'EV-05: humano 1 y juez 0. La respuesta afirma envío a costo cero sin tarifa registrada; el juez señala una invención decisiva. Se conserva la etiqueta humana original.',
'EV-09: humano 0.5 y juez 1. El texto se abstiene y deriva al vendedor como indica el contexto. El juez separa la omisión de los dos precios, medida en completitud, de la fidelidad. No se proporcionó justificación humana individual.',
'El acuerdo fue calculado con las seis notas originales, sin corregirlas tras ver al juez. Las observaciones cualitativas del informe no son nuevas etiquetas humanas.'];
write('resultados/analisis.json',JSON.stringify(a,null,2)+'\n');
let r=fs.readFileSync(new URL('README.md',root),'utf8');
const start=r.indexOf('Ollama debe tener'),end=r.indexOf('```powershell',start);
r=r.slice(0,start)+`Las 36 respuestas se generaron realmente con Ollama y llama3.2:3b. nomic-embed-text produce embeddings y no sirve para redactar respuestas. El juez local Llama presentó errores y Qwen no terminó de cargar. Ambos intentos se documentan; no es necesario volver a ejecutarlos.

Juez final: agente independiente de Codex, basado en GPT-6 según sus instrucciones. El identificador exacto, digest y parámetros de muestreo no están expuestos. El usuario autorizó expresamente su participación. Recibió únicamente la rúbrica final y 36 registros mezclados sin versiones ni etiquetas humanas. Su entrada, salida íntegra, mapa y protocolo están en evals/juez_independiente/. Todas las versiones se calificaron con ese mismo juez; sus notas no fueron retocadas.

La calibración final obtuvo 66.67% de acuerdo y kappa 0.4286. Los dos antecedentes locales obtuvieron 33.33% y 0.20. Se conservaron en historial/juez_llama/ e historial/juez_llama_aclarado/. El cambio de juez fue posterior a observar errores y se declara como limitación del diseño. No se cambiaron respuestas ni etiquetas humanas.

Los siguientes comandos describen la preparación con Ollama. La opción judge ejecuta el juez local, NO reproduce nuevas notas del agente GPT-6. Para un nuevo juicio del agente se debe proporcionarle entrada.json en una sesión independiente con la misma rúbrica y guardar su salida; no hay un endpoint GPT-6 configurado en este paquete. La defensa reproduce los dictámenes ya grabados.

`+r.slice(end);
r=r.replace('No cambiar dataset, rúbrica ni umbrales para conseguir PASA.','En un nuevo experimento, fijar dataset, rúbrica y umbrales antes de comparar. Esta entrega preserva y documenta la revisión del instrumento de juicio.');
r=r.replace('Los archivos registran modelo, digest, fecha, parámetros y hashes.','Las respuestas Ollama registran modelo, digest, fecha, parámetros y hashes. El juez final registra la identidad disponible, fecha y hashes; no se inventan parámetros ni digest.');
r=r.replace('## Estructura prevista de entrega','## Estructura de entrega');
write('README.md',r);
