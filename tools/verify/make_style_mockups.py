# -*- coding: utf-8 -*-
"""生成 4 套风格样板的 HTML（同一套内容，只换 CSS 与布局语言），再用 Chrome 无头截图。

用法：
    python tools/verify/make_style_mockups.py          # 生成 HTML 到 docs/style-samples/
    然后对每个 html 跑一次 Chrome 无头截图，见 tools/verify/render_style_mockups.sh
"""
import os

# HTML 与 PNG 都放在 docs/style-samples/ 下，方便对照
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


# ----------------------------------------------------------------- 风格 A：墨纸
CSS_A = """
.board { color: #1B1A17; }
.phone { background: #F7F4ED; border: 1px solid #D9D3C5; box-shadow: 0 10px 26px rgba(60,50,30,.14); }
.status { color: #6C6656; font-family: Georgia, serif; }
.h-title { font-family: "STZhongsong","SimSun",serif; font-size: 30px; letter-spacing: 3px;
           border-bottom: 1px solid #1B1A17; padding-bottom: 10px; margin-bottom: 18px; }
.metric { margin-bottom: 16px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; color: #3C3A33; }
.metric-top b { font-family: Georgia, serif; font-size: 17px; color: #1B1A17; }
.bar { height: 1px; background: #CFC7B6; margin-top: 8px; position: relative; }
.bar i { position: absolute; left: 0; top: -1px; height: 3px; background: #B23A2E; }
.hint { font-size: 12px; color: #7C755F; margin: 14px 0 18px; font-family: Georgia,"SimSun",serif; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 11px 0; font-size: 14px; border: 1px solid #1B1A17; color: #1B1A17;
       letter-spacing: 1px; }
.btn.primary { background: #1B1A17; color: #F7F4ED; }
.btn.wide { display: block; margin-bottom: 18px; }
.card { border: 1px solid #D9D3C5; border-left: 3px solid #B23A2E; padding: 12px 14px; background: #FBF9F4; }
.tag { font-size: 11px; color: #B23A2E; letter-spacing: 2px; }
.card-title { font-family: Georgia, serif; font-size: 17px; line-height: 1.35; margin: 6px 0 4px; }
.card-sub { font-size: 12px; color: #7C755F; }
.section { display: flex; justify-content: space-between; font-size: 14px; margin: 20px 0 6px;
           border-bottom: 1px solid #CFC7B6; padding-bottom: 6px; }
.more { color: #B23A2E; font-size: 12px; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #E4DFD3; }
.word { font-family: Georgia, serif; font-size: 19px; }
.ph { font-size: 12px; color: #7C755F; font-family: Georgia, serif; }
.cn { font-size: 12px; color: #4A473D; margin-top: 2px; }
.play { color: #B23A2E; font-size: 13px; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #D9D3C5;
       font-size: 12px; color: #7C755F; background: #F2EEE4; }
.nav .on { color: #1B1A17; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 14px; margin-bottom: 16px; }
.count { font-family: Georgia, serif; color: #7C755F; }
.wordcard { border: 1px solid #D9D3C5; background: #FBF9F4; padding: 22px 18px; }
.bigword { font-family: Georgia, serif; font-size: 40px; }
.bigph { font-family: Georgia, serif; color: #7C755F; margin: 6px 0 18px; }
.pos { font-size: 14px; margin-bottom: 6px; }
.quote { font-family: Georgia, serif; font-size: 14px; margin-top: 18px; padding-left: 12px; border-left: 2px solid #B23A2E; }
.quote-cn { font-size: 12px; color: #7C755F; padding-left: 12px; margin-top: 4px; }
.ratings { display: flex; gap: 8px; margin-top: 20px; }
.rate { flex: 1; text-align: center; padding: 10px 0; font-size: 13px; border: 1px solid #1B1A17; line-height: 1.5; }
.rate b { display: block; font-size: 10px; font-weight: 400; color: #6C6656; font-family: Georgia, serif; }
.rate.r1 { background: #B23A2E; color: #F7F4ED; border-color: #B23A2E; }
.rate.r1 b { color: #F0D9D5; }
.rate.r2 { background: #C8862F; color: #FBF9F4; border-color: #C8862F; }
.rate.r2 b { color: #F4E3CB; }
.rate.r3 { background: #2F6B57; color: #FBF9F4; border-color: #2F6B57; }
.rate.r3 b { color: #CFE3DA; }
.rate.r4 { background: #35506F; color: #FBF9F4; border-color: #35506F; }
.rate.r4 b { color: #D2DEEB; }
.a-title { font-family: Georgia, serif; font-size: 22px; line-height: 1.3; }
.a-sub { font-family: "STZhongsong","SimSun",serif; font-size: 13px; color: #7C755F; margin: 6px 0 16px; }
.para { font-family: Georgia, serif; font-size: 14.5px; line-height: 1.95; margin-bottom: 14px; text-align: justify; }
mark { background: transparent; border-bottom: 2px solid #B23A2E; color: #B23A2E; font-weight: 700; }
"""

