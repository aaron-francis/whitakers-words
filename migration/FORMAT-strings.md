# Line format for the Strings_Package migration

This document defines the `<input>` and `<result>` fields of
`migration/CONTRACT.md` for the migration of
`Latin_Utils.Strings_Package.Lower_Case`, `Upper_Case`, `Trim` and `Head`.
Both implementations — the Ada reference and the Java candidate — implement
this format identically; it is transport, not behavior.

## Input line

    <symbol>|<arg>[|<arg>]

| symbol       | fields | second argument                          |
|--------------|--------|------------------------------------------|
| `Lower_Case` | 2      | —                                        |
| `Upper_Case` | 2      | —                                        |
| `Trim`       | 2 or 3 | `Left` \| `Right` \| `Both`; absent means the Ada default `Both` |
| `Head`       | 3      | decimal `Natural`, `0 .. 100000`         |

The first argument is the `Source` string, escape-encoded (below). The second
argument is a bare keyword or number and is not encoded.

Anything else — unknown symbol, wrong field count, unknown `Side`, a `Count`
that is not a decimal number in range, or a malformed escape — produces the
result `!ERROR`. This is the transport's error signal, not legacy behavior;
both sides implement it identically and the corpus exercises it.

## Escape encoding

Arguments and results are encoded so that a line contains no raw whitespace
and no raw `|`:

| character           | encoded |
|---------------------|---------|
| `\` (0x5C)          | `\\`    |
| `|` (0x7C)          | `\|`    |
| space (0x20)        | `\s`    |
| tab (0x09)          | `\t`    |
| LF (0x0A)           | `\n`    |
| VT (0x0B)           | `\v`    |
| FF (0x0C)           | `\f`    |
| CR (0x0D)           | `\r`    |

Every other character is literal. The encoding is total and injective over the
alphabet in use, so the full alphabet — printable ASCII **plus all five ASCII
whitespace controls** — is covered, including characters that could not
otherwise survive a line-oriented protocol.

Why encode at all, rather than putting raw strings on the line:

1. `CONTRACT.md` requires inputs with no leading or trailing whitespace, and
   `compare.py` discards lines that are whitespace-only. Leading and trailing
   whitespace is precisely what `Trim` is about, and the padding `Head` adds is
   trailing whitespace, so raw arguments would make the interesting half of the
   domain unreachable.
2. `compare.py` reads process output with `str.splitlines()` under Python's
   universal-newline translation. A raw LF, CR, VT or FF in a result would be
   read as a line break and the run would abort as a contract violation. The
   only alternatives are to encode, or to modify the harness — and modifying
   the harness is forbidden by §4 of the playbook.

## Output line

    <input><TAB><result>

`<input>` is echoed verbatim. `<result>` is the encoded return value of the
symbol, or `!ERROR`.
