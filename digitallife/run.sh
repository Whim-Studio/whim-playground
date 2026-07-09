#!/usr/bin/env bash
# Compile every source file and launch the quiz. Requires a Java 8+ JDK.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
find src -name '*.java' > sources.txt
javac -d out @sources.txt
java -cp out com.whim.digitallife.Main
