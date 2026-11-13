import sys,glob,os,shutil,re
from PIL import Image
src,dst,app=sys.argv[1],sys.argv[2],sys.argv[3]
os.makedirs(dst,exist_ok=True); seen={}
for f in sorted(glob.glob(f'{src}/*_{app}_*.jpg'),key=os.path.getmtime):
    name=f.split(f'_{app}_',1)[1]
    im=Image.open(f).convert('L').resize((24,48)); p=list(im.getdata())
    key=p
    dup=next((k for k,v in seen.items() if sum(abs(a-b) for a,b in zip(k,key))/len(key)<2),None)
    if dup is not None: print('DUP',name,'==',seen[dup]); continue
    seen[tuple(key)]=name; shutil.copy(f,f'{dst}/{name}')
print(len(seen),'kept')
