package jahjah.bal;

import jahjah.dal.BrandRepository;
import jahjah.dal.CategoryRepository;
import jahjah.dal.DataPaths;
import jahjah.dal.ImageRepository;
import jahjah.dal.ImportRepository;
import jahjah.dal.ReportRepository;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** نقطة الدخول الوحيدة للـ UI: تبني DAL وتربط الخدمات. */
public final class Services {
    private final ProductService products;
    private final CategoryService categories;
    private final SyncService sync;
    private final BrandService brands;
    private final int migratedProducts;

    private Services(ProductService products, CategoryService categories, SyncService sync, BrandService brands, int migrated) {
        this.products = products;
        this.categories = categories;
        this.sync = sync;
        this.brands = brands;
        this.migratedProducts = migrated;
    }

    public static Services create(Path root) {
        DataPaths paths = new DataPaths(root);
        BrandRepository brandRepo = new BrandRepository(paths);
        CatalogStore store = new CatalogStore(brandRepo, new CategoryRepository(paths));
        store.load();

        CategoryService categories = new CategoryService(store);
        ProductService products = new ProductService(store, categories, new ImageRepository(paths));
        SyncService sync = new SyncService(store, new ImportRepository(), new ReportRepository(paths));
        BrandService brands = new BrandService(store, brandRepo);
        int migrated = migrateLegacyCategories(store, categories);
        return new Services(products, categories, sync, brands, migrated);
    }

    /**
     * الملفات القديمة تحفظ category كنص و categoryLabel كوصف أدق.
     * تتحول إلى شجرة: category (أب) → categoryLabel (ورقة)، ويُخزَّن معرف الورقة في المنتج.
     */
    private static int migrateLegacyCategories(CatalogStore store, CategoryService categories) {
        Set<String> dirty = new LinkedHashSet<>();
        boolean categoriesChanged = false;
        int count = 0;
        for (Product p : store.allProducts()) {
            if (p.getLegacyCategory() == null || !p.getCategory().isEmpty()) continue;
            String rootName = Names.clean(p.getLegacyCategory());
            String leafName = Names.clean(p.getCategoryLabel());
            if (rootName.isEmpty()) rootName = "عام";
            if (leafName.isEmpty()) leafName = "عام";

            Category root = categories.findChild(null, rootName);
            if (root == null) { root = categories.create(rootName, null); categoriesChanged = true; }
            Category leaf = categories.findChild(root.getCategoryId(), leafName);
            if (leaf == null) { leaf = categories.create(leafName, root.getCategoryId()); categoriesChanged = true; }

            p.setCategory(List.of(leaf.getCategoryId()));
            p.setCategoryLabel(leaf.getCategoryName());
            p.setLegacyCategory(null);
            dirty.add(p.getBrandFile());
            count++;
        }
        if (categoriesChanged) store.saveCategories();
        store.saveBrands(dirty);
        return count;
    }

    public ProductService products() { return products; }

    public CategoryService categories() { return categories; }

    public SyncService sync() { return sync; }

    public BrandService brands() { return brands; }

    /** عدد المنتجات التي رُحّل تصنيفها النصي القديم عند التشغيل. */
    public int migratedProducts() { return migratedProducts; }
}
