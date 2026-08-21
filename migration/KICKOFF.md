# Kickoff cards

Paste one of these into a new Devin session. The policy lives in the repo, so
the paste only names the target — and anything the room asks to add.

---

## Characterize

```
Repository: aaron-francis/whitakers-words, branch master.

Read migration/PROMPT-characterize.md and follow it exactly.

§1 TARGET:
  source_module:   src/words_engine/words_engine-roman_numerals_package.adb
  source_symbol:   Roman_Number
  source_language: Ada
  test_convention: golden-file, test/NN_name/{input.txt,expected.txt}
  test_command:    make test
```

---

## Port

```
Repository: aaron-francis/whitakers-words, branch master.

Read migration/PROMPT.md and migration/CONTRACT.md and follow them exactly.

§1 TARGET:
  source_module:      src/words_engine/words_engine-roman_numerals_package.adb
  source_symbol:      Roman_Number
  source_language:    Ada
  target_language:    Java
  target_location:    migration/java/
  verification_mode:  exhaustive
  alphabet:           MDCLXVI
  max_length:         7
```

Change `target_language` to Go, Rust, or C++ and everything else still holds —
the contract is a process boundary, not a language binding.

---

