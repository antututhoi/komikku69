import sys,re,glob,os,html
def strings(root):
    d={}
    fs=[f for f in glob.glob(root+'/**/strings.xml',recursive=True)+glob.glob(root+'/**/plurals.xml',recursive=True)
        if '/build/' not in f and ('/base/' in f or f.endswith('/values/strings.xml') or f.endswith('/values/plurals.xml')) and '/debug/' not in f and '/nightly/' not in f and '/preview/' not in f]
    for f in fs:
        t=open(f,encoding='utf8',errors='ignore').read()
        for m in re.finditer(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>',t,re.S):
            d[m.group(1)]=html.unescape(re.sub(r'\s+',' ',m.group(2))).strip()
        for m in re.finditer(r'<plurals\s+name="([^"]+)"[^>]*>.*?<item[^>]*>(.*?)</item>',t,re.S):
            d[m.group(1)]=html.unescape(re.sub(r'\s+',' ',m.group(2))).strip()
    return d
def prefs(root):
    k=set()
    for f in glob.glob(root+'/**/*.kt',recursive=True)+glob.glob(root+'/**/*.xml',recursive=True):
        if '/build/' in f or '/test' in f: continue
        try: t=open(f,encoding='utf8',errors='ignore').read()
        except: continue
        if f.endswith('.kt'):
            for m in re.finditer(r'\.get(?:Boolean|Int|String|Long|Float|StringSet|Enum|Object|ObjectFromString|ObjectFromInt)\(\s*(?:Preference\.\w+\()?\s*"([^"$]+)"',t): k.add(m.group(1))
            for m in re.finditer(r'const val KEY_\w+\s*=\s*"([^"]+)"',t): k.add(m.group(1))
        elif '/xml/' in f:
            for m in re.finditer(r'android:key="([^"]+)"',t): k.add(m.group(1))
    return k
norm=lambda s:re.sub(r'[^a-z0-9]+',' ',s.lower()).strip()
if __name__=='__main__':
    base,app,out=sys.argv[1],sys.argv[2],sys.argv[3]
    bs,as_=strings(base),strings(app); bv={norm(v) for v in bs.values()}
    bp,ap=prefs(base),prefs(app)
    new=[(n,v) for n,v in sorted(as_.items()) if n not in bs and norm(v) not in bv]
    np_=sorted(ap-bp)
    os.makedirs(os.path.dirname(out),exist_ok=True)
    with open(out,'w') as o:
        o.write(f'strings: app {len(as_)} base {len(bs)} | not in base {len(new)}\nprefs: app {len(ap)} base {len(bp)} | not in base {len(np_)}\n\n## PREF KEYS NOT IN 69\n'+'\n'.join(np_)+'\n\n## STRINGS NOT IN 69\n'+'\n'.join(f'{n}\t{v[:160]}' for n,v in new))
    print(open(out).read().split('\n\n')[0])
