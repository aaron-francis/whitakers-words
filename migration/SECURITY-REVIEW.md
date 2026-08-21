# Security review — `Roman_Number`, Ada to Java migration

Per `migration/PROMPT-security.md`. **Report only: nothing in the migrated
implementation was changed.** Every finding below carries a recommendation for
a human, not a patch.

    review_branch:      devin/1787277015-roman-numeral-java-migration
    migrated_code:      migration/java/Roman.java
    reference:          migration/ada/roman_oracle.adb (+ unmodified legacy body)
    equivalence_claim:  proven over MDCLXVI, length <= 7 (960,799 inputs)
    outcome:            reviewed
    findings:           6
    highest_severity:   high (F1)

Toolchain used for every reproduction: GNAT 10.5.0, gprbuild 18.0w,
javac/java 17.0.13, default max heap 2,086,666,240 bytes. Reference is
`migration/ada/roman_oracle`; candidate is `java -cp migration/java/classes
Roman`. Both were built exactly as `migration/verify.sh` builds them.

---

## What the equivalence proof does and does not cover

`enumerate.py exhaustive --alphabet MDCLXVI --max-len 7` emits every string
over the seven upper-case digits of length 1..7, and `compare.py` additionally
drops blank lines (`if ln.strip()`). So the proof covers upper-case,
well-formed-alphabet, short, single-line input and nothing else. Outside it,
and therefore invisible to the report of zero divergences:

- the empty string (dropped by the harness before comparison);
- lower-case and mixed case — the entire hand-written `upperCase` path;
- every byte that is not `MDCLXVI` — the entire `onlyRomanDigits == false`
  early-out, i.e. the whole reject path;
- any input of length 8 or more;
- control bytes, whitespace, CR, NUL, non-ASCII and UTF-8;
- resource behavior: line length, input volume, memory;
- process-level failure modes and exit status.

I probed all of these directly. Most of them agree byte for byte (table under
F6). Two areas do not: carriage returns (F1) and resource limits (F2, F3).

---

## F1 — `\r` is stripped from anywhere in the line, changing both the value and the echoed input — **high**

**Severity justification:** on input a fielded system will plausibly receive —
any CRLF-terminated file — the candidate silently returns a valid numeral where
the reference returned 0, and it echoes bytes that differ from the bytes it was
given, so a downstream consumer cannot detect the substitution.

**Reproduction**

    $ printf 'MI\r\n' | migration/ada/roman_oracle | od -c
    0000000   M   I  \r  \t   0  \n                     (6 bytes)
    $ printf 'MI\r\n' | java -cp migration/java/classes Roman | od -c
    0000000   M   I  \t   1   0   0   1  \n              (8 bytes)

    $ printf 'M\rI\n' | migration/ada/roman_oracle     ->  M \r I \t 0 \n
    $ printf 'M\rI\n' | java -cp .../classes Roman     ->  M I \t 1 0 0 1 \n

Both sides exit 0. `0` versus `1001`, on a two-character numeral.

**Mechanism.** `Roman.java:305` — `} else if (b != '\r') {` — drops every
`0x0D` byte in the line, not just one before the terminating `0x0A`. Ada's
`Ada.Text_IO.Get_Line` treats only the line terminator as a terminator and
keeps `\r` as an ordinary `Character`, so `Only_Roman_Digits` sees a non-digit
and `Roman_Number` returns 0. The stripping also makes `Roman.java:322`
(`out.write(input.getBytes("ISO-8859-1"))`) echo the *stripped* line, which
violates `migration/CONTRACT.md`'s `<input><TAB><result>` requirement: the
first field is no longer the input.

**Recommendation.** Decide the CR policy explicitly and write it down, then
re-run the harness with CR inside the alphabet so the decision is covered by
the proof. Note that neither current behavior is obviously right — the Ada
side rejects every numeral in a CRLF file, the Java side accepts every one of
them — which is precisely why this needs a human decision rather than a quiet
fix.

