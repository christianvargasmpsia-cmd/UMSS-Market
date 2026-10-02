import {performance} from 'node:perf_hooks';
import {read,readText,save} from './io.mjs';
import {evaluate,hash,requireThat} from './core.mjs';
const start=performance.now();
try {
  const version=process.argv[2]||'v3';
  requireThat(['v1','v2','v3'].includes(version),'Versión inválida');
  const r=read(`respuestas/${version}.json`);
  requireThat(r.prompt_hash===hash(readText(`prompts/${version}.txt`)),'Prompt cambiado');
  const report=evaluate(read('dataset/golden.json'),r,read(`evals/dictamenes/${version}.json`),read('evals/config.json').thresholds,readText('evals/rubrica.txt'));
  console.log(`UMSS MARKET | EVAL OFFLINE | ${version} | 12 casos`);
  console.log('Completitud:',report.metrics.completeness.toFixed(4));
  console.log('Fidelidad:',report.metrics.fidelity.toFixed(4));
  console.log('Sin prohibidos:',report.metrics.without_prohibited.toFixed(4));
  console.log('Criticos fallados:',report.metrics.critical_failed);
  for(const c of report.cases.filter(c=>!c.passed)) console.log(`Revisar ${c.id}: C=${c.completeness.toFixed(2)} F=${c.fidelity} P=${c.without_prohibited}`);
  console.log(`COMPUERTA: ${report.status}`);
  console.log(`Codigo de salida: ${report.exit_code}`);
  console.log(`Duracion: ${((performance.now()-start)/1000).toFixed(3)} s`);
  save(`resultados/${version}.json`,report);
  process.exitCode=report.exit_code;
} catch(e) {console.error(`NO PASA | ${e.message}`);process.exitCode=1;}
