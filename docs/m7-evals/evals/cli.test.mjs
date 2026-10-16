// Integración en carpeta temporal con servidor de prueba; nunca crea evidencia académica.
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import http from 'node:http';
import {spawn} from 'node:child_process';
import {root} from './io.mjs';
import {hash} from './core.mjs';
function command(cwd,args,env={}) {return new Promise(resolve=>{const child=spawn(process.execPath,args,{cwd,env:{...process.env,...env},windowsHide:true});let stdout='',stderr='';child.stdout.on('data',x=>stdout+=x);child.stderr.on('data',x=>stderr+=x);child.on('close',code=>resolve({code,stdout,stderr}));});}
test('CLI: genera, juzga, compara, calibra y falla cerrado con entradas rotas',async()=>{
  const temp=fs.mkdtempSync(path.join(os.tmpdir(),'umss-m7-test-'));
  for(const dir of ['dataset','prompts','evals'])fs.cpSync(path.join(root,dir),path.join(temp,dir),{recursive:true,filter:p=>!p.includes('dictamenes')&&!p.endsWith('calificacion_humana.json')});
  fs.writeFileSync(path.join(temp,'prompts/v3.txt'),'Prompt exclusivo del test');
  const dataset=JSON.parse(fs.readFileSync(path.join(temp,'dataset/golden.json'),'utf8'));
  let invalidJudge=false;
  const server=http.createServer(async(req,res)=>{
    res.setHeader('Content-Type','application/json');
    if(req.url==='/api/tags'){res.end(JSON.stringify({models:['llama3.2:3b','qwen2.5-coder:7b'].map(name=>({name,digest:'test-digest'}))}));return;}
    let body='';for await(const chunk of req)body+=chunk;
    const input=JSON.parse(body),user=JSON.parse(input.messages[1].content);
    const c=dataset.cases.find(c=>c.question===user.question);
    const content=input.format?JSON.stringify(invalidJudge?{score:1,reason:'Incompleto'}:{score:1,reason:'Solo test',unsupported_claims:[]}):c.key_points.map(k=>k.alternatives[0]).join('. ');
    res.end(JSON.stringify({done:true,done_reason:'stop',message:{content}}));
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const env={OLLAMA_URL:`http://127.0.0.1:${server.address().port}`};
  try {
    const missing=await command(temp,['evals/run.mjs','v3']);assert.equal(missing.code,1);assert.match(missing.stderr,/NO PASA/);
    for(const v of ['v1','v2','v3']){
      for(const mode of ['generate','judge']){const result=await command(temp,['evals/record.mjs',mode,v],env);assert.equal(result.code,0,result.stderr);}
      const result=await command(temp,['evals/run.mjs',v]);assert.equal(result.code,0,result.stderr);assert.match(result.stdout,/COMPUERTA: PASA/);
    }
    assert.equal((await command(temp,['evals/record.mjs','generate','v1'],env)).code,1,'No sobrescribe grabaciones');
    assert.equal((await command(temp,['evals/compare.mjs'])).code,0);
    const recorded=JSON.parse(fs.readFileSync(path.join(temp,'respuestas/v1.json'),'utf8'));
    const labels={annotator:'Anotador sintético exclusivo del test',independent:true,records:recorded.records.slice(0,6).map(r=>({id:r.id,version:'v1',response_hash:hash(r.response),score:1,reason:'Solo test'}))};
    fs.writeFileSync(path.join(temp,'evals/calificacion_humana.json'),JSON.stringify(labels));
    assert.equal((await command(temp,['evals/calibrate.mjs'])).code,0);
    labels.records[0].response_hash='alterado';fs.writeFileSync(path.join(temp,'evals/calificacion_humana.json'),JSON.stringify(labels));
    assert.equal((await command(temp,['evals/calibrate.mjs'])).code,1);
    const judgeFile=path.join(temp,'evals/dictamenes/v3.json');
    fs.renameSync(judgeFile,judgeFile+'.backup');invalidJudge=true;
    assert.equal((await command(temp,['evals/record.mjs','judge','v3'],env)).code,1);
    assert.equal(fs.existsSync(judgeFile),false,'Un juez inválido no crea una grabación final');
    assert.equal(fs.readdirSync(path.join(temp,'historial')).length,1,'Se conserva la salida inválida para diagnosticar');
    fs.renameSync(judgeFile+'.backup',judgeFile);invalidJudge=false;
    const j=JSON.parse(fs.readFileSync(judgeFile));j.records.pop();fs.writeFileSync(judgeFile,JSON.stringify(j));
    assert.equal((await command(temp,['evals/run.mjs','v3'])).code,1);
    assert.equal((await command(temp,['evals/run.mjs','../../otro'])).code,1);
    fs.appendFileSync(path.join(temp,'prompts/v1.txt'),' cambio no registrado');assert.equal((await command(temp,['evals/compare.mjs'])).code,1);
  } finally {server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
});
