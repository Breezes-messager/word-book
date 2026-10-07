# -*- coding: utf-8 -*-
"""生成 4 套**新**风格候选的样板 HTML（E 旧课本 / F 极简白 / G 拿铁 / H 霓虹夜）。
模板与 style-a~d 完全一致：同一套内容，只换 CSS。"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "docs", "style-samples")
os.makedirs(OUT, exist_ok=True)

BODY = """
<div class="board">
  <div class="head">
    <div class="name">__NAME__</div>
    <div class="desc">__DESC__</div>
  </div>
  <div class="row">
    <div class="phone">
      <div class="status"><span>18:43</span><span class="dots">5G &nbsp; &#9679;&#9679;&#9679;</span></div>
      <div class="screen home">
        <div class="h-title">今日任务</div>
        <div class="metric">
          <div class="metric-top"><span>新词</span><b>29 / 70</b></div>
          <div class="bar"><i style="width:41%"></i></div>
        </div>
        <div class="metric">
          <div class="metric-top"><span>待复习</span><b>21 个</b></div>
          <div class="bar"><i style="width:0%"></i></div>
        </div>
        <div class="hint">连续打卡 1 天 · 词库共 5046 词</div>
        <div class="btn-row"><div class="btn primary">开始学习</div><div class="btn">开始复习</div></div>
        <div class="btn wide">&#9654;&nbsp; 今日文章</div>
        <div class="card article-card">
          <div class="tag">最近生成的文章</div>
          <div class="card-title">A Journey of Discovery in a Small Group</div>
          <div class="card-sub">2026-10-06 · 小群体中的发现之旅</div>
        </div>
        <div class="section">词书预览（前 20 个词）<span class="more">查看全部</span></div>
        <div class="list-row"><div><div class="word">paragraph</div><div class="ph">/'pærəgræf/</div><div class="cn">n. 段落;短评;段落符号</div></div><div class="play">&#9654;</div></div>
      </div>
      <div class="nav"><span class="on">首页</span><span>文章</span><span>统计</span><span>设置</span></div>
    </div>

    <div class="phone">
      <div class="status"><span>18:43</span><span class="dots">5G &nbsp; &#9679;&#9679;&#9679;</span></div>
      <div class="screen study">
        <div class="study-top"><span>&#10005;</span><span>学习新词</span><span class="count">3 / 20</span></div>
        <div class="wordcard">
          <div class="bigword">paragraph</div>
          <div class="bigph">/'pærəgræf/</div>
          <div class="pos">n. 段落;短评;段落符号</div>
          <div class="pos">v. 将…分段</div>
          <div class="quote">the opening paragraphs of the novel</div>
          <div class="quote-cn">小说开篇的几个段落</div>
        </div>
        <div class="ratings">
          <div class="rate r1">重来<b>1 分钟</b></div>
          <div class="rate r2">困难<b>6 分钟</b></div>
          <div class="rate r3">良好<b>10 分钟</b></div>
          <div class="rate r4">简单<b>8 天</b></div>
        </div>
      </div>
    </div>

    <div class="phone">
      <div class="status"><span>18:43</span><span class="dots">5G &nbsp; &#9679;&#9679;&#9679;</span></div>
      <div class="screen article">
        <div class="a-title">A Journey of Discovery in a Small Group</div>
        <div class="a-sub">小群体中的发现之旅</div>
        <p class="para">Last week I read a short <mark>paragraph</mark> about a famous scientist.
        It was not long, but it had a deep <mark>influence</mark> on me.</p>
        <p class="para">The paragraph described how the scientist used his
        <mark>intellectual</mark> power to solve a problem that many people thought was too hard.</p>
        <div class="btn wide">显示中文翻译</div>
        <div class="hint">风格：小故事 · 生成时间：2026 年 10 月 6 日 09:03</div>
      </div>
    </div>
  </div>
</div>
"""

def doc(name, desc, css, board_bg):
    return """<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: %s; font-family: "Microsoft YaHei", sans-serif; }
