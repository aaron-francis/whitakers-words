#!/bin/bash
#
# Reproduces the whole equivalence check from a clean clone:
#
#     ./migration/verify.sh
#
# 1. builds the reference implementation (the oracle) from the legacy Ada
# 2. builds the migrated implementation (Java)
# 3. enumerates the input domain
# 4. compares them line for line and writes migration/report.json
#
# Exit status is the harness's: zero means equivalent over the domain.
#
# Requires gnat/gprbuild, a JDK, and python3. Nothing else.

set -euo pipefail

cd "$(dirname "$0")/.."

ALPHABET=MDCLXVI
MAX_LEN=7
MODE=exhaustive

echo "== building reference implementation (Ada) =="
gprbuild -p -j0 -P migration/roman_oracle.gpr

echo "== building migrated implementation (Java) =="
rm -rf migration/java/classes
javac -d migration/java/classes migration/java/Roman.java

echo "== enumerating input domain =="
python3 migration/harness/enumerate.py "$MODE" \
    --alphabet "$ALPHABET" --max-len "$MAX_LEN" > migration/inputs.txt

echo "== comparing =="
python3 migration/harness/compare.py \
    --reference migration/bin/roman_oracle \
    --candidate "java -cp migration/java/classes Roman" \
    --inputs migration/inputs.txt \
    --mode "$MODE" \
    --report migration/report.json
