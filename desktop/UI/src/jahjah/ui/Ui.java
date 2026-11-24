package jahjah.ui;

import jahjah.shared.exceptions.BusinessException;
import jahjah.shared.exceptions.DataAccessException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** أدوات مشتركة للواجهة + التحقق من البيانات (Data Validation). */
final class Ui {
    static final ComponentOrientation RTL = ComponentOrientation.RIGHT_TO_LEFT;
    static final List<String> IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

    private Ui() { }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "معلومة", JOptionPane.INFORMATION_MESSAGE);
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "خطأ", JOptionPane.ERROR_MESSAGE);
    }

    /** ينفذ عملية BAL ويعرض أخطاء القواعد أو القراءة/الكتابة برسالة مفهومة. */
    static boolean run(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (BusinessException ex) {
            error(parent, ex.getMessage());
        } catch (DataAccessException ex) {
            error(parent, "خطأ في البيانات:\n" + ex.getMessage());
        } catch (RuntimeException ex) {
            error(parent, "خطأ غير متوقع:\n" + ex);
        }
        return false;
    }

    // ------------------------------------------------------------- Data validation

    /** يعيد رسالة الخطأ أو null. */
    static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return "الحقل \"" + field + "\" مطلوب.";
        if (value.trim().length() > maxLength) return "الحقل \"" + field + "\" يجب ألا يتجاوز " + maxLength + " حرفاً.";
        return null;
    }

    static String optional(String value, String field, int maxLength) {
        if (value != null && value.trim().length() > maxLength) {
            return "الحقل \"" + field + "\" يجب ألا يتجاوز " + maxLength + " حرفاً.";
        }
        return null;
    }

    /** يحوّل نصاً إلى رقم غير سالب، أو يعيد null إن لم يكن صالحاً. */
    static Double nonNegativeNumber(String value) {
        if (value == null) return null;
        String t = toLatinDigits(value.trim()).replace(",", "");
        if (t.isEmpty()) return null;
        try {
            double d = Double.parseDouble(t);
            if (Double.isNaN(d) || Double.isInfinite(d) || d < 0) return null;
            return d;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String imageFile(Path p) {
        if (p == null) return null;
        if (!Files.isRegularFile(p)) return "ملف الصورة غير موجود.";
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (IMAGE_EXTENSIONS.stream().noneMatch(n::endsWith)) return "صيغة الصورة غير مدعومة (png, jpg, jpeg, webp, gif).";
        return null;
    }

    static String jsonFile(Path p) {
        if (p == null) return "لم يتم اختيار ملف.";
        if (!Files.isRegularFile(p)) return "الملف غير موجود.";
        if (!p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) return "يجب اختيار ملف بصيغة .json";
        return null;
    }

    static String brandName(String value) {
        String err = required(value, "اسم الماركة", 60);
        if (err != null) return err;
        if (!value.trim().matches("[\\p{L}\\p{N} _-]+")) return "اسم الماركة يجب أن يحتوي على أحرف وأرقام و - _ فقط (يُستخدم كاسم ملف ومجلد).";
        return null;
    }

    /** يحول الأرقام العربية-الهندية إلى لاتينية. */
    static String toLatinDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '٠' && c <= '٩') sb.append((char) ('0' + (c - '٠')));
            else if (c >= '۰' && c <= '۹') sb.append((char) ('0' + (c - '۰')));
            else if (c == '٫') sb.append('.');
            else sb.append(c);
        }
        return sb.toString();
    }
}
