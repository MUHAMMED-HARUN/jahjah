package jahjah.shared.entities;

import java.util.ArrayList;
import java.util.List;

/** محتوى ملف ماركة واحد: الماركة + منتجاتها. */
public class BrandCatalog {
    private Brand brand;
    private List<Product> products = new ArrayList<>();

    public BrandCatalog() { }

    public BrandCatalog(Brand brand, List<Product> products) {
        this.brand = brand;
        this.products = products == null ? new ArrayList<>() : products;
    }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}
