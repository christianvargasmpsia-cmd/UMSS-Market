"""Empaqueta únicamente la lista de archivos documentados y verifica el ZIP."""
import json
import zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'m7-evals'
inventory=json.loads((ROOT/'resultados/inventario.json').read_text(encoding='utf-8'))
cal=json.loads((ROOT/'resultados/calibracion.json').read_text(encoding='utf-8'))
report=json.loads((ROOT/'resultados/v3.json').read_text(encoding='utf-8'))
comparison=json.loads((ROOT/'resultados/comparacion.json').read_text(encoding='utf-8'))
evidence=json.loads((ROOT/'evidencia/ejecucion_v3.json').read_text(encoding='utf-8'))
assert cal['n']>=6 and report['exit_code']==0 and evidence['exit_code']==0
assert len(comparison['rows'])==3 and (ROOT/'informe_evals.pdf').is_file()
for item in inventory:
    path=(ROOT/item['path']).resolve()
    assert path.is_relative_to(ROOT.resolve()) and path.is_file(),item['path']
zip_path=ROOT.parent/'M7_Evals_UMSS_Market.zip'
with zipfile.ZipFile(zip_path,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for item in inventory:z.write(ROOT/item['path'],'M7_Evals_UMSS_Market/'+item['path'])
with zipfile.ZipFile(zip_path) as z:
    assert z.testzip() is None
    assert len(z.namelist())==len(inventory)
    assert not any('/.env' in n or 'node_modules' in n for n in z.namelist())
print(f'{zip_path} ({zip_path.stat().st_size} bytes, {len(inventory)} archivos)')
