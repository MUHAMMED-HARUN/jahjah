package jahjah.bal;

import jahjah.dal.ConfigRepository;
import jahjah.shared.entities.AppConfig;
import jahjah.shared.entities.ThemeColor;
import jahjah.shared.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * إعدادات خدمة الطلب والألوان في الواجهة (config.json):
 *  - رقم واتساب بالصيغة الدولية (أرقام فقط، 8–15 رقماً، بدون + أو 00).
 *  - مدة بقاء عناصر السلة في localStorage بالأيام (1–365).
 *  - نوع الرسالة: صورة طويلة أو رسالة نصية.
 *  - ألوان الواجهة: #RRGGBB، مع حد أدنى للتباين حتى يبقى النص مقروءاً.
 */
public class ConfigService {
    public static final int MIN_TTL_DAYS = 1;
    public static final int MAX_TTL_DAYS = 365;
    /** تباين النص العادي (WCAG AA). */
    public static final double MIN_TEXT_CONTRAST = 4.5;
    /** تباين النص الأبيض على الأزرار الملونة. */
    public static final double MIN_BUTTON_CONTRAST = 3.0;

    /** الألوان المعروفة بالترتيب: المفتاح، الاسم، اللون الافتراضي (من شعار JAHJAH)، والوصف. */
    private static final String[][] THEME_DEFAULTS = {
            {"background", "خلفية الصفحة", "#fdf8e5", "خلفية الصفحة كاملة (خلف الفلاتر والكروت والسلة)"},
            {"surface", "خلفية الكروت", "#ffffff", "خلفية الكروت والجداول ولوحة الفلاتر والنافذة المنبثقة وحقول الإدخال"},
            {"text", "النص الأساسي", "#2b1d17", "النص الأساسي: أسماء المنتجات والعناوين والأرقام"},
            {"mutedText", "النص الثانوي", "#7d6b5d", "النص الثانوي: التصنيف، عناوين الحقول، العدادات، النص المعطّل"},
            {"border", "الحدود", "#ebe1cb", "حدود الكروت والجداول والحقول والفواصل بين الصفوف"},
            {"primary", "اللون الرئيسي", "#6d191b", "اللون الرئيسي: زر «أضف إلى السلة»، الشاشة وطريقة العرض المختارة، السعر، نوع التعبئة المختار، روابط «المزيد...»، ورأس صورة الفاتورة"},
            {"primaryHover", "الرئيسي عند المرور", "#521214", "اللون الرئيسي عند مرور الفأرة على الأزرار الرئيسية"},
            {"primarySoft", "الرئيسي الفاتح", "#f6e9e4", "خلفية فاتحة من اللون الرئيسي: خلفية رمز المنتج (SKU)، تمرير الفأرة على الأزرار الفاتحة، ورأس جدول الفاتورة"},
            {"accent", "اللون الثانوي", "#b19154", "اللون الثانوي (الذهبي): العنوان الصغير أعلى الصفحة، عدّاد السلة، ونص شارة الماركة"},
            {"accentSoft", "الثانوي الفاتح", "#f5edda", "خلفية فاتحة من اللون الثانوي: شارة الماركة، خلفية الصورة البديلة، وخلفية الملاحظات في الفاتورة"},
            {"success", "الإرسال / في السلة", "#1f8a4c", "زر «إرسال إلى واتساب» وحالة «في السلة ✓»"},
            {"danger", "الحذف والتحذير", "#b42318", "أزرار «إزالة» و«تفريغ السلة»، نافذة التحذير، والكميات غير الصالحة"},
            {"stockLot", "توفر: كثير", "#16a34a", "دائرة التوفر: كمية كثيرة"},
            {"stockMiddle", "توفر: متوسط", "#eab308", "دائرة التوفر: كمية متوسطة"},
            {"stockLittle", "توفر: قليل", "#dc2626", "دائرة التوفر: كمية قليلة"},
            {"stockNothing", "توفر: منتهي", "#9ca3af", "دائرة التوفر: منتهي"},
            {"pattern", "نقوش الخلفية", "#6d191b", "لون النقوش الشفافة في خلفية الصفحة (DCEL / dsp / LAPÔN) — تظهر بشفافية 6%"}
    };

    private final ConfigRepository repo;

    ConfigService(ConfigRepository repo) {
        this.repo = repo;
    }

    /** الإعدادات الحالية؛ الألوان المفقودة أو غير الصالحة في الملف تأخذ القيمة الافتراضية. */
    public AppConfig get() {
        AppConfig c = repo.read();
        if (c == null) c = new AppConfig();
        c.setTheme(mergeTheme(c.getTheme()));
        return c;
    }

    /** ألوان العلامة الافتراضية (لزر الاستعادة). */
    public List<ThemeColor> defaultTheme() {
        List<ThemeColor> list = new ArrayList<>();
        for (String[] d : THEME_DEFAULTS) list.add(new ThemeColor(d[0], d[1], d[2], d[3]));
        return list;
    }