## F2 — the single-line resource ceiling is raised roughly 80x, and exhaustion emits a truncated record — **medium**

**Severity justification:** it converts input the reference refused to touch
into either a successful answer or a corrupt output record, and the memory
cost per hostile byte is high enough (~6.7x) that one line can exhaust the
default heap.

**Reproduction** (single line of `M` bytes, plus LF)

| line length | reference | candidate |
|---|---|---|
| 1,000,000 | exit 0, value 0 | exit 0, value 0 |
| 5,000,000 | **exit 1**, `raised STORAGE_ERROR : stack overflow or erroneous memory access`, 0 bytes of stdout | exit 0, value 0 |
| 10,000,000 | exit 1, `STORAGE_ERROR`, 0 bytes stdout, peak RSS 12 MB | exit 0, value 0, peak RSS 101,688 kB |
| 50,000,000 | exit 1, `STORAGE_ERROR` | exit 0, value 0, peak RSS 327,236 kB (**6.7 bytes of RSS per input byte**) |
| 400,000,000 | exit 1, `STORAGE_ERROR` | **exit 1**, `java.lang.OutOfMemoryError: Java heap space` at `Roman.upperCase(Roman.java:43)`, after writing 400,000,000 bytes of a record with no TAB, no value and no LF |

**Mechanism.** The reference is bounded by GNAT's `Get_Line` recursion
(`Get_Rest` allocates its buffer on the stack, so a long line becomes
`STORAGE_ERROR` at a few MB). The candidate has no bound at all: `Roman.java:294`
accumulates the line in a `StringBuilder` (2 bytes per input byte), `line.toString()`
at `:302` copies it, `upperCase` at `:39` allocates a second `char[]` of the same
length and `:43` copies it again, and `emit` at `:322` produces yet another byte
array — hence the measured ~6.7x. Because `emit` writes the echoed input
(`:322`) *before* computing the value (`:324`), a failure inside `romanNumber`
leaves a half-written record on stdout: at 400 MB the candidate emitted
400,000,000 bytes with no terminator, which a line-oriented consumer will
either treat as an unterminated record or concatenate with whatever follows.

Buffered results are *not* lost: with 100,000 good lines ahead of the hostile
one, both sides emitted all 100,000 complete records (the 1 MB
`BufferedOutputStream` at `:292` had long since flushed). Multi-line volume is
also fine: 2,000,000 lines, both sides exit 0 (candidate 0.40 s / 127 MB RSS,
reference 1.29 s / 4 MB RSS).

**Recommendation.** Set an explicit maximum line length at the input boundary
and reject beyond it, rather than inheriting a limit from whichever runtime is
underneath; and if the truncated-record behavior matters to the consumer,
compute the value before writing any bytes. Both are behavior changes and so
must be made deliberately, outside the migration.

## F3 — failure-mode inversion: where the reference aborted, the candidate returns a value — **medium**

**Severity justification:** a caller that relied on a non-zero exit to reject a
batch now receives a well-formed `0` for the same input and accepts it.

**Reproduction.** A single line of 5,000,000 `M` bytes: reference exits 1 with
no stdout at all; candidate exits 0 and prints `<5,000,000 bytes>\t0`. With the
hostile line *first* and 100,000 good lines after it (Probe 3), both sides
abandon the remaining 100,000 lines, but they do so with different visible
state: the reference emitted 0 bytes, the candidate emitted 400,000,000.

**Mechanism.** Ada's `Roman_Number` catches `Constraint_Error` and returns 0
(`words_engine-roman_numerals_package.adb`, `exception when Constraint_Error =>
return 0`), but `Storage_Error` from `Get_Line` is raised in the *driver*, is
not handled by `roman_oracle.adb` (which handles only `End_Error`), and
terminates the process. Java has no analogue of either: `main` declares `throws
IOException` and catches nothing, so anything unexpected leaves the process via
an uncaught exception, while everything the reference would have refused is
answered normally.

