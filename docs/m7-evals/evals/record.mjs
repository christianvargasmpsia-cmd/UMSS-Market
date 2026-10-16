// Preparación con modelo real. La defensa utiliza run.mjs, sin red.
import {read,readText,saveNew,exists} from './io.mjs';
import {hash,requireThat,validateDataset,validateJudge} from './core.mjs';
const base=process.env.OLLAMA_URL||'http://localhost:11434';
async function request(endpoint,body) {
  const res=await fetch(`${base}/api/${endpoint}`,body?{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal:AbortSignal.timeout(600000)}:{signal:AbortSignal.timeout(10000)});
  requireThat(res.ok,`Ollama HTTP ${res.status}`);return res.json();
}
try {
  const [mode,version]=process.argv.slice(2);
  requireThat(['generate','judge'].includes(mode)&&['v1','v2','v3'].includes(version),'Uso: node evals/record.mjs generate|judge v1|v2|v3');
  const destination=mode==='generate'?`respuestas/${version}.json`:`evals/dictamenes/${version}.json`;
  requireThat(!exists(destination),`Ya existe ${destination}; no se sobrescribe ni se llama al modelo.`);
  const dataset=read('dataset/golden.json');validateDataset(dataset);
  const config=read('evals/config.json');
  const model=mode==='generate'?(process.env.GENERATOR_MODEL||'llama3.2:3b'):(process.env.JUDGE_MODEL||'llama3.2:3b');
  const tags=await request('tags');const installed=tags.models.find(m=>m.name===model);
  requireThat(installed,`Modelo no instalado: ${model}`);
  const prompt=readText(mode==='generate'?`prompts/${version}.txt`:'evals/rubrica.txt');
  const options=mode==='generate'?config.generation:config.judge;
  const responses=mode==='judge'?read(`respuestas/${version}.json`):null;
  const artifact={version,created_at:new Date().toISOString(),provider:'ollama-local',model,model_digest:installed.digest,options,dataset_hash:hash(dataset),...(mode==='generate'?{prompt_hash:hash(prompt)}:{rubric_hash:hash(prompt),responses_hash:hash(responses)}),records:[]};
  for(const c of dataset.cases) {
    const response=responses?.records.find(r=>r.id===c.id)?.response;
    const user=JSON.stringify({question:c.question,context:c.context,...(mode==='judge'?{response}: {})});
    const schema={type:'object',properties:{score:{type:'number',enum:[0,0.5,1]},reason:{type:'string'},unsupported_claims:{type:'array',items:{type:'string'}}},required:['score','reason','unsupported_claims'],additionalProperties:false};
    const output=await request('chat',{model,messages:[{role:'system',content:prompt},{role:'user',content:user}],stream:false,options,...(mode==='judge'?{format:schema}:{})});
    requireThat(output.done && output.done_reason!=='length','Salida truncada o incompleta');
    const raw=output.message?.content;
    requireThat(typeof raw==='string'&&raw.trim(),'Respuesta vacía del modelo');
    if(mode==='judge') {
      try { const j=JSON.parse(raw);validateJudge(j);artifact.records.push({id:c.id,response_hash:hash(response),...j,raw}); }
      catch(e) {saveNew(`historial/juez_invalido_${version}_${Date.now()}.json`,{...artifact,invalid_case:c.id,raw_error:raw,error:e.message});throw e;}
    }
    else artifact.records.push({id:c.id,response:raw});
    console.log(`${mode} ${version} ${c.id} registrado`);
  }
  saveNew(destination,artifact);
} catch(e) {console.error(e.message);process.exitCode=1;}