# ------------------------------------------------------------- 风格 B：暗夜专注
CSS_B = """
.board { color: #EEF1F5; }
.phone { background: #0B0D10; border: 1px solid #262B33; box-shadow: 0 10px 30px rgba(0,0,0,.5); }
.status { color: #6B7480; font-family: Consolas, monospace; }
.h-title { font-size: 15px; letter-spacing: 6px; color: #7C8794; text-transform: uppercase; margin-bottom: 22px; }
.metric { margin-bottom: 20px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; color: #8892A0; }
.metric-top b { font-family: Consolas, monospace; font-size: 19px; color: #EEF1F5; }
.bar { height: 3px; background: #1E232B; margin-top: 10px; }
.bar i { display: block; height: 3px; background: #3DDC97; box-shadow: 0 0 10px #3DDC9788; }
.bar i[style*="0%"] { background: #2A3038; box-shadow: none; }
.hint { font-family: Consolas, monospace; font-size: 11px; color: #5E6773; margin: 18px 0 22px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 14px; border: 1px solid #2A3038; color: #C7CFD9; }
.btn.primary { background: #3DDC97; color: #06231A; border-color: #3DDC97; font-weight: 700; }
.btn.wide { display: block; margin-bottom: 22px; }
.card { background: #12161B; border: 1px solid #222831; padding: 14px; }
.tag { font-size: 10px; letter-spacing: 3px; color: #3DDC97; }
.card-title { font-size: 17px; line-height: 1.4; margin: 8px 0 4px; }
.card-sub { font-family: Consolas, monospace; font-size: 11px; color: #5E6773; }
.section { display: flex; justify-content: space-between; font-size: 12px; color: #7C8794; margin: 24px 0 8px;
           letter-spacing: 1px; }
.more { color: #3DDC97; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #171C22; }
.word { font-size: 19px; }
.ph { font-family: Consolas, monospace; font-size: 11px; color: #5E6773; }
.cn { font-size: 12px; color: #8892A0; margin-top: 3px; }
.play { color: #3DDC97; }
.nav { height: 52px; display: flex; align-items: center; justify-content: space-around; border-top: 1px solid #171C22;
       font-size: 12px; color: #5E6773; background: #0E1115; }
.nav .on { color: #3DDC97; }
.study-top { display: flex; align-items: center; justify-content: space-between; font-size: 13px; color: #8892A0; }
.study-top span:first-child { font-size: 18px; }
.wordcard { text-align: center; padding: 10px 0; }
.bigword { font-size: 44px; letter-spacing: -1px; }
.bigph { font-family: Consolas, monospace; font-size: 13px; color: #5E6773; margin: 12px 0 30px; }
.pos { font-size: 15px; color: #C7CFD9; margin-bottom: 8px; }
.quote { font-size: 13px; color: #8892A0; margin-top: 26px; }
.quote-cn { font-size: 12px; color: #5E6773; margin-top: 6px; }
.screen.study { display: flex; flex-direction: column; }
.wordcard { margin: auto 0; }
.ratings { display: flex; gap: 8px; }
.rate { flex: 1; text-align: center; padding: 12px 0; font-size: 13px; border: 1px solid #2A3038; color: #C7CFD9; line-height: 1.6; }
.rate b { display: block; font-family: Consolas, monospace; font-size: 10px; font-weight: 400; color: #6B7480; }
.rate.r1 { border-color: #FF6B6B; color: #FF6B6B; }
.rate.r2 { border-color: #E8A33D; color: #E8A33D; }
.rate.r3 { border-color: #3DDC97; color: #3DDC97; }
.rate.r4 { border-color: #4D9BFF; color: #4D9BFF; }
.rate.r3 { background: #3DDC9715; }
.a-title { font-size: 22px; line-height: 1.35; }
.a-sub { font-size: 12px; color: #5E6773; margin: 8px 0 20px; }
.para { font-size: 15px; line-height: 2; color: #C7CFD9; margin-bottom: 18px; }
mark { background: #3DDC9722; color: #3DDC97; padding: 0 2px; }
"""

