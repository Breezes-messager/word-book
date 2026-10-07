#!/usr/bin/env bash
# 把 docs/style-samples/*.html 渲染成同名的 2 倍图 PNG
set -uo pipefail
# 自动定位仓库根目录，避免写死本机路径
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHROME="${CHROME:-/c/Program Files/Google/Chrome/Application/chrome.exe}"
cd "$ROOT"
python tools/verify/make_style_mockups.py >/dev/null
for f in docs/style-samples/style-*.html; do
  name=$(basename "$f" .html)
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
    --window-size=1210,898 --screenshot="$ROOT/docs/style-samples/$name.png" \
    "file:///$ROOT/docs/style-samples/$name.html" 2>&1 | grep -v "^\[" | tail -1
done
