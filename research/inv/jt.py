import drive,sys
def f(o): return "\n".join(l[6:] for l in o.splitlines() if ("t=''" not in l or "d=''" not in l) and "block" not in l)
def b(*c,tag="J",show=True):
    o=drive.blk(*c,tag=tag)
    if show: print(f(o)); print("-----")
    return o
def g(o,n=60): print([l[19:85].split("' id=")[0].replace("' d='","|") for l in o.splitlines() if (("t='" in l and "t=''" not in l) or "d=''" not in l) and "focus" not in l][:n])
