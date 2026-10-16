import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {hash,validateJudge,requireThat} from '../m7-evals/evals/core.mjs';
const support=path.dirname(fileURLToPath(import.meta.url)),root=path.resolve(support,'../m7-evals');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const save=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');
const source=path.join(support,'juez_independiente');
const input=read(path.join(source,'entrada.json')),map=read(path.join(source,'mapa.json')),output=read(path.join(source,'dictamenes.json'));
requireThat(output.records.length===36 && new Set(output.records.map(r=>r.id)).size===36,'36 dictámenes únicos requeridos');
const dataset=read(path.join(root,'dataset/golden.json')),rubric=fs.readFileSync(path.join(root,'evals/rubrica.txt'),'utf8');
requireThat(input.rubric===rubric,'Rúbrica distinta');
for(const m of map){
 const j=output.records.find(r=>r.id===m.blind_id),i=input.records.find(r=>r.id===m.blind_id);
 requireThat(j&&i&&hash(i.response)===m.response_hash,'Mapeo inválido');validateJudge(j);
 const c=dataset.cases.find(c=>c.id===m.id);
 requireThat(c.question===i.question&&c.context===i.context,'Contexto distinto');
}
const archive=path.join(root,'historial/juez_llama_aclarado');
fs.mkdirSync(archive,{recursive:true});
const keep=(relative,name)=>{const dest=path.join(archive,name);if(!fs.existsSync(dest))fs.copyFileSync(path.join(root,relative),dest);};
for(const v of ['v1','v2','v3']){keep(`evals/dictamenes/${v}.json`,`dictamenes_${v}.json`);keep(`resultados/${v}.json`,`resultado_${v}.json`);}
keep('resultados/calibracion.json','calibracion.json');keep('resultados/comparacion.json','comparacion.json');keep('evals/rubrica.txt','rubrica.txt');
const dest=path.join(root,'evals/juez_independiente');fs.mkdirSync(dest,{recursive:true});
for(const f of ['entrada.json','mapa.json','dictamenes.json'])fs.copyFileSync(path.join(source,f),path.join(dest,f));
const metadata={provider:'agente independiente de Codex',model:output.model_identity,model_digest:null,options:{configuration:'Heredada de la sesión; parámetros de muestreo y digest no expuestos'},model_identity_basis:output.model_identity_basis};
save(path.join(dest,'protocolo.json'),{...metadata,created_at:new Date().toISOString(),authorization:'El usuario autorizó explícitamente un segundo agente como juez.',procedure:'Un agente nuevo, sin historial de conversación, recibió únicamente entrada.json: rúbrica y 36 registros mezclados sin versiones, puntos esperados, notas humanas ni dictámenes anteriores. Emitió todas las notas en una única revisión; el agente principal solo validó estructura e integridad y las vinculó mediante mapa.json. No se solicitó lograr PASA ni se editaron sus notas.',limitations:'El agente principal preparó los prompts y seleccionó el juez después de observar errores del juez local. Es una corrección del instrumento de medida posterior a la observación. Se reevaluaron las tres versiones con el mismo juez y rúbrica; se preservan los resultados anteriores. El orden mezclado no impide que un juez infiera patrones entre respuestas similares.',input_hash:hash(input),output_hash:hash(output)});
for(const v of ['v1','v2','v3']){
 const responses=read(path.join(root,`respuestas/${v}.json`));
 const records=responses.records.map(r=>{const m=map.find(m=>m.version===v&&m.id===r.id);requireThat(hash(r.response)===m.response_hash,'Respuesta alterada');const j=output.records.find(j=>j.id===m.blind_id);return {id:r.id,response_hash:m.response_hash,score:j.score,reason:j.reason,unsupported_claims:j.unsupported_claims,blind_id:m.blind_id};});
 save(path.join(root,`evals/dictamenes/${v}.json`),{version:v,created_at:new Date().toISOString(),...metadata,dataset_hash:hash(dataset),rubric_hash:hash(rubric),responses_hash:hash(responses),records});
}
console.log('36 dictámenes importados íntegros. Juez:',metadata.model);