# ------------------------------------------------------------- 风格 C：薄荷圆润
CSS_C = """
.board { color: #17332B; }
.phone { background: #F3F8F4; border: 1px solid #DCEAE0; box-shadow: 0 12px 30px rgba(31,111,92,.12); }
.status { color: #6E8C81; }
.h-title { font-size: 24px; font-weight: 700; margin-bottom: 16px; color: #10322A; }
.metric { background: #FFFFFF; border-radius: 18px; padding: 14px 16px; margin-bottom: 12px;
          box-shadow: 0 4px 14px rgba(31,111,92,.07); }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; color: #4E7167; }
.metric-top b { font-size: 18px; color: #10322A; }
.bar { height: 8px; border-radius: 99px; background: #E3EFE8; margin-top: 10px; overflow: hidden; }
.bar i { display: block; height: 8px; border-radius: 99px; background: linear-gradient(90deg,#4CC49A,#2F9E7E); }
.bar i[style*="0%"] { background: #E3EFE8; }
.hint { font-size: 12px; color: #6E8C81; margin: 14px 2px 18px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 13px 0; font-size: 14px; border-radius: 99px; background: #FFFFFF;
       color: #2F9E7E; border: 1.5px solid #BFE2D4; font-weight: 600; }
.btn.primary { background: linear-gradient(135deg,#43BB92,#2F9E7E); color: #FFFFFF; border-color: transparent;
               box-shadow: 0 6px 16px rgba(47,158,126,.32); }
.btn.wide { display: block; margin-bottom: 16px; }
.card { background: #FFFFFF; border-radius: 20px; padding: 16px; box-shadow: 0 4px 16px rgba(31,111,92,.08); }
.tag { font-size: 11px; color: #2F9E7E; background: #DFF3EC; display: inline-block; padding: 3px 10px; border-radius: 99px; }
.card-title { font-size: 17px; line-height: 1.4; margin: 10px 0 5px; font-weight: 700; }
.card-sub { font-size: 12px; color: #6E8C81; }
.section { display: flex; justify-content: space-between; align-items: center; font-size: 15px; font-weight: 700;
           margin: 20px 2px 8px; color: #10322A; }
.more { font-size: 12px; color: #2F9E7E; font-weight: 500; }
.list-row { display: flex; justify-content: space-between; align-items: center; background: #FFFFFF; border-radius: 16px;
            padding: 12px 14px; margin-bottom: 8px; box-shadow: 0 3px 10px rgba(31,111,92,.06); }
.word { font-size: 19px; font-weight: 700; color: #10322A; }
.ph { font-size: 12px; color: #6E8C81; }
.cn { font-size: 12px; color: #40685C; margin-top: 3px; }
.play { color: #2F9E7E; background: #DFF3EC; width: 32px; height: 32px; border-radius: 50%;
        display: flex; align-items: center; justify-content: center; font-size: 12px; }
.nav { height: 56px; display: flex; align-items: center; justify-content: space-around; background: #FFFFFF;
       border-top: 1px solid #E3EFE8; font-size: 12px; color: #7C9A90; }
.nav .on { color: #2F9E7E; font-weight: 700; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 15px; color: #4E7167; }
.wordcard { background: #FFFFFF; border-radius: 24px; padding: 26px 20px; margin-top: 14px;
            box-shadow: 0 8px 24px rgba(31,111,92,.1); }
.bigword { font-size: 38px; font-weight: 700; text-align: center; color: #10322A; }
.bigph { text-align: center; color: #6E8C81; margin: 8px 0 22px; }
.pos { font-size: 14.5px; color: #2A4C42; margin-bottom: 6px; }
.quote { font-size: 14px; color: #40685C; margin-top: 20px; background: #F1F9F4; border-radius: 12px; padding: 10px 12px; }
.quote-cn { font-size: 12px; color: #6E8C81; padding: 6px 12px 0; }
.ratings { display: flex; gap: 8px; margin-top: 18px; }
.rate { flex: 1; text-align: center; padding: 12px 0; font-size: 13px; border-radius: 16px; color: #FFFFFF;
        line-height: 1.55; font-weight: 600; }
.rate b { display: block; font-size: 10px; font-weight: 400; opacity: .9; }
.rate.r1 { background: #E4695F; } .rate.r2 { background: #E8A33D; }
.rate.r3 { background: #2F9E7E; box-shadow: 0 6px 14px rgba(47,158,126,.35); } .rate.r4 { background: #4A86C8; }
.a-title { font-size: 21px; font-weight: 700; line-height: 1.35; color: #10322A; }
.a-sub { font-size: 12px; color: #6E8C81; margin: 6px 0 16px; }
.para { font-size: 15px; line-height: 1.95; color: #2A4C42; margin-bottom: 14px; }
mark { background: #CDEEDF; color: #1E6B55; border-radius: 4px; padding: 1px 4px; font-weight: 700; }
"""

