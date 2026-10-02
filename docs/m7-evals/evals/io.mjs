import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
export const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
export const readText=p=>fs.readFileSync(path.join(root,p),'utf8');
export const read=p=>JSON.parse(readText(p));
export const exists=p=>fs.existsSync(path.join(root,p));
export function save(p,value) { const full=path.join(root,p); fs.mkdirSync(path.dirname(full),{recursive:true});fs.writeFileSync(full,JSON.stringify(value,null,2)+'\n'); }
export function saveNew(p,value) { const full=path.join(root,p);fs.mkdirSync(path.dirname(full),{recursive:true});fs.writeFileSync(full,JSON.stringify(value,null,2)+'\n',{flag:'wx'}); }
