"""Standard screen tour: the same ~30 screens in every app, named <T>_s<NN>_<screen>."""
import re,sys
from jt import *
def q(*c,T): return b(*c,tag=T,show=False)
def setup_m(pkg,T,done_onboarding=False):
    if not done_onboarding:
        q(f"sh pm clear {pkg}","sleep 2",f"launch {pkg}","sleep 9","tapt ^OK$|^Close$|^Cancel$","sleep 2",T=T)
        q("tapt ^Next$","sleep 2","tapt ^Select a folder$","sleep 3","tapt ^Show roots$","sleep 2","tapt ^Documents$","sleep 2","tapt ^USE THIS FOLDER$","sleep 2","tapt ^ALLOW$|^Allow$","sleep 3",T=T)
        q("tapt ^Next$","sleep 2","tapt ^Next$","sleep 2","tapt ^Get started$","sleep 3",T=T)
        q(f"stop {pkg}",f"launch {pkg}","sleep 7","tapt ^More$","sleep 2","tapt ^Data and storage$","sleep 3","tapt ^Restore backup$","sleep 4","tapt ^Show roots$","sleep 2","tap 441 642","sleep 2","tapt ^tour$","sleep 3","tapt ^seed2.tachibk$","sleep 5",T=T)
    q("tapt ^Restore$","sleep 20",T=T)
    q(f"stop {pkg}",f"launch {pkg}","sleep 7","tapt ^Browse$","sleep 3","tapt ^Extensions$","sleep 3","tapt ^Trust$","sleep 2","tapt ^Trust$|^OK$","sleep 4",T=T)
    q(f"stop {pkg}",f"launch {pkg}","sleep 7","tapt ^More options$","sleep 2","tapt ^Update library$","sleep 90",T=T)
def tour_m(pkg,T,title="Mieruko-chan"):
    F=[f"stop {pkg}",f"launch {pkg}","sleep 7"]
    for t in ["Tensei Shitara Slime","SPY.FAMILY","Osoraku Kanojo","Mieruko-chan","Mairimashita","Kumo desu ga","Kaoru Hana"]:
        q(*F,f"tapt ^{t}","sleep 8","down","sleep 9",T=T)
    q(*F,f"shot {T}_s01_library",f"longt ^{title}","sleep 2",f"shot {T}_s02_library_selection","tapt ^More options$","sleep 2",f"shot {T}_s03_library_selection_menu",T=T)
    q(*F,"tapt ^More options$","sleep 2",f"shot {T}_s04_library_menu","back","sleep 1","tapt ^Filter$","sleep 2",f"shot {T}_s05_library_filter","tapt ^Sort$","sleep 1",f"shot {T}_s06_library_sort","tapt ^Display$","sleep 1",f"shot {T}_s07_library_display","tapt ^Group$","sleep 1",f"shot {T}_s08_library_group",T=T)
    q(*F,f"tapt ^{title}","sleep 9",f"shot {T}_s10_entry_top","tapt ^More options$","sleep 2",f"shot {T}_s11_entry_menu",T=T)
    q(*F,f"tapt ^{title}","sleep 8","tapt ^Tracking$|^Track$","sleep 3",f"shot {T}_s12_entry_tracking",T=T)
    q(*F,f"tapt ^{title}","sleep 8","up","sleep 2",f"shot {T}_s13_entry_chapters",T=T)
    q("longt ^(Vol|Ch|Chapter)[. ]","sleep 2",f"shot {T}_s14_chapter_selection","back","sleep 1","tapt ^Filter$","sleep 2",f"shot {T}_s15_chapter_filter","tapt ^Sort$","sleep 1",f"shot {T}_s16_chapter_sort","tapt ^Display$","sleep 1",f"shot {T}_s17_chapter_display",T=T)
    q(*F,"tapt ^Updates$","sleep 4",f"shot {T}_s20_updates","longt ^(Vol|Ch)[. ]","sleep 2",f"shot {T}_s20b_updates_selection","back","sleep 1","tapt ^History$","sleep 3",f"shot {T}_s21_history","tapt ^Browse$","sleep 3",f"shot {T}_s22_browse_sources","tapt ^MangaDex$","sleep 10",f"shot {T}_s23_source_list","tapt ^Filter$","sleep 3",f"shot {T}_s24_source_filter",T=T)
    q(*F,"tapt ^Browse$","sleep 3","tapt ^Extensions$","sleep 3",f"shot {T}_s25_extensions","tapt ^Migrate$","sleep 3",f"shot {T}_s26_migrate","tapt ^Sources$","sleep 2","tapt ^Global search$","sleep 2","text mieruko","sleep 1","key 66","sleep 1","key 66","sleep 14",f"shot {T}_s27_global_search",T=T)
    q(*F,f"tapt ^{title}","sleep 8","tapt ^Download$","sleep 2",f"shot {T}_s18_entry_download_menu","tapt ^All","sleep 3","tapt ^Reading$|^OK$","sleep 2",T=T)
    q(*F,"tapt ^Kaoru Hana","sleep 8","tapt ^Download$","sleep 2","tapt ^All","sleep 3","tapt ^Reading$|^OK$","sleep 2",T=T)
    gaps_m(pkg,T)
    q(*F,"tapt ^More$","sleep 2","up","sleep 1","tapt ^Settings$","sleep 2",f"shot {T}_s34_settings",T=T)
    reader_m(pkg,T)
