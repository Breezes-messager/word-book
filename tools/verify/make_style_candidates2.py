# -*- coding: utf-8 -*-
"""第二批风格候选（I~M），复用第一批的模板，并直接应用调研结论：
- 评分按钮 pill 化（M3 Expressive：按钮默认全圆角）
- 深色系用"越高层越亮"的表面阶梯，不用阴影
- 不在背景上用渐变（教育类 App 深色模式论文点名的坑）
"""
import importlib.util
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
spec = importlib.util.spec_from_file_location(
    "mk", os.path.join(ROOT, "tools", "verify", "make_style_candidates.py")
)
mk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mk)

RATES = """
.ratings { display: flex; gap: 8px; margin-top: 20px; }
.rate { flex: 1; text-align: center; padding: 11px 0; font-size: 13px; line-height: 1.5; border-radius: 999px; }
.rate b { display: block; font-size: 10px; font-weight: 400; }
"""

# ------------------------------------------------- I · 铅字（报纸感）
CSS_I = """
.board { color: #141414; }
.phone { background: #FBFAF7; border-radius: 8px; box-shadow: 0 10px 28px rgba(0,0,0,.16); }
.status { color: #8A8A86; font-family: Georgia, serif; }
.h-title { font-family: Georgia,"Times New Roman",serif; font-size: 27px; font-weight: 700; letter-spacing: 1px;
           border-top: 3px solid #141414; border-bottom: 1px solid #141414; padding: 8px 0 6px; margin-bottom: 14px; }
.metric { margin-bottom: 14px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13.5px; color: #4A4A46; }
.metric-top b { font-family: Georgia, serif; font-size: 19px; color: #141414; }
.bar { height: 4px; background: #E2E0DA; margin-top: 6px; position: relative; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #141414; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 11.5px; color: #8A8A86; margin: 12px 0 16px; font-family: Georgia, serif; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 11px 0; font-size: 13.5px; border: 1px solid #141414; color: #141414;
       border-radius: 999px; font-weight: 700; letter-spacing: .5px; }
.btn.primary { background: #141414; color: #FBFAF7; }
.btn.wide { display: block; margin-bottom: 16px; }
.card { border-top: 1px solid #141414; border-bottom: 1px solid #D8D4CC; padding: 12px 0; }
.tag { font-size: 10.5px; color: #A02020; letter-spacing: 2px; font-weight: 700; text-transform: uppercase; }
.card-title { font-family: Georgia, serif; font-size: 17px; line-height: 1.35; margin: 6px 0 4px; }
.card-sub { font-size: 11.5px; color: #8A8A86; }
.section { display: flex; justify-content: space-between; font-size: 13.5px; margin: 18px 0 2px;
           border-bottom: 2px solid #141414; padding-bottom: 5px; font-family: Georgia, serif; font-weight: 700; }
.more { color: #A02020; font-size: 11.5px; font-family: "Microsoft YaHei",sans-serif; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-bottom: 1px solid #E6E4DE; }
.word { font-family: Georgia, serif; font-size: 19px; }
.ph { font-size: 11.5px; color: #8A8A86; font-family: Georgia, serif; }
.cn { font-size: 11.5px; color: #4A4A46; margin-top: 2px; }
.play { color: #A02020; font-size: 13px; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 2px solid #141414;
       font-size: 12px; color: #8A8A86; background: #F5F3EE; }
.nav .on { color: #141414; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13.5px; margin-bottom: 14px; color: #4A4A46; }
.count { font-family: Georgia, serif; }
.wordcard { border: 1px solid #D8D4CC; border-top: 3px solid #141414; background: #FFFDF9; padding: 22px 18px; }
.bigword { font-family: Georgia, serif; font-size: 44px; letter-spacing: -1px; }
.bigph { font-family: Georgia, serif; color: #8A8A86; margin: 6px 0 18px; }
.pos { font-size: 14px; margin-bottom: 6px; }
.quote { font-family: Georgia, serif; font-size: 13.5px; margin-top: 18px; padding-left: 12px;
         border-left: 3px solid #141414; color: #333; }
.quote-cn { font-size: 11.5px; color: #8A8A86; padding-left: 12px; margin-top: 4px; }
""" + RATES + """
.rate { border: 1px solid #141414; color: #141414; font-weight: 700; }
.rate b { color: #8A8A86; }
.rate.r1 { background: #A02020; border-color: #A02020; color: #fff; }
.rate.r2 { background: #6B6B67; border-color: #6B6B67; color: #fff; }
.rate.r3 { background: #141414; border-color: #141414; color: #fff; }
.rate.r4 { background: #FBFAF7; border-color: #B8B6B0; color: #4A4A46; }
.a-title { font-family: Georgia, serif; font-size: 26px; line-height: 1.25; }
.a-sub { font-family: Georgia, serif; font-size: 12.5px; color: #8A8A86; margin: 6px 0 16px; }
.para { font-family: Georgia, serif; font-size: 14.5px; line-height: 1.9; margin-bottom: 14px; text-align: justify; }
mark { background: transparent; border-bottom: 2px solid #A02020; font-weight: 700; }
"""

