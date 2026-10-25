#!/usr/bin/env bash
# Renders the Play Store assets in this folder to PNG with headless Chrome.
set -euo pipefail
cd "$(dirname "$0")"
CHROME=${CHROME:-"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"}
mkdir -p out/screenshots

shoot() {
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --allow-file-access-from-files \
    --force-device-scale-factor=1 --virtual-time-budget=3000 \
    --window-size="$2" --screenshot="$PWD/$3" "file://$PWD/$1" >/dev/null 2>&1
}

shoot icon.svg 512,512 out/icon-512.png
shoot feature-graphic.html 1024,500 out/feature-graphic.png

while IFS='|' read -r shot caption; do
  shoot "screenshot.html?shot=$shot&caption=$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$caption")" \
    1080,1920 "out/screenshots/$shot.png"
done <<'EOF'
today|Log your day in one tap
rhythm|See how steady your week is
routines|Routines for every part of the day
editor|Build your own, step by step
player|Guided cues that keep time for you
player_dark|Keeps going with the screen off
rhythm_dark|Easy on the eyes at night
settings|Your cues, your way
EOF
