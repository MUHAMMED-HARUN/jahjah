package jahjah.dal;

import jahjah.shared.entities.SyncReportItem;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** كتابة ملف JSON بالمواد المعزولة (موجودة في طرف واحد فقط). */
public class ReportRepository {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final DataPaths paths;

    public ReportRepository(DataPaths paths) {
        this.paths = paths;
    }

    /** يكتب التقرير ويعيد مساره الكامل. */
    public String write(String operationName, String sourceFile, List<SyncReportItem> items) {
        LocalDateTime now = LocalDateTime.now();
        Path file = paths.reportsDir().resolve(operationName + "_" + now.format(STAMP) + ".json");

        List<Object> list = new ArrayList<>();
        for (SyncReportItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("operation", it.operation().getJsonValue());
            m.put("missingFrom", it.missingFrom().getJsonValue());
            m.put("nameingAsProgram", it.nameingAsProgram());
            m.put("productName", it.productName());
            m.put("brand", it.brand());
            m.put("guid", it.guid());
            m.put("value", it.value());
            m.put("notes", it.note());
            list.add(m);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("operation", operationName);
        root.put("sourceFile", sourceFile);
        root.put("createdAt", now.toString());
        root.put("count", items.size());
        root.put("items", list);
        FileIO.writeJson(file, root);
        return file.toString();
    }
}
