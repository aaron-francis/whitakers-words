# Blocked — a stateful module that is not ready to port

We pointed the migration playbook at `Syncope`, a module that mutates parser
state, rolls it back on failure, and calls into dictionary lookup. Unlike the
pure functions already migrated in this repository, it could not be ported.

The run opened no pull request and committed nothing, per §5.

**This is the useful result.** Knowing in week one that a module cannot be
verified in isolation is cheaper than discovering it after someone has written
the replacement.


    outcome:          blocked
    inputs_compared:  0
    tests_added:      0

## Why it stopped

§5 stop condition: the module cannot be separated from its dependencies without changing behavior. Specifically, §3.4 ('The migrated implementation depends on nothing outside its language's standard library') contradicts §2.1/§2.3 ('Preserve behavior... You may not modify the legacy implementation except to expose a seam'). Syncope (src/words_engine/words_engine-tricks.adb:56-252) calls Words_Engine.Word_Package.Word five times (lines 89, 121, 157, 185, 216) — the full ~2,000-line dictionary lookup — and every match/no-match decision (Pa_Last > Pa_Save + 1, Pofs = V, Key = 3) is determined by Word's dictionary results. A std-lib-only Java implementation reproducing that behavior requires porting the entire engine plus Ada Direct_IO binary dictionary formats (out of scope for source_symbol Syncope); any stub of Word changes observable behavior, which the run note forbids. Secondary conflict: CONTRACT.md states '<result> is defined per module in the migration prompt', but §1 defines no <result> serialization for Syncope's outputs (mutated Pa parse records incl. IR/MNPC/D_K, Pa_Last, Xp.Yyy_Meaning, Put_Stat side effect), so §3.1/§3.2 are not objectively checkable as written.

## What Syncope actually depends on

Syncope depends on: Words_Engine.Word_Package.Word (full dictionary lookup engine, ~2,000 lines), Support_Utils.Word_Support_Package (Ada.Direct_IO binary STEMFILE/INDXFILE access), Dict_IO binary DICTFILE access, Support_Utils.Addons_Package, Support_Utils.Uniques_Package, Words_Mode/Words_Mdev runtime flags (Word_Parameters/Developer_Parameters), Word_Package globals Line_Number/Word_Number, Words_Engine.Put_Stat (developer stats file), Words_Engine.Trick_Tables (Member/Strings), Latin_Utils.Strings_Package (Lower_Case/Head), and Xp.Yyy_Meaning (Explanation_Package).

## Evidence

Static analysis of unmodified legacy source, with line references: (1) Syncope calls Word at tricks.adb lines 89, 121, 157, 185, 216; Word (words_engine-word_package.adb:603) drives Dictionary_Search/Search_Dictionaries over binary dictionary files opened via Ada.Direct_IO instantiations (Stem_Io and Stem_File array, support_utils-word_support_package.ads:33-39; Dict_IO throughout word_package.adb — 51 matches for dictionary-IO symbols), loads General/Special dictionaries at init (word_package.adb:1894-1896), and consults Addons_Package, Uniques_Package, and 12 references to Words_Mode/Words_Mdev runtime mode flags. (2) Syncope's Explain_Syncope (tricks.adb:67-73) reads Line_Number/Word_Number, which are package globals of Word_Package (word_package.ads:24), not parameters, and writes via Put_Stat to a developer stats file when open (words_engine-put_stat.adb:21-23). (3) Syncope's observable output is the mutated Pa array of Parse_Record values containing Inflection_Record, MNPC (Dict_IO file pointers) and D_K — meaningful only relative to the loaded binary dictionaries. Therefore Syncope's behavior on any input is a function of the entire dictionary database and engine plus ambient global state, not of its string-rewrite logic alone; no seam-only change can isolate it. No PR opened, nothing committed, harness not run — per §5.

## What to do instead

A reviewer should first verify the dependency claim directly: confirm at tricks.adb lines 89/121/157/185/216 that Syncope's control flow is decided by Word's dictionary results, and that Line_Number/Word_Number in Explain_Syncope resolve to Word_Package globals. Then decide the policy question: either redefine §1's target as Syncope's pure candidate-generation logic with Word behind an explicit recorded-oracle seam (capture Word's stem-to-parse-result traces from the reference run and replay identically to both implementations), define a <result> serialization of Syncope's full observable state (Pa records, Pa_Last, Xp.Yyy_Meaning, stat line), or sequence the migration bottom-up starting with the dictionary engine and its binary data formats.

## Disagreements between code and comments

Comment at tricks.adb:97-98 shows the intended conjugation restriction '(3, 4)/(6, 1)' is commented out; the code accepts any verb with Key = 3. Per §2.2 the code is the specification; the disagreement is reported, not fixed.

## Defects a future port would have to reproduce

None reproduced (run stopped before shipping an implementation). Defects a behavior-preserving port would have to reproduce: a bare 'when others' exception handler (tricks.adb:247-250) that silently swallows all errors and resets Pa_Last; the 'ii => ivi' guard comments out the conjugation check (line 98) so any verb with Key=3 matches, not just conjugation 3/4 as documented; Explain_Syncope reads Pa(Pa_Save + 1).Stem which is a Syncope marker record, not a dictionary stem; the 's/x => +is' pass inserts 'is' after ANY 's' or 'x' at position <= length-2, not only word-final perfect stems.
