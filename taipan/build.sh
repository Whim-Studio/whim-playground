#!/usr/bin/env bash
# Build Taipan! into a single runnable JAR using plain javac + jar (no Maven/Gradle).
# Requires a JDK 8+ on PATH. Compiles at Java 8 language/bytecode level.
set -euo pipefail
cd "$(dirname "$0")"

echo "Compiling (Java 8 target)..."
rm -rf out
mkdir -p out
find src -name '*.java' > sources.txt
javac --release 8 -d out @sources.txt
rm -f sources.txt

echo "Packaging taipan.jar..."
jar cfe taipan.jar com.taipan.Main -C out .

echo "Done. Run with:  java -jar taipan.jar"