# ------------------------------------------------- J · 蓝图
CSS_J = """
.board { color: #DCE9F7; }
.phone { background: #0F2440; border: 1px solid #1E3A5F; border-radius: 6px;
         box-shadow: 0 12px 30px rgba(0,20,45,.6); }
.status { color: #6C90B8; font-family: Consolas, monospace; }
.h-title { font-family: Consolas, monospace; font-size: 14px; letter-spacing: 4px; color: #6C90B8;
           border-bottom: 1px dashed #2A4C78; padding-bottom: 8px; margin-bottom: 16px; text-transform: uppercase; }
.metric { margin-bottom: 16px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; color: #8FB2D6; }
.metric-top b { font-family: Consolas, monospace; font-size: 18px; color: #EAF3FF; }
.bar { height: 6px; background: #16304F; margin-top: 7px; position: relative; border: 1px solid #24507F; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: repeating-linear-gradient(90deg,#4FC3F7,#4FC3F7 4px,#2E9FD4 4px,#2E9FD4 8px); }
.bar i[style*="0%"] { background: transparent; }
.hint { font-family: Consolas, monospace; font-size: 11px; color: #6C90B8; margin: 14px 0 18px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 11px 0; font-size: 13.5px; border: 1px dashed #4FC3F7; color: #9FD9F7;
       border-radius: 999px; font-family: Consolas, monospace; }
.btn.primary { background: #4FC3F7; border-style: solid; border-color: #4FC3F7; color: #06243D; font-weight: 700; }
.btn.wide { display: block; margin-bottom: 18px; }
.card { border: 1px dashed #2A4C78; padding: 12px 14px; background: #12294A; border-radius: 4px; }
.tag { font-family: Consolas, monospace; font-size: 10.5px; color: #FFD166; letter-spacing: 1.5px; }
.card-title { font-size: 16.5px; line-height: 1.35; margin: 6px 0 4px; color: #EAF3FF; }
.card-sub { font-size: 11.5px; color: #6C90B8; font-family: Consolas, monospace; }
.section { display: flex; justify-content: space-between; font-size: 13.5px; margin: 18px 0 2px;
           border-bottom: 1px dashed #2A4C78; padding-bottom: 6px; font-family: Consolas, monospace; color: #9FD9F7; }
.more { color: #FFD166; font-size: 11.5px; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-bottom: 1px dashed #1E3A5F; }
.word { font-size: 19px; font-weight: 700; color: #EAF3FF; }
.ph { font-size: 11.5px; color: #6C90B8; font-family: Consolas, monospace; }
.cn { font-size: 11.5px; color: #8FB2D6; margin-top: 2px; }
.play { color: #4FC3F7; font-size: 13px; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #1E3A5F;
       font-size: 11.5px; color: #6C90B8; background: #0C1D34; font-family: Consolas, monospace; }
.nav .on { color: #4FC3F7; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13.5px; margin-bottom: 14px; color: #8FB2D6; }
.count { font-family: Consolas, monospace; color: #4FC3F7; }
.wordcard { border: 1px solid #24507F; background: #12294A; padding: 24px 20px; border-radius: 6px;
            background-image: linear-gradient(#1B3A61 1px, transparent 1px), linear-gradient(90deg,#1B3A61 1px, transparent 1px);
            background-size: 22px 22px; }
.bigword { font-size: 42px; font-weight: 800; color: #EAF3FF; letter-spacing: -.5px; }
.bigph { color: #6C90B8; margin: 6px 0 18px; font-family: Consolas, monospace; }
.pos { font-size: 14px; margin-bottom: 6px; color: #C6DAF0; }
.quote { font-size: 13.5px; margin-top: 18px; padding-left: 12px; border-left: 2px solid #FFD166; color: #8FB2D6; }
.quote-cn { font-size: 11.5px; color: #6C90B8; padding-left: 12px; margin-top: 4px; }
""" + RATES + """
.rate { border: 1px solid #2A4C78; color: #9FD9F7; background: #12294A; }
.rate b { color: #6C90B8; font-family: Consolas, monospace; }
.rate.r1 { border-color: #F0616D; color: #F0616D; }
.rate.r2 { border-color: #FFD166; color: #FFD166; }
.rate.r3 { background: #4FC3F7; border-color: #4FC3F7; color: #06243D; font-weight: 700; }
.rate.r4 { border-color: #8FB2D6; color: #8FB2D6; }
.a-title { font-size: 22px; line-height: 1.3; color: #EAF3FF; font-weight: 800; }
.a-sub { font-size: 12.5px; color: #6C90B8; margin: 6px 0 16px; font-family: Consolas, monospace; }
.para { font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; color: #C6DAF0; }
mark { background: rgba(79,195,247,.16); color: #9FD9F7; font-weight: 700; border-bottom: 1px solid #4FC3F7; }
"""

