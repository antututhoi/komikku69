import sys,re
from jt import *
def texts(o): return [re.sub(r"' (d|id)=.*","",l.split("t='",1)[1]) for l in o.splitlines() if "t='" in l and "t=''" not in l]
def walk(pkg,T,app,pages,more="^More$",ups=1):
    out=open(f'research/{app}_settings2.log','w')
    for p in pages:
        b(f"stop {pkg}",f"launch {pkg}","sleep 5",f"tapt {more}","sleep 2","up","sleep 1","tapt ^Settings$","sleep 2",tag=T,show=False)
        o=b(f"tapt ^{p}$","sleep 2",tag=T,show=False)
        if "! tapt" in o and "not found" in o.lower():
            o=b("up","sleep 1",f"tapt ^{p}$","sleep 2",tag=T,show=False)
        seen=[];n=re.sub(r'\W+','_',p)
        for i in range(7):
            o=b(f"shot {T}_settings_{n}_p{i}",*(["up","sleep 1"] if i else []),tag=T,show=False) if i==0 else b("up","sleep 1",f"shot {T}_settings_{n}_p{i}",tag=T,show=False)
            t=[x for x in texts(o) if x not in seen]
            if i and not t: break
            seen+=t
        out.write(f"##### {p}\n"+"\n".join(seen)+"\n"); out.flush()
if __name__=="__main__": walk(sys.argv[1],sys.argv[2],sys.argv[3],sys.argv[4].split("|"),*(sys.argv[5:6]))
