package jahjah.dal;

import jahjah.shared.entities.AppConfig;
import jahjah.shared.enums.EnMessageType;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** قراءة/كتابة config.json في جذر المشروع. */
public class ConfigRepository {
    private final DataPaths paths;

    public ConfigRepository(DataPaths paths) {
        this.paths = paths;
    }

    /** يعيد null إذا لم يوجد الملف. */
    public AppConfig read() {
        if (!Files.exists(paths.config())) return null;
        Map<String, Object> m = Json.asObject(FileIO.readJson(paths.config()), "config.json");
        AppConfig c = new AppConfig();
        Object number = m.get("whatsappNumber");
        c.setWhatsappNumber(number == null ? "" : String.valueOf(number));
        Double days = Json.num(m, "cartTtlDays");
        if (days != null) c.setCartTtlDays(days.intValue());
        c.setMessageType(EnMessageType.fromJson(Json.str(m, "messageType")));
        return c;
    }

    public void write(AppConfig config) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("whatsappNumber", config.getWhatsappNumber());
        m.put("cartTtlDays", config.getCartTtlDays());
        m.put("messageType", config.getMessageType().getJsonValue());
        FileIO.writeJson(paths.config(), m);
    }
}