# ------------------------------------------------- K · 便签
CSS_K = """
.board { color: #4A3B2A; }
.phone { background: #FFFDF5; border-radius: 22px; box-shadow: 0 12px 30px rgba(120,95,50,.20); }
.status { color: #A08C6E; }
.h-title { font-family: "STKaiti","KaiTi",serif; font-size: 27px; font-weight: 700; color: #C4642F; margin-bottom: 14px;
           text-decoration: underline; text-decoration-style: wavy; text-decoration-color: #E8C9A0; }
.metric { margin-bottom: 14px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13.5px; color: #7A6752; }
.metric-top b { font-size: 18px; color: #4A3B2A; font-weight: 800; }
.bar { height: 12px; background: #F4EAD5; border-radius: 999px; margin-top: 6px; position: relative; overflow: hidden; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #E8B04B; border-radius: 999px; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 11.5px; color: #A08C6E; margin: 12px 0 16px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 13.5px; border: 2px solid #E8C9A0; color: #8A5A2B;
       border-radius: 999px; font-weight: 700; background: #FFF8E6; }
.btn.primary { background: #C4642F; border-color: #C4642F; color: #FFF6E8; }
.btn.wide { display: block; margin-bottom: 16px; }
.card { background: #FFF6C9; border-radius: 6px; padding: 13px 15px; box-shadow: 2px 3px 0 #E4D5A8;
        transform: rotate(-.6deg); }
.tag { font-size: 10.5px; color: #B08A2E; font-weight: 700; letter-spacing: 1px; }
.card-title { font-family: "STKaiti","KaiTi",serif; font-size: 17.5px; line-height: 1.35; margin: 6px 0 4px; color: #4A3B2A; }
.card-sub { font-size: 11.5px; color: #A08C6E; }
.section { display: flex; justify-content: space-between; font-size: 14px; margin: 18px 0 4px;
           font-family: "STKaiti","KaiTi",serif; font-weight: 700; color: #C4642F; }
.more { color: #C4642F; font-size: 12px; font-family: "Microsoft YaHei",sans-serif; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-bottom: 2px dotted #EFE0C2; }
.word { font-size: 19px; font-weight: 800; color: #4A3B2A; }
.ph { font-size: 11.5px; color: #A08C6E; }
.cn { font-size: 11.5px; color: #7A6752; margin-top: 2px; }
.play { color: #C4642F; font-size: 13px; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; border-top: 2px solid #F4EAD5;
       font-size: 12px; color: #A08C6E; background: #FFFBF0; }
.nav .on { color: #C4642F; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13.5px; margin-bottom: 14px; color: #7A6752; }
.count { color: #C4642F; font-weight: 700; }
.wordcard { background: #FFF6C9; border-radius: 8px; padding: 24px 20px; box-shadow: 3px 4px 0 #E4D5A8; transform: rotate(-.5deg); }
.bigword { font-size: 42px; font-weight: 800; color: #4A3B2A; letter-spacing: -.5px; }
.bigph { color: #A08C6E; margin: 6px 0 18px; font-size: 13px; }
.pos { font-size: 14px; margin-bottom: 6px; color: #5C4A34; }
.quote { font-family: "STKaiti","KaiTi",serif; font-size: 15px; margin-top: 16px; padding-left: 12px;
         border-left: 3px solid #E8B04B; color: #7A6752; }
.quote-cn { font-size: 11.5px; color: #A08C6E; padding-left: 12px; margin-top: 4px; }
""" + RATES + """
.rate { border-radius: 999px; color: #fff; font-weight: 700; }
.rate b { color: rgba(255,255,255,.85); }
.rate.r1 { background: #E07A6B; }
.rate.r2 { background: #E8B04B; }
.rate.r3 { background: #7FAF7A; }
.rate.r4 { background: #7FA6C9; }
.a-title { font-family: "STKaiti","KaiTi",serif; font-size: 25px; line-height: 1.3; color: #4A3B2A; }
.a-sub { font-size: 12.5px; color: #A08C6E; margin: 6px 0 16px; }
.para { font-size: 15px; line-height: 1.95; margin-bottom: 14px; color: #5C4A34; }
mark { background: #FFE9A8; color: #8A5A2B; font-weight: 700; border-radius: 4px; padding: 0 2px; }
"""

