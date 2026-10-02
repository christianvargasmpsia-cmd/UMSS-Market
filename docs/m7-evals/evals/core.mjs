import {createHash} from 'node:crypto';
export const hash = value => createHash('sha256').update(typeof value === 'string' ? value : JSON.stringify(value)).digest('hex');
export const normalize = value => String(value).normalize('NFD').replace(/\p{Diacritic}/gu,'').toLowerCase().replace(/\s+/g,' ').trim();
export const requireThat = (condition, message) => { if (!condition) throw new Error(message); };
const mean = values => values.reduce((a,b)=>a+b,0)/values.length;
export function validateDataset(dataset) {
  requireThat(dataset.cases?.length === 12, 'Se requieren exactamente 12 casos');
  requireThat(new Set(dataset.cases.map(c=>c.id)).size === 12, 'IDs duplicados');
  const types = ['hecho','escalar','fuera_de_alcance','seguridad','sin_respuesta'];
  requireThat(types.every(t=>dataset.cases.some(c=>c.type===t)), 'Falta un tipo de caso');
  requireThat(dataset.cases.filter(c=>c.critical).length >= 3, 'Faltan críticos');
  for (const c of dataset.cases) {
    requireThat(typeof c.id==='string' && types.includes(c.type) && typeof c.critical==='boolean', 'Metadatos de caso inválidos');
    requireThat(typeof c.question==='string' && c.question.trim() && typeof c.context==='string', `Pregunta/contexto inválidos: ${c.id}`);
    requireThat(c.key_points?.length>0 && c.key_points.every(k=>k.description && k.alternatives?.length && k.alternatives.every(a=>typeof a==='string' && a.trim())), `Puntos vacíos: ${c.id}`);
    requireThat(Array.isArray(c.forbidden) && c.forbidden.every(f=>typeof f==='string' && f.trim()), `Prohibidos inválidos: ${c.id}`);
  }
}
export function textMetrics(c, response) {
  const text=normalize(response);
  const points=c.key_points.map(k=>({description:k.description,covered:k.alternatives.some(a=>text.includes(normalize(a)))}));
  const prohibited=c.forbidden.filter(f=>text.includes(normalize(f)));
  return {completeness:mean(points.map(k=>Number(k.covered))),without_prohibited:prohibited.length?0:1,points,prohibited};
}
export function validateJudge(j) {
  requireThat([0,0.5,1].includes(j.score), 'Nota del juez inválida');
  requireThat(typeof j.reason==='string' && j.reason.trim(), 'Falta justificación del juez');
  requireThat(Array.isArray(j.unsupported_claims) && j.unsupported_claims.every(x=>typeof x==='string'), 'Faltan afirmaciones del juez');
}
export function evaluate(dataset, responses, judgments, thresholds, rubric) {
  validateDataset(dataset);
  requireThat(responses.dataset_hash===hash(dataset) && judgments.dataset_hash===hash(dataset),'Dataset cambiado');
  requireThat(judgments.rubric_hash===hash(rubric),'Rúbrica cambiada');
  requireThat(responses.version===judgments.version && ['v1','v2','v3'].includes(responses.version),'Versión inconsistente');
  requireThat(judgments.responses_hash===hash(responses),'Respuestas alteradas después del juicio');
  for(const [label, rows] of [['respuestas',responses.records],['dictámenes',judgments.records]]) {
    requireThat(Array.isArray(rows) && rows.length===12 && new Set(rows.map(r=>r.id)).size===12,`${label}: faltantes o duplicados`);
    requireThat(rows.every(r=>dataset.cases.some(c=>c.id===r.id)),`${label}: ID desconocido`);
  }
  for(const key of ['completeness','fidelity','without_prohibited']) requireThat(Number.isFinite(thresholds[key]) && thresholds[key]>=0 && thresholds[key]<=1,`Umbral inválido: ${key}`);
  requireThat(thresholds.critical_failed===0,'La compuerta exige cero críticos fallados');
  const cases=dataset.cases.map(c=>{
    const r=responses.records.find(r=>r.id===c.id), j=judgments.records.find(j=>j.id===c.id);
    requireThat(typeof r.response==='string' && r.response.trim(),`Respuesta vacía: ${c.id}`);
    requireThat(j.response_hash===hash(r.response),`Respuesta no corresponde al dictamen: ${c.id}`);
    validateJudge(j);
    const m=textMetrics(c,r.response);
    const passed=m.completeness===1 && m.without_prohibited===1 && j.score===1;
    return {id:c.id,type:c.type,critical:c.critical,...m,fidelity:j.score,reason:j.reason,passed};
  });
  const metrics={completeness:mean(cases.map(c=>c.completeness)),without_prohibited:mean(cases.map(c=>c.without_prohibited)),fidelity:mean(cases.map(c=>c.fidelity)),critical_failed:cases.filter(c=>c.critical&&!c.passed).length};
  const passed=['completeness','without_prohibited','fidelity'].every(k=>metrics[k]>=thresholds[k]) && metrics.critical_failed===0;
  return {version:responses.version,dataset_hash:hash(dataset),thresholds,metrics,cases,status:passed?'PASA':'NO PASA',exit_code:passed?0:1};
}
export function calibration(pairs) {
  requireThat(pairs.length>=6,'Se requieren al menos seis calificaciones humanas');
  requireThat(new Set(pairs.map(p=>`${p.version}/${p.id}`)).size===pairs.length,'Pares duplicados');
  requireThat(pairs.every(p=>[0,0.5,1].includes(p.human)&&[0,0.5,1].includes(p.judge)),'Notas inválidas');
  const observed=mean(pairs.map(p=>Number(p.human===p.judge)));
  const expected=[0,0.5,1].reduce((sum,s)=>sum+pairs.filter(p=>p.human===s).length/pairs.length*pairs.filter(p=>p.judge===s).length/pairs.length,0);
  return {n:pairs.length,agreement_percent:100*observed,kappa:expected===1?null:(observed-expected)/(1-expected),note:expected===1?'Kappa indefinido: acuerdo esperado igual a 1':'Kappa de Cohen sin ponderar',pairs};
}
