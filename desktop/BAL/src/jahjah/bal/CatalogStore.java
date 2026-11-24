package jahjah.bal;

import jahjah.dal.BrandRepository;
import jahjah.dal.CategoryRepository;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.BrandCatalog;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;
import jahjah.shared.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** الحالة في الذاكرة لكل الماركات والتصنيفات، مشتركة بين الخدمات. */
final class CatalogStore {
    private final BrandRepository brandRepo;
    private final CategoryRepository categoryRepo;
    private final Map<String, BrandCatalog> brands = new LinkedHashMap<>();
    private final List<Category> categories = new ArrayList<>();

    CatalogStore(BrandRepository brandRepo, CategoryRepository categoryRepo) {
        this.brandRepo = brandRepo;
        this.categoryRepo = categoryRepo;
    }

    void load() {
        brands.clear();
        for (String file : brandRepo.readManifest()) {
            if (brandRepo.exists(file)) brands.put(file, brandRepo.read(file));
        }
        categories.clear();
        categories.addAll(categoryRepo.readAll());
    }

    Collection<BrandCatalog> catalogs() { return brands.values(); }

    BrandCatalog catalog(String brandFile) {
        BrandCatalog c = brands.get(brandFile);
        if (c == null) throw new BusinessException("الماركة غير موجودة: " + brandFile);
        return c;
    }

    void addCatalog(BrandCatalog catalog) {
        brands.put(catalog.getBrand().getFileName(), catalog);
        brandRepo.write(catalog);
        brandRepo.writeManifest(new ArrayList<>(brands.keySet()));
    }

    List<Brand> brands() {
        List<Brand> list = new ArrayList<>();
        for (BrandCatalog c : brands.values()) list.add(c.getBrand());
        return list;
    }

    List<Product> allProducts() {
        List<Product> list = new ArrayList<>();
        for (BrandCatalog c : brands.values()) list.addAll(c.getProducts());
        return list;
    }

    Product product(String guid) {
        for (BrandCatalog c : brands.values()) {
            for (Product p : c.getProducts()) if (Objects.equals(p.getGuid(), guid)) return p;
        }
        throw new BusinessException("المنتج غير موجود: " + guid);
    }

    void saveBrand(String brandFile) {
        brandRepo.write(catalog(brandFile));
    }

    void saveBrands(Collection<String> brandFiles) {
        for (String f : brandFiles) saveBrand(f);
    }

    List<Category> categories() { return categories; }

    Category category(int id) {
        for (Category c : categories) if (c.getCategoryId() == id) return c;
        return null;
    }

    void saveCategories() {
        categoryRepo.writeAll(categories);
    }
}
