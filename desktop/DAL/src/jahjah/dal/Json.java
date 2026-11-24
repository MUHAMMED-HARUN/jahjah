package jahjah.dal;

import jahjah.shared.exceptions.DataAccessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * قارئ/كاتب JSON بسيط بلا مكتبات خارجية.
 * object → LinkedHashMap, array → ArrayList, number → Long/Double, string, boolean, null.
 */
final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    static Object parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("بيانات زائدة بعد نهاية JSON");
        return v;
    }

    private Object value() {
        if (i >= s.length()) throw err("نهاية غير متوقعة");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        i++;
        ws();
        if (peek() == '}') { i++; return map; }
        while (true) {
            ws();
            String key = string();
            ws();
            expect(':');
            ws();
            map.put(key, value());
            ws();
            char c = next();
            if (c == ',') continue;
            if (c == '}') return map;
            throw err("متوقع ',' أو '}'");
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++;
        ws();
        if (peek() == ']') { i++; return list; }
        while (true) {
            ws();
            list.add(value());
            ws();
            char c = next();
            if (c == ',') continue;
            if (c == ']') return list;
            throw err("متوقع ',' أو ']'");
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = next();
            switch (e) {
                case '"': case '\\': case '/': sb.append(e); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw err("هروب unicode ناقص");
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: throw err("هروب غير معروف \\" + e);
            }
        }
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String t = s.substring(start, i);
        if (t.isEmpty()) throw err("قيمة غير متوقعة");
        try {
            if (t.contains(".") || t.contains("e") || t.contains("E")) return Double.parseDouble(t);
            return Long.parseLong(t);
        } catch (NumberFormatException ex) {
            try { return Double.parseDouble(t); } catch (NumberFormatException ex2) { throw err("رقم غير صالح: " + t); }
        }
    }

    private void literal(String word) {
        if (!s.startsWith(word, i)) throw err("قيمة غير متوقعة");
        i += word.length();
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private char peek() {
        if (i >= s.length()) throw err("نهاية غير متوقعة");
        return s.charAt(i);
    }

    private char next() {
        char c = peek();
        i++;
        return c;
    }

    private void expect(char c) {
        if (next() != c) throw err("متوقع '" + c + "'");
    }

    private DataAccessException err(String msg) {
        int line = 1;
        for (int k = 0; k < Math.min(i, s.length()); k++) if (s.charAt(k) == '\n') line++;
        return new DataAccessException("JSON غير صالح (سطر " + line + "): " + msg);
    }

    // ------------------------------------------------------------------ writer

    static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object v, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String str) {
            quote(sb, str);
        } else if (v instanceof Boolean b) {
            sb.append(b);
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append((long) d);
            else sb.append(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString());
        } else if (v instanceof BigDecimal bd) {
            sb.append(bd.stripTrailingZeros().toPlainString());
        } else if (v instanceof Number n) {
            sb.append(n.longValue());
        } else if (v instanceof Map<?, ?> map) {
            if (map.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int k = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                pad(sb, indent + 2);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                write(sb, e.getValue(), indent + 2);
                if (++k < map.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append('}');
        } else if (v instanceof Collection<?> list) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            int k = 0;
            for (Object item : list) {
                pad(sb, indent + 2);
                write(sb, item, indent + 2);
                if (++k < list.size()) sb.append(',');
                sb.append('\n');
            }
            pad(sb, indent);
            sb.append(']');
        } else {
            quote(sb, v.toString());
        }
    }

    private static void pad(StringBuilder sb, int n) {
        sb.append(" ".repeat(n));
    }

    private static void quote(StringBuilder sb, String str) {
        sb.append('"');
        for (int k = 0; k < str.length(); k++) {
            char c = str.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ helpers للقراءة

    static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    static Double num(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String t) {
            try { return Double.parseDouble(t.trim().replace(",", "")); } catch (NumberFormatException ex) { return null; }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object v, String what) {
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new DataAccessException(what + ": متوقع كائن JSON");
    }

    @SuppressWarnings("unchecked")
    static List<Object> asArray(Object v, String what) {
        if (v instanceof List<?> l) return (List<Object>) l;
        throw new DataAccessException(what + ": متوقع مصفوفة JSON");
    }
}
