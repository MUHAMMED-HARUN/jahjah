package jahjah.bal;

import jahjah.dal.ImageRepository;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.BrandCatalog;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;
import jahjah.shared.exceptions.BusinessException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** دوال المنتجات: add item, update, active, Add Category. */
public class ProductService {
    private final CatalogStore store;
    private final CategoryService categories;
    private final ImageRepository images;

    ProductService(CatalogStore store, CategoryService categories, ImageRepository images) {
        this.store = store;
        this.categories = categories;
        this.images = images;
    }

    public List<Brand> getBrands() {
        return store.brands();
    }

    public List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        for (Product p : store.allProducts()) list.add(p.copy());
        return list;
    }

    public Product get(String guid) {
        return store.product(guid).copy();
    }

    /** المسار الكامل لصورة المنتج إن وُجدت. */
    public Path resolveImage(String imagePath) {
        return images.resolve(imagePath);
    }

    /** add item */
    public Product add(Product input, String brandFile, Path imageSource) {
        BrandCatalog catalog = store.catalog(brandFile);
        Product p = input.copy();
        p.setGuid(UUID.randomUUID().toString());
        p.setBrandFile(brandFile);
        p.setLegacyCategory(null);
        normalize(p);
        validate(p, null);

        if (imageSource != null) {
            p.setImagePath(images.save(imageSource, catalog.getBrand().getImageFolder(), p.getGuid()));
        }
        catalog.getProducts().add(p);
        store.saveBrand(brandFile);
        return p.copy();
    }

    /** update (الماركة ثابتة؛ الصورة تُستبدل فقط إن اختيرت صورة جديدة). */
    public Product update(Product input, Path imageSource) {
        Product existing = store.product(input.getGuid());
        Product p = input.copy();
        p.setBrandFile(existing.getBrandFile());
        p.setImagePath(existing.getImagePath());
        p.setLegacyCategory(p.getCategory().isEmpty() ? existing.getLegacyCategory() : null);
        normalize(p);
        validate(p, existing.getGuid());

        if (imageSource != null) {
            Brand brand = store.catalog(existing.getBrandFile()).getBrand();
            p.setImagePath(images.save(imageSource, brand.getImageFolder(), p.getGuid()));
        }

        List<Product> list = store.catalog(existing.getBrandFile()).getProducts();
        list.set(list.indexOf(existing), p);
        store.saveBrand(existing.getBrandFile());
        return p.copy();
    }

    /** active: تفعيل/إيقاف عنصر أو عدة عناصر. يعيد عدد العناصر التي تغيّرت. */
    public int setActive(Collection<String> guids, boolean active) {
        requireSelection(guids);
        Set<String> dirty = new LinkedHashSet<>();
        int changed = 0;
        for (String guid : guids) {
            Product p = store.product(guid);
            if (p.isActive() != active) {
                p.setActive(active);
                dirty.add(p.getBrandFile());
                changed++;
            }
        }
        store.saveBrands(dirty);
        return changed;
    }

    /**
     * Add Category: يضيف تصنيف (ورقة فقط) إلى متغير التصنيف لعنصر أو عدة عناصر.
     * إذا كان التصنيف موجوداً في المنتج لا يُضاف. يعيد عدد العناصر التي أُضيف لها.
     */
    public int addCategory(Collection<String> guids, int categoryId) {
        requireSelection(guids);
        categories.requireLeaf(categoryId);
        Set<String> dirty = new LinkedHashSet<>();
        int added = 0;
        for (String guid : guids) {
            Product p = store.product(guid);
            if (p.getCategory().contains(categoryId)) continue;
            List<Integer> ids = new ArrayList<>(p.getCategory());
            ids.add(categoryId);
            p.setCategory(ids);
            p.setLegacyCategory(null);
            p.setCategoryLabel(categories.labelFor(ids));
            dirty.add(p.getBrandFile());
            added++;
        }
        store.saveBrands(dirty);
        return added;
    }

    private void normalize(Product p) {
        p.setName(Names.clean(p.getName()));
        p.setSku(Names.clean(p.getSku()));
        p.setNameingAsProgram(Names.clean(p.getNameingAsProgram()));
        p.setPacking(Names.clean(p.getPacking()));
        List<Integer> distinct = new ArrayList<>(new LinkedHashSet<>(p.getCategory()));
        p.setCategory(distinct);
        if (!distinct.isEmpty()) p.setCategoryLabel(categories.labelFor(distinct));
    }

    private void validate(Product p, String selfGuid) {
        if (p.getName().isEmpty()) throw new BusinessException("اسم المنتج مطلوب.");
        if (p.getPrice() < 0) throw new BusinessException("السعر لا يمكن أن يكون سالباً.");

        for (Integer id : p.getCategory()) {
            Category c = categories.requireLeaf(id);
            Objects.requireNonNull(c);
        }

        for (Product other : store.allProducts()) {
            if (Objects.equals(other.getGuid(), selfGuid)) continue;
            if (!p.getNameingAsProgram().isEmpty() && Names.same(other.getNameingAsProgram(), p.getNameingAsProgram())) {
                throw new BusinessException("الاسم كما في البرنامج \"" + p.getNameingAsProgram()
                        + "\" مستخدم في منتج آخر: " + other.getName());
            }
            if (!p.getSku().isEmpty() && Objects.equals(other.getBrandFile(), p.getBrandFile())
                    && Names.same(other.getSku(), p.getSku())) {
                throw new BusinessException("الرمز \"" + p.getSku() + "\" مستخدم في نفس الماركة: " + other.getName());
            }
        }
    }

    private static void requireSelection(Collection<String> guids) {
        if (guids == null || guids.isEmpty()) throw new BusinessException("لم يتم تحديد أي عنصر.");
    }
}
