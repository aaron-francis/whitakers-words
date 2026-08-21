# Security review playbook — the core prompt

Runs **after** a migration, **in parallel** with equivalence verification, and
answers a different question.

The harness asks: *does the migrated code behave identically over the tested
domain?* This asks: *did the migration import risk the harness cannot see?*

Those are not the same question, and a passing harness is not an answer to this
one. Equivalence is only claimed over the domain that was enumerated. Anything
outside it — malformed input, resource exhaustion, arithmetic at the edges — is
invisible to a differential comparison, because both sides were only ever run
on inputs the comparison chose.

Sections marked **[EDITABLE]** are yours. **[LOCKED]** sections are not
editable during a run.

---

## §1 TARGET — [EDITABLE]

    repository:        aaron-francis/whitakers-words
    review_branch:     devin/1787277015-roman-numeral-java-migration
    migrated_code:     migration/java/Roman.java
    source_language:   Ada
    target_language:   Java
    equivalence_claim: proven over MDCLXVI, length <= 7 (960,799 inputs)

## §2 POLICY — [LOCKED]

1. **Report. Do not fix.** A security change during a migration invalidates the
   equivalence proof, because the reference no longer describes the candidate.
   Findings go to a human with a recommendation, not into the code.

2. **Every finding needs a concrete reproduction** — a specific input, or a
   specific condition, that a reviewer can check without trusting you. A
   finding you cannot reproduce is labelled `speculative` and ranked below
   everything you can.

3. **Do not pad.** "No material findings" is a valid and valuable result. A
   report inflated to look thorough wastes the reviewer's attention, which is
   the scarcest thing in this process.

4. **Severity is your judgment and must be justified in one sentence.** Do not
   assign a severity you cannot defend to someone who disagrees.

## §3 ACCEPTANCE CRITERIA — [EDITABLE]

1. **Safety properties lost in translation.** Identify every guarantee the
   source language provided by default that the target language does not.
   Ada checks ranges and arithmetic overflow at runtime and raises; Java's
   primitive integer arithmetic wraps silently. State concretely where the
   migrated code relies on a check the source language made for it.
2. Input handling at and beyond the boundaries of the verified domain —
   including inputs longer than the enumerated maximum, empty input, and
   malformed encoding.
3. Resource behavior on hostile input: unbounded allocation, unbounded line
   length, unbounded input volume.
4. Failure-mode differences: where the source raised, what does the target do?
   Where the source terminated, does the target continue with a wrong value?
5. Dependency and provenance: every import, its origin, and whether it is
   outside the standard library.

## §4 VERIFICATION — [MECHANICAL]

For each finding, provide either:

- a concrete input and the observed behavior of both implementations, or
- the exact source line and the language rule that makes it a risk.

A finding with neither is `speculative`.

## §5 STOP CONDITIONS — [LOCKED]

Stop and report `blocked` if:

- the migrated code cannot be built or run in isolation;
- a §3 requirement would require modifying the code to evaluate — say which,
  and what you would need instead;
- you are asked to fix rather than report.

## §6 REPORTING

- `outcome` — `reviewed` | `blocked`
- `findings_count` — integer, and `0` is an acceptable answer
- `properties_lost` — safety guarantees the target language does not provide
- `outside_verified_domain` — behavior the equivalence proof does not cover
- `dependencies` — every import and its provenance
- `highest_severity` and a one-sentence justification
- `risks` — what a reviewer should check first
