package jahjah.bal;

import jahjah.dal.ConfigRepository;
import jahjah.shared.entities.AppConfig;
import jahjah.shared.exceptions.BusinessException;

/**
 * إعدادات خدمة الطلب في الواجهة (config.json):
 *  - رقم واتساب بالصيغة الدولية (أرقام فقط، 8–15 رقماً، بدون + أو 00).
 *  - مدة بقاء عناصر السلة في localStorage بالأيام (1–365).
 *  - نوع الرسالة: صورة طويلة أو رسالة نصية.
 */
public class ConfigService {
    public static final int MIN_TTL_DAYS = 1;
    public static final int MAX_TTL_DAYS = 365;

    private final ConfigRepository repo;

    ConfigService(ConfigRepository repo) {
        this.repo = repo;
    }

    public AppConfig get() {
        AppConfig c = repo.read();
        return c == null ? new AppConfig() : c;
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
        repo.write(c);
        return c.copy();
    }

    /** يحذف المسافات والرموز ويحوّل البادئة 00 أو + إلى أرقام فقط. */
    static String normalizeNumber(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        return digits;
    }
}
