#!/usr/bin/env bash
# Build and launch "The Emotional Bank Statement" (pure Java 8, no external libs).
set -euo pipefail
cd "$(dirname "$0")"

SRC_DIR="src/main/java"
OUT_DIR="out"

echo "Compiling (Java 8 target)..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
find "$SRC_DIR" -name '*.java' > sources.txt
javac -source 8 -target 8 -d "$OUT_DIR" @sources.txt
rm -f sources.txt

echo "Launching..."
java -cp "$OUT_DIR" com.whim.ebs.Main