    public AppConfig update(AppConfig input) {
        AppConfig c = input.copy();
        c.setWhatsappNumber(normalizeNumber(c.getWhatsappNumber()));
        if (c.getWhatsappNumber().isEmpty()) throw new BusinessException("رقم واتساب مطلوب.");
        if (c.getWhatsappNumber().length() < 8 || c.getWhatsappNumber().length() > 15) {
            throw new BusinessException("رقم واتساب يجب أن يكون بالصيغة الدولية (8 إلى 15 رقماً مع رمز الدولة).");
        }
        if (c.getWhatsappNumber().startsWith("0")) {
            throw new BusinessException("رقم واتساب يجب أن يبدأ برمز الدولة (مثل 963...) وليس بصفر.");
        }
        if (c.getCartTtlDays() < MIN_TTL_DAYS || c.getCartTtlDays() > MAX_TTL_DAYS) {
            throw new BusinessException("مدة بقاء السلة يجب أن تكون بين " + MIN_TTL_DAYS + " و " + MAX_TTL_DAYS + " يوماً.");
        }

        // الألوان: مفاتيح معروفة فقط، صيغة صحيحة، وتباين كافٍ
        Map<String, ThemeColor> byKey = new LinkedHashMap<>();
        for (ThemeColor t : c.getTheme()) {
            if (t == null || t.getKey() == null) continue;
            if (!isKnownKey(t.getKey())) throw new BusinessException("لون غير معروف: " + t.getKey());
            String hex = normalizeHex(t.getColor());
            if (hex == null) {
                throw new BusinessException("قيمة اللون \"" + labelOf(t.getKey()) + "\" غير صالحة: " + t.getColor() + " (الصيغة: #RRGGBB)");
            }
            byKey.put(t.getKey(), new ThemeColor(t.getKey(), null, hex, null));
        }
        List<ThemeColor> theme = mergeTheme(new ArrayList<>(byKey.values()));
        checkContrast(theme);
        c.setTheme(theme);

        repo.write(c);
        return get();
    }

    // ------------------------------------------------------------------ الألوان

    private List<ThemeColor> mergeTheme(List<ThemeColor> fromFile) {
        Map<String, String> colors = new LinkedHashMap<>();
        if (fromFile != null) {
            for (ThemeColor t : fromFile) {
                String hex = t == null ? null : normalizeHex(t.getColor());
                if (hex != null && t.getKey() != null) colors.put(t.getKey(), hex);
            }
        }
        List<ThemeColor> list = defaultTheme();
        for (ThemeColor t : list) {
            if (colors.containsKey(t.getKey())) t.setColor(colors.get(t.getKey()));
        }
        return list;
    }

    private static void checkContrast(List<ThemeColor> theme) {
        Map<String, String> c = new LinkedHashMap<>();
        for (ThemeColor t : theme) c.put(t.getKey(), t.getColor());
        List<String> problems = new ArrayList<>();
        need(problems, c, "text", "surface", MIN_TEXT_CONTRAST, "النص الأساسي على خلفية الكروت");
        need(problems, c, "text", "background", MIN_TEXT_CONTRAST, "النص الأساسي على خلفية الصفحة");
        need(problems, c, "mutedText", "surface", MIN_BUTTON_CONTRAST, "النص الثانوي على خلفية الكروت");
        need(problems, c, "primary", "surface", MIN_BUTTON_CONTRAST, "اللون الرئيسي (السعر والروابط) على خلفية الكروت");
        needWhite(problems, c, "primary", "النص الأبيض على الأزرار الرئيسية");
        needWhite(problems, c, "primaryHover", "النص الأبيض على الأزرار الرئيسية عند المرور");
        needWhite(problems, c, "success", "النص الأبيض على زر الإرسال");
        needWhite(problems, c, "danger", "النص الأبيض على زر تأكيد التفريغ");
        if (!problems.isEmpty()) {
            throw new BusinessException("الألوان غير مقروءة بشكل كافٍ:\n• " + String.join("\n• ", problems));
        }
    }

    private static void need(List<String> problems, Map<String, String> c, String fg, String bg, double min, String what) {
        double r = contrast(c.get(fg), c.get(bg));
        if (r < min) problems.add(what + String.format(Locale.ROOT, " (التباين %.1f والحد الأدنى %.1f)", r, min));
    }

    private static void needWhite(List<String> problems, Map<String, String> c, String bg, String what) {
        double r = contrast("#ffffff", c.get(bg));
        if (r < MIN_BUTTON_CONTRAST) problems.add(what + String.format(Locale.ROOT, " (التباين %.1f والحد الأدنى %.1f)", r, MIN_BUTTON_CONTRAST));
    }

    /** نسبة التباين حسب WCAG بين لونين #RRGGBB. */
    public static double contrast(String hexA, String hexB) {
        double a = luminance(hexA), b = luminance(hexB);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double[] ch = {(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255};
        for (int i = 0; i < 3; i++) {
            double v = ch[i] / 255.0;
            ch[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2];
    }

    /** #RGB أو #RRGGBB (بحروف صغيرة) أو null إن لم تكن صالحة. */
    static String normalizeHex(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!s.startsWith("#")) s = "#" + s;
        if (s.matches("#[0-9a-f]{3}")) s = "#" + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2) + s.charAt(3) + s.charAt(3);
        return s.matches("#[0-9a-f]{6}") ? s : null;
    }

    private static boolean isKnownKey(String key) {
        for (String[] d : THEME_DEFAULTS) if (d[0].equals(key)) return true;
        return false;
    }

    private static String labelOf(String key) {
        for (String[] d : THEME_DEFAULTS) if (d[0].equals(key)) return d[1];
        return key;
    }

    /** يحذف المسافات والرموز ويحوّل البادئة 00 أو + إلى أرقام فقط. */
    static String normalizeNumber(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        return digits;
    }
}
