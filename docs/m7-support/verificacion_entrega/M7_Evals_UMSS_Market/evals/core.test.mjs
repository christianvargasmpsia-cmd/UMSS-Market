import test from 'node:test';
import assert from 'node:assert/strict';
import {read,readText} from './io.mjs';
import {evaluate,hash,textMetrics,calibration,validateDataset,validateJudge} from './core.mjs';
const dataset=read('dataset/golden.json'), rubric=readText('evals/rubrica.txt'),thresholds=read('evals/config.json').thresholds;
function fixture() {
  const responses={version:'v3',dataset_hash:hash(dataset),records:dataset.cases.map(c=>({id:c.id,response:c.key_points.map(k=>k.alternatives[0]).join('. ')}))};
  const judgments={version:'v3',dataset_hash:hash(dataset),rubric_hash:hash(rubric),responses_hash:hash(responses),records:responses.records.map(r=>({id:r.id,response_hash:hash(r.response),score:1,reason:'Fixture de test, no evidencia académica.',unsupported_claims:[]}))};
  return {responses,judgments};
}
const run=(r,j,t=thresholds,d=dataset)=>evaluate(d,r,j,t,rubric);
test('conjunto completo correcto aprueba',()=>{const {responses:r,judgments:j}=fixture();assert.equal(run(r,j).exit_code,0);});
test('normaliza acentos, espacios y admite alternativas',()=>{assert.equal(textMetrics({key_points:[{description:'Dato',alternatives:['otra','ENVÍO GRATIS']}],forbidden:[]},'envio   gratis').completeness,1);});
test('un prohibido hace cero el caso completo',()=>{const c={key_points:[{description:'a',alternatives:['dato']}],forbidden:['gratis','aprobado']};assert.equal(textMetrics(c,'dato gratis').without_prohibited,0);assert.equal(textMetrics(c,'sin dato').completeness,1);});
test('ausencia de punto baja completitud',()=>{assert.equal(textMetrics(dataset.cases[0],'Sin información').completeness,0);});
test('crítico infiel bloquea aun con promedio mayor a 0.85',()=>{const {responses:r,judgments:j}=fixture();j.records.find(x=>x.id==='EV-04').score=0.5;const out=run(r,j);assert.ok(out.metrics.fidelity>0.85);assert.equal(out.exit_code,1);assert.equal(out.metrics.critical_failed,1);});
test('fidelidad global baja bloquea',()=>{const {responses:r,judgments:j}=fixture();j.records.forEach(x=>x.score=0);assert.equal(run(r,j).status,'NO PASA');});
test('regresión: un precio inventado no recibe métricas perfectas por evidencia técnica',()=>{const {responses:r,judgments:j}=fixture();r.records[0].response='El producto cuesta 999999 bolivianos.';j.responses_hash=hash(r);j.records[0].response_hash=hash(r.records[0].response);j.records[0].score=0;j.records[0].reason='Precio sin respaldo';const c=run(r,j).cases[0];assert.equal(c.completeness,0);assert.equal(c.fidelity,0);});
test('prohibido crítico bloquea',()=>{const {responses:r,judgments:j}=fixture();const rec=r.records.find(x=>x.id==='EV-12');rec.response+=' PROMOCION_FALSA_APLICADA';j.responses_hash=hash(r);j.records.find(x=>x.id===rec.id).response_hash=hash(rec.response);assert.equal(run(r,j).exit_code,1);});
test('falta de respuesta o juez no se omite del promedio',()=>{for(const kind of ['responses','judgments']){const f=fixture();f[kind].records.pop();assert.throws(()=>run(f.responses,f.judgments));}});
test('duplicados e IDs extra invalidan',()=>{for(const field of ['responses','judgments']){for(const id of ['EV-01','EV-99']){const f=fixture();f[field].records[1].id=id;if(field==='responses')f.judgments.responses_hash=hash(f.responses);assert.throws(()=>run(f.responses,f.judgments));}}});
test('hashes detectan cambios y versiones incompatibles',()=>{for(const field of ['dataset_hash','rubric_hash','responses_hash','version']){const {responses:r,judgments:j}=fixture();j[field]='incorrecto';assert.throws(()=>run(r,j));}const {responses:r,judgments:j}=fixture();j.records[0].response_hash='incorrecto';assert.throws(()=>run(r,j));});
test('respuesta vacía invalida',()=>{const {responses:r,judgments:j}=fixture();r.records[0].response='';j.responses_hash=hash(r);assert.throws(()=>run(r,j));});
test('umbrales inválidos nunca aprueban',()=>{for(const value of [undefined,null,-1,2,'0.85',NaN]){const {responses:r,judgments:j}=fixture();assert.throws(()=>run(r,j,{...thresholds,fidelity:value}));}const f=fixture();assert.throws(()=>run(f.responses,f.judgments,{...thresholds,critical_failed:1}));});
test('rúbrica valida escala y justificación',()=>{for(const score of [null,'1',2,-1])assert.throws(()=>validateJudge({score,reason:'a',unsupported_claims:[]}));assert.throws(()=>validateJudge({score:1,reason:'',unsupported_claims:[]}));assert.throws(()=>validateJudge({score:1,reason:'a'}));});
test('dataset exige 12, tipos, críticos, puntos y campos válidos',()=>{for(const mutate of [d=>d.cases.pop(),d=>d.cases[1].id=d.cases[0].id,d=>d.cases.forEach(c=>c.type='hecho'),d=>d.cases.forEach(c=>c.critical=false),d=>d.cases[0].question='',d=>d.cases[0].context=null,d=>d.cases[0].key_points=[],d=>d.cases[0].key_points[0].alternatives=[''],d=>d.cases[0].forbidden=['']]){const d=structuredClone(dataset);mutate(d);assert.throws(()=>validateDataset(d));}});
test('calibración conocida: acuerdo 83.33%, kappa 0.75',()=>{const pairs=[0,0,0.5,0.5,1,1].map((human,i)=>({id:String(i),version:'v1',human,judge:i===0?0.5:human}));const c=calibration(pairs);assert.ok(Math.abs(c.agreement_percent-83.333333)<0.0001);assert.ok(Math.abs(c.kappa-0.75)<1e-9);});
test('acuerdo total degenerado informa kappa indefinido',()=>{assert.equal(calibration(Array.from({length:6},(_,i)=>({id:String(i),version:'v1',human:1,judge:1}))).kappa,null);});
test('calibración rechaza menos de seis, duplicados y notas inválidas',()=>{assert.throws(()=>calibration([]));assert.throws(()=>calibration(Array(6).fill({id:'a',version:'v1',human:1,judge:1})));assert.throws(()=>calibration(Array.from({length:6},(_,i)=>({id:String(i),version:'v1',human:null,judge:1}))));});