**Recommendation.** Decide, at the call site, whether "value 0" and "process
refused the input" are allowed to be the same outcome. Under
`migration/CONTRACT.md` they are not: exit 0 asserts every line was processed.

## F4 — the runtime checks the source language performed are gone; safety now rests on an argument, which I supply — **low**

**Severity justification:** low because I can show the checks cannot fire on any
input today, but the loss is real and permanent: the code no longer carries the
mechanism that made it safe.

Concretely, what Ada provided and Java does not:

1. **`Total : Natural`** — every `Total := Total + n` is a range-checked
   assignment into a subtype with a lower bound of 0 and an upper bound of
   `Integer'Last`, and the function catches the resulting `Constraint_Error` and
   returns 0. `Roman.java:73` is a plain `int total`: an overflow wraps to a
   negative number silently and the negative would be printed as the answer.
2. **Index checks.** Every `S (J)` in Ada is a checked index, and its
   `Constraint_Error` is caught by the same handler and turned into 0. The 49
   `s.charAt(j)` calls in Java throw `StringIndexOutOfBoundsException`, uncaught,
   which kills the process mid-stream and — see F2 — can do so after a partial
   record has been written.
3. **Arithmetic overflow.** Ada raises on overflow of the base type; Java wraps
   (JLS 15.18.2: integer addition "never indicate[s] overflow").

**Reachability — the checks cannot fire today.** `total` is bounded well below
`Integer.MAX_VALUE` for input of *any* length, by this argument: for the outer
loop to reach the top again with `j >= 0`, control must pass the `M` region at
`:255-283`. If the `M` branch ran, its repetition loop consumed every `M`, so
the only way past the trailing rejects at `:278-281` is the `C` subtraction at
`:271`, which requires `total <= 1099` and then subtracts 100 — so the total at
the loop boundary is at most 999. If the `M` branch did not run, the same
reasoning applied to `D`, `C`, `L` and `X` leaves exactly two possibilities: the
`C` path via the `X` subtraction at `:208` (`total <= 109`, then -10, so at most
99) and the `X` path via the `I` subtraction at `:138` (`total == 10`, then -1,
so 9). Every loop iteration therefore starts at `total <= 999`, and a single
iteration can add at most a few thousand before one of the `>= 5`, `>= 50`,
`>= 500`, `>= 5000` guards returns 0. Empirically: exhaustive over `MDCLXVI` at
lengths 1..8 (6,725,600 inputs) gives max 4990 (`MMMMCMXC`) and no negative
result; at length 9 (40,353,607 inputs) max 4995 (`MMMMCMXCV`), no negative
result. `j` decreases monotonically and every decrement is followed by a `j < 0`
test before the next `charAt`, so the index cannot go out of range either.

**Recommendation.** Do not add checks now (that would break the equivalence
reference). Record the bound above as an invariant next to the code, and treat
any future edit to the thresholds, the digit set, or the index arithmetic as
requiring it to be re-derived — in Ada such an edit failed loudly, in Java it
will not.

## F5 — the *reference* over-emits a record for a final line whose length is 500·2^k — **informational (defect in the oracle, not the candidate)**

**Severity justification:** informational for the shipped code, because the
candidate is the side that is correct here; it matters because it means the
oracle is not trustworthy outside the verified length domain.

**Reproduction.** A file containing exactly L bytes of `M` and exactly one LF
(byte-verified: `wc -c` = L+1, one `\n`):

| L | 250 | 500 | 999 | 1000 | 1001 | 1500 | 2000 | 2500 | 3000 | 4000 | 6000 | 8000 | 16000 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| reference records | 1 | **2** | 1 | **2** | 1 | 1 | **2** | 1 | 1 | **2** | 1 | **2** | **2** |
| candidate records | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 1 |

