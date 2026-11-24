package jahjah.bal;

import jahjah.dal.ImageRepository;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.BrandCatalog;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.ImageInput;
import jahjah.shared.entities.Product;
import jahjah.shared.entities.ProductImage;
import jahjah.shared.exceptions.BusinessException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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

    /** add item — images: الصور الجديدة بالترتيب المطلوب (يمكن أن تكون فارغة). */
    public Product add(Product input, String brandFile, List<ImageInput> imageList) {
        BrandCatalog catalog = store.catalog(brandFile);
        Product p = input.copy();
        p.setGuid(UUID.randomUUID().toString());
        p.setBrandFile(brandFile);
        p.setLegacyCategory(null);
        p.setImage(List.of());
        normalize(p);
        validate(p, null);

        List<String> removed = applyImages(p, List.of(), imageList, catalog.getBrand());
        catalog.getProducts().add(p);
        store.saveBrand(brandFile);
        deleteUnreferenced(removed);
        return p.copy();
    }

    /**
     * update (الماركة ثابتة).
     * imageList = القائمة النهائية للصور بالترتيب، أو null لإبقاء الصور كما هي.
     */
    public Product update(Product input, List<ImageInput> imageList) {
        Product existing = store.product(input.getGuid());
        Product p = input.copy();
        p.setBrandFile(existing.getBrandFile());
        p.setImage(existing.getImage());
        p.setLegacyCategory(p.getCategory().isEmpty() ? existing.getLegacyCategory() : null);
        normalize(p);
        validate(p, existing.getGuid());

        List<String> removed = imageList == null ? List.of()
                : applyImages(p, existing.getImage(), imageList, store.catalog(existing.getBrandFile()).getBrand());

        List<Product> list = store.catalog(existing.getBrandFile()).getProducts();
        list.set(list.indexOf(existing), p);
        store.saveBrand(existing.getBrandFile());
        deleteUnreferenced(removed);
        return p.copy();
    }

    /** الصور مرتبة حسب order. */
    public List<ProductImage> getImages(String guid) {
        return sortedImages(store.product(guid));
    }

    /**
     * يطبّق القائمة النهائية للصور على المنتج: يتحقق منها، ينسخ الجديدة، ويعيد ترقيم order من 1.
     * يعيد مسارات الصور التي أُزيلت من المنتج (تُحذف ملفاتها بعد الحفظ).
     */
    private List<String> applyImages(Product p, List<ProductImage> current, List<ImageInput> inputs, Brand brand) {
        List<ImageInput> list = inputs == null ? List.of() : inputs;
        Set<String> currentPaths = new LinkedHashSet<>();
        for (ProductImage i : current) currentPaths.add(i.getPath());

        // التحقق المنطقي قبل أي كتابة
        Set<String> seenExisting = new HashSet<>();
        Set<Path> seenNew = new HashSet<>();
        for (ImageInput in : list) {
            if (in == null || (in.existingPath() == null) == (in.newSource() == null)) {
                throw new BusinessException("عنصر صورة غير صالح.");
            }
            if (in.isNew()) {
                if (!seenNew.add(in.newSource().toAbsolutePath().normalize())) {
                    throw new BusinessException("الصورة " + in.newSource().getFileName() + " مضافة أكثر من مرة.");
                }
            } else {
                if (!currentPaths.contains(in.existingPath())) {
                    throw new BusinessException("الصورة " + in.existingPath() + " لا تخص هذا المنتج.");
                }
                if (!seenExisting.add(in.existingPath())) {
                    throw new BusinessException("الصورة " + in.existingPath() + " مكررة في القائمة.");
                }
            }
        }

        // نسخ الجديدة وبناء القائمة بالترتيب
        Set<String> usedNames = new HashSet<>();
        List<ProductImage> result = new ArrayList<>();
        for (ImageInput in : list) {
            String path = in.isNew()
                    ? images.save(in.newSource(), brand.getImageFolder(), freeBaseName(brand.getImageFolder(), p.getGuid(), usedNames))
                    : in.existingPath();
            result.add(new ProductImage(path, result.size() + 1));
        }
        p.setImage(result);

        List<String> removed = new ArrayList<>(currentPaths);
        removed.removeAll(seenExisting);
        return removed;
    }

    /** guid، ثم guid_2، guid_3 ... أول اسم غير مستخدم في المجلد. */
    private String freeBaseName(String brandFolder, String guid, Set<String> usedNames) {
        for (int n = 1; ; n++) {
            String base = n == 1 ? guid : guid + "_" + n;
            if (!usedNames.contains(base) && !images.baseNameExists(brandFolder, base)) {
                usedNames.add(base);
                return base;
            }
        }
    }

    /** يحذف ملفات الصور المُزالة إن لم يعد أي منتج يستخدمها. */
    private void deleteUnreferenced(List<String> removed) {
        for (String path : removed) {
            boolean used = false;
            for (Product other : store.allProducts()) {
                for (ProductImage i : other.getImage()) if (path.equals(i.getPath())) used = true;
            }
            if (!used) images.delete(path);
        }
    }

    private static List<ProductImage> sortedImages(Product p) {
        List<ProductImage> list = new ArrayList<>();
        for (ProductImage i : p.getImage()) list.add(i.copy());
        list.sort(Comparator.comparingInt(ProductImage::getOrder));
        return list;
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
