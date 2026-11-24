package jahjah.shared.entities;

public class Category {
    private int categoryId;
    private String categoryName = "";
    /** null = تصنيف رئيسي (بدون أب). */
    private Integer categoryFather;
    private boolean isLeef = true;

    public Category() { }

    public Category(int categoryId, String categoryName, Integer categoryFather, boolean isLeef) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryFather = categoryFather;
        this.isLeef = isLeef;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Integer getCategoryFather() { return categoryFather; }
    public void setCategoryFather(Integer categoryFather) { this.categoryFather = categoryFather; }

    public boolean isLeef() { return isLeef; }
    public void setLeef(boolean leef) { isLeef = leef; }

    public Category copy() {
        return new Category(categoryId, categoryName, categoryFather, isLeef);
    }

    @Override
    public String toString() { return categoryName; }
}
