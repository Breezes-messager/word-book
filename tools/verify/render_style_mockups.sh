#!/usr/bin/env bash
# 把 docs/style-samples/*.html 渲染成同名的 2 倍图 PNG
set -uo pipefail
ROOT="C:/Users/29559/Desktop/dsh/Word-book"
CHROME="/c/Program Files/Google/Chrome/Application/chrome.exe"
cd "$ROOT"
python tools/verify/make_style_mockups.py >/dev/null
for f in docs/style-samples/style-*.html; do
  name=$(basename "$f" .html)
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
    --window-size=1210,898 --screenshot="$ROOT/docs/style-samples/$name.png" \
    "file:///$ROOT/docs/style-samples/$name.html" 2>&1 | grep -v "^\[" | tail -1
done
