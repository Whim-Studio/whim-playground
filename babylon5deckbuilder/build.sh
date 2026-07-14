#!/usr/bin/env bash
# Build the Babylon 5 deck-builder: compile, test, and package the runnable JAR.
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven (mvn) not found. Install JDK 8 + Maven 3.6+ and retry." >&2
  exit 1
fi

mvn -q clean package
echo
echo "Build complete. Runnable JAR: target/babylon5-deckbuilder.jar"
echo "Run UI:   java -jar target/babylon5-deckbuilder.jar"
echo "Run sim:  java -jar target/babylon5-deckbuilder.jar --sim --games 1000 --players 4 --seed 42 --ai hard"
