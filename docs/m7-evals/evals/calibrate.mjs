import {read,save} from './io.mjs';
import {calibration,requireThat,hash} from './core.mjs';
try {
  const labels=read('evals/calificacion_humana.json');
  requireThat(typeof labels.annotator==='string'&&labels.annotator.trim(),'Falta anotador humano');
  requireThat(labels.independent===true,'Falta confirmar calificación sin ver las notas del juez');
  const pairs=labels.records.map(r=>{
    requireThat(['v1','v2','v3'].includes(r.version),'Versión inválida');
    const response=read(`respuestas/${r.version}.json`).records.find(x=>x.id===r.id);
    const j=read(`evals/dictamenes/${r.version}.json`).records.find(x=>x.id===r.id);
    requireThat(response && j && r.response_hash===hash(response.response)&&j.response_hash===r.response_hash,'La etiqueta no corresponde a la respuesta');
    return {version:r.version,id:r.id,human:r.score,judge:j.score,human_reason:r.reason,judge_reason:j.reason};
  });
  const result={annotator:labels.annotator,...calibration(pairs)};
  save('resultados/calibracion.json',result);
  console.log(JSON.stringify(result,null,2));
} catch(e) {console.error(e.message);process.exitCode=1;}
