import fs from 'node:fs';
const file=new URL('../m7-evals/resultados/analisis.json',import.meta.url);
let text=fs.readFileSync(file,'utf8');
const words={'grabaci?n':'grabación','b?squeda':'búsqueda','conserv?':'conservó','inspecci?n':'inspección','detect?':'detectó','confund?a':'confundía','comparaci?n':'comparación','descripci?n':'descripción','revis?':'revisó','evaluaci?n':'evaluación','preparaci?n':'preparación','interrumpi?':'interrumpió','v?lida':'válida','v?lido':'válido','gener?':'generó','dict?menes':'dictámenes','r?brica':'rúbrica','segu?a':'seguía','omit?a':'omitía','explicaci?n':'explicación','versi?n':'versión','tambi?n':'también','aclar?':'aclaró','producci?n':'producción','aclaraci?n':'aclaración','ocurri?':'ocurrió','asign?':'asignó','env?o':'envío','corrigi?':'corrigió','a?ade':'añade','recomendaci?n':'recomendación','calific?':'calificó','instrucci?n':'instrucción','s?':'sí','omisi?n':'omisión','recibi?':'recibió','aprobaci?n':'aprobación','interpretaci?n':'interpretación','negaci?n':'negación','revisi?n':'revisión','m?s':'más'};
words['validaci?n']='validación';
for(const [bad,good] of Object.entries(words))text=text.replaceAll(bad,good);
JSON.parse(text);fs.writeFileSync(file,text);
