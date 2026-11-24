package jahjah.bal;

import java.util.Locale;

/** توحيد الأسماء للمقارنة (مسافات، حالة الأحرف). */
final class Names {
    private Names() { }

    static String key(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static boolean same(String a, String b) {
        return key(a).equals(key(b));
    }

    static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
