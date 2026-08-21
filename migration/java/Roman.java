import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Migrated implementation of Words_Engine.Roman_Numerals_Package.Roman_Number,
 * transcribed from the Ada legacy source in
 * src/words_engine/words_engine-roman_numerals_package.adb.
 *
 * Speaks the oracle contract in migration/CONTRACT.md: reads one candidate per
 * line from stdin and writes "<input><TAB><value>" per line to stdout, where
 * value is the decimal value of the numeral or 0 if it is not well formed by
 * the legacy rules.
 *
 * The legacy implementation is the specification. Where its behavior departs
 * from the rules stated in its own comments, this port reproduces the
 * behavior, not the comments.
 */
public final class Roman {

    /** Ada's Invalid exception: control flow, so no stack trace needed. */
    private static final class Invalid extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Invalid() {
            super(null, null, false, false);
        }
    }

    private static final Invalid INVALID = new Invalid();

    private Roman() {
    }

    /** Ada.Characters.Handling.To_Upper, which is defined over Latin-1. */
    private static char upperCase(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 32);
        }
        if (c >= 0xE0 && c <= 0xFE && c != 0xF7) {
            return (char) (c - 32);
        }
        return c;
    }

    private static String upperCase(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            b.append(upperCase(s.charAt(i)));
        }
        return b.toString();
    }

    private static boolean aRomanDigit(char c) {
        switch (c) {
            case 'M': case 'm': case 'D': case 'd': case 'C': case 'c':
            case 'L': case 'l': case 'X': case 'x': case 'V': case 'v':
            case 'I': case 'i':
                return true;
            default:
                return false;
        }
    }

    static boolean onlyRomanDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!aRomanDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Total is an Ada Natural: driving it below zero raises Constraint_Error,
     * which the legacy exception handler turns into 0.
     */
    private static int natural(int value) {
        if (value < 0) {
            throw INVALID;
        }
        return value;
    }

    static int romanNumber(String st) {
        String s = upperCase(st);
        final int first = 1;
        final int last = s.length();
        int total = 0;
        int j;

        if (!onlyRomanDigits(s)) {
            return total;
        }

        try {
            j = last;
            evaluate:
            while (j >= first) {
                //  Ones
                if (at(s, j) == 'I') {
                    total = natural(total + 1);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    while (at(s, j) == 'I') {
                        total = natural(total + 1);
                        if (total >= 5) {
                            throw INVALID;
                        }
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                }

                if (at(s, j) == 'V') {
                    total = natural(total + 5);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    if (at(s, j) == 'I' && total == 5) {
                        total = natural(total - 1);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'I' || at(s, j) == 'V') {
                        throw INVALID;
                    }
                }

                //  Tens
                if (at(s, j) == 'X') {
                    total = natural(total + 10);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    while (at(s, j) == 'X') {
                        total = natural(total + 10);
                        if (total >= 50) {
                            throw INVALID;
                        }
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }

                    if (at(s, j) == 'I' && total == 10) {
                        total = natural(total - 1);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }

                    if (at(s, j) == 'I' || at(s, j) == 'V') {
                        throw INVALID;
                    }
                }

                if (at(s, j) == 'L') {
                    total = natural(total + 50);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }

                    if (at(s, j) == 'X' && total <= 59) {
                        total = natural(total - 10);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }

                    if (at(s, j) == 'I' || at(s, j) == 'V'
                            || at(s, j) == 'X' || at(s, j) == 'L') {
                        throw INVALID;
                    }

                    if (at(s, j) == 'C') {
                        total = natural(total + 100);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                        if (at(s, j) == 'X' && total == 100) {
                            total = natural(total - 10);
                            j = j - 1;
                            if (j < first) {
                                break evaluate;
                            }
                        }
                    }

                    if (at(s, j) == 'I' || at(s, j) == 'V'
                            || at(s, j) == 'X' || at(s, j) == 'L') {
                        throw INVALID;
                    }
                }

                if (at(s, j) == 'C') {
                    total = natural(total + 100);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    while (at(s, j) == 'C') {
                        total = natural(total + 100);
                        if (total >= 500) {
                            throw INVALID;
                        }
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'X' && total <= 109) {
                        total = natural(total - 10);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'I' || at(s, j) == 'V'
                            || at(s, j) == 'X' || at(s, j) == 'L') {
                        throw INVALID;
                    }
                }

                if (at(s, j) == 'D') {
                    total = natural(total + 500);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    if (at(s, j) == 'C' && total <= 599) {
                        total = natural(total - 100);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'M') {
                        total = natural(total + 1000);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'C' && total <= 1099) {
                        total = natural(total - 100);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'I' || at(s, j) == 'V' || at(s, j) == 'X'
                            || at(s, j) == 'L' || at(s, j) == 'C'
                            || at(s, j) == 'D') {
                        throw INVALID;
                    }
                }

                if (at(s, j) == 'M') {
                    total = natural(total + 1000);
                    j = j - 1;
                    if (j < first) {
                        break evaluate;
                    }
                    while (at(s, j) == 'M') {
                        total = natural(total + 1000);
                        if (total >= 5000) {
                            throw INVALID;
                        }
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'C' && total <= 1099) {
                        total = natural(total - 100);
                        j = j - 1;
                        if (j < first) {
                            break evaluate;
                        }
                    }
                    if (at(s, j) == 'I' || at(s, j) == 'V' || at(s, j) == 'X'
                            || at(s, j) == 'L' || at(s, j) == 'C'
                            || at(s, j) == 'D') {
                        throw INVALID;
                    }
                }
            }
        } catch (Invalid e) {
            return 0;
        }

        return total;
    }

    /** S (J) for an Ada string whose first index is 1. */
    private static char at(String s, int j) {
        return s.charAt(j - 1);
    }

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                1 << 20);
        String line;
        while ((line = in.readLine()) != null) {
            out.write(line);
            out.write('\t');
            out.write(Integer.toString(romanNumber(line)));
            out.write('\n');
        }
        out.flush();
    }
}
