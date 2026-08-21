/*
 * Migrated implementation of Words_Engine.Roman_Numerals_Package.Roman_Number,
 * transliterated statement for statement from
 * src/words_engine/words_engine-roman_numerals_package.adb.
 *
 * The Ada implementation is the specification. Its quirks are reproduced, not
 * corrected. See migration/NOTES.md.
 *
 * Contract (migration/CONTRACT.md): reads one candidate per line from stdin,
 * writes "<input>\t<result>" per line to stdout in the same order.
 */
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Roman {

    private Roman() {
    }

    /*
     * Ada.Characters.Handling.To_Upper over Latin-1: 'a'..'z' and the
     * lowercase Latin-1 letters 0xE0..0xFE except 0xF7 (division sign) fold to
     * upper case; 0xFF (small y with diaeresis) has no Latin-1 upper case and
     * is left alone. Character.toUpperCase would map it to U+0178.
     */
    static char upperCase(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 32);
        }
        if (c >= 0xE0 && c <= 0xFE && c != 0xF7) {
            return (char) (c - 32);
        }
        return c;
    }

    static String upperCase(String s) {
        char[] out = new char[s.length()];
        for (int i = 0; i < s.length(); i++) {
            out[i] = upperCase(s.charAt(i));
        }
        return new String(out);
    }

    static boolean aRomanDigit(char c) {
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

    /*
     * Ada raises the local exception Invalid, whose handler returns 0; here
     * that is a direct "return 0". Ada's "exit Evaluate when J < S'First"
     * becomes "break evaluate" on j < 0, with j the 0-based index of S'Last
     * counting down.
     */
    static int romanNumber(String st) {
        int total = 0;
        final String s = upperCase(st);

        if (onlyRomanDigits(s)) {
            int j = s.length() - 1;

            evaluate:
            while (j >= 0) {

                //  Ones
                if (s.charAt(j) == 'I') {
                    total = total + 1;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    while (s.charAt(j) == 'I') {
                        total = total + 1;
                        if (total >= 5) {
                            return 0;
                        }
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                }

                if (s.charAt(j) == 'V') {
                    total = total + 5;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    if (s.charAt(j) == 'I' && total == 5) {
                        total = total - 1;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }

                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V') {
                        return 0;
                    }
                }

                //  Tens
                if (s.charAt(j) == 'X') {
                    total = total + 10;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    while (s.charAt(j) == 'X') {
                        total = total + 10;
                        if (total >= 50) {
                            return 0;
                        }
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }

                    if (s.charAt(j) == 'I' && total == 10) {
                        total = total - 1;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }

                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V') {
                        return 0;
                    }
                }

                if (s.charAt(j) == 'L') {
                    total = total + 50;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }

                    if (s.charAt(j) == 'X' && total <= 59) {
                        total = total - 10;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }

                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V'
                        || s.charAt(j) == 'X' || s.charAt(j) == 'L') {
                        return 0;
                    }

                    if (s.charAt(j) == 'C') {
                        total = total + 100;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                        if (s.charAt(j) == 'X' && total == 100) {
                            total = total - 10;
                            j = j - 1;
                            if (j < 0) {
                                break evaluate;
                            }
                        }
                    }

                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V'
                        || s.charAt(j) == 'X' || s.charAt(j) == 'L') {
                        return 0;
                    }
                }

                if (s.charAt(j) == 'C') {
                    total = total + 100;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    while (s.charAt(j) == 'C') {
                        total = total + 100;
                        if (total >= 500) {
                            return 0;
                        }
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'X' && total <= 109) {
                        total = total - 10;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V'
                        || s.charAt(j) == 'X' || s.charAt(j) == 'L') {
                        return 0;
                    }
                }

                if (s.charAt(j) == 'D') {
                    total = total + 500;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    if (s.charAt(j) == 'C' && total <= 599) {
                        total = total - 100;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'M') {
                        total = total + 1000;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'C' && total <= 1099) {
                        total = total - 100;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V'
                        || s.charAt(j) == 'X' || s.charAt(j) == 'L'
                        || s.charAt(j) == 'C' || s.charAt(j) == 'D') {
                        return 0;
                    }
                }

                if (s.charAt(j) == 'M') {
                    total = total + 1000;
                    j = j - 1;
                    if (j < 0) {
                        break evaluate;
                    }
                    while (s.charAt(j) == 'M') {
                        total = total + 1000;
                        if (total >= 5000) {
                            return 0;
                        }
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'C' && total <= 1099) {
                        total = total - 100;
                        j = j - 1;
                        if (j < 0) {
                            break evaluate;
                        }
                    }
                    if (s.charAt(j) == 'I' || s.charAt(j) == 'V'
                        || s.charAt(j) == 'X' || s.charAt(j) == 'L'
                        || s.charAt(j) == 'C' || s.charAt(j) == 'D') {
                        return 0;
                    }
                }
            }
        }

        return total;
    }

    public static void main(String[] args) throws IOException {
        final InputStream in = System.in;
        final OutputStream out = new BufferedOutputStream(System.out, 1 << 20);
        final byte[] buf = new byte[1 << 16];
        final StringBuilder line = new StringBuilder();
        int n;
        boolean pending = false;

        while ((n = in.read(buf)) > 0) {
            for (int i = 0; i < n; i++) {
                final int b = buf[i] & 0xFF;
                if (b == '\n') {
                    emit(out, line.toString());
                    line.setLength(0);
                    pending = false;
                } else if (b != '\r') {
                    line.append((char) b);
                    pending = true;
                }
            }
        }
        if (pending) {
            emit(out, line.toString());
        }
        out.flush();
    }

    private static void emit(OutputStream out, String input) throws IOException {
        /*
         * Bytes were read as Latin-1 so that each byte is one Ada Character;
         * echo them back unchanged.
         */
        out.write(input.getBytes("ISO-8859-1"));
        out.write('\t');
        out.write(Integer.toString(romanNumber(input)).getBytes("ISO-8859-1"));
        out.write('\n');
    }
}