# ------------------------------------------------- L · 森林
CSS_L = """
.board { color: #E4EFE2; }
.phone { background: #16221A; border: 1px solid #223326; border-radius: 20px;
         box-shadow: 0 14px 34px rgba(0,20,8,.55); }
.status { color: #7A9A80; }
.h-title { font-size: 24px; font-weight: 700; color: #A7D08C; margin-bottom: 16px; }
.metric { margin-bottom: 16px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13.5px; color: #8FB295; }
.metric-top b { font-size: 18px; color: #E4EFE2; font-weight: 800; }
.bar { height: 9px; background: #1F2E23; border-radius: 999px; margin-top: 7px; position: relative; overflow: hidden; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #7FB069; border-radius: 999px; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 11.5px; color: #6E8C74; margin: 13px 0 17px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 13.5px; border: 1px solid #2E4634; color: #BCD6B8;
       border-radius: 999px; background: #1C2A20; }
.btn.primary { background: #7FB069; border-color: #7FB069; color: #10200F; font-weight: 800; }
.btn.wide { display: block; margin-bottom: 17px; }
.card { background: #1C2A20; border: 1px solid #26382B; border-left: 3px solid #A7D08C; border-radius: 14px; padding: 13px 15px; }
.tag { font-size: 10.5px; color: #A7D08C; letter-spacing: 1.5px; font-weight: 700; }
.card-title { font-size: 16.5px; line-height: 1.35; margin: 6px 0 4px; color: #E4EFE2; }
.card-sub { font-size: 11.5px; color: #6E8C74; }
.section { display: flex; justify-content: space-between; font-size: 13.5px; margin: 19px 0 4px;
           border-bottom: 1px solid #26382B; padding-bottom: 6px; color: #A7D08C; font-weight: 700; }
.more { color: #E0C17A; font-size: 11.5px; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #1F2E23; }
.word { font-size: 19px; font-weight: 700; color: #E4EFE2; }
.ph { font-size: 11.5px; color: #6E8C74; }
.cn { font-size: 11.5px; color: #8FB295; margin-top: 2px; }
.play { color: #A7D08C; font-size: 13px; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #223326;
       font-size: 11.5px; color: #6E8C74; background: #131E17; }
.nav .on { color: #A7D08C; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13.5px; margin-bottom: 15px; color: #8FB295; }
.count { color: #A7D08C; font-weight: 700; }
.wordcard { background: #1C2A20; border: 1px solid #2A3C2F; border-radius: 18px; padding: 24px 20px; }
.bigword { font-size: 42px; font-weight: 800; color: #E9F5E4; letter-spacing: -.5px; }
.bigph { color: #6E8C74; margin: 6px 0 18px; font-size: 13px; }
.pos { font-size: 14px; margin-bottom: 6px; color: #BCD6B8; }
.quote { font-size: 14px; margin-top: 18px; padding-left: 13px; border-left: 3px solid #7FB069; color: #8FB295; }
.quote-cn { font-size: 11.5px; color: #6E8C74; padding-left: 13px; margin-top: 4px; }
""" + RATES + """
.rate { background: #1C2A20; border: 1px solid #2E4634; color: #BCD6B8; }
.rate b { color: #6E8C74; }
.rate.r1 { background: #B96A5E; border-color: #B96A5E; color: #FFF3F0; }
.rate.r2 { background: #C79A4E; border-color: #C79A4E; color: #241A06; }
.rate.r3 { background: #7FB069; border-color: #7FB069; color: #10200F; font-weight: 800; }
.rate.r4 { background: #5E8CA8; border-color: #5E8CA8; color: #F0F7FB; }
.a-title { font-size: 22px; line-height: 1.3; font-weight: 800; color: #E9F5E4; }
.a-sub { font-size: 12.5px; color: #6E8C74; margin: 6px 0 16px; }
.para { font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; color: #BCD6B8; }
mark { background: rgba(127,176,105,.18); color: #C8E6B4; font-weight: 700; border-radius: 4px; }
"""

