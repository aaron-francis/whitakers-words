#!/usr/bin/env bash
# Reproduces the entire Strings_Package migration verification from a clean
# clone. Requires gprbuild/GNAT, a JDK, and python3 on PATH.
#
#     ./migration/verify-strings.sh
#
# Exit status is compare.py's: 0 means no divergence.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"
COUNT="${CORPUS_SIZE:-200000}"

echo "== building reference (Ada, unmodified legacy source) =="
gprbuild -p -q -P "$HERE/ada-strings/strings_oracle.gpr"

echo "== building candidate (Java, standard library only) =="
mkdir -p "$HERE/build/java"
javac -d "$HERE/build/java" "$HERE/java-strings/Strings.java"

echo "== generating corpus ($COUNT inputs) =="
python3 "$HERE/gen_strings_inputs.py" --count "$COUNT" --out "$HERE/inputs.txt"

echo "== comparing =="
cd "$ROOT"
python3 migration/harness/compare.py \
    --reference migration/build/strings_oracle \
    --candidate "java -cp migration/build/java Strings" \
    --inputs migration/inputs.txt \
    --mode corpus \
    --report migration/report.json
