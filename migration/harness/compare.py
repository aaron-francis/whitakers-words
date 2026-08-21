#!/usr/bin/env python3
"""Differential equivalence check between a reference and a candidate.

Runs two executables against the same inputs and compares their output line
for line, per the contract in migration/CONTRACT.md.

    ./compare.py --reference ./bin/roman_oracle \
                 --candidate "java -cp target Roman" \
                 --inputs inputs.txt --mode exhaustive --report report.json

Exit status is 0 only if every input produced identical output.
"""
import argparse, json, shlex, subprocess, sys, time


def run(cmd: str, payload: str, label: str):
    t0 = time.time()
    try:
        proc = subprocess.run(shlex.split(cmd), input=payload,
                              capture_output=True, text=True, timeout=1800)
    except FileNotFoundError:
        sys.exit(f"FATAL: {label} executable not found: {cmd}")
    except subprocess.TimeoutExpired:
        sys.exit(f"FATAL: {label} exceeded 1800s")
    if proc.returncode != 0:
        sys.exit(f"FATAL: {label} exited {proc.returncode}\n"
                 f"stderr tail:\n{proc.stderr[-2000:]}")
    return proc.stdout.splitlines(), time.time() - t0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--reference", required=True,
                   help="command for the legacy implementation (the oracle)")
    p.add_argument("--candidate", required=True,
                   help="command for the migrated implementation")
    p.add_argument("--inputs", required=True)
    p.add_argument("--mode", choices=["exhaustive", "corpus"], required=True,
                   help="exhaustive proves the domain; corpus is evidence only")
    p.add_argument("--show", type=int, default=10,
                   help="how many divergences to print")
    p.add_argument("--report", help="write a JSON summary here")
    a = p.parse_args()

    with open(a.inputs) as fh:
        inputs = [ln.rstrip("\n") for ln in fh if ln.strip()]
    payload = "\n".join(inputs) + "\n"

    ref, ref_s = run(a.reference, payload, "reference")
    cand, cand_s = run(a.candidate, payload, "candidate")

    problems = []
    if len(ref) != len(inputs):
        problems.append(f"reference emitted {len(ref)} lines for "
                        f"{len(inputs)} inputs")
    if len(cand) != len(inputs):
        problems.append(f"candidate emitted {len(cand)} lines for "
                        f"{len(inputs)} inputs")
    if problems:
        for msg in problems:
            print(f"CONTRACT VIOLATION: {msg}", file=sys.stderr)
        sys.exit(2)

    divergences = []
    for i, (r, c) in enumerate(zip(ref, cand)):
        if r != c:
            divergences.append({"input": inputs[i],
                                "reference": r, "candidate": c})

    total = len(inputs)
    n_div = len(divergences)

    print(f"inputs compared : {total:,}")
    print(f"mode            : {a.mode}"
          f"{'  (PROOF over this domain)' if a.mode == 'exhaustive' else '  (evidence, not proof)'}")
    print(f"reference time  : {ref_s:.1f}s")
    print(f"candidate time  : {cand_s:.1f}s")
    print(f"divergences     : {n_div:,}")

    if divergences:
        print(f"\nfirst {min(a.show, n_div)} divergences:")
        for d in divergences[:a.show]:
            print(f"  {d['input']!r}: reference={d['reference']!r} "
                  f"candidate={d['candidate']!r}")

    if a.report:
        with open(a.report, "w") as fh:
            json.dump({"total": total, "mode": a.mode,
                       "divergences": n_div,
                       "equivalent": n_div == 0,
                       "claim": ("proven over this domain"
                                 if a.mode == "exhaustive" and n_div == 0
                                 else "evidence only" if n_div == 0
                                 else "NOT equivalent"),
                       "examples": divergences[:a.show]}, fh, indent=2)

    if n_div:
        print("\nRESULT: NOT EQUIVALENT", file=sys.stderr)
        sys.exit(1)
    print(f"\nRESULT: EQUIVALENT over {total:,} inputs"
          f"{' — exhaustive' if a.mode == 'exhaustive' else ' — sampled'}")


if __name__ == "__main__":
    main()
