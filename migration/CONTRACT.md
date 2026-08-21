# The oracle contract

A migration is accepted when two independent implementations of the same
behavior cannot be told apart by this harness. Nothing else counts — not a
passing test suite, not a code review, not the agent's own confidence.

## What an implementation must be

An **implementation** is an executable. It:

1. reads candidate inputs from **stdin**, one per line, UTF-8, no leading or
   trailing whitespace;
2. writes exactly **one line to stdout per input line**, in the same order:

       <input><TAB><result>

3. writes nothing else to stdout. Diagnostics go to stderr and are ignored;
4. exits 0 if it processed every line, non-zero otherwise.

`<result>` is defined per module in the migration prompt. For
`Roman_Number` it is the decimal value of the numeral, or `0` if the input is
not a well-formed Roman numeral.

## The two verification modes

**Exhaustive.** Where the input domain is bounded, do not sample it. Enumerate
every string over the alphabet up to a given length and compare all of them.
For a 7-character alphabet at length 7 that is 960,799 inputs, which runs in
seconds. A migration verified this way is not tested — it is *proven* over
that domain.

**Corpus.** Where the domain is unbounded or the module is stateful,
exhaustiveness is impossible. Generate a large structured corpus, state the
sampling strategy explicitly, and report coverage honestly. A corpus result is
evidence, not proof, and must be labelled as such.
