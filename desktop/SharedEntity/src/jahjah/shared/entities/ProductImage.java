package jahjah.shared.entities;

/** صورة منتج: المسار النسبي (images/Brand/xxx.png) + ترتيبها (1 = الصورة الرئيسية). */
public class ProductImage {
    private String path;
    private int order;

    public ProductImage() { }

    public ProductImage(String path, int order) {
        this.path = path;
        this.order = order;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public ProductImage copy() { return new ProductImage(path, order); }
}
