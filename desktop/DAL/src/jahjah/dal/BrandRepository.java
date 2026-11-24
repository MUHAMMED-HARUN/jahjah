package jahjah.dal;

import jahjah.shared.entities.Brand;
import jahjah.shared.entities.BrandCatalog;
import jahjah.shared.entities.Product;
import jahjah.shared.enums.EnHasQuantity;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** قراءة/كتابة ملفات الماركات (data base/<brand>.json) وملف manifest.json. */
public class BrandRepository {
    private final DataPaths paths;

    public BrandRepository(DataPaths paths) {
        this.paths = paths;
    }

    public List<String> readManifest() {
        List<String> files = new ArrayList<>();
        if (!Files.exists(paths.manifest())) return files;
        for (Object o : Json.asArray(FileIO.readJson(paths.manifest()), "manifest.json")) {
            if (o != null) files.add(String.valueOf(o));
        }
        return files;
    }

    public void writeManifest(List<String> files) {
        FileIO.writeJson(paths.manifest(), new ArrayList<>(files));
    }

    public boolean exists(String fileName) {
        return Files.exists(paths.brandFile(fileName));
    }

    public BrandCatalog read(String fileName) {
        Map<String, Object> root = Json.asObject(FileIO.readJson(paths.brandFile(fileName)), fileName);

        Brand brand = new Brand();
        brand.setFileName(fileName);
        Object b = root.get("brand");
        if (b instanceof Map<?, ?> bm) {
            brand.setId(Json.str(bm, "id"));
            brand.setName(Json.str(bm, "name"));
        }

        List<Product> products = new ArrayList<>();
        Object list = root.get("products");
        if (list != null) {
            for (Object o : Json.asArray(list, fileName + " / products")) {
                Product p = toProduct(Json.asObject(o, fileName + " / product"));
                p.setBrandFile(fileName);
                products.add(p);
            }
        }
        return new BrandCatalog(brand, products);
    }

    public void write(BrandCatalog catalog) {
        Map<String, Object> brand = new LinkedHashMap<>();
        brand.put("id", catalog.getBrand().getId());
        brand.put("name", catalog.getBrand().getName());

        List<Object> products = new ArrayList<>();
        for (Product p : catalog.getProducts()) products.add(fromProduct(p));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("brand", brand);
        root.put("products", products);
        FileIO.writeJson(paths.brandFile(catalog.getBrand().getFileName()), root);
    }

    private static Product toProduct(Map<String, Object> m) {
        Product p = new Product();
        p.setGuid(Json.str(m, "guid"));
        p.setSku(Json.str(m, "sku"));
        p.setName(Json.str(m, "name"));

        Object cat = m.get("category");
        if (cat instanceof List<?> ids) {
            List<Integer> list = new ArrayList<>();
            for (Object id : ids) if (id instanceof Number n) list.add(n.intValue());
            p.setCategory(list);
        } else if (cat != null) {
            p.setLegacyCategory(String.valueOf(cat));
        }

        p.setCategoryLabel(Json.str(m, "categoryLabel"));
        Double price = Json.num(m, "price");
        p.setPrice(price == null ? 0 : price);
        p.setImagePath(Json.str(m, "imagePath"));
        p.setPacking(Json.str(m, "packing"));

        List<String> desc = new ArrayList<>();
        Object d = m.get("description");
        if (d instanceof List<?> dl) {
            for (Object line : dl) if (line != null) desc.add(String.valueOf(line));
        } else if (d != null) {
            desc.add(String.valueOf(d));
        }
        p.setDescription(desc);

        p.setNameingAsProgram(Json.str(m, "nameingAsProgram"));
        if (m.get("isActive") instanceof Boolean active) p.setActive(active);
        if (m.containsKey("enHasQuantity")) p.setEnHasQuantity(EnHasQuantity.fromJson(Json.str(m, "enHasQuantity")));
        return p;
    }

    private static Map<String, Object> fromProduct(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("guid", p.getGuid());
        m.put("sku", p.getSku());
        m.put("name", p.getName());
        if (p.getLegacyCategory() != null && p.getCategory().isEmpty()) {
            m.put("category", p.getLegacyCategory());
        } else {
            m.put("category", new ArrayList<>(p.getCategory()));
        }
        m.put("categoryLabel", p.getCategoryLabel());
        m.put("price", p.getPrice());
        m.put("imagePath", p.getImagePath());
        m.put("packing", p.getPacking());
        m.put("description", new ArrayList<>(p.getDescription()));
        m.put("nameingAsProgram", p.getNameingAsProgram());
        m.put("isActive", p.isActive());
        m.put("enHasQuantity", p.getEnHasQuantity().getJsonValue());
        return m;
    }
}
