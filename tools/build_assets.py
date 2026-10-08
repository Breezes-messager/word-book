#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tools/build_assets.py —— 词库构建脚本（可重复运行，assets 里的文件全部由它生成）

产物：
    app/src/main/assets/words.db   考研词书（KaoYan_2 顺序版 + KaoYan_3 顺序版 + KaoYanluan_1 乱序版）
    app/src/main/assets/dict.db    离线查词词典（ECDICT 裁剪版，含词形还原表 lemma）

数据来源（公开仓库，脚本自动下载到 tools/.cache/ 后复用）：
    学习词库  https://github.com/KyleBing/dict   book-json-full-formatted/
    查词词典  https://github.com/skywind3000/ECDICT
              优先用 HuggingFace 上的 ECDICT SQLite 打包（快），失败再退回官方 ecdict.csv

版权提醒：KyleBing/dict 是 kajweb/dict 的 fork，词书内容源自商业词典。
本项目仅供个人学习自用：不得上架商店、不得公开分发带词库的 APK、不得商用。

用法：
    python tools/build_assets.py                 # 缺数据时自动下载，然后构建
    python tools/build_assets.py --no-download   # 只用 tools/.cache 里已有的数据
    python tools/build_assets.py --limit 500     # 每本词书只取前 N 条（调试用）
