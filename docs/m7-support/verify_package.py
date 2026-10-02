import json, zipfile, subprocess
from pathlib import Path
from pypdf import PdfReader
from PIL import Image, ImageChops
support=Path(__file__).resolve().parent
archive=support.parent/'M7_Evals_UMSS_Market.zip'
dest=support/'verificacion_entrega'
dest.mkdir(exist_ok=True)
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for name in z.namelist():
        assert (dest/name).resolve().is_relative_to(dest.resolve())
    z.extractall(dest)
root=dest/'M7_Evals_UMSS_Market'
result=subprocess.run(['node','evals/run.mjs','v3'],cwd=root,capture_output=True,text=True,encoding='utf-8')
assert result.returncode==0,result.stdout+result.stderr
assert 'COMPUERTA: PASA' in result.stdout
pdf=PdfReader(root/'informe_evals.pdf')
texts=[p.extract_text() for p in pdf.pages]
assert all(t.strip() for t in texts)
assert all(word in '\n'.join(texts) for word in ['66.67','0.4286','GPT-6','PASA','EV-12'])
report={'zip':str(archive),'exit_code':result.returncode,'stdout':result.stdout,'pages':len(pdf.pages),'files':len(list(root.rglob('*')))}
(support/'verificacion_entrega.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
imgs=sorted(support.glob('final-page-*.png'))
thumbs=[]
for img in imgs:
    im=Image.open(img).convert('RGB');im.thumbnail((389,550));thumbs.append(im)
for batch in range(0,len(thumbs),6):
    sheet=Image.new('RGB',(389*3,550*2),'#cccccc')
    for i,im in enumerate(thumbs[batch:batch+6]):sheet.paste(im,((i%3)*389,(i//3)*550))
    sheet.save(support/f'final-contact-{batch//6+1}.png')
