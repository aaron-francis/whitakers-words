# Migration playbook — the core prompt

This file is the interface to the agent. It is deliberately readable, because
the engineering policy it encodes has to be arguable by the people who own the
system, not buried in a vendor's product.

Sections are marked **[EDITABLE]** or **[LOCKED]**.

- **[EDITABLE]** sections are yours. Change the target, add requirements,
  tighten constraints. This is how you adapt the workflow to your program.
- **[LOCKED]** sections are the stopping conditions. They are not editable
  during a run, and that is deliberate: if the party requesting the change can
  also delete the conditions under which the agent refuses, then compliance
  means nothing. You can change them between runs, in review, on purpose.

---

## §1 TARGET — [EDITABLE]

    source_repository:  aaron-francis/whitakers-words
    source_module:      src/words_engine/words_engine-roman_numerals_package.adb
    source_symbol:      Roman_Number
    source_language:    Ada
    target_language:    Java
    target_location:    migration/java/
    verification_mode:  exhaustive
    alphabet:           MDCLXVI
    max_length:         7

## §2 POLICY — [LOCKED]

1. **Preserve behavior. Do not improve it.** If the legacy implementation is
   wrong, the migrated implementation must be wrong in exactly the same way.
   Fixing a defect during a migration makes the migration unverifiable, because
   there is no longer a reference to compare against.

2. **The legacy implementation is the specification.** Not the comments, not
   the documentation, not what the behavior obviously ought to be. Where the
   code and its comments disagree, the code wins and you report the
   disagreement.

3. **You may not modify the legacy implementation** except to expose a seam —
   making an existing symbol visible so it can be called. Exposing a seam
   changes no behavior. Anything that changes behavior is out of scope.

4. **You do not decide whether the migration succeeded.** The harness decides.
   Your opinion of your own work is not evidence.

## §3 ACCEPTANCE CRITERIA — [EDITABLE]

Add requirements here. Each one must be objectively checkable by someone who
did not write it.

1. A reference implementation executable conforming to `migration/CONTRACT.md`,
   built from the unmodified legacy source.
2. A migrated implementation executable in the target language, conforming to
   the same contract.
3. `migration/harness/compare.py` reports zero divergences in the mode named
   in §1.
4. The migrated implementation depends on nothing outside its language's
   standard library.
5. A single command, checked into the repository, reproduces the entire
   verification from a clean clone.

## §4 VERIFICATION — [MECHANICAL]

Generate the input space and run the comparison:

    python3 migration/harness/enumerate.py exhaustive \
        --alphabet <alphabet> --max-len <max_length> > migration/inputs.txt

    python3 migration/harness/compare.py \
        --reference <reference command> \
        --candidate <candidate command> \
        --inputs migration/inputs.txt \
        --mode <verification_mode> \
        --report migration/report.json

The harness's exit status is the result. Zero means equivalent. Anything else
means you are not done.

Do not modify the harness. If you believe the harness is wrong, stop and say
so — that is a legitimate finding, and changing the instrument to make your
own result pass is not.

**On the word "proven":** you may describe a result as proven only when the
mode is `exhaustive` and the domain was genuinely enumerated. A corpus run is
evidence and must be labelled as evidence, with the sampling strategy stated.
Overclaiming here is the one unrecoverable error in this workflow.

## §5 STOP CONDITIONS — [LOCKED]

Stop, open no pull request, and report `blocked` with your evidence if:

- two requirements in §3 contradict each other, or a §3 requirement
  contradicts §2 — **name both requirements and explain the conflict**;
- equivalence cannot be reached and you cannot explain the divergence;
- reaching equivalence would require modifying the legacy implementation
  beyond exposing a seam;
- reaching equivalence would require modifying the harness;
- the module cannot be separated from its dependencies without changing
  behavior.

A run that stops for one of these reasons has succeeded. The purpose of this
workflow is to find out whether a migration is safe, and "not yet, here is
precisely why" is the answer that saves the most time.

## §6 REPORTING

Fill the structured output with:

- `outcome` — `migrated` | `blocked`
- `pr_url`
- `inputs_compared` — integer
- `verification_mode` — `exhaustive` | `corpus`
- `claim` — copied verbatim from `migration/report.json`, not paraphrased
- `legacy_defects_preserved` — anything you found wrong and deliberately
  reproduced
- `blocked_reason` — when applicable, naming the conflicting requirements
- `risks` — what a human reviewer should check first
