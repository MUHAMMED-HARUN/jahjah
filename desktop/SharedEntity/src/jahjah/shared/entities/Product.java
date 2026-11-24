package jahjah.shared.entities;

import jahjah.shared.enums.EnHasQuantity;

import java.util.ArrayList;
import java.util.List;

public class Product {
    private String guid;
    private String sku = "";
    private String name = "";
    /** معرفات التصنيفات (أوراق فقط). */
    private List<Integer> category = new ArrayList<>();
    private String categoryLabel = "";
    private double price;
    /** صور المنتج مرتبة حسب order. */
    private List<ProductImage> image = new ArrayList<>();
    private String packing = "";
    private List<String> description = new ArrayList<>();
    private String nameingAsProgram = "";
    private boolean isActive = true;
    private EnHasQuantity enHasQuantity = EnHasQuantity.NOTHING;

    /** اسم ملف الماركة الذي يحوي المنتج (لا يُكتب داخل المنتج). */
    private String brandFile;
    /** قيمة category القديمة إن كانت نصاً (تُستخدم للترحيل فقط). */
    private String legacyCategory;

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku == null ? "" : sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }

    public List<Integer> getCategory() { return category; }
    public void setCategory(List<Integer> category) { this.category = category == null ? new ArrayList<>() : new ArrayList<>(category); }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel == null ? "" : categoryLabel; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public List<ProductImage> getImage() { return image; }
    public void setImage(List<ProductImage> image) {
        this.image = new ArrayList<>();
        if (image != null) for (ProductImage i : image) this.image.add(i.copy());
    }

    /** الصورة الرئيسية (أصغر order) أو null. */
    public String getMainImagePath() {
        ProductImage main = null;
        for (ProductImage i : image) if (main == null || i.getOrder() < main.getOrder()) main = i;
        return main == null ? null : main.getPath();
    }

    public String getPacking() { return packing; }
    public void setPacking(String packing) { this.packing = packing == null ? "" : packing; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description == null ? new ArrayList<>() : new ArrayList<>(description); }

    public String getNameingAsProgram() { return nameingAsProgram; }
    public void setNameingAsProgram(String nameingAsProgram) { this.nameingAsProgram = nameingAsProgram == null ? "" : nameingAsProgram; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public EnHasQuantity getEnHasQuantity() { return enHasQuantity; }
    public void setEnHasQuantity(EnHasQuantity enHasQuantity) { this.enHasQuantity = enHasQuantity == null ? EnHasQuantity.NOTHING : enHasQuantity; }

    public String getBrandFile() { return brandFile; }
    public void setBrandFile(String brandFile) { this.brandFile = brandFile; }

    public String getLegacyCategory() { return legacyCategory; }
    public void setLegacyCategory(String legacyCategory) { this.legacyCategory = legacyCategory; }

    public Product copy() {
        Product p = new Product();
        p.guid = guid;
        p.sku = sku;
        p.name = name;
        p.category = new ArrayList<>(category);
        p.categoryLabel = categoryLabel;
        p.price = price;
        p.setImage(image);
        p.packing = packing;
        p.description = new ArrayList<>(description);
        p.nameingAsProgram = nameingAsProgram;
        p.isActive = isActive;
        p.enHasQuantity = enHasQuantity;
        p.brandFile = brandFile;
        p.legacyCategory = legacyCategory;
        return p;
    }
}
