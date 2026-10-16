// Captura visual de la salida REAL del proceso; no simula una terminal.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
import {performance} from 'node:perf_hooks';
import {chromium} from 'playwright';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../m7-evals');
const start=performance.now();
const result=spawnSync(process.execPath,['evals/run.mjs','v3'],{cwd:root,encoding:'utf8',windowsHide:true});
if(result.status!==0)throw new Error('v3 no pasó: '+result.stdout+result.stderr);
const record={command:'node evals/run.mjs v3',timestamp:new Date().toISOString(),exit_code:result.status,duration_seconds:(performance.now()-start)/1000,stdout:result.stdout,stderr:result.stderr,node:process.version};
fs.mkdirSync(path.join(root,'evidencia'),{recursive:true});
fs.writeFileSync(path.join(root,'evidencia/ejecucion_v3.json'),JSON.stringify(record,null,2)+'\n');
const browser=await chromium.launch({channel:'msedge',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1200,height:780},deviceScaleFactor:1});
 await page.setContent('<html lang="es"><meta charset="utf-8"><style>body{background:#edf3f7;color:#163950;font:20px Arial;padding:42px}h1{font-size:32px;margin-bottom:8px}p{font-size:17px}pre{background:#112b3a;color:#e3f5ed;padding:28px;border-radius:10px;font:21px/1.6 Consolas,monospace;white-space:pre-wrap}.foot{color:#4b6575}</style><h1>UMSS Market · Evals offline · v3</h1><p>Salida capturada de una ejecución real del proceso Node.js</p><pre id="output"></pre><p id="foot" class="foot"></p></html>');
 await page.locator('#output').evaluate((el,text)=>el.textContent=text,`> ${record.command}\n\n${record.stdout}\nCódigo de salida observado por el proceso padre: ${record.exit_code}`);
 await page.locator('#foot').evaluate((el,text)=>el.textContent=text,`${record.timestamp} · ${record.node} · Duración total: ${record.duration_seconds.toFixed(3)} s · Sin llamadas al modelo`);
 await page.screenshot({path:path.join(root,'evidencia/compuerta_v3.png'),fullPage:true});
}finally{await browser.close();}
console.log(JSON.stringify(record,null,2));
