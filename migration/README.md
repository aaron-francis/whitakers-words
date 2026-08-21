# Migration workflow

Legacy code nobody can safely change, moved to a modern language, with a
machine-checkable proof that behavior did not change.

## The pipeline

| Step | Who does it | Output |
|---|---|---|
| 1. Characterize | agent | tests pinning current behavior; whether a seam exists |
| 2. Port | agent | two executables, both speaking the contract |
| 3. Prove | **a script, not an agent** | `report.json` — proven, evidence, or not equivalent |
| 4. Security review | agent | what the proof cannot see |

Step 3 is deliberately not an agent. An agent that checks another agent's work
is two things that can be wrong together. A diff is a diff.

## The playbooks

| File | Purpose |
|---|---|
| [PROMPT-characterize.md](PROMPT-characterize.md) | Record what the code does today. Do not correct it. |
| [PROMPT.md](PROMPT.md) | Port it. Preserve behavior exactly. |
| [PROMPT-security.md](PROMPT-security.md) | Ask what the equivalence proof cannot answer. |
| [CONTRACT.md](CONTRACT.md) | What an implementation must be, in any language. |
| [KICKOFF.md](KICKOFF.md) | Paste-ready cards for starting a run. |

Every playbook has the same six sections:

    §1 TARGET              editable
    §2 POLICY              code-owner review required
    §3 ACCEPTANCE          editable — add your requirements here
    §4 VERIFICATION        mechanical
    §5 STOP CONDITIONS     code-owner review required
    §6 REPORTING

**You can change the policy. You cannot change it quietly.** §2 and §5 are
covered by [CODEOWNERS](../.github/CODEOWNERS) and branch protection: every
edit is a reviewed diff with a name on it, kept in the history. Policy should
change — in review, on purpose, by the right person.

## The harness

[`harness/enumerate.py`](harness/enumerate.py) builds the input space.
[`harness/compare.py`](harness/compare.py) runs both implementations and
compares every line.

Two modes, and the difference is the whole point:

- **exhaustive** — the domain was enumerated, not sampled. A result here is
  *proven* over that domain.
- **corpus** — the domain is unbounded. A result here is *evidence*, and must
  say so.

Claiming the first when you did the second is the one unrecoverable error in
this workflow.

## Runs so far

| Run | Result |
|---|---|
| [PR #4](https://github.com/aaron-francis/whitakers-words/pull/4) — `Roman_Number` → Java | **Proven.** 960,799 inputs, 0 divergences. Bounded domain, fully enumerated. |
| [PR #5](https://github.com/aaron-francis/whitakers-words/pull/5) — `Strings_Package` → Java | **Evidence.** 200,000 samples, 0 divergences. Unbounded domain — labelled accordingly. |
| [REFUSED-CONTRADICTORY-REQUIREMENT.md](REFUSED-CONTRADICTORY-REQUIREMENT.md) — added a contradictory requirement | **Blocked.** Built the oracle, ran all 960,799, found exactly 2 divergences, stopped. No PR, nothing committed. |

Run C is the one worth reading. It was told to reject two inputs the legacy
code accepts — a requirement that conflicts with preserving behavior. It did
not pick an interpretation. It proved the conflict was real, named both
requirements, and handed the decision back.
