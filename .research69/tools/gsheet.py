"""grid for one app: python3 gsheet.py out.jpg TAG s31,s22,... cols"""
import sys,glob,re
from PIL import Image,ImageDraw
out,T,screens,cols=sys.argv[1],sys.argv[2],sys.argv[3].split(','),int(sys.argv[4])
w,h=360,780; rows=(len(screens)+cols-1)//cols
M=Image.new('RGB',(cols*w,rows*(h+18)),'white'); d=ImageDraw.Draw(M)
for i,s in enumerate(screens):
    fs=sorted(glob.glob(f'ctlout*/out/*_{T}_{s}_*.jpg')+glob.glob(f'ctlout*/out/*_{T}_{s}.jpg'),key=lambda p:int(re.sub(r'\D','',p.split('/')[-1].split('_')[0][len(T):]) or 0))
    x,y=(i%cols)*w,(i//cols)*(h+18); d.text((x+3,y+3),f'{T} {s}',fill='black')
    if fs: M.paste(Image.open(fs[-1]).resize((w,h)),(x,y+18))
M.save(out,quality=82); print(M.size)
