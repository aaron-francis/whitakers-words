#!/usr/bin/env python3
"""Build migration/inputs.txt for the Strings_Package migration.

The harness generator (migration/harness/enumerate.py) draws raw strings over
an alphabet. The contract line format for this migration carries a symbol name
and one or two arguments (see migration/FORMAT-strings.md), so this script is
the adapter: it draws the raw argument strings *from enumerate.py* and wraps
each one in the line format. It also adds the deterministic edge cases that
enumerate.py cannot produce (the empty string, in particular).

Nothing here modifies the harness; this script only feeds it.

Sampling strategy (deterministic; no clock, no unseeded RNG):

  1. Edge pool, hand-written and exhaustive over its own small domain:
     the empty string, every single character of the alphabet, whitespace-only
     strings, and strings whose leading/trailing/interior whitespace sits at a
     boundary Trim or Head cares about.
  2. Random pool, drawn by enumerate.py corpus mode in alternating passes of
     short strings (max-len 12) and long strings (max-len 40) with fixed
     seeds, until the requested corpus size is filled.
  3. Every base string is expanded into lines for all four symbols, with Trim
     exercised at its default and at Left/Right/Both, and Head exercised at
     counts clustered on the length boundary (len-1, len, len+1) plus 0, 1 and
     a few far values.
  4. A small fixed set of malformed lines, to pin the error path.

Edge-derived lines are emitted first, so truncation at the requested corpus
size can only ever drop random-pool lines.
"""
import argparse
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ENUMERATE = os.path.join(HERE, "harness", "enumerate.py")

# printable ASCII (0x20-0x7E) plus the ASCII whitespace controls
ALPHABET = "".join(chr(c) for c in range(0x20, 0x7F)) + "\t\n\v\f\r"

ESCAPES = {
    "\\": r"\\",
    "|": r"\|",
    " ": r"\s",
    "\t": r"\t",
    "\n": r"\n",
    "\v": r"\v",
    "\f": r"\f",
    "\r": r"\r",
}


def encode(s: str) -> str:
    return "".join(ESCAPES.get(c, c) for c in s)


def edge_pool() -> list:
    ws = [" ", "\t", "\n", "\v", "\f", "\r"]
    pool = [""]
    pool += list(ALPHABET)                      # every single character
    for w in ws:                                # whitespace-only runs
        for n in (1, 2, 3, 5, 8):
            pool.append(w * n)
    pool.append("".join(ws))
    pool.append("".join(ws) * 2)
    for w in ws:                                # whitespace at the boundaries
        for core in ("a", "ab", "abc", "a b", "a\tb", "AbC", "|", "\\"):
            pool.append(w + core)
            pool.append(core + w)
            pool.append(w + core + w)
            pool.append(w * 3 + core + w * 3)
            pool.append(w + w.swapcase() + core)
    for n in (0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 39, 40, 41):
        pool.append("x" * n)                    # exact Head target lengths
        pool.append(" " * n + "x")
        pool.append("x" + " " * n)
        pool.append(" " * n)
    pool += [
        "Trim|Head", "a|b", "\\s", "\\\\", "\\t", "!ERROR",
        "Lower_Case", "  MiXeD case  ", "\tMiXeD\tcase\t",
        "aBcDeFgHiJkL", "ZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZzZz",
    ]
    seen, unique = set(), []
    for s in pool:
        if s not in seen:
            seen.add(s)
            unique.append(s)
    return unique


def enumerate_corpus(count: int, max_len: int, seed: int) -> list:
    proc = subprocess.run(
        [sys.executable, ENUMERATE, "corpus", "--alphabet", ALPHABET,
         "--count", str(count), "--max-len", str(max_len), "--seed", str(seed)],
        capture_output=True, text=True, check=True)
    # enumerate.py writes one string per line; strings containing \n or \r
    # arrive split, which is exactly why the wrapper below re-encodes them.
    return [ln for ln in proc.stdout.split("\n")[:-1]]


def head_counts(n: int) -> list:
    raw = [0, 1, max(0, n - 1), n, n + 1, n + 2, 2 * n, 12, 40]
    out = []
    for c in raw:
        if c >= 0 and c not in out:
            out.append(c)
    return out


def lines_for(base: str) -> list:
    enc = encode(base)
    out = [f"Lower_Case|{enc}", f"Upper_Case|{enc}", f"Trim|{enc}"]
    out += [f"Trim|{enc}|{side}" for side in ("Left", "Right", "Both")]
    out += [f"Head|{enc}|{c}" for c in head_counts(len(base))]
    return out


MALFORMED = [
    "Lower_Case",
    "Lower_Case|a|b",
    "Upper_Case|a|Both",
    "Trim|a|both",
    "Trim|a|LEFT",
    "Trim|a|Left|Right",
    "Head|a",
    "Head|a|-1",
    "Head|a|1x",
    "Head|a|",
    "Head|a|999999",
    "Head|a|100000",
    "Head|a|100001",
    "lower_case|a",
    "|a",
    "|",
    "Trim|a\\q",
    "Trim|a\\",
    "Trim|a b",
    "Trim| a",
    "Trim|a ",
]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--count", type=int, default=200000,
                   help="number of input lines to emit")
    p.add_argument("--out", required=True)
    a = p.parse_args()

    emitted, seen = [], set()

    def add(line: str):
        if line not in seen and len(emitted) < a.count:
            seen.add(line)
            emitted.append(line)

    for line in MALFORMED:
        add(line)
    for base in edge_pool():
        for line in lines_for(base):
            add(line)

    edge_lines = len(emitted)

    passes = []
    for k in range(16):
        passes.append((12, 20260820 + k))
        passes.append((40, 20270000 + k))

    for max_len, seed in passes:
        if len(emitted) >= a.count:
            break
        needed = a.count - len(emitted)
        for base in enumerate_corpus(max(1, needed // 10), max_len, seed):
            for line in lines_for(base):
                add(line)
            if len(emitted) >= a.count:
                break

    with open(a.out, "w") as fh:
        fh.write("\n".join(emitted) + "\n")

    print(f"edge-derived lines : {edge_lines:,}", file=sys.stderr)
    print(f"total lines        : {len(emitted):,}", file=sys.stderr)


if __name__ == "__main__":
    main()
