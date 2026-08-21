/*
 * Migrated implementation of Latin_Utils.Strings_Package.Lower_Case,
 * Upper_Case, Trim and Head.
 *
 * Behavior is copied from the Ada implementation, not from the comments in
 * the Ada spec. In particular:
 *
 *   - Trim removes only the space character (0x20). Ada.Strings.Fixed.Trim
 *     pads/trims with Ada.Strings.Space, so tabs, CR, LF, VT and FF are NOT
 *     trimmed. Java's String.trim()/strip() would remove them, which is why
 *     neither is used here.
 *   - Head pads with the space character and truncates from the left,
 *     returning a string of exactly Count characters.
 *   - Lower_Case/Upper_Case are Ada.Characters.Handling.To_Lower/To_Upper,
 *     which are Latin-1 mappings, not locale-aware Unicode mappings.
 *
 * Conforms to migration/CONTRACT.md; see migration/FORMAT-strings.md for the
 * line format. Standard library only.
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class Strings {

    private static final String ERROR_RESULT = "!ERROR";
    private static final int MAX_COUNT = 100_000;

    // ---- legacy behavior -------------------------------------------------

    /** Ada.Characters.Handling.To_Lower (Latin-1). */
    private static char lowerCase(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        }
        // Latin-1 upper case letters, excluding 0xD7 (multiplication sign).
        // Unreachable in the verified domain (printable ASCII plus whitespace).
        if (c >= 0xC0 && c <= 0xDE && c != 0xD7) {
            return (char) (c + 32);
        }
        return c;
    }

    /** Ada.Characters.Handling.To_Upper (Latin-1). */
    private static char upperCase(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 32);
        }
        // Latin-1 lower case letters, excluding 0xF7 (division sign) and
        // 0xDF/0xFF which have no Latin-1 upper case form.
        // Unreachable in the verified domain.
        if (c >= 0xE0 && c <= 0xFE && c != 0xF7) {
            return (char) (c - 32);
        }
        return c;
    }

    private static String lowerCase(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            out[i] = lowerCase(out[i]);
        }
        return new String(out);
    }

    private static String upperCase(String s) {
        char[] out = s.toCharArray();
        for (int i = 0; i < out.length; i++) {
            out[i] = upperCase(out[i]);
        }
        return new String(out);
    }

    private enum Side { LEFT, RIGHT, BOTH }

    /** Ada.Strings.Fixed.Trim: removes leading/trailing spaces only. */
    private static String trim(String source, Side side) {
        int first = 0;
        int last = source.length() - 1;
        if (side == Side.LEFT || side == Side.BOTH) {
            while (first <= last && source.charAt(first) == ' ') {
                first++;
            }
        }
        if (side == Side.RIGHT || side == Side.BOTH) {
            while (last >= first && source.charAt(last) == ' ') {
                last--;
            }
        }
        return source.substring(first, last + 1);
    }

    /** Ada.Strings.Fixed.Head (Source, Count, ' '). */
    private static String head(String source, int count) {
        if (count <= source.length()) {
            return source.substring(0, count);
        }
        StringBuilder sb = new StringBuilder(count);
        sb.append(source);
        for (int i = source.length(); i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ---- line protocol ---------------------------------------------------

    private static String decode(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                if (i + 1 >= text.length()) {
                    return null;
                }
                char e = text.charAt(i + 1);
                switch (e) {
                    case '\\': sb.append('\\'); break;
                    case '|':  sb.append('|');  break;
                    case 's':  sb.append(' ');  break;
                    case 't':  sb.append('\t'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'v':  sb.append((char) 11); break;
                    case 'f':  sb.append('\f'); break;
                    case 'r':  sb.append('\r'); break;
                    default:   return null;
                }
                i += 2;
            } else if (c == ' ' || c == '\t') {
                return null; // raw whitespace is not a legal encoding
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String encode(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '|':  sb.append("\\|");  break;
                case ' ':  sb.append("\\s");  break;
                case '\t': sb.append("\\t");  break;
                case '\n': sb.append("\\n");  break;
                case 11:   sb.append("\\v");  break;
                case '\f': sb.append("\\f");  break;
                case '\r': sb.append("\\r");  break;
                default:   sb.append(c);      break;
            }
        }
        return sb.toString();
    }

    /** Splits into at most 4 fields so that over-long lines are rejected. */
    private static String[] split(String line) {
        String[] fields = new String[] {"", "", "", ""};
        int n = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '|') {
                fields[n] = current.toString();
                current.setLength(0);
                n++;
                if (n > 3) {
                    return new String[] {fields[0], fields[1], fields[2], "4"};
                }
            } else {
                current.append(c);
            }
        }
        fields[n] = current.toString();
        return new String[] {fields[0], fields[1], fields[2],
                             Integer.toString(n + 1)};
    }

    private static int parseCount(String text) {
        if (text.isEmpty()) {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
            if (value > MAX_COUNT) {
                return -1;
            }
        }
        return value;
    }

    private static String evaluate(String line) {
        String[] parts = split(line);
        int n = Integer.parseInt(parts[3]);
        if (n < 2) {
            return ERROR_RESULT;
        }
        String source = decode(parts[1]);
        if (source == null) {
            return ERROR_RESULT;
        }
        String symbol = parts[0];
        String extra = parts[2];

        if (symbol.equals("Lower_Case")) {
            return n == 2 ? encode(lowerCase(source)) : ERROR_RESULT;
        }
        if (symbol.equals("Upper_Case")) {
            return n == 2 ? encode(upperCase(source)) : ERROR_RESULT;
        }
        if (symbol.equals("Trim")) {
            if (n == 2) {
                return encode(trim(source, Side.BOTH)); // Ada default: Both
            }
            if (n == 3) {
                if (extra.equals("Left")) {
                    return encode(trim(source, Side.LEFT));
                }
                if (extra.equals("Right")) {
                    return encode(trim(source, Side.RIGHT));
                }
                if (extra.equals("Both")) {
                    return encode(trim(source, Side.BOTH));
                }
            }
            return ERROR_RESULT;
        }
        if (symbol.equals("Head")) {
            if (n != 3) {
                return ERROR_RESULT;
            }
            int count = parseCount(extra);
            return count < 0 ? ERROR_RESULT : encode(head(source, count));
        }
        return ERROR_RESULT;
    }

    public static void main(String[] args) throws IOException {
        InputStream in = System.in;
        OutputStream out = System.out;
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        StringBuilder sink = new StringBuilder();
        byte[] buffer = new byte[1 << 16];
        boolean pending = false;
        int read;
        while ((read = in.read(buffer)) != -1) {
            for (int i = 0; i < read; i++) {
                byte b = buffer[i];
                if (b == '\n') {
                    emit(sink, line);
                    pending = false;
                } else {
                    line.write(b);
                    pending = true;
                }
            }
            if (sink.length() > (1 << 20)) {
                out.write(sink.toString().getBytes(StandardCharsets.ISO_8859_1));
                sink.setLength(0);
            }
        }
        if (pending) {
            emit(sink, line);
        }
        out.write(sink.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
    }

    private static void emit(StringBuilder sink, ByteArrayOutputStream line) {
        String text = new String(line.toByteArray(), StandardCharsets.ISO_8859_1);
        line.reset();
        sink.append(text).append('\t').append(evaluate(text)).append('\n');
    }
}
