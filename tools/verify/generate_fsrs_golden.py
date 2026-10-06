#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
tools/verify/generate_fsrs_golden.py

用 FSRS 官方 Python 实现（open-spaced-repetition/py-fsrs）生成排程黄金值，
供 app/src/test/.../FsrsGoldenTest.kt 逐值比对，确保 Kotlin 移植版与官方算法完全一致。

前置：tools/.cache/refs/py-fsrs-main（由本脚本自动下载）
输出：app/src/test/resources/fsrs_golden.json

用法：python tools/verify/generate_fsrs_golden.py
"""
import json
import os
import sys
import urllib.request
import zipfile
import io
from datetime import datetime, timedelta, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
REFS = os.path.join(ROOT, "tools", ".cache", "refs")
PYFSRS_DIR = os.path.join(REFS, "py-fsrs-main")
OUT = os.path.join(ROOT, "app", "src", "test", "resources", "fsrs_golden.json")

PYFSRS_URL = "https://codeload.github.com/open-spaced-repetition/py-fsrs/tar.gz/refs/heads/main"
PYFSRS_MIRROR = "https://cdn.jsdelivr.net/gh/open-spaced-repetition/py-fsrs@main/fsrs/scheduler.py"


def ensure_pyfsrs():
    if os.path.exists(os.path.join(PYFSRS_DIR, "fsrs", "scheduler.py")):
        return
    os.makedirs(REFS, exist_ok=True)
    print("下载 py-fsrs 源码 ...")
    try:
        with urllib.request.urlopen(PYFSRS_URL, timeout=120) as resp:
            data = resp.read()
        with zipfile.ZipFile(io.BytesIO(data)) as zf:
            zf.extractall(REFS)
    except Exception as exc:  # noqa: BLE001
        raise SystemExit("无法下载 py-fsrs：%s" % exc)


def main():
    ensure_pyfsrs()
    sys.path.insert(0, PYFSRS_DIR)
    from fsrs import Scheduler, Card, Rating  # noqa: E402

    print("py-fsrs 版本：", getattr(__import__("fsrs"), "__version__", "unknown"))
    scheduler = Scheduler(enable_fuzzing=False)

    base = datetime(2026, 1, 1, 9, 0, 0, tzinfo=timezone.utc)

    # (名称, [(评分, 距上次复习的分钟数), ...])
    scenarios = [
        ("新卡连评良好", [(3, 0), (3, 1), (3, 1440), (3, 4320)]),
        ("新卡评重来后重学", [(1, 0), (3, 1), (1, 1440), (3, 10)]),
        ("四档各不相同", [(3, 0), (3, 1), (2, 1440), (4, 5000), (1, 20000), (3, 10), (3, 1440)]),
        ("简单直接跳到复习", [(4, 0), (4, 100000)]),
        ("困难连续", [(2, 0), (2, 1), (2, 1440), (2, 1440)]),
        ("长期复习稳定性增长", [(3, 0), (3, 1), (3, 1440)] + [(3, 0) for _ in range(3)]),
    ]
    # 把最后一个场景展开成真实的递进复习
    scenarios[-1] = ("连续十次良好", [(3, 0)] + [(3, 0)] * 9)

    out = {"generatedBy": "py-fsrs (open-spaced-repetition) Scheduler(enable_fuzzing=False)",
           "note": "间隔单位为秒；stability/difficulty 为复习后的值",
           "scenarios": []}

    for name, reviews in scenarios:
        card = Card()
        now = base
        records = []
        for rating_value, elapsed_minutes in reviews:
            now = now + timedelta(minutes=elapsed_minutes)
            card, _log = scheduler.review_card(card, Rating(rating_value), now)
            records.append({
                "rating": rating_value,
                "elapsedMinutesFromPrevious": elapsed_minutes,
                "stateAfter": int(card.state),
                "stepAfter": card.step,
                "stabilityAfter": round(card.stability, 12),
                "difficultyAfter": round(card.difficulty, 12),
                "intervalSeconds": int((card.due - now).total_seconds()),
                "dueAfter": card.due.isoformat(),
            })
        out["scenarios"].append({"name": name, "reviews": records})

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)
    print("已生成", OUT)
    print("场景数：", len(out["scenarios"]))
    for sc in out["scenarios"]:
        print(" ", sc["name"], "→", [r["intervalSeconds"] for r in sc["reviews"]])


if __name__ == "__main__":
    main()
