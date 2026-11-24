package jahjah.dal;

import jahjah.shared.entities.Category;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** قراءة/كتابة جدول التصنيفات (data base/categories.json). */
public class CategoryRepository {
    private final DataPaths paths;

    public CategoryRepository(DataPaths paths) {
        this.paths = paths;
    }

    public List<Category> readAll() {
        List<Category> result = new ArrayList<>();
        if (!Files.exists(paths.categories())) return result;
        for (Object o : Json.asArray(FileIO.readJson(paths.categories()), "categories.json")) {
            Map<String, Object> m = Json.asObject(o, "categories.json / item");
            Category c = new Category();
            Double id = Json.num(m, "CategoryID");
            c.setCategoryId(id == null ? 0 : id.intValue());
            c.setCategoryName(Json.str(m, "CategoryName"));
            Double father = Json.num(m, "CategoryFather");
            c.setCategoryFather(father == null ? null : father.intValue());
            c.setLeef(!(m.get("IsLeef") instanceof Boolean b) || b);
            result.add(c);
        }
        return result;
    }

    public void writeAll(List<Category> categories) {
        List<Object> list = new ArrayList<>();
        for (Category c : categories) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("CategoryID", c.getCategoryId());
            m.put("CategoryName", c.getCategoryName());
            m.put("CategoryFather", c.getCategoryFather());
            m.put("IsLeef", c.isLeef());
            list.add(m);
        }
        FileIO.writeJson(paths.categories(), list);
    }
}
