"""side-by-side: python3 xsheet.py out.jpg screen1,screen2 TAG1,TAG2,..  (rows=screens, cols=apps)"""
import sys,glob,re
from PIL import Image,ImageDraw
out,screens,tags=sys.argv[1],sys.argv[2].split(','),sys.argv[3].split(',')
w,h=360,780
M=Image.new('RGB',(len(tags)*w,len(screens)*(h+18)),'white'); d=ImageDraw.Draw(M)
for r,s in enumerate(screens):
    for c,T in enumerate(tags):
        fs=sorted(glob.glob(f'ctlout*/out/*_{T}_{s}_*.jpg')+glob.glob(f'ctlout*/out/*_{T}_{s}.jpg'),key=lambda p:int(re.sub(r'\D','',p.split('/')[-1].split('_')[0][len(T):]) or 0))
        x,y=c*w,r*(h+18); d.text((x+3,y+3),f'{T} {s}',fill='black')
        if fs: M.paste(Image.open(fs[-1]).resize((w,h)),(x,y+18))
M.save(out,quality=82); print(M.size)