# ------------------------------------------------------------- 风格 D：马克笔
CSS_D = """
.board { color: #101010; }
.phone { background: #FFFFFF; border: 2px solid #101010; box-shadow: 6px 6px 0 #101010; border-radius: 4px; }
.status { color: #101010; font-weight: 700; }
.h-title { font-family: "Arial Black","Microsoft YaHei",sans-serif; font-size: 28px; letter-spacing: -1px;
           background: #FFE94A; display: inline-block; padding: 2px 10px; border: 2px solid #101010;
           box-shadow: 3px 3px 0 #101010; margin-bottom: 18px; }
.metric { margin-bottom: 14px; }
.metric-top { display: flex; justify-content: space-between; align-items: baseline; font-size: 14px; font-weight: 700; }
.metric-top b { font-size: 20px; }
.bar { height: 12px; border: 2px solid #101010; margin-top: 8px; background: #FFFFFF; }
.bar i { display: block; height: 100%; background: repeating-linear-gradient(45deg,#FFE94A,#FFE94A 6px,#FFD400 6px,#FFD400 12px); }
.hint { font-size: 12px; font-weight: 700; margin: 16px 0 18px; }
.btn-row { display: flex; gap: 10px; margin-bottom: 10px; }
.btn { flex: 1; text-align: center; padding: 12px 0; font-size: 14px; font-weight: 700; border: 2px solid #101010;
       background: #FFFFFF; box-shadow: 3px 3px 0 #101010; }
.btn.primary { background: #101010; color: #FFFFFF; }
.btn.wide { display: block; margin-bottom: 18px; }
.card { border: 2px solid #101010; box-shadow: 4px 4px 0 #101010; padding: 12px 14px; background: #FFFFFF; }
.tag { font-size: 10px; font-weight: 700; background: #101010; color: #FFE94A; display: inline-block; padding: 2px 8px; }
.card-title { font-size: 17px; font-weight: 700; line-height: 1.35; margin: 8px 0 4px; }
.card-sub { font-size: 12px; font-weight: 700; color: #4A4A4A; }
.section { display: flex; justify-content: space-between; font-size: 16px; font-weight: 700; margin: 20px 0 8px;
           border-bottom: 3px solid #101010; padding-bottom: 5px; }
.more { font-size: 12px; background: #FFE94A; border: 2px solid #101010; padding: 1px 7px; }
.list-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #D5D5D5; }
.word { font-size: 20px; font-weight: 700; }
.ph { font-size: 12px; color: #5A5A5A; }
.cn { font-size: 12px; font-weight: 700; margin-top: 3px; }
.play { font-size: 12px; border: 2px solid #101010; width: 28px; height: 28px; display: flex; align-items: center;
        justify-content: center; box-shadow: 2px 2px 0 #101010; }
.nav { height: 54px; display: flex; align-items: center; justify-content: space-around; font-size: 12px;
       font-weight: 700; border-top: 2px solid #101010; background: #FFFFFF; }
.nav .on { background: #FFE94A; padding: 4px 10px; border: 2px solid #101010; box-shadow: 2px 2px 0 #101010; }
.study-top { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 700; }
.wordcard { border: 2px solid #101010; box-shadow: 5px 5px 0 #101010; padding: 22px 18px; margin-top: 14px; }
.bigword { font-family: "Arial Black","Microsoft YaHei",sans-serif; font-size: 40px; }
.bigph { font-size: 13px; color: #5A5A5A; margin: 6px 0 18px; }
.pos { font-size: 14px; font-weight: 700; margin-bottom: 6px; }
.quote { font-size: 13.5px; margin-top: 18px; background: #FFE94A; padding: 4px 6px; font-weight: 700; }
.quote-cn { font-size: 12px; color: #5A5A5A; margin-top: 6px; }
.ratings { display: flex; gap: 8px; margin-top: 18px; }
.rate { flex: 1; text-align: center; padding: 10px 0; font-size: 13px; font-weight: 700; border: 2px solid #101010;
        line-height: 1.5; box-shadow: 3px 3px 0 #101010; }
.rate b { display: block; font-size: 10px; font-weight: 700; }
.rate.r1 { background: #FF8A80; } .rate.r2 { background: #FFC46B; }
.rate.r3 { background: #9BE8B5; } .rate.r4 { background: #9CC7FF; }
.a-title { font-family: "Arial Black","Microsoft YaHei",sans-serif; font-size: 21px; line-height: 1.3; }
.a-sub { font-size: 12px; font-weight: 700; color: #5A5A5A; margin: 8px 0 16px; }
.para { font-size: 15px; line-height: 1.9; margin-bottom: 14px; }
mark { background: #FFE94A; font-weight: 700; padding: 1px 3px; box-shadow: 2px 2px 0 #10101080; }
"""

STYLES = [
    ("style-a", "风格 A · 墨纸 Editorial Ink", "米白纸感 + 宋体标题 + 朱红点睛，像一本精装词书；适合长时间阅读", CSS_A, "#EFEAE0"),
    ("style-b", "风格 B · 暗夜专注 Midnight Focus", "近黑底 + 薄荷荧光 + 巨型单词，界面全部退让；适合晚上刷词", CSS_B, "#1A1D21"),
    ("style-c", "风格 C · 薄荷圆润 Soft Mint", "奶油白 + 大圆角卡片 + 柔和阴影，轻松友好；适合日常使用", CSS_C, "#E6F1EA"),
    ("style-d", "风格 D · 马克笔 Marker", "粗黑描边 + 荧光黄 + 硬阴影，像学生笔记；辨识度最高", CSS_D, "#FFFDF2"),
]

for slug, name, desc, css, bg in STYLES:
    path = os.path.join(OUT, slug + ".html")
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(doc(name, desc, css, bg))
    print("wrote", path)
