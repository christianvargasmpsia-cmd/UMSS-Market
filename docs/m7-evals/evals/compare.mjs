import {read,readText,save} from './io.mjs';
import {evaluate,hash,requireThat} from './core.mjs';
try {
  const dataset=read('dataset/golden.json'), rubric=readText('evals/rubrica.txt'),config=read('evals/config.json');
  const artifacts=['v1','v2','v3'].map(v=>read(`respuestas/${v}.json`));
  const baseline=artifacts[0];
  for(const a of artifacts) {
    requireThat(a.model===baseline.model && a.model_digest===baseline.model_digest && hash(a.options)===hash(baseline.options),'Cambió más que el prompt entre versiones');
    requireThat(a.prompt_hash===hash(readText(`prompts/${a.version}.txt`)),'Prompt no corresponde a grabación');
  }
  const judges=artifacts.map(a=>read(`evals/dictamenes/${a.version}.json`));
  for(const j of judges) requireThat(j.model===judges[0].model && j.model_digest===judges[0].model_digest && hash(j.options)===hash(judges[0].options),'Juez no comparable');
  const rows=artifacts.map((a,i)=>evaluate(dataset,a,judges[i],config.thresholds,rubric));
  save('resultados/comparacion.json',{dataset_hash:hash(dataset),rows});
  console.table(rows.map(r=>({version:r.version,...r.metrics,gate:r.status})));
}catch(e){console.error(e.message);process.exitCode=1;}
