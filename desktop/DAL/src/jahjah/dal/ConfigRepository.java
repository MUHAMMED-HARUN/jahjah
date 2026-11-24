package jahjah.dal;

import jahjah.shared.entities.AppConfig;
import jahjah.shared.entities.ThemeColor;
import jahjah.shared.enums.EnMessageType;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

        // theme: { key: { color, description } }  أو  { key: "#hex" }
        List<ThemeColor> theme = new ArrayList<>();
        if (m.get("theme") instanceof Map<?, ?> tm) {
            for (Map.Entry<?, ?> e : tm.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (e.getValue() instanceof Map<?, ?> v) {
                    theme.add(new ThemeColor(key, null, Json.str(v, "color"), Json.str(v, "description")));
                } else if (e.getValue() != null) {
                    theme.add(new ThemeColor(key, null, String.valueOf(e.getValue()), null));
                }
            }
        }
        c.setTheme(theme);
        return c;
    }

    public void write(AppConfig config) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("whatsappNumber", config.getWhatsappNumber());
        m.put("cartTtlDays", config.getCartTtlDays());
        m.put("messageType", config.getMessageType().getJsonValue());

        Map<String, Object> theme = new LinkedHashMap<>();
        for (ThemeColor t : config.getTheme()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("color", t.getColor());
            v.put("description", t.getDescription());
            theme.put(t.getKey(), v);
        }
        m.put("theme", theme);
        FileIO.writeJson(paths.config(), m);
    }
}
