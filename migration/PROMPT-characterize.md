# Characterization playbook — the core prompt

Step one of any migration. You cannot port what nobody has written down, and
you cannot safely change legacy code in place without a record of what it did
before you touched it.

Sections marked **[EDITABLE]** are yours. Sections marked **[LOCKED]** are the
stopping conditions and are not editable during a run — if the party requesting
the work can also delete the conditions under which the agent refuses, then
compliance means nothing.

---

## §1 TARGET — [EDITABLE]

    source_repository:  aaron-francis/whitakers-words
    source_module:      src/words_engine/words_engine-tricks.adb
    source_symbol:      Syncope
    source_language:    Ada
    test_convention:    golden-file, test/NN_name/{input.txt,expected.txt}
    test_command:       make test

## §2 POLICY — [LOCKED]

1. **Record behavior. Do not correct it.** If the code is wrong, the tests must
   encode the wrong behavior. A characterization test that asserts what the
   code *should* do is not a characterization test — it is a bug report wearing
   a costume, and it will fail for the wrong reason later.

2. **The implementation is the specification.** Not the comments, not the
   documentation. Where code and comments disagree, the code wins and you
   report the disagreement prominently.

3. **Generate every expected output from the unmodified build.** Never
   hand-write an expected value. Never adjust an expected value to make a test
   pass.

4. **Tests land in their own commit, containing no production change**, with a
   message beginning `test:`. That commit must pass against the untouched
   implementation. This is the proof that the tests describe the code as found.

5. **Match the project's existing test convention.** Do not introduce a
   framework. A fielded legacy system is not the place to impose your
   preferences.

## §3 ACCEPTANCE CRITERIA — [EDITABLE]

1. Every distinct branch or code path in the target symbol has at least one
   test that exercises it.
2. At least one test covers the failure path, where the code finds no match and
   must leave state unchanged.
3. `<test_command>` passes, covering both new and pre-existing tests.
4. No production source file is modified.
5. The pull request states, in plain language, what the symbol does — written
   for a reader who has never seen the source language.

## §4 VERIFICATION — [MECHANICAL]

- `<test_command>` exits zero.
- The `test:` commit, checked out alone, also exits zero.

If the second check fails, your tests describe code you already changed, and
the work is invalid regardless of how good the tests look.

## §5 STOP CONDITIONS — [LOCKED]

Stop, open no pull request, and report `blocked` if:

- you cannot establish a green baseline before making changes;
- a behavior cannot be observed through the project's existing test convention
  — say which behavior and why, rather than pretending coverage you don't have;
- covering a path would require modifying production code;
- two requirements in §3 contradict each other or contradict §2 — name both.

Reporting a path you could not reach is a successful outcome. Silently omitting
it is the failure mode this section exists to prevent.

## §6 REPORTING

- `outcome` — `characterized` | `blocked`
- `pr_url`
- `tests_added` — integer
- `paths_covered` / `paths_uncovered` — with reasons for the second
- `spec_deviations` — where behavior contradicts the documented rules
- `blocked_reason` — when applicable
- `risks` — what a reviewer should check first
