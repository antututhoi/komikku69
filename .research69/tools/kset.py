"""LANE=x python3 kset.py futon|dropsauce|j2k  -> fresh setup with test backup (+ feed rows for Kotatsu types)"""
import sys,subprocess
from jt import *
app=sys.argv[1]
PICK=["tapt ^Show roots$","sleep 2","tap 441 642","sleep 2","tapt ^tour$","sleep 3"]
SQL=open('/tmp/feed.sql').read() if __import__('os').path.exists('/tmp/feed.sql') else open('/home/claude/feed.sql').read()
def perm(p): return [f"sh pm clear {p}",f"sh appops set {p} MANAGE_EXTERNAL_STORAGE allow",f"sh appops set {p} REQUEST_INSTALL_PACKAGES allow",f"launch {p}","sleep 9"]
if app=="futon":
    p,T="io.github.landwarderer.futon.nightly","F3"
    b(*perm(p),"tapt ^Skip$","sleep 4","tap 474 552","sleep 2","tap 718 291","sleep 3","tapt ^Manga$","sleep 3","tap 970 291","sleep 2","tapt ^Settings$","sleep 3","tapt ^Backup and restore$","sleep 3","tapt ^Restore from backup$","sleep 4",*PICK,"tapt ^seed_kotatsu2.zip$","sleep 6","tapt ^Restore$","sleep 15",tag=T,show=False)
    subprocess.run(["python3","sqlrun.py",p,"futon-db",T,SQL])
elif app=="dropsauce":
    p,T="org.haziffe.dropsauce","D3"
    b(*perm(p),"tapt ^Next$","sleep 2","tapt ^Next$","sleep 2","tapt ^Next$","sleep 2","tapt ^Confirm$","sleep 5","tapt ^Close$","sleep 1","tap 91 199","sleep 3","tapt ^Backup and restore$","sleep 3","tapt ^Restore from Tachiyomi/Mihon$","sleep 4",*PICK,"tapt ^seed2.tachibk$","sleep 8","tapt ^OK$","sleep 22",tag=T,show=False)
    subprocess.run(["python3","sqlrun.py",p,"kotatsu-db",T,SQL])
elif app=="dropsauce2":
    p,T="org.haziffe.dropsauce","D4"
    b(*perm(p),"tapt ^Next$","sleep 2","tapt ^Next$","sleep 2","tapt ^Next$","sleep 2","tapt ^Confirm$","sleep 5","tapt ^Close$","sleep 1","tap 91 199","sleep 3","tapt ^Backup and restore$","sleep 3","tapt ^Restore from backup$|^Restore backup$|^Restore$","sleep 4",*PICK,"tapt ^seed_kotatsu2.zip$","sleep 8","tapt ^Restore$|^OK$","sleep 22",tag=T,show=False)
    subprocess.run(["python3","sqlrun.py",p,"kotatsu-db",T,SQL])
elif app=="j2k":
    p,T="eu.kanade.tachiyomi.j2k","J3"
    b(*perm(p),"tap 1016 201","sleep 2","tapt ^Settings$","sleep 2","tapt ^Data and storage$|^Backup","sleep 2","tapt ^Restore backup$","sleep 4",*PICK,"tapt ^seed2.tachibk$","sleep 5","tapt ^Allow$","sleep 3","tapt ^Restore$","sleep 25",f"stop {p}",f"launch {p}","sleep 7","tap 900 2242","sleep 3","tapt ^Extensions$","sleep 3","tapt ^Trust$|^TRUST$","sleep 2","tapt ^Trust$|^TRUST$|^OK$","sleep 4",tag=T,show=False)
if app=="kaisoku":
    p,T="io.github.glitch228.kaisoku","KA"
    b(*perm(p),"tapt ^Allow$","sleep 3","up","sleep 2","tapt ^Restore from backup$","sleep 4",*PICK,"tapt ^seed_kotatsu2.zip$","sleep 6","tapt ^Restore$","sleep 15","tapt ^Done$|^OK$|^Close$","sleep 2",tag=T,show=False)
    subprocess.run(["python3","sqlrun.py",p,"kotatsu-db",T,SQL])
print("setup done",app)
