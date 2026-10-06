#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tools/verify/check_assets.py —— 校验生成的 words.db / dict.db

检查内容：
  1) words.db 表结构与行数、三本词书的词数、抽样单词是否带音标/释义/例句
  2) dict.db 精确查询、exchange 词形还原（running→run、went→go、better→good…）、前缀模糊查询
  3) 列名与 Kotlin 端 WordEntity / DictRepository 的读取顺序一致

用法：python tools/verify/check_assets.py
"""
import os
import sqlite3
import sys

# Windows 控制台默认 GBK，强制 UTF-8 输出，避免中文与音标乱码
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORDS = os.path.join(ROOT, "app", "src", "main", "assets", "words.db")
DICT = os.path.join(ROOT, "app", "src", "main", "assets", "dict.db")

WORD_COLUMNS = ["id", "headword", "phoneticUs", "phoneticUk", "transCn", "transEn",
                "examplesJson", "phrasesJson", "rank", "deck", "deckPriority", "shuffleKey"]

failures = []


def check(condition, message):
    print(("  [OK]   " if condition else "  [FAIL] ") + message)
    if not condition:
        failures.append(message)


def check_words():
    print("words.db")
    if not os.path.exists(WORDS):
        check(False, "words.db 不存在，请先运行 python tools/build_assets.py")
        return
    con = sqlite3.connect(WORDS)
    cols = [row[1] for row in con.execute("PRAGMA table_info(words)")]
    check(cols == WORD_COLUMNS, "表结构与 Kotlin WordEntity 一致：" + ", ".join(cols))

    total = con.execute("SELECT COUNT(*) FROM words").fetchone()[0]
    check(total > 4000, "词条总数 %d（考研词书去重后应大于 4000）" % total)

    for deck, count in con.execute("SELECT deck, COUNT(*) FROM words GROUP BY deck ORDER BY deckPriority"):
        print("         %-16s %5d 词" % (deck, count))

    max_id = con.execute("SELECT MAX(id) FROM words").fetchone()[0]
    check(max_id == total, "id 连续且从 1 开始（max=%s, total=%s）" % (max_id, total))

    row = con.execute("SELECT headword, phoneticUs, transCn, examplesJson FROM words WHERE headword='paragraph'").fetchone()
    check(row is not None, "能查到 paragraph")
    if row:
        check(bool(row[1]), "paragraph 有美式音标：" + str(row[1]))
        check(bool(row[2]), "paragraph 有中文释义")
        check(bool(row[3]), "paragraph 有例句 JSON")

    empty = con.execute("SELECT COUNT(*) FROM words WHERE headword IS NULL OR TRIM(headword)=''").fetchone()[0]
    check(empty == 0, "没有空单词")
    dup = con.execute("SELECT COUNT(*) FROM (SELECT LOWER(headword) h FROM words GROUP BY h HAVING COUNT(*)>1)").fetchone()[0]
    check(dup == 0, "没有重复单词")
    con.close()


def lookup(con, word):
    return con.execute(
        "SELECT word, translation FROM dict WHERE word = ? COLLATE NOCASE LIMIT 1", (word,)
    ).fetchone()


def lemma(con, surface):
    row = con.execute(
        "SELECT lemma FROM lemma WHERE surface = ? COLLATE NOCASE LIMIT 1", (surface,)
    ).fetchone()
    return row[0] if row else None


def check_dict():
    print("dict.db")
    if not os.path.exists(DICT):
        check(False, "dict.db 不存在，请先运行 python tools/build_assets.py")
        return
    con = sqlite3.connect(DICT)
    cols = [row[1] for row in con.execute("PRAGMA table_info(dict)")]
    expected = ["word", "phonetic", "definition", "translation", "pos", "collins", "oxford",
                "tag", "bnc", "frq", "exchange"]
    check(cols == expected, "dict 表字段与 DictRepository 的读取顺序一致")

    total = con.execute("SELECT COUNT(*) FROM dict").fetchone()[0]
    check(total > 10000, "收录词条 %d 条" % total)

    for word in ["run", "abandon", "significant", "apple"]:
        row = lookup(con, word)
        check(row is not None, "精确命中 " + word + ("（" + row[1][:20].replace("\n", " ") + "…）" if row else ""))

    # 词形还原：变形词能查到原形
    cases = [("running", "run"), ("went", "go"), ("decided", "decide"), ("better", "good"),
             ("studies", "study"), ("improved", "improve"), ("thinking", "think"), ("children", "child")]
    for surface, expected_lemma in cases:
        direct = lookup(con, surface)
        resolved = direct[0].lower() if direct else lemma(con, surface)
        if resolved is None:
            mapped = lemma(con, surface)
            check(False, "%s 无法还原（期望 %s）" % (surface, expected_lemma))
            continue
        if direct:
            print("         %-12s → 词典里直接收录为 %s" % (surface, resolved))
        else:
            check(resolved.lower() == expected_lemma,
                  "%s → %s（期望 %s）" % (surface, resolved, expected_lemma))

    # 前缀模糊
    prefix = con.execute(
        "SELECT word FROM dict WHERE word LIKE ? ESCAPE '\\' ORDER BY frq DESC, word ASC LIMIT 5",
        ("run%",),
    ).fetchall()
    check(len(prefix) > 0, "前缀模糊查询 run% → " + ", ".join(p[0] for p in prefix))

    not_covered = con.execute("SELECT COUNT(*) FROM dict WHERE tag LIKE '%ky%'").fetchone()[0]
    check(not_covered > 3000, "带考研标签的词 %d 个" % not_covered)
    con.close()


def main():
    print("=" * 66)
    print("assets 校验")
    print("=" * 66)
    check_words()
    print()
    check_dict()
    print()
    if failures:
        print("有 %d 项检查未通过：" % len(failures))
        for item in failures:
            print("  - " + item)
        return 1
    print("全部检查通过。")
    return 0


if __name__ == "__main__":
    sys.exit(main())