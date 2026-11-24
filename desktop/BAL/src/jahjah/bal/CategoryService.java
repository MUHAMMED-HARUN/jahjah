package jahjah.bal;

import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;
import jahjah.shared.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * قواعد جدول التصنيفات:
 *  - لا يتكرر اسم التصنيف لنفس الأب.
 *  - لا يمكن إضافة أبناء لورقة مستخدمة كتصنيف لمنتجات.
 *  - في التعديل لا يمكن تغيير الاسم إلى اسم مستخدم في نفس مسار الأب.
 */
public class CategoryService {
    private final CatalogStore store;

    CategoryService(CatalogStore store) {
        this.store = store;
    }

    public List<Category> getAll() {
        List<Category> list = new ArrayList<>();
        for (Category c : store.categories()) list.add(c.copy());
        return list;
    }

    public List<Category> getLeaves() {
        List<Category> list = new ArrayList<>();
        for (Category c : store.categories()) if (c.isLeef()) list.add(c.copy());
        list.sort((a, b) -> getPath(a.getCategoryId()).compareTo(getPath(b.getCategoryId())));
        return list;
    }

    /** المسار الكامل: أب / ابن / حفيد. */
    public String getPath(int categoryId) {
        List<String> parts = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        Category c = store.category(categoryId);
        while (c != null && seen.add(c.getCategoryId())) {
            parts.add(c.getCategoryName());
            c = c.getCategoryFather() == null ? null : store.category(c.getCategoryFather());
        }
        Collections.reverse(parts);
        return String.join(" / ", parts);
    }

    /** هل الورقة مستخدمة كتصنيف لمنتج واحد على الأقل؟ */
    public boolean isUsed(int categoryId) {
        for (Product p : store.allProducts()) if (p.getCategory().contains(categoryId)) return true;
        return false;
    }

    /** add item: إضافة تصنيف (fatherId = null لتصنيف رئيسي). */
    public Category addItem(String name, Integer fatherId) {
        Category created = create(name, fatherId);
        store.saveCategories();
        return created.copy();
    }

    /** addChild: إضافة ابن لتصنيف موجود. */
    public Category addChild(int fatherId, String name) {
        return addItem(name, fatherId);
    }

    /** update: تعديل اسم التصنيف. */
    public Category update(int categoryId, String newName) {
        Category c = store.category(categoryId);
        if (c == null) throw new BusinessException("التصنيف غير موجود.");
        String clean = requireName(newName);
        ensureUniqueAmongSiblings(clean, c.getCategoryFather(), categoryId);
        c.setCategoryName(clean);
        store.saveCategories();

        // تحديث categoryLabel في المنتجات التي تستخدم هذا التصنيف
        Set<String> dirty = new LinkedHashSet<>();
        for (Product p : store.allProducts()) {
            if (p.getCategory().contains(categoryId)) {
                p.setCategoryLabel(labelFor(p.getCategory()));
                dirty.add(p.getBrandFile());
            }
        }
        store.saveBrands(dirty);
        return c.copy();
    }

    /** يتحقق أن المعرف موجود وأنه ورقة. */
    Category requireLeaf(int categoryId) {
        Category c = store.category(categoryId);
        if (c == null) throw new BusinessException("التصنيف رقم " + categoryId + " غير موجود.");
        if (!c.isLeef()) throw new BusinessException("التصنيف \"" + getPath(categoryId) + "\" ليس ورقة. يمكن الإضافة إلى الأوراق فقط.");
        return c;
    }

    String labelFor(List<Integer> ids) {
        List<String> names = new ArrayList<>();
        for (Integer id : ids) {
            Category c = store.category(id);
            if (c != null) names.add(c.getCategoryName());
        }
        return String.join("، ", names);
    }

    /** إنشاء بدون حفظ (يُستخدم أيضاً في الترحيل). */
    Category create(String name, Integer fatherId) {
        String clean = requireName(name);
        Category father = null;
        if (fatherId != null) {
            father = store.category(fatherId);
            if (father == null) throw new BusinessException("التصنيف الأب غير موجود.");
            if (father.isLeef() && isUsed(fatherId)) {
                throw new BusinessException("لا يمكن إضافة أبناء للتصنيف \"" + father.getCategoryName()
                        + "\" لأنه ورقة مستخدمة كتصنيف لمنتجات.");
            }
        }
        ensureUniqueAmongSiblings(clean, fatherId, null);

        int nextId = 1;
        for (Category c : store.categories()) nextId = Math.max(nextId, c.getCategoryId() + 1);
        Category created = new Category(nextId, clean, fatherId, true);
        if (father != null) father.setLeef(false);
        store.categories().add(created);
        return created;
    }

    Category findChild(Integer fatherId, String name) {
        for (Category c : store.categories()) {
            if (Objects.equals(c.getCategoryFather(), fatherId) && Names.same(c.getCategoryName(), name)) return c;
        }
        return null;
    }

    private void ensureUniqueAmongSiblings(String name, Integer fatherId, Integer exceptId) {
        Category existing = findChild(fatherId, name);
        if (existing != null && !Objects.equals(existing.getCategoryId(), exceptId)) {
            String where = fatherId == null ? "التصنيفات الرئيسية" : "\"" + getPath(fatherId) + "\"";
            throw new BusinessException("الاسم \"" + name + "\" مستخدم مسبقاً ضمن " + where + ".");
        }
    }

    private static String requireName(String name) {
        String clean = Names.clean(name);
        if (clean.isEmpty()) throw new BusinessException("اسم التصنيف مطلوب.");
        return clean;
    }
}
