"""LANE=x python3 ktour.py futon|dropsauce|kaisoku TAG"""
import sys
from jt import *
app,T=sys.argv[1],sys.argv[2]
P={"futon":"io.github.landwarderer.futon.nightly","dropsauce":"org.haziffe.dropsauce","kaisoku":"io.github.glitch228.kaisoku"}[app]
if app=="dropsauce": NAV={"hist":"tapt ^History$","fav":"tapt ^Favorites$","exp":"tapt ^Explore$","feed":"tapt ^Feed","menu":"tapt ^More$"}
else: NAV={"hist":"tap 135 2260","fav":"tap 405 2260","exp":"tap 675 2260","feed":"tap 945 2260","menu":"tapt ^More options$"}
F=[f"stop {P}",f"launch {P}","sleep 8"]
def q(*c): return b(*c,tag=T,show=False)
def s(n): return f"shot {T}_{n}"
# feed
q(*F,NAV["feed"],"sleep 4",s("s20_feed"),"up","sleep 2",s("s20a_feed_scrolled"),NAV["menu"],"sleep 2",s("s20m_feed_menu"),"back","sleep 1","tapt ^Unread$|^Reading$","sleep 3",s("s20f_feed_chip"))
q(*F,NAV["feed"],"sleep 4","swipe 300 1050 950 1050 300","sleep 3",s("s20g_feed_swipe_right"),"swipe 950 1250 200 1250 300","sleep 3",s("s20h_feed_swipe_left"),"longt ^Blue Lock|^Kumo|^Tensei|^Chiyu","sleep 2",s("s20i_feed_longpress"))
# history
q(*F,NAV["hist"],"sleep 4",s("s21_history"),"up","sleep 2",s("s21a_history_scrolled"),NAV["menu"],"sleep 2",s("s21m_history_menu"),"tapt ^Display options$|^List options$","sleep 3",s("s21o_history_options"),"back","sleep 1","tapt ^New chapters$|^On device$","sleep 3",s("s21f_history_chip"))
q(*F,NAV["hist"],"sleep 4","longt ^Mairimashita|^Mieruko|^Kumo","sleep 2",s("s21s_history_selection"),"tapt ^More options$","sleep 2",s("s21t_history_selection_menu"))
# favourites
q(*F,NAV["fav"],"sleep 4",s("s01_favourites"),"up","sleep 2",s("s01a_favourites_scrolled"),NAV["menu"],"sleep 2",s("s04_favourites_menu"),"back","sleep 1","tapt ^Reading$","sleep 3",s("s01b_category_tab"),"longt ^Mairimashita|^Mieruko|^Kumo","sleep 2",s("s02_selection"),"tapt ^More options$","sleep 2",s("s03_selection_menu"))
# details
q(*F,NAV["fav"],"sleep 4","tapt ^Mairimashita","sleep 9",s("s10_details"),"up","sleep 2",s("s10a_details_scrolled"),"tapt ^More options$","sleep 2",s("s11_details_menu"),"back","sleep 1","tapt ^Download$","sleep 3",s("s18_download_dialog"))
o=q("tapt ^Download$|^Confirm$|^OK$|^Start$","sleep 4",s("s18b_after_download")); g(o,60)
q(*F,NAV["fav"],"sleep 4","tapt ^Mairimashita","sleep 9","swipe 540 2150 540 500 300","sleep 3",s("s13_chapters"),"tapt ^Pages$|^Pages","sleep 5",s("s13b_pages"),"tapt ^Bookmarks$|^Bookmarks","sleep 3",s("s13c_bookmarks"))
# downloads screen
q(*F,"tapt ^Downloads$","sleep 4",s("s31_downloads"),"tapt ^More options$","sleep 2",s("s31b_downloads_menu"),"back","sleep 1","longt ^Mairimashita","sleep 2",s("s31c_downloads_selection"))
# reader
q(*F,NAV["fav"],"sleep 4","tapt ^Mairimashita","sleep 9","tapt ^Continue$|^Read$","sleep 14","tap 540 1200","sleep 2",s("s40_reader_menu"),"tapt ^Options$|^More options$|^Settings$","sleep 3",s("s42_reader_options"),"up","sleep 2",s("s42b_reader_options_2"))
# explore
q(*F,NAV["exp"],"sleep 5",s("s22_explore"),"up","sleep 2",s("s22a_explore_scrolled"),"tapt ^MangaDex$","sleep 10",s("s23_source_list"),"tapt ^Filter$|^Filter","sleep 3",s("s24_source_filter"))
print("tour done",app)
