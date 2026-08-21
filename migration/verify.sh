#!/usr/bin/env bash
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
missing=()

command -v gprbuild >/dev/null 2>&1 ||
   missing+=("gprbuild (apt package: gprbuild)")
command -v gnat >/dev/null 2>&1 ||
   missing+=("gnat (apt package: gnat)")
command -v javac >/dev/null 2>&1 ||
   missing+=("javac (apt package: default-jdk)")
command -v java >/dev/null 2>&1 ||
   missing+=("java (apt package: default-jdk)")
command -v python3 >/dev/null 2>&1 ||
   missing+=("python3 (apt package: python3)")

if ((${#missing[@]} > 0)); then
   printf 'Missing required tools:\n' >&2
   printf '  %s\n' "${missing[@]}" >&2
   printf 'Install the missing packages with: sudo apt-get install -y' >&2
   printf ' gnat gprbuild default-jdk python3\n' >&2
   exit 127
fi

cd "$ROOT"

sed 's|@datadir@|.|' \
   src/latin_utils/latin_utils-config.adb.in \
   > src/latin_utils/latin_utils-config.adb

gprbuild -p -P migration/ada/roman_oracle.gpr
javac -d migration/java/classes migration/java/Roman.java
python3 migration/harness/enumerate.py exhaustive \
   --alphabet MDCLXVI --max-len 7 > migration/inputs.txt

if python3 migration/harness/compare.py \
   --reference migration/ada/roman_oracle \
   --candidate "java -cp migration/java/classes Roman" \
   --inputs migration/inputs.txt \
   --mode exhaustive \
   --report migration/report.json
then
   exit 0
else
   status=$?
   exit "$status"
fi
