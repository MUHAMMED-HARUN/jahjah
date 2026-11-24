package jahjah.bal;

import jahjah.dal.BrandRepository;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.BrandCatalog;
import jahjah.shared.exceptions.BusinessException;

import java.util.ArrayList;
import java.util.List;

/** إضافة ماركة جديدة (ملف JSON + مدخل في manifest). */
public class BrandService {
    private final CatalogStore store;
    private final BrandRepository brandRepo;

    BrandService(CatalogStore store, BrandRepository brandRepo) {
        this.store = store;
        this.brandRepo = brandRepo;
    }

    public List<Brand> getAll() {
        return store.brands();
    }

    public Brand add(String name) {
        String clean = Names.clean(name);
        if (clean.isEmpty()) throw new BusinessException("اسم الماركة مطلوب.");
        for (Brand b : store.brands()) {
            if (Names.same(b.getName(), clean)) throw new BusinessException("الماركة \"" + clean + "\" موجودة مسبقاً.");
        }
        String fileName = clean + ".json";
        if (brandRepo.exists(fileName)) throw new BusinessException("الملف " + fileName + " موجود مسبقاً في مجلد البيانات.");

        Brand brand = new Brand("brand-" + clean, clean, fileName);
        store.addCatalog(new BrandCatalog(brand, new ArrayList<>()));
        return brand;
    }
}