"""

from __future__ import annotations

import argparse
import concurrent.futures
import csv
import hashlib
import json
import os
import re
import sqlite3
import sys
import time
import unicodedata
import urllib.request
import zipfile

# Windows 控制台默认 GBK，这里强制 UTF-8 输出，避免中文与音标乱码
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", ".cache")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")

# ---------------------------------------------------------------- 数据源定义

WORD_BOOKS = {
    "KaoYan_2.json": ([
        "https://cdn.jsdelivr.net/gh/KyleBing/dict@master/book-json-full-formatted/KaoYan_2.json",
        "https://raw.githubusercontent.com/KyleBing/dict/master/book-json-full-formatted/KaoYan_2.json",
    ], 8658569),
    "KaoYan_3.json": ([
        "https://cdn.jsdelivr.net/gh/KyleBing/dict@master/book-json-full-formatted/KaoYan_3.json",
        "https://raw.githubusercontent.com/KyleBing/dict/master/book-json-full-formatted/KaoYan_3.json",
    ], 6465375),
    "KaoYanluan_1.json": ([
        "https://cdn.jsdelivr.net/gh/KyleBing/dict@master/book-json-full-formatted/KaoYanluan_1.json",
        "https://raw.githubusercontent.com/KyleBing/dict/master/book-json-full-formatted/KaoYanluan_1.json",
    ], 2687240),
}

# ECDICT：首选 HuggingFace 上的 SQLite 打包（54MB zip → 131MB sqlite），备选官方 CSV
ECDICT_ZIP = {
    "file": "ecdict-hf.zip",
    "size": 54594964,
    "urls": [
        "https://hf-mirror.com/datasets/iamzhangship/fluency-ecdict-offline/resolve/main/ecdict-ecdict-bc015ed2-focus13-v2.zip",
        "https://huggingface.co/datasets/iamzhangship/fluency-ecdict-offline/resolve/main/ecdict-ecdict-bc015ed2-focus13-v2.zip",
    ],
}
ECDICT_SQLITE_SIZE = 131756032
ECDICT_CSV = {
    "file": "ecdict.csv",
    "size": 65933428,
    "urls": [
        "https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv",
        "https://ghproxy.net/https://raw.githubusercontent.com/skywind3000/ECDICT/master/ecdict.csv",
    ],
}

# 备用轻量词书（首选源全部失败时使用）
FALLBACK_WORDS = {
    "5-考研-顺序.json": ([
        "https://cdn.jsdelivr.net/gh/KyleBing/english-vocabulary@master/json/5-%E8%80%83%E7%A0%94-%E9%A1%BA%E5%BA%8F.json",
    ], 0),
}

# 词书顺序：KaoYan_2 是主词书，KaoYan_3 补充，KaoYanluan_1 是乱序版
DECKS = [
    ("KaoYan_2", 0),
    ("KaoYan_3", 1),
    ("KaoYanluan_1", 2),
]

MAX_SENTENCES = 5
MAX_PHRASES = 10
# 记忆法 / 同近义 / 同根词的截断上限（避免单条过大）
MAX_SYNO_GROUPS = 6
MAX_SYNO_WORDS = 8
MAX_REL_GROUPS = 6
MAX_REL_WORDS = 8
WORD_RE = re.compile(r"^[A-Za-z][A-Za-z' .\-]{0,29}$")


# ---------------------------------------------------------------- 下载

def head_size(url: str) -> int:
    req = urllib.request.Request(url, method="HEAD")
    with urllib.request.urlopen(req, timeout=30) as resp:
        return int(resp.headers.get("Content-Length", 0) or 0)


def _grab(url: str, start: int, end: int, retries: int = 4) -> bytes:
    last = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"Range": "bytes=%d-%d" % (start, end)})
            with urllib.request.urlopen(req, timeout=60) as resp:
                return resp.read()
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError("分片 %d-%d 下载失败: %s" % (start, end, last))


def download(urls, dest: str, expected: int = 0, workers: int = 16) -> str:
    """多连接分片下载；GitHub raw 单连接会被限速，分片能快 10 倍以上。"""
    if os.path.exists(dest) and expected and abs(os.path.getsize(dest) - expected) < 4096:
        return dest
    last_error = None
    for url in urls:
        try:
            size = expected or head_size(url)
            if size <= 0:
                raise RuntimeError("无法获取文件大小")
            chunk = max(1, size // workers)
            jobs = []
            for i in range(workers):
                start = i * chunk
                end = size - 1 if i == workers - 1 else start + chunk - 1
                if start < size:
                    jobs.append((start, end))
            with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
                futures = [(s, pool.submit(_grab, url, s, e)) for s, e in jobs]
                parts = [(s, f.result()) for s, f in futures]
            with open(dest, "wb") as fh:
                for _, data in sorted(parts):
                    fh.write(data)
            print("    已下载 %s：%.1f MB" % (os.path.basename(dest), os.path.getsize(dest) / 1048576))
            return dest
        except Exception as exc:  # noqa: BLE001
            last_error = exc
            print("    源失败（%s）：%s" % (url[:70], exc))
    raise RuntimeError("全部下载源失败：%s" % last_error)


def ensure_sources(no_download: bool) -> dict:
    os.makedirs(CACHE, exist_ok=True)
    paths = {}
    for name, (urls, expected) in WORD_BOOKS.items():
        dest = os.path.join(CACHE, name)
        if os.path.exists(dest) and abs(os.path.getsize(dest) - expected) < 4096:
            paths[name] = dest
            continue
        if no_download:
            if os.path.exists(dest):
                paths[name] = dest
            continue
        print("  下载 %s ..." % name)
        try:
            paths[name] = download(urls, dest, expected)
        except Exception as exc:  # noqa: BLE001
            print("  !! %s 下载失败：%s" % (name, exc))
    return paths


def ensure_ecdict(no_download: bool):
    """返回 (kind, path)，kind 为 'sqlite' 或 'csv'；都拿不到时返回 (None, None)。"""
    # 1) 已经解压好的 sqlite
    sqlite_path = os.path.join(CACHE, "ecdict.sqlite")
    if os.path.exists(sqlite_path) and abs(os.path.getsize(sqlite_path) - ECDICT_SQLITE_SIZE) < 1048576:
        return "sqlite", sqlite_path

    # 2) 从 zip 解压
    zip_path = os.path.join(CACHE, ECDICT_ZIP["file"])
    if not os.path.exists(zip_path) and not no_download:
        print("  下载 ECDICT 离线包（54 MB）...")
        try:
            download(ECDICT_ZIP["urls"], zip_path, ECDICT_ZIP["size"], workers=8)
        except Exception as exc:  # noqa: BLE001
            print("  !! ECDICT 离线包下载失败：%s" % exc)
    if os.path.exists(zip_path):
        try:
            with zipfile.ZipFile(zip_path) as zf:
                for member in zf.namelist():
                    if member.endswith(".sqlite"):
                        print("  解压 %s ..." % member)
                        with zf.open(member) as src, open(sqlite_path, "wb") as dst:
                            dst.write(src.read())
                        return "sqlite", sqlite_path
        except Exception as exc:  # noqa: BLE001
            print("  !! 解压 ECDICT 失败：%s" % exc)

    # 3) 退回官方 CSV
    csv_path = os.path.join(CACHE, ECDICT_CSV["file"])
    if os.path.exists(csv_path) and abs(os.path.getsize(csv_path) - ECDICT_CSV["size"]) < 4096:
        return "csv", csv_path
    if not no_download:
        print("  下载 ecdict.csv（63 MB，多连接）...")
        try:
            download(ECDICT_CSV["urls"], csv_path, ECDICT_CSV["size"])
            return "csv", csv_path
        except Exception as exc:  # noqa: BLE001
            print("  !! ecdict.csv 下载失败：%s" % exc)
    return None, None


# ---------------------------------------------------------------- 文本清洗

def clean(value) -> str:
    if value is None:
        return ""
    text = unicodedata.normalize("NFKC", str(value))
    text = text.replace("\u0000", "")
    text = "".join(ch for ch in text if ch in "\n\t" or ord(ch) >= 32)
    return text.strip()


def clean_word(value) -> str:
    return re.sub(r"\s+", " ", clean(value))


def normalize_phonetic(value) -> str:
    """
    把音标里的非标准写法换成规范 IPA 符号。

    词书（KyleBing/dict）和 ECDICT 都用 ASCII 字符凑音标，直接显示会显得很业余：
      '  →  ˈ   主重音（3479/5007 条用撇号）
      ,  →  ˌ   次重音（只在开头 / 空格后，避免误伤 "ˈnɑmɪk, ˌɛkə" 里的分隔逗号）
      .  →  ˌ   ECDICT 用点当次重音
      ә  →  ə   西里尔字母 schwa（U+04D9）长得像但不是拉丁 ə（U+0259），字体渲染不一致
      :  →  ː   长音符
    """
    text = clean(value)
    if not text:
        return ""

    text = text.replace("\u04d9", "\u0259").replace(":", "\u02d0")
    text = text.replace("'", "\u02c8")

    out = []
    for index, ch in enumerate(text):
        prev = text[index - 1] if index else " "
        nxt = text[index + 1] if index + 1 < len(text) else ""
        if ch in ",." and (index == 0 or prev == " ") and nxt != " ":
            out.append("\u02cc")
        else:
            out.append(ch)
    return "".join(out)


def clean_multiline(value) -> str:
    """
    ECDICT 的 translation / definition 字段里，换行是**字面量 \\n（反斜杠 + n 两个字符）**，
    不是真正的换行符。不清洗的话客户端会把 "\\n" 直接显示出来。
    这里统一还原成真正的换行。
    """
    text = clean(value)
    text = text.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n")
    return text


# ---------------------------------------------------------------- words.db

def parse_book(path: str, deck: str, limit: int):
    """解析一本词书，返回 [(headword, rank, data), ...]"""
    with open(path, "r", encoding="utf-8") as fh:
        raw = json.load(fh)
    out = []
    for entry in raw:
        if limit and len(out) >= limit:
            break
        headword = clean_word(entry.get("headWord"))
        if not headword or not WORD_RE.match(headword):
            continue
        content = (entry.get("content") or {}).get("word", {}).get("content", {}) or {}
        rank = entry.get("wordRank") or (len(out) + 1)

        cn_lines, en_lines = [], []
        for t in content.get("trans") or []:
            pos = clean(t.get("pos"))
            cn = clean(t.get("tranCn"))
            en = clean(t.get("tranOther"))
            if cn:
                cn_lines.append((pos + ". " + cn) if pos else cn)
            if en:
                en_lines.append((pos + ". " + en) if pos else en)

        examples = []
        for s in ((content.get("sentence") or {}).get("sentences") or [])[:MAX_SENTENCES]:
            en = clean(s.get("sContent"))
            cn = clean(s.get("sCn"))
            if en:
                examples.append({"en": en, "cn": cn})

        phrases = []
        for p in ((content.get("phrase") or {}).get("phrases") or [])[:MAX_PHRASES]:
            en = clean(p.get("pContent"))
            cn = clean(p.get("pCn"))
            if en:
                phrases.append({"en": en, "cn": cn})

        # 记忆法（词根词缀拆解），例如 para(在旁边)+graph(写)→写在文字旁边→段
        rem_method = clean((content.get("remMethod") or {}).get("val"))

        # 同近义词：按词性分组，每组给出若干同义词
        syno = []
        for group in ((content.get("syno") or {}).get("synos") or [])[:MAX_SYNO_GROUPS]:
            words = [
                clean(w.get("w"))
                for w in (group.get("hwds") or [])[:MAX_SYNO_WORDS]
                if clean(w.get("w"))
            ]
            if words:
                syno.append({
                    "pos": clean(group.get("pos")),
                    "tran": clean(group.get("tran")),
                    "words": words,
                })

        # 同根词：按词性分组，给出同根词 + 中文
        rel_word = []
        for group in ((content.get("relWord") or {}).get("rels") or [])[:MAX_REL_GROUPS]:
            words = []
            for w in (group.get("words") or [])[:MAX_REL_WORDS]:
                hwd = clean(w.get("hwd"))
                if hwd:
                    words.append({"hwd": hwd, "tran": clean(w.get("tran"))})
            if words:
                rel_word.append({"pos": clean(group.get("pos")), "words": words})

        out.append((headword, rank, {
            "phoneticUs": normalize_phonetic(content.get("usphone")),
            "phoneticUk": normalize_phonetic(content.get("ukphone")),
            "transCn": "\n".join(cn_lines),
            "transEn": "\n".join(en_lines),
            "examplesJson": json.dumps(examples, ensure_ascii=False) if examples else "",
            "phrasesJson": json.dumps(phrases, ensure_ascii=False) if phrases else "",
            "remMethod": rem_method,
            "synoJson": json.dumps(syno, ensure_ascii=False) if syno else "",
            "relWordJson": json.dumps(rel_word, ensure_ascii=False) if rel_word else "",
        }))
    return out


def parse_fallback_book(path: str, limit: int):
    """备用词书结构：[{word, translations:[{tranCn}], phrases:[...]}]"""
    with open(path, "r", encoding="utf-8") as fh:
        raw = json.load(fh)
    out = []
    for i, entry in enumerate(raw):
        if limit and len(out) >= limit:
            break
        headword = clean_word(entry.get("word"))
        if not headword or not WORD_RE.match(headword):
            continue
        cn_lines, en_lines = [], []
        for t in entry.get("translations") or []:
            cn = clean(t.get("tranCn") or t.get("translation"))
            pos = clean(t.get("pos"))
            if cn:
                cn_lines.append((pos + ". " + cn) if pos else cn)
            en = clean(t.get("tranOther") or t.get("definition"))
            if en:
                en_lines.append(en)
        examples = []
        for s in (entry.get("sentences") or [])[:MAX_SENTENCES]:
            en = clean(s.get("sContent") or s.get("sentence"))
            if en:
                examples.append({"en": en, "cn": clean(s.get("sCn") or s.get("translation"))})
        phrases = []
        for p in (entry.get("phrases") or [])[:MAX_PHRASES]:
            en = clean(p.get("pContent") or p.get("phrase"))
            if en:
                phrases.append({"en": en, "cn": clean(p.get("pCn") or p.get("translation"))})
        out.append((headword, i + 1, {
            "phoneticUs": normalize_phonetic(entry.get("usphone") or entry.get("phonetic")),
            "phoneticUk": normalize_phonetic(entry.get("ukphone")),
            "transCn": "\n".join(cn_lines),
            "transEn": "\n".join(en_lines),
            "examplesJson": json.dumps(examples, ensure_ascii=False) if examples else "",
            "phrasesJson": json.dumps(phrases, ensure_ascii=False) if phrases else "",
            "remMethod": "",
            "synoJson": "",
            "relWordJson": "",
        }))
    return out


def shuffle_key(word: str) -> int:
    """稳定的乱序键：同一个词每次构建都落在同一个位置"""
    digest = hashlib.md5(word.lower().encode("utf-8")).hexdigest()
    return int(digest[:8], 16) % 2000000000


WORDS_SCHEMA = """
DROP TABLE IF EXISTS words;
CREATE TABLE words (
    id           INTEGER PRIMARY KEY,
    headword     TEXT    NOT NULL,
    phoneticUs   TEXT,
    phoneticUk   TEXT,
    transCn      TEXT,
    transEn      TEXT,
    examplesJson TEXT,
    phrasesJson  TEXT,
    rank         INTEGER NOT NULL,
    deck         TEXT    NOT NULL,
    deckPriority INTEGER NOT NULL,
    shuffleKey   INTEGER NOT NULL,
    remMethod    TEXT,
    synoJson     TEXT,
    relWordJson  TEXT
);
CREATE UNIQUE INDEX idx_words_headword ON words(headword);
CREATE INDEX idx_words_order ON words(deckPriority, rank);
CREATE INDEX idx_words_shuffle ON words(shuffleKey);
DROP TABLE IF EXISTS decks;
CREATE TABLE decks (
    name         TEXT PRIMARY KEY,
    label        TEXT NOT NULL,
    deckPriority INTEGER NOT NULL,
    size         INTEGER NOT NULL
);
"""


def build_words_db(books, out_path: str):
    """books: [(deck, priority, [(headword, rank, data)])]"""
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    if os.path.exists(out_path):
        os.remove(out_path)
    conn = sqlite3.connect(out_path)
    conn.executescript(WORDS_SCHEMA)

    seen = set()
    stats = []
    for deck, priority, entries in books:
        kept = 0
        dup = 0
        for headword, rank, data in entries:
            key = headword.lower()
            if key in seen:
                dup += 1
                continue
            seen.add(key)
            kept += 1
            conn.execute(
                "INSERT INTO words (headword, phoneticUs, phoneticUk, transCn, transEn,"
                " examplesJson, phrasesJson, rank, deck, deckPriority, shuffleKey,"
                " remMethod, synoJson, relWordJson)"
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                (headword, data["phoneticUs"], data["phoneticUk"], data["transCn"], data["transEn"],
                 data["examplesJson"], data["phrasesJson"], rank, deck, priority, shuffle_key(headword),
                 data.get("remMethod", ""), data.get("synoJson", ""), data.get("relWordJson", "")),
            )
        conn.execute("INSERT INTO decks VALUES (?,?,?,?)", (deck, deck, priority, kept))
        stats.append((deck, len(entries), kept, dup))

    # 按 (deckPriority, rank) 重新分配稳定 id
    rows = conn.execute("SELECT rowid FROM words ORDER BY deckPriority, rank, rowid").fetchall()
    for new_id, (rowid,) in enumerate(rows, start=1):
        conn.execute("UPDATE words SET id = ? WHERE rowid = ?", (new_id, rowid))
    conn.commit()
    conn.execute("VACUUM")
    conn.commit()
    conn.close()
    return stats


# ---------------------------------------------------------------- dict.db

DICT_SCHEMA = """
DROP TABLE IF EXISTS dict;
CREATE TABLE dict (
    word        TEXT PRIMARY KEY COLLATE NOCASE,
    phonetic    TEXT,
    definition  TEXT,
    translation TEXT,
    pos         TEXT,
    collins     INTEGER,
    oxford      INTEGER,
    tag         TEXT,
    bnc         INTEGER,
    frq         INTEGER,
    exchange    TEXT
) WITHOUT ROWID;
DROP TABLE IF EXISTS lemma;
CREATE TABLE lemma (
    surface TEXT PRIMARY KEY COLLATE NOCASE,
    lemma   TEXT NOT NULL COLLATE NOCASE
) WITHOUT ROWID;
CREATE INDEX idx_dict_frq ON dict(frq);
"""

KEEP_TAGS = ("ky", "cet4", "cet6", "gk")
EXCHANGE_KEYS = ("p", "d", "i", "3", "r", "t", "s")

DICT_COLUMNS = "word, phonetic, definition, translation, pos, collins, oxford, tag, bnc, frq, exchange"


def parse_exchange(exchange: str):
    """ECDICT exchange：0:原形/p:过去式/d:过去分词/i:现在分词/3:三单/r:比较级/t:最高级/s:复数"""
    result = {}
    if not exchange:
        return result
    for part in exchange.split("/"):
        if ":" not in part:
            continue
        key, value = part.split(":", 1)
        key = key.strip()
        values = [v.strip().lower() for v in value.replace(",", " ").split() if v.strip()]
        if values:
            result.setdefault(key, []).extend(values)
    return result


def iter_ecdict_rows(kind: str, path: str):
    """统一产出 ECDICT 词条字典，屏蔽 SQLite / CSV 两种来源"""
    if kind == "sqlite":
        conn = sqlite3.connect("file:" + path.replace("\\", "/") + "?mode=ro", uri=True)
        conn.text_factory = str
        cursor = conn.execute("SELECT " + DICT_COLUMNS + " FROM entries")
        names = [d[0] for d in cursor.description]
        for row in cursor:
            yield dict(zip(names, row))
        conn.close()
    else:
        with open(path, "r", encoding="utf-8", newline="") as fh:
            for row in csv.DictReader(fh):
                yield row


def build_dict_db(kind: str, path: str, out_path: str):
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    if os.path.exists(out_path):
        os.remove(out_path)

    total_rows = 0
    tagged_words = set()
    lemma_map = {}
    variant_words = set()

    # 第一遍：找出带考试标签的词，并展开它们的词形变化
    for row in iter_ecdict_rows(kind, path):
        total_rows += 1
        word = clean(row.get("word")).lower()
        if not word:
            continue
        tag = clean(row.get("tag")).lower()
        if not any(t in tag.split() for t in KEEP_TAGS):
            continue
        tagged_words.add(word)
        for key, values in parse_exchange(clean(row.get("exchange"))).items():
            if key == "0":
                for v in values:
                    if v and v != word:
                        lemma_map[word] = v
                        lemma_map.setdefault(v, v)
                continue
            if key in EXCHANGE_KEYS:
                for v in values:
                    if v and v != word:
                        lemma_map.setdefault(v, word)
                        variant_words.add(v)

    keep = tagged_words | variant_words

    # 第二遍：只把需要的词条写进 SQLite
    written = 0
    conn = sqlite3.connect(out_path)
    conn.executescript(DICT_SCHEMA)
    batch = []
    for row in iter_ecdict_rows(kind, path):
        word = clean(row.get("word")).lower()
        if word not in keep:
            continue
        batch.append((
            word,
            normalize_phonetic(row.get("phonetic")),
            clean_multiline(row.get("definition")),
            clean_multiline(row.get("translation")),
            clean(row.get("pos")),
            int(clean(row.get("collins")) or 0),
            int(clean(row.get("oxford")) or 0),
            clean(row.get("tag")),
            int(clean(row.get("bnc")) or 0),
            int(clean(row.get("frq")) or 0),
            clean(row.get("exchange")),
        ))
        if len(batch) >= 5000:
            conn.executemany("INSERT OR IGNORE INTO dict VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
            written += len(batch)
            batch = []
    if batch:
        conn.executemany("INSERT OR IGNORE INTO dict VALUES (?,?,?,?,?,?,?,?,?,?,?)", batch)
        written += len(batch)

    lemma_rows = [(surface, lemma) for surface, lemma in lemma_map.items() if surface and lemma]
    conn.executemany("INSERT OR IGNORE INTO lemma VALUES (?,?)", lemma_rows)
    conn.commit()
    conn.execute("VACUUM")
    conn.commit()
    conn.close()
    return {
        "source_rows": total_rows,
        "tagged": len(tagged_words),
        "variants": len(variant_words),
        "dict_rows": written,
        "lemma_rows": len(lemma_rows),
    }


# ---------------------------------------------------------------- 主流程

def human(size: int) -> str:
    if size >= 1048576:
        return "%.2f MB" % (size / 1048576)
    if size >= 1024:
        return "%.1f KB" % (size / 1024)
    return "%d B" % size


def main() -> int:
    parser = argparse.ArgumentParser(description="生成 app/src/main/assets 下的词库文件")
    parser.add_argument("--no-download", action="store_true", help="只用 tools/.cache 里已下载的数据")
    parser.add_argument("--limit", type=int, default=0, help="每本词书只取前 N 条（调试）")
    args = parser.parse_args()

    print("=" * 70)
    print("词库构建 tools/build_assets.py")
    print("=" * 70)

    print("\n[1/3] 准备数据文件")
    books_paths = ensure_sources(args.no_download)
    ecdict_kind, ecdict_path = ensure_ecdict(args.no_download)
    if ecdict_kind:
        print("  ECDICT 来源：%s（%s）" % (ecdict_kind, ecdict_path))
    else:
        print("  !! 没有可用的 ECDICT 数据，dict.db 将跳过")

    print("\n[2/3] 生成 words.db")
    books = []
    for deck, priority in DECKS:
        path = books_paths.get(deck + ".json")
        if not path or not os.path.exists(path):
            print("  跳过 %s（数据缺失）" % deck)
            continue
        entries = parse_book(path, deck, args.limit)
        print("  %-16s 解析 %5d 条" % (deck, len(entries)))
        books.append((deck, priority, entries))

    if not books and not args.no_download:
        for name, (urls, _expected) in FALLBACK_WORDS.items():
            dest = os.path.join(CACHE, name)
            if not os.path.exists(dest):
                print("  首选词书缺失，尝试备用词书 %s" % name)
                try:
                    download(urls, dest, 0)
                except Exception as exc:  # noqa: BLE001
                    print("  !! 备用词书也失败：%s" % exc)
                    continue
            entries = parse_fallback_book(dest, args.limit)
            print("  %-16s 解析 %5d 条（备用源）" % (name, len(entries)))
            books.append((name.replace(".json", ""), 0, entries))

    if not books:
        print("!! 没有可用的词书数据，构建中止")
        return 1

    words_path = os.path.join(ASSETS, "words.db")
    stats = build_words_db(books, words_path)

    print("\n[3/3] 生成 dict.db")
    dict_path = os.path.join(ASSETS, "dict.db")
    dict_stats = None
    if ecdict_kind and ecdict_path:
        dict_stats = build_dict_db(ecdict_kind, ecdict_path, dict_path)

    print("\n" + "=" * 70)
    print("构建结果")
    print("=" * 70)
    print("%-16s %10s %10s %10s" % ("词书", "解析", "去重后", "重复丢弃"))
    for deck, parsed, kept, dup in stats:
        print("%-16s %10d %10d %10d" % (deck, parsed, kept, dup))
    print("%-16s %10s %10d" % ("合计", "", sum(s[2] for s in stats)))

    if os.path.exists(words_path):
        conn = sqlite3.connect(words_path)
        for deck, count in conn.execute("SELECT deck, COUNT(*) FROM words GROUP BY deck ORDER BY deckPriority"):
            print("  words.db  %-16s %6d 词" % (deck, count))
        print("  words.db  总计 %d 词" % conn.execute("SELECT COUNT(*) FROM words").fetchone()[0])
        conn.close()

    if dict_stats and os.path.exists(dict_path):
        print("  dict.db   来源 %d 条 → 考研/四六级/高考词 %d 个 + 词形变体 %d 个 → 收录 %d 条，词形还原表 %d 条"
              % (dict_stats["source_rows"], dict_stats["tagged"], dict_stats["variants"],
                 dict_stats["dict_rows"], dict_stats["lemma_rows"]))

    print("\n产物体积：")
    for path in (words_path, dict_path):
        if os.path.exists(path):
            rel = os.path.relpath(path, ROOT).replace("\\", "/")
            print("  %-46s %10s" % (rel, human(os.path.getsize(path))))

    print("\nassets 目录：")
    if os.path.isdir(ASSETS):
        for name in sorted(os.listdir(ASSETS)):
            full = os.path.join(ASSETS, name)
            if os.path.isfile(full):
                print("  %-24s %10s" % (name, human(os.path.getsize(full))))
    print("\n完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())