.board { padding: 28px 34px 34px; }
.head { margin-bottom: 18px; }
.name { font-size: 26px; font-weight: 700; letter-spacing: .5px; }
.desc { font-size: 14px; opacity: .62; margin-top: 4px; }
.row { display: flex; gap: 26px; }
.phone { width: 360px; height: 760px; border-radius: 26px; overflow: hidden; display: flex; flex-direction: column; }
.screen { flex: 1; overflow: hidden; padding: 16px 18px; }
.status { height: 26px; display: flex; justify-content: space-between; align-items: center; padding: 0 16px; font-size: 11px; }
%s
</style></head><body>%s</body></html>""" % (board_bg, css, BODY.replace("__NAME__", name).replace("__DESC__", desc))

RATES = """
.ratings { display: flex; gap: 8px; margin-top: 20px; }
.rate { flex: 1; text-align: center; padding: 10px 0; font-size: 13px; line-height: 1.5; }
.rate b { display: block; font-size: 10px; font-weight: 400; }
"""

# ------------------------------------------------------- E · 旧课本
CSS_E = """
.board { color: #23304A; }
.phone { background: #F7F1E1; border: 1px solid #C9BFA6; box-shadow: 0 10px 26px rgba(70,60,35,.18); border-radius: 10px; }
.status { color: #7A7057; font-family: Georgia, serif; }
.h-title { font-family: Georgia,"STZhongsong","SimSun",serif; font-size: 26px; color: #23407A;
           border-bottom: 2px solid #23407A; padding-bottom: 8px; margin-bottom: 16px; }
.metric { margin-bottom: 14px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; color: #4A5470; }
.metric-top b { font-family: Georgia, serif; font-size: 17px; color: #23407A; }
.bar { height: 6px; background: #E4DCC6; margin-top: 6px; border: 1px solid #CFC5A9; position: relative; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #23407A; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 12px; color: #7A7057; margin: 12px 0 16px; font-family: Georgia,serif; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 11px 0; font-size: 14px; border: 1.5px solid #23407A; color: #23407A;
       border-radius: 4px; font-weight: 600; }
.btn.primary { background: #23407A; color: #F7F1E1; }
.btn.wide { display: block; margin-bottom: 16px; }
.card { border: 1px solid #CFC5A9; border-left: 4px solid #C0392B; padding: 12px 14px; background: #FFFCF2; border-radius: 4px; }
.tag { font-size: 11px; color: #C0392B; letter-spacing: 1.5px; font-weight: 700; }
.card-title { font-family: Georgia, serif; font-size: 17px; line-height: 1.35; margin: 6px 0 4px; }
.card-sub { font-size: 12px; color: #7A7057; }
.section { display: flex; justify-content: space-between; font-size: 14px; margin: 18px 0 4px;
           border-bottom: 1px solid #CFC5A9; padding-bottom: 6px; font-weight: 600; }
.more { color: #C0392B; font-size: 12px; font-weight: 400; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 11px 0; border-bottom: 1px dashed #DED4B8; }
.word { font-family: Georgia, serif; font-size: 19px; color: #23304A; }
.ph { font-size: 12px; color: #7A7057; font-family: Georgia, serif; }
.cn { font-size: 12px; color: #4A5470; margin-top: 2px; }
.play { color: #C0392B; font-size: 13px; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 2px solid #23407A;
       font-size: 12px; color: #7A7057; background: #F1EAD8; }
.nav .on { color: #23407A; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 14px; color: #4A5470; }
.count { font-family: Georgia, serif; color: #7A7057; }
.wordcard { border: 1px solid #CFC5A9; background: #FFFCF2; padding: 20px 18px; border-radius: 4px;
            background-image: repeating-linear-gradient(transparent, transparent 27px, #C9D8EA 27px, #C9D8EA 28px); }
.bigword { font-family: Georgia, serif; font-size: 40px; color: #23407A; }
.bigph { font-family: Georgia, serif; color: #7A7057; margin: 6px 0 16px; }
.pos { font-size: 14px; margin-bottom: 6px; }
.quote { font-family: Georgia, serif; font-size: 14px; margin-top: 16px; padding-left: 12px; border-left: 3px solid #C0392B; }
.quote-cn { font-size: 12px; color: #7A7057; padding-left: 12px; margin-top: 4px; }
""" + RATES + """
.rate { border: 1.5px solid #23407A; border-radius: 4px; background: #FFFCF2; color: #23304A; font-weight: 600; }
.rate b { color: #7A7057; font-family: Georgia, serif; }
.rate.r1 { background: #C0392B; border-color: #C0392B; color: #FFF6F2; }
.rate.r2 { background: #C8862F; border-color: #C8862F; color: #FFF9EE; }
.rate.r3 { background: #2F6B57; border-color: #2F6B57; color: #F0FBF6; }
.rate.r4 { background: #23407A; border-color: #23407A; color: #EEF3FF; }
.a-title { font-family: Georgia, serif; font-size: 22px; line-height: 1.3; color: #23304A; }
.a-sub { font-family: Georgia,"STZhongsong","SimSun",serif; font-size: 13px; color: #7A7057; margin: 6px 0 14px; }
.para { font-family: Georgia, serif; font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; text-align: justify; }
mark { background: #FCEFC2; color: #23407A; font-weight: 700; box-shadow: 0 1px 0 #C8862F; }
"""

# ------------------------------------------------------- F · 极简白
CSS_F = """
.board { color: #111; }
.phone { background: #fff; border-radius: 12px; box-shadow: 0 1px 0 #DDD, 0 14px 34px rgba(0,0,0,.10); }
.status { color: #999; font-family: Helvetica, Arial, sans-serif; }
.h-title { font-size: 13px; font-weight: 700; letter-spacing: 3px; color: #111; margin-bottom: 20px;
           text-transform: uppercase; }
.metric { margin-bottom: 18px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; color: #666; }
.metric-top b { font-size: 22px; color: #111; font-weight: 800; letter-spacing: -.5px; }
.bar { height: 2px; background: #ECECEC; margin-top: 8px; position: relative; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #E02020; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 11.5px; color: #9A9A9A; margin: 14px 0 20px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 13.5px; border: 1px solid #111; color: #111;
       border-radius: 6px; font-weight: 600; }
.btn.primary { background: #111; color: #fff; }
.btn.wide { display: block; margin-bottom: 18px; }
.card { border-top: 1px solid #ECECEC; border-bottom: 1px solid #ECECEC; padding: 13px 0; }
.tag { font-size: 10.5px; color: #E02020; letter-spacing: 2px; font-weight: 700; }
.card-title { font-size: 16.5px; line-height: 1.35; margin: 6px 0 4px; font-weight: 700; letter-spacing: -.2px; }
.card-sub { font-size: 11.5px; color: #9A9A9A; }
.section { display: flex; justify-content: space-between; font-size: 13px; margin: 20px 0 2px;
           padding-bottom: 8px; border-bottom: 1px solid #111; font-weight: 700; }
.more { color: #E02020; font-size: 11.5px; font-weight: 600; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 13px 0; border-bottom: 1px solid #F0F0F0; }
.word { font-size: 18px; font-weight: 800; letter-spacing: -.3px; }
.ph { font-size: 11.5px; color: #9A9A9A; }
.cn { font-size: 11.5px; color: #666; margin-top: 3px; }
.play { color: #111; font-size: 12px; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #ECECEC;
       font-size: 11.5px; color: #AAA; background: #fff; }
.nav .on { color: #111; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 13px; margin-bottom: 16px; color: #666; }
.count { color: #111; font-weight: 700; }
.wordcard { border: 1px solid #ECECEC; border-radius: 10px; background: #fff; padding: 26px 20px; }
.bigword { font-size: 42px; font-weight: 800; letter-spacing: -1.5px; }
.bigph { color: #9A9A9A; margin: 6px 0 20px; font-size: 13px; }
.pos { font-size: 14px; margin-bottom: 6px; }
.quote { font-size: 13.5px; margin-top: 18px; padding-left: 12px; border-left: 2px solid #111; color: #333; }
.quote-cn { font-size: 11.5px; color: #9A9A9A; padding-left: 12px; margin-top: 4px; }
""" + RATES + """
.rate { border: 1px solid #111; border-radius: 6px; color: #111; font-weight: 700; }
.rate b { color: #999; }
.rate.r1 { background: #E02020; border-color: #E02020; color: #fff; }
.rate.r2 { background: #fff; border-color: #111; }
.rate.r3 { background: #111; border-color: #111; color: #fff; }
.rate.r4 { background: #fff; border-color: #BBB; color: #666; }
.rate.r3 b { color: #bbb; }
.a-title { font-size: 24px; line-height: 1.25; font-weight: 800; letter-spacing: -.6px; }
.a-sub { font-size: 12.5px; color: #9A9A9A; margin: 6px 0 16px; }
.para { font-size: 14.5px; line-height: 1.9; margin-bottom: 14px; color: #222; }
mark { background: transparent; border-bottom: 2px solid #E02020; font-weight: 700; }
"""

# ------------------------------------------------------- G · 拿铁
CSS_G = """
.board { color: #3A2E26; }
.phone { background: #FFFBF5; border-radius: 30px; box-shadow: 0 14px 34px rgba(120,90,60,.20); }
.status { color: #A08C77; }
.h-title { font-size: 24px; font-weight: 800; color: #6B4423; margin-bottom: 16px; letter-spacing: -.3px; }
.metric { margin-bottom: 16px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; color: #7A6653; }
.metric-top b { font-size: 18px; color: #6B4423; font-weight: 800; }
.bar { height: 10px; background: #F1E4D4; border-radius: 999px; margin-top: 8px; position: relative; overflow: hidden; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #B4703A; border-radius: 999px; }
.bar i[style*="0%"] { background: transparent; }
.hint { font-size: 12px; color: #A08C77; margin: 14px 0 18px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 13px 0; font-size: 14px; border: 1.5px solid #D9C3A8; color: #6B4423;
       border-radius: 16px; font-weight: 700; background: #FFF6EA; }
.btn.primary { background: #B4703A; border-color: #B4703A; color: #FFF9F2; }
.btn.wide { display: block; margin-bottom: 18px; }
.card { background: #FFF6EA; border-radius: 18px; padding: 14px 16px; box-shadow: 0 2px 8px rgba(150,110,70,.10); }
.tag { font-size: 11px; color: #B4703A; font-weight: 700; letter-spacing: 1px; }
.card-title { font-size: 17px; line-height: 1.35; margin: 6px 0 4px; font-weight: 700; color: #3A2E26; }
.card-sub { font-size: 12px; color: #A08C77; }
.section { display: flex; justify-content: space-between; font-size: 14px; margin: 20px 0 4px;
           padding-bottom: 6px; border-bottom: 1.5px solid #F1E4D4; font-weight: 700; color: #6B4423; }
.more { color: #B4703A; font-size: 12px; font-weight: 600; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #F6EDE1; }
.word { font-size: 19px; font-weight: 800; color: #3A2E26; }
.ph { font-size: 12px; color: #A08C77; }
.cn { font-size: 12px; color: #7A6653; margin-top: 3px; }
.play { color: #B4703A; font-size: 13px; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; border-top: 1.5px solid #F1E4D4;
       font-size: 12px; color: #A08C77; background: #FFF8F0; }
.nav .on { color: #6B4423; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 16px; color: #7A6653; }
.count { color: #6B4423; font-weight: 700; }
.wordcard { background: #FFF6EA; border-radius: 24px; padding: 26px 20px; box-shadow: 0 4px 14px rgba(150,110,70,.12); }
.bigword { font-size: 42px; font-weight: 800; color: #3A2E26; letter-spacing: -.8px; }
.bigph { color: #A08C77; margin: 6px 0 18px; font-size: 13px; }
.pos { font-size: 14px; margin-bottom: 6px; color: #4A3A30; }
.quote { font-size: 14px; margin-top: 18px; padding-left: 14px; border-left: 3px solid #D9C3A8; color: #6B5A4C; }
.quote-cn { font-size: 12px; color: #A08C77; padding-left: 14px; margin-top: 4px; }
""" + RATES + """
.rate { border-radius: 16px; color: #fff; font-weight: 700; }
.rate b { color: rgba(255,255,255,.82); }
.rate.r1 { background: #D2705C; }
.rate.r2 { background: #D99A4E; }
.rate.r3 { background: #6E9E78; }
.rate.r4 { background: #7E93B8; }
.a-title { font-size: 23px; line-height: 1.3; font-weight: 800; color: #3A2E26; }
.a-sub { font-size: 13px; color: #A08C77; margin: 6px 0 16px; }
.para { font-size: 15px; line-height: 1.95; margin-bottom: 14px; color: #4A3A30; }
mark { background: #FBE6C8; color: #6B4423; font-weight: 700; border-radius: 4px; padding: 0 2px; }
"""

# ------------------------------------------------------- H · 霓虹夜
CSS_H = """
.board { color: #E7ECFF; }
.phone { background: #111729; border: 1px solid #1E2A45; border-radius: 18px;
         box-shadow: 0 0 24px rgba(34,211,238,.14), 0 16px 36px rgba(0,0,0,.55); }
.status { color: #5E6A8C; font-family: Consolas, monospace; }
.h-title { font-size: 14px; letter-spacing: 5px; color: #7C89B4; text-transform: uppercase; margin-bottom: 20px; }
.metric { margin-bottom: 18px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; color: #8A94B8; }
.metric-top b { font-family: Consolas, monospace; font-size: 19px; color: #E7ECFF; }
.bar { height: 4px; background: #1B2440; border-radius: 999px; margin-top: 10px; position: relative; overflow: hidden; }
.bar i { position: absolute; left: 0; top: 0; bottom: 0; background: #22D3EE; box-shadow: 0 0 12px #22D3EE99; border-radius: 999px; }
.bar i[style*="0%"] { background: transparent; box-shadow: none; }
.hint { font-family: Consolas, monospace; font-size: 11px; color: #5E6A8C; margin: 16px 0 20px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 14px; border: 1px solid #2A3A5E; color: #B9C4E4;
       border-radius: 12px; background: #151D33; }
.btn.primary { background: linear-gradient(135deg,#22D3EE,#A78BFA); border: none; color: #0A0E1A; font-weight: 800; }
.btn.wide { display: block; margin-bottom: 20px; }
.card { background: #151D33; border: 1px solid #22304F; border-left: 3px solid #A78BFA; border-radius: 12px; padding: 13px 15px; }
.tag { font-size: 11px; color: #A78BFA; letter-spacing: 1.5px; font-weight: 700; }
.card-title { font-size: 16.5px; line-height: 1.35; margin: 6px 0 4px; font-weight: 700; }
.card-sub { font-size: 11.5px; color: #5E6A8C; }
.section { display: flex; justify-content: space-between; font-size: 14px; margin: 20px 0 4px;
           padding-bottom: 6px; border-bottom: 1px solid #22304F; font-weight: 700; color: #B9C4E4; }
.more { color: #22D3EE; font-size: 12px; font-weight: 600; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #1A2440; }
.word { font-size: 19px; font-weight: 800; color: #E7ECFF; }
.ph { font-size: 12px; color: #5E6A8C; font-family: Consolas, monospace; }
.cn { font-size: 12px; color: #8A94B8; margin-top: 3px; }
.play { color: #22D3EE; font-size: 13px; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #1E2A45;
       font-size: 12px; color: #5E6A8C; background: #0E1424; }
.nav .on { color: #22D3EE; font-weight: 700; text-shadow: 0 0 10px #22D3EE88; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 16px; color: #8A94B8; }
.count { font-family: Consolas, monospace; color: #22D3EE; }
.wordcard { background: #151D33; border: 1px solid #253457; border-radius: 16px; padding: 26px 20px;
            box-shadow: 0 0 22px rgba(34,211,238,.10) inset; }
.bigword { font-size: 42px; font-weight: 800; letter-spacing: -.5px;
           background: linear-gradient(135deg,#22D3EE,#A78BFA); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.bigph { color: #5E6A8C; margin: 6px 0 18px; font-size: 13px; font-family: Consolas, monospace; }
.pos { font-size: 14px; margin-bottom: 6px; color: #C6D0EE; }
.quote { font-size: 14px; margin-top: 18px; padding-left: 14px; border-left: 3px solid #22D3EE; color: #8A94B8; }
.quote-cn { font-size: 12px; color: #5E6A8C; padding-left: 14px; margin-top: 4px; }
""" + RATES + """
.rate { border-radius: 12px; background: #151D33; border: 1px solid #2A3A5E; color: #B9C4E4; font-weight: 700; }
.rate b { color: #5E6A8C; font-family: Consolas, monospace; }
.rate.r1 { border-color: #F0616D; color: #F0616D; box-shadow: 0 0 14px rgba(240,97,109,.22); }
.rate.r2 { border-color: #F0B45A; color: #F0B45A; box-shadow: 0 0 14px rgba(240,180,90,.20); }
.rate.r3 { border-color: #22D3EE; color: #22D3EE; box-shadow: 0 0 16px rgba(34,211,238,.28); }
.rate.r4 { border-color: #A78BFA; color: #A78BFA; box-shadow: 0 0 14px rgba(167,139,250,.24); }
.rate b { color: inherit; opacity: .7; }
.a-title { font-size: 22px; line-height: 1.3; font-weight: 800; color: #E7ECFF; }
.a-sub { font-size: 13px; color: #5E6A8C; margin: 6px 0 16px; }
.para { font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; color: #C6D0EE; }
mark { background: rgba(34,211,238,.14); color: #22D3EE; font-weight: 700; border-radius: 4px; box-shadow: 0 1px 0 #22D3EE; }
"""

CANDIDATES = [
    ("style-e", "E · 旧课本", "泛黄纸 · 蓝墨水标题 · 红批改 · 英语横格线 —— 像翻一本用旧的教材", CSS_E, "#DDD5C2"),
    ("style-f", "F · 极简白", "纯白 · 发丝细线 · 一抹正红 · 大量留白 —— 最不打扰的那种干净", CSS_F, "#E9E9E9"),
    ("style-g", "G · 拿铁", "奶油暖棕 · 大圆角 · 柔和阴影 —— 暖色护眼，久看不累", CSS_G, "#E6DCCE"),
    ("style-h", "H · 霓虹夜", "深蓝紫底 · 青紫霓虹 · 发光描边 —— 夜间想有点科技感", CSS_H, "#080B14"),
]

for slug, name, desc, css, bg in CANDIDATES:
    path = os.path.join(OUT, slug + ".html")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(doc(name, desc, css, bg))
    print("已生成", slug + ".html")
