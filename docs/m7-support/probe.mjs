import fs from 'node:fs';
import {read,saveNew} from '../m7-evals/evals/io.mjs';
const [promptFile,name,...ids]=process.argv.slice(2);
const prompt=fs.readFileSync(promptFile,'utf8'),d=read('dataset/golden.json'),cfg=read('evals/config.json'),records=[];
for(const id of ids){const c=d.cases.find(c=>c.id===id);const r=await fetch('http://127.0.0.1:11435/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({model:'llama3.2:3b',messages:[{role:'system',content:prompt},{role:'user',content:JSON.stringify({question:c.question,context:c.context})}],stream:false,options:cfg.generation}),signal:AbortSignal.timeout(600000)});const out=await r.json();records.push({id,response:out.message?.content});console.log(id,out.message?.content);}
saveNew(`historial/v3_prueba_prompt/${name}.json`,{purpose:'Prueba parcial de desarrollo; no evaluación completa',model:'llama3.2:3b',options:cfg.generation,prompt,records});
