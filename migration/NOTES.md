# Migration notes — `Roman_Number`, Ada to Java

Scope: `Words_Engine.Roman_Numerals_Package.Roman_Number`, from
`src/words_engine/words_engine-roman_numerals_package.adb` to
`migration/java/Roman.java`.

Per §2 of `migration/PROMPT.md`, the legacy implementation is the
specification. `Roman.java` is a statement-for-statement transliteration of
the Ada body: same branch order, same running `Total`, same threshold
constants, `raise Invalid` rendered as `return 0` (the Ada handler returns 0),
and `exit Evaluate when J < S'First` rendered as `break evaluate` on a
0-based index falling below 0.

## The seam

`Roman_Number` was implemented in the package body but not declared in the
package spec, so it was not callable from outside. The only edit to legacy
source is one declaration added to
`src/words_engine/words_engine-roman_numerals_package.ads`. No body was
touched; visibility changes no behavior.

## Legacy behavior deliberately preserved

Verified against the Ada oracle; all of these are reproduced in Java rather
than corrected.

1. **Non-canonical additive forms are accepted.** `IIII` = 4, `XXXX` = 40,
   `LXXXX` = 90, `CCCC` = 400, `DCCCC` = 900, `MMMM` = 4000. The body's own
   comment tables list these as legal, so code and comments agree here.

2. **Validity depends on a running total, not on run length.** Every guard
   (`Total >= 5`, `>= 50`, `>= 500`, `>= 5000`) tests a total that already
   includes the lower-order digits to the right. So `MMMMD` = 4500 is
   accepted while `MMMMMD` = 0, and `MMMMM` = 0 — the function silently
   rejects numerals above an implicit ceiling rather than reporting overflow.

3. **The hundreds digit is handled in two places.** A `C` block is nested
   inside the `L` branch (this is what makes `CL` = 150 work) in addition to
   the top-level `C` branch, and the `L` branch consequently runs its
   "invalid" check twice, once before and once after that nested block.

4. **A subtraction branch that can never fire.** In the `D` branch, after
   `S (J) = 'M'` has added 1000, the following
   `if S (J) = 'C' and Total <= 1099` can never be true, because `Total` is at
   least 1500 at that point. Reproduced as written.

5. **Inconsistent subtraction thresholds.** The `X` branch requires
   `Total = 10` exactly to subtract a preceding `I`, and the nested `C` block
   requires `Total = 100` exactly, while the `L`, `C`, `D` and `M` branches use
   `<= 59`, `<= 109`, `<= 599` and `<= 1099`. This asymmetry is behavioral and
   is preserved.

6. **`V` has no repetition loop.** `VV` = 0 falls out of the trailing
   `S (J) = 'I' or S (J) = 'V'` check rather than a repetition guard.

7. **The `Constraint_Error` handler is unreachable over this domain.** `Total`
   never goes negative (every subtraction is guarded) and never overflows at
   these lengths. Java therefore needs no analogue; a plain `int` suffices.

8. **Case folding.** The Ada function upper-cases its argument with
   `Ada.Characters.Handling.To_Upper`, which folds Latin-1 letters and leaves
   `0xFF` alone. `Roman.upperCase` mirrors that byte mapping rather than
   calling `Character.toUpperCase`, which would map `0xFF` to `U+0178`. The
   verified domain is ASCII `MDCLXVI`, so this only matters outside it.

`Bad_Roman_Number` and `Value` are separate symbols in the same package and are
out of scope; they were not migrated.

## Verification

`migration/verify.sh` reproduces everything from a clean clone. It enumerates
every string over `MDCLXVI` up to length 7 (960,799 inputs) and runs
`migration/harness/compare.py` in `exhaustive` mode. Result and claim live in
`migration/report.json`, which the harness writes.
