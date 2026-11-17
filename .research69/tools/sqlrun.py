"""LANE=x python3 sqlrun.py PKG DB TAG 'SQL...'  -> runs SQL in the app's database on the virtual phone (root)."""
import sys,base64
from jt import *
pkg,db,T,sql=sys.argv[1:5]
e=base64.b64encode(sql.encode()).decode()
o=b(f'host adb root >/dev/null 2>&1; sleep 2; echo {e} | base64 -d > /tmp/s.sql; adb push /tmp/s.sql /data/local/tmp/s.sql >/dev/null; adb shell "am force-stop {pkg}; sqlite3 /data/data/{pkg}/databases/{db} < /data/local/tmp/s.sql" 2>&1 | tail -40',tag=T,show=False)
print("\n".join(l for l in o.splitlines() if l.startswith("!") or (l and l[0] not in "[#"))[:3000])