The extra record is `\t0\n`: the reference reports an input line that was never
in the file. The lengths that trigger it are exactly 500, 1000, 2000, 4000,
8000, 16000. Content is irrelevant — a 1,000-byte line of `CMXCV` repeated 200
times behaves the same way (reference 1,006 bytes / 2 records, candidate 1,003
bytes / 1 record), which is how this was first noticed.

**Mechanism.** GNAT's `Ada.Text_IO.Get_Line` *function*
(`/usr/lib/gcc/x86_64-linux-gnu/10/adainclude/a-textio.adb`) is built on
`Get_Rest`, whose buffer is `String (1 .. Integer'Max (500, S'Length))` — so the
cumulative capacities are 500, 1000, 2000, 4000, ... When the line length is
exactly one of those, the `Get_Line` *procedure* fills the buffer without
consuming the line terminator, `Get_Rest` then finds `End_Of_File` true (Ada
`End_Of_File` is true when only terminators remain, RM A.10.3) and returns the
line early, leaving the terminator unread. The next `Get_Line` in
`roman_oracle.adb` consumes it and returns the empty string, which the driver
faithfully prints as another record.

**Recommendation.** Do not "fix" the candidate to match. If `max_length` is ever
raised past 7, or a corpus run includes long lines, this will show up as a
divergence that is in fact an oracle defect — flag it in the harness notes now
so the next run is not sent chasing the Java side. Under
`migration/CONTRACT.md` ("exactly one line to stdout per input line") the
reference violates the contract at these lengths.

## F6 — `StringBuilder` capacity is never released, so one long line inflates the process for its lifetime — **low**

**Severity justification:** low because it is bounded by the largest line
already accepted, but it converts a single hostile line into a permanent
footprint rather than a transient one.

**Mechanism (source line + language rule).** `Roman.java:303` resets the line
with `line.setLength(0)`, which per the `StringBuilder` specification changes
the length only; the backing `char[]` keeps its high-water capacity (there is no
`trimToSize` call). A process that accepts one 50,000,000-byte line therefore
holds ~100 MB of `char[]` for as long as it runs, even if every subsequent line
is 7 bytes. The Ada driver reallocates per line and returns to ~4 MB RSS.

**Recommendation.** Bound the line length at the boundary (same fix as F2);
that makes this moot.

---

## Inputs outside the verified domain that agree byte for byte

Reported because they are the parts of the migration most likely to be wrong by
hand — the Latin-1 case folding written out longhand in `upperCase` — and they
are not wrong. All exit 0 on both sides, identical stdout bytes:

| input | both sides emit |
|---|---|
| `mdclxvi`, `MdClXvI`, `iiii` | `1666`, `1666`, `4` |
| empty line | `\t0` |
| `MI` with no trailing newline | `MI\t1001` |
| zero-byte input | no output, exit 0 |
| `0xEC`, `0xF7`, `0xFF`, `0xE0` | `0` (the `0xFF` and `0xF7` special cases in `upperCase` behave as the comment claims) |
| UTF-8 `í`, U+2169 ROMAN NUMERAL TEN, `M`+NBSP+`I` | `0` |
| leading/trailing space, leading TAB | `0` |
| `M`+NUL+`I`, `M`+VT+`I`, `M`+FF+`I` | `0` |
| `MMMMD`×500, `M`×3000, `M`×1000+`D`, `MMMMCMXC`, `MMMMCMXCV` | `0`, `0`, `0`, `4990`, `4995` |

`Latin_Utils.Strings_Package.Upper_Case` is confirmed to be a rename of
`Ada.Characters.Handling.To_Upper` (`latin_utils-strings_package.adb:30-34`), so
the premise of the `upperCase` comment in `Roman.java:22-27` is accurate.

## Dependencies and provenance

**Candidate** — four imports, all `java.base` in the JDK standard library, all
`java.io`: `BufferedOutputStream`, `IOException`, `InputStream`, `OutputStream`.
Nothing outside the standard library, nothing from this repository, no
reflection, no dynamic class loading, no filesystem or network access, no
`System.exit`, no threads, no `Runtime.exec`, no serialization. Byte decoding is
pinned explicitly to `ISO-8859-1` rather than the platform default, so behavior
does not depend on `file.encoding` or the ambient locale.