# ------------------------------------------------- M · 莫兰迪
CSS_M = """
.board { color: #3F4448; }
.phone { background: #F7F5F1; border-radius: 24px; box-shadow: 0 12px 30px rgba(90,95,100,.16); }
.status { color: #9AA0A4; }
.h-title { font-size: 22px; font-weight: 700; color: #5E6A70; letter-spacing: .5px; margin-bottom: 16px; }
.metric { margin-bottom: 16px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13.5px; color: #7C8489; }
.metric-top b { font-size: 18px; color: #3F4448; font-weight: 800; }
.bar { height: 8px; background: #E8E4DD; border-radius: 999px; margin-top: 7px; position: relative; overflow: hidden; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #8C9A9E; border-radius: 999px; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 11.5px; color: #9AA0A4; margin: 13px 0 17px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 13.5px; border: 1px solid #D6D2CB; color: #5E6A70;
       border-radius: 999px; background: #fff; }
.btn.primary { background: #8C9A9E; border-color: #8C9A9E; color: #FBFBFA; font-weight: 700; }
.btn.wide { display: block; margin-bottom: 17px; }
.card { background: #fff; border-radius: 16px; padding: 14px 16px; box-shadow: 0 2px 10px rgba(120,125,130,.10); }
.tag { font-size: 10.5px; color: #B08B80; letter-spacing: 1.5px; font-weight: 700; }
.card-title { font-size: 16.5px; line-height: 1.35; margin: 6px 0 4px; color: #3F4448; }
.card-sub { font-size: 11.5px; color: #9AA0A4; }
.section { display: flex; justify-content: space-between; font-size: 13.5px; margin: 19px 0 4px;
           border-bottom: 1px solid #E8E4DD; padding-bottom: 6px; color: #5E6A70; font-weight: 700; }
.more { color: #B08B80; font-size: 11.5px; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #EDE9E2; }
.word { font-size: 19px; font-weight: 700; color: #3F4448; }
.ph { font-size: 11.5px; color: #9AA0A4; }
.cn { font-size: 11.5px; color: #7C8489; margin-top: 2px; }
.play { color: #8C9A9E; font-size: 13px; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #E8E4DD;
       font-size: 11.5px; color: #9AA0A4; background: #F2EFEA; }
.nav .on { color: #5E6A70; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13.5px; margin-bottom: 15px; color: #7C8489; }
.count { color: #5E6A70; font-weight: 700; }
.wordcard { background: #fff; border-radius: 20px; padding: 26px 20px; box-shadow: 0 4px 16px rgba(120,125,130,.12); }
.bigword { font-size: 42px; font-weight: 800; color: #3F4448; letter-spacing: -.5px; }
.bigph { color: #9AA0A4; margin: 6px 0 18px; font-size: 13px; }
.pos { font-size: 14px; margin-bottom: 6px; color: #5E6A70; }
.quote { font-size: 14px; margin-top: 18px; padding-left: 13px; border-left: 3px solid #D6D2CB; color: #7C8489; }
.quote-cn { font-size: 11.5px; color: #9AA0A4; padding-left: 13px; margin-top: 4px; }
""" + RATES + """
.rate { color: #fff; font-weight: 700; }
.rate b { color: rgba(255,255,255,.85); }
.rate.r1 { background: #C08C86; }
.rate.r2 { background: #C7A98A; }
.rate.r3 { background: #93A891; }
.rate.r4 { background: #93A3B5; }
.a-title { font-size: 22px; line-height: 1.3; font-weight: 800; color: #3F4448; }
.a-sub { font-size: 12.5px; color: #9AA0A4; margin: 6px 0 16px; }
.para { font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; color: #5E6A70; }
mark { background: #E4E9E1; color: #4F5F4B; font-weight: 700; border-radius: 4px; }
"""

CANDIDATES = [
    ("style-i", "I · 铅字", "黑白 · 报纸报头 · 衬线大标题 · 灰度分级评分 —— 最学术严肃", CSS_I, "#D9D5CD"),
    ("style-j", "J · 蓝图", "深蓝底 · 虚线网格 · 等宽数字 · 刻度进度 —— 理性/工程感", CSS_J, "#08182C"),
    ("style-k", "K · 便签", "奶油黄便签 · 楷体 · 手写波浪线 · 纸片斜贴 —— 轻松不严肃", CSS_K, "#EDE4D2"),
    ("style-l", "L · 森林", "墨绿底 · 苔绿强调 · 暖金点缀 —— 暗色但自然，夜里不刺眼", CSS_L, "#0D1710"),
    ("style-m", "M · 莫兰迪", "低饱和灰调 · 雾霾蓝灰+灰粉+燕麦 —— 高级、柔和不抢眼", CSS_M, "#E4E1DB"),
]

for slug, name, desc, css, bg in CANDIDATES:
    path = os.path.join(mk.OUT, slug + ".html")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(mk.doc(name, desc, css, bg))
    print("已生成", slug + ".html")
