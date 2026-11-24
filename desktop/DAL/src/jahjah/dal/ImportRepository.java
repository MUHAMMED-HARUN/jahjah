package jahjah.dal;

import jahjah.shared.entities.PriceEntry;
import jahjah.shared.entities.StockEntry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * قراءة ملفات الإدخال الخارجية.
 * الشكل المقبول: مصفوفة كائنات، أو كائن فيه "items": [...].
 *   تسعير: [{"nameingAsProgram": "...", "price": 12.5}]
 *   كميات: [{"nameingAsProgram": "...", "quantity": 48}]
 */
public class ImportRepository {

    public List<PriceEntry> readPriceEntries(Path file) {
        List<PriceEntry> result = new ArrayList<>();
        for (Map<String, Object> m : items(file)) {
            result.add(new PriceEntry(Json.str(m, "nameingAsProgram"), Json.num(m, "price")));
        }
        return result;
    }

    public List<StockEntry> readStockEntries(Path file) {
        List<StockEntry> result = new ArrayList<>();
        for (Map<String, Object> m : items(file)) {
            result.add(new StockEntry(Json.str(m, "nameingAsProgram"), Json.num(m, "quantity")));
        }
        return result;
    }

    private static List<Map<String, Object>> items(Path file) {
        Object root = FileIO.readJson(file);
        if (root instanceof Map<?, ?> m && m.get("items") != null) root = m.get("items");
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object o : Json.asArray(root, file.getFileName().toString())) {
            list.add(Json.asObject(o, file.getFileName() + " / item"));
        }
        return list;
    }
}