**Reference** — `Ada.Strings.Fixed`, `Ada.Strings.Unbounded`, `Ada.Text_IO` from
the Ada standard library, plus the in-repo `Words_Engine.Roman_Numerals_Package`
and `Latin_Utils.Strings_Package`. The oracle project file pulls in
`words_engine.gpr`, i.e. the whole engine library, but only `Roman_Number` is
exercised.

**Legacy modification.** `git diff origin/master..HEAD -- src/` is exactly two
added lines: the `Roman_Number` declaration in
`src/words_engine/words_engine-roman_numerals_package.ads`. No package body was
touched. This is the seam `migration/PROMPT.md` §2.3 permits, and it changes no
behavior.

**Build inputs.** `migration/verify.sh` generates
`src/latin_utils/latin_utils-config.adb` from the checked-in `.in` template with
`sed`; the generated file is gitignored. No download, no package fetch, no
network access anywhere in the build or verification path.

## Considered and not reported as findings

- `while ((n = in.read(buf)) > 0)` at `:298` treats a 0-length read as
  end-of-input. `InputStream.read(byte[])` may not return 0 when the array
  length is non-zero, so this cannot truncate input for any conforming stream;
  listing it as a finding would be `speculative`.
- `compare.py` splits implementation output with `str.splitlines()`, which also
  breaks on `\r`, `\v`, `\f`, `\x1c`-`\x1e`, `\u2028` and `\u2029`. No input in
  the verified domain contains any of them, so this did not affect the reported
  result; it is a reason not to extend the alphabet without also reading F1.
- Java's silent `char`/byte truncation in `emit` cannot lose data, because every
  `char` in the line came from a single byte at `:306`.

## What a reviewer should check first

1. **F1.** Establish how input actually reaches this program in production. If
   it can ever be a CRLF file, F1 is live today and changes every value in it.
2. **F3 with the caller.** Decide whether `0` and "refused to process" are
   allowed to look the same to the consumer; the contract says they are not.
3. **F2's ceiling.** Pick a maximum line length deliberately instead of
   inheriting GNAT's stack limit or the JVM's heap.
4. **F4's bound.** Read the argument, and if you accept it, record it next to
   the code as the reason no overflow check is present.
5. **F5 before the next run.** If anyone raises `max_length`, they need to know
   the oracle over-emits at 500·2^k, or they will debug the wrong side.

## Reproducing this review

Build both sides exactly as `migration/verify.sh` does (it also generates
`src/latin_utils/latin_utils-config.adb`), then:

    gprbuild -p -P migration/ada/roman_oracle.gpr
    javac -d migration/java/classes migration/java/Roman.java

    # F1
    printf 'MI\r\n' | ./migration/ada/roman_oracle | od -c
    printf 'MI\r\n' | java -cp migration/java/classes Roman | od -c

    # F2 / F3   (needs ~2.5 GB free; the 400 MB case is expected to fail)
    python3 -c "import sys; sys.stdout.write('M'*5000000+'\n')" > /tmp/l5m
    ./migration/ada/roman_oracle < /tmp/l5m; echo "ada exit $?"
    java -cp migration/java/classes Roman < /tmp/l5m > /tmp/j5m
    echo "java exit $? bytes $(wc -c < /tmp/j5m)"    # expect: exit 0 bytes 5000003

    # F5
    for L in 999 1000 1001; do
      python3 -c "import sys;sys.stdout.write('M'*$L+'\n')" > /tmp/lf
      printf '%s: ada=%s java=%s\n' "$L" \
        "$(./migration/ada/roman_oracle < /tmp/lf | wc -l)" \
        "$(java -cp migration/java/classes Roman < /tmp/lf | wc -l)"
    done
