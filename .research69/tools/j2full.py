import sys
from jt import *
p,T=sys.argv[1],sys.argv[2]
def q(*c): return b(*c,tag=T,show=False)
def s(n): return f"shot {T}_{n}"
F=[f"stop {p}",f"launch {p}","sleep 8"]
q(*F,"tap 180 2242","sleep 4",s("s01_library"),"tap 889 201","sleep 2","tapt ^Update library$|^Update$","sleep 60")
q(*F,"tap 180 2242","sleep 3","tapt ^Mieruko-chan","sleep 8",s("s10_entry"),"up","sleep 2",s("s13_chapters"),"tapt ^Download$ 0","tapt ^Download$ 2","tapt ^Download$ 4","tapt ^Download$ 6","tapt ^Download$ 8","sleep 2",s("s13b_chapters_downloading"),"longt ^Vol.14 Ch.68","sleep 2",s("s14_chapter_longpress"))
q(*F,"tap 540 2242","sleep 4",s("s20_recents_grouped"),"tapt ^All$","sleep 3",s("s20a_recents_all"),"tapt ^History$","sleep 3",s("s21_history"),"tapt ^Updates$","sleep 3",s("s20b_updates"),"tapt ^Display options$","sleep 3",s("s20o_recents_display_options"),"back","sleep 1","swipe 540 2090 540 600 300","sleep 3",s("s31_download_queue"))
q(*F,"tap 540 2242","sleep 4","tapt ^History$","sleep 3","longt ^Vol|^Ch","sleep 2",s("s21s_history_longpress"),"back","sleep 1","swipe 300 900 900 900 300","sleep 2",s("s21w_history_swipe"))
q(*F,"tap 1016 201","sleep 2",s("s30_more_menu"),"tapt ^Stats$|^Statistics$","sleep 4",s("s33_stats"),"tapt ^View detailed statistics$","sleep 4",s("s33b_stats_detailed"))
print("done")
