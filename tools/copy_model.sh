#!/usr/bin/env bash
# Reproducible step: package the trained YOLO model into the app assets.
# Source model is never modified — it is copied from the project Model/ directory.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/Model/best.onnx"
DST="$ROOT/app/src/main/assets/models/best.onnx"
if [ ! -f "$SRC" ]; then
  echo "ERROR: source model not found at $SRC" >&2
  exit 1
fi
mkdir -p "$(dirname "$DST")"
cp -f "$SRC" "$DST"
echo "Copied $(du -h "$DST" | cut -f1) model -> app/src/main/assets/models/best.onnx"
sha256sum "$SRC" "$DST" | awk '{print $1}' | uniq -c
