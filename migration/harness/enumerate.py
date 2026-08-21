#!/usr/bin/env python3
"""Generate the input space for a differential migration check.

Exhaustive mode enumerates every string over an alphabet up to a maximum
length. Corpus mode draws a structured random sample for domains too large to
exhaust.

    ./enumerate.py exhaustive --alphabet MDCLXVI --max-len 6
    ./enumerate.py corpus --alphabet MDCLXVImdclxvi --count 200000 --max-len 12
"""
import argparse, itertools, random, sys


def exhaustive(alphabet: str, max_len: int):
    for n in range(1, max_len + 1):
        for tup in itertools.product(alphabet, repeat=n):
            yield "".join(tup)


def corpus(alphabet: str, count: int, max_len: int, seed: int):
    rng = random.Random(seed)
    seen = set()
    while len(seen) < count:
        n = rng.randint(1, max_len)
        s = "".join(rng.choice(alphabet) for _ in range(n))
        if s not in seen:
            seen.add(s)
            yield s


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="mode", required=True)

    e = sub.add_parser("exhaustive")
    e.add_argument("--alphabet", required=True)
    e.add_argument("--max-len", type=int, required=True)

    c = sub.add_parser("corpus")
    c.add_argument("--alphabet", required=True)
    c.add_argument("--count", type=int, required=True)
    c.add_argument("--max-len", type=int, required=True)
    c.add_argument("--seed", type=int, default=20260820)

    a = p.parse_args()
    out = sys.stdout
    if a.mode == "exhaustive":
        total = sum(len(a.alphabet) ** n for n in range(1, a.max_len + 1))
        print(f"# exhaustive over {a.alphabet!r} up to length {a.max_len}: "
              f"{total} inputs", file=sys.stderr)
        for s in exhaustive(a.alphabet, a.max_len):
            out.write(s + "\n")
    else:
        print(f"# corpus: {a.count} distinct inputs, seed {a.seed}",
              file=sys.stderr)
        for s in corpus(a.alphabet, a.count, a.max_len, a.seed):
            out.write(s + "\n")


if __name__ == "__main__":
    main()