def gaps_m(pkg,T,big="Mairimashita"):
    F=[f"stop {pkg}",f"launch {pkg}","sleep 7"]
    q(*F,"tapt ^More$","sleep 2",f"shot {T}_s30_more",T=T)
    q(*F,f"tapt ^{big}","sleep 8","tapt ^Download$","sleep 2","tapt ^All","sleep 2","tapt ^Reading$|^OK$","sleep 1","back","sleep 1","tapt ^More$","sleep 2","tapt ^Download queue$","sleep 2",f"shot {T}_s31_download_queue","tapt ^More options$","sleep 2",f"shot {T}_s31b_download_queue_menu","back","sleep 1","longt ^(Vol|Ch)[. ]","sleep 2",f"shot {T}_s31c_download_queue_item_menu","back","sleep 1","tapt ^More options$","sleep 2","tapt ^Cancel all$|^Clear","sleep 2",T=T)
    q(*F,"tapt ^More$","sleep 2","tapt ^Categories$","sleep 3",f"shot {T}_s32_categories",T=T)
    q(*F,"tapt ^More$","sleep 2","tapt ^Statistics$","sleep 4",f"shot {T}_s33_statistics","up","sleep 2",f"shot {T}_s33b_statistics_2",T=T)
    q(*F,"tapt ^Browse$","sleep 3","tapt ^Global search$|^Search$","sleep 2","text mieruko","sleep 1","key 66","sleep 2","tap 995 2117","sleep 15",f"shot {T}_s27_global_search",T=T)
def reader_m(pkg,T,rd="Mairimashita"):
    F=[f"stop {pkg}",f"launch {pkg}","sleep 7"]
    q(*F,f"tapt ^{rd}","sleep 8","up","sleep 2","tapt ^(Vol|Ch)[. ]","sleep 14","tapt ^Got it$","sleep 1","tap 540 1200","sleep 2",f"shot {T}_s40_reader_menu","tap 540 1200","sleep 1","hold 540 1300 1000","sleep 2",f"shot {T}_s41_reader_page_menu","back","sleep 1","tap 540 1200","sleep 1","tapt ^Settings$","sleep 2",f"shot {T}_s42_reader_settings",T=T)
if __name__=="__main__":
    pkg,T=sys.argv[1],sys.argv[2]
    if "setup" in sys.argv: setup_m(pkg,T)
    if "setup2" in sys.argv: setup_m(pkg,T,True)
    if "reader" in sys.argv: reader_m(pkg,T)
    elif "gaps" in sys.argv: gaps_m(pkg,T)
    elif "restore" in sys.argv:
        q(f"stop {pkg}",f"launch {pkg}","sleep 7","tapt ^More$","sleep 2","tapt ^Data and storage$","sleep 3","tapt ^Restore backup$","sleep 4","tapt ^Show roots$","sleep 2","tap 441 642","sleep 2","tapt ^tour$","sleep 3","tapt ^seed2.tachibk$","sleep 5",T=T)
        setup_m(pkg,T,True); tour_m(pkg,T)
    else: tour_m(pkg,T)
