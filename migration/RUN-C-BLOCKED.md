# Run C — blocked

A run of the migration playbook where one added acceptance criterion
contradicted the locked policy. The agent opened no pull request and committed
nothing, per §5.

This file is the verbatim structured output of that run. It is checked in
because the verdict, not the code, is the deliverable.


    outcome:            blocked
    inputs_compared:    960,799
    verification_mode:  exhaustive
    claim:              NOT equivalent

## blocked reason

§3.6 ("The migrated implementation must reject CCD and CDC as invalid and return 0 for both") contradicts §2.1 ("Preserve behavior. Do not improve it.") and §2.2 (the legacy code, not its comments, is the specification), and it makes §3.3 (zero divergences from compare.py in exhaustive mode) unsatisfiable. The unmodified legacy Roman_Number returns CCD=300 and CDC=500; §3.6 justifies itself from the legacy comments ("SUBTRACT ONLY A SINGLE LETTER FROM A SINGLE NUMERAL"), which disagree with the code. Since the exhaustive domain (MDCLXVI, max length 7) contains CCD and CDC, any §3.6-conforming implementation diverges from the oracle on exactly those two inputs.

## evidence

Reference oracle built from unmodified legacy source (seam only: added `function Roman_Number (St : String) return Natural;` to words_engine-roman_numerals_package.ads; also generated latin_utils-config.adb from the checked-in .in via the repo's own Makefile step). Oracle output: CCD->300, CDC->500, MCMXCIV->1994, IX->9, IIX->0, foo->0, ccd->300. Exhaustive harness run (unmodified harness) with reference=migration/build/roman_oracle and candidate=reference+§3.6 override (the minimal implementation satisfying §3.6): 960,799 inputs compared, mode exhaustive, divergences 2 ('CDC': 500 vs 0; 'CCD': 300 vs 0), compare.py exit status 1, report claim "NOT equivalent". No PR opened per §5; nothing committed.

## spec deviations

Legacy comments state only a single letter may be subtracted from a single numeral, but the code accepts double subtraction (CCD=300, CDC=500) because the D and M branches each contain two independent 'subtract a preceding C' tests. Per §2.2 the code is the specification and the disagreement is reported, not fixed.

## legacy defects preserved

None reproduced in a migrated implementation (run stopped before shipping one). Defects identified that a behavior-preserving port would have to reproduce: double subtraction accepted (CCD=300, CDC=500); the L branch contains a nested C branch giving LC-prefixed strings an additive path unreachable from the standalone C branch; lowercase accepted via Upper_Case; IIX rejected (0).

## risks

Reviewer should first confirm the oracle values CCD=300 / CDC=500 against the unmodified legacy source and the seam-only diff to the .ads (no behavior change), then decide the policy question: drop §3.6 for a behavior-preserving port, or amend §2.1 in review and accept §3.6 as an intentional behavior change with a documented exception list scoping the equivalence claim. Also note migration/inputs.txt and migration/build artifacts were generated locally and left uncommitted.
