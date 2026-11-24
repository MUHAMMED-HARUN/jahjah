package jahjah.ui;

import jahjah.shared.entities.Product;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ProductTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "فعال", "الماركة", "الرمز", "الاسم", "الاسم كما في البرنامج", "التصنيف", "السعر", "التعبئة", "التوفر"
    };

    private List<Product> rows = new ArrayList<>();
    private Map<String, String> brandNames = Map.of();

    void setData(List<Product> rows, Map<String, String> brandNames) {
        this.rows = rows;
        this.brandNames = brandNames;
        fireTableDataChanged();
    }

    Product at(int modelRow) { return rows.get(modelRow); }

    String brandName(Product p) { return brandNames.getOrDefault(p.getBrandFile(), p.getBrandFile()); }

    @Override public int getRowCount() { return rows.size(); }

    @Override public int getColumnCount() { return COLUMNS.length; }

    @Override public String getColumnName(int c) { return COLUMNS[c]; }

    @Override
    public Class<?> getColumnClass(int c) {
        return switch (c) {
            case 0 -> Boolean.class;
            case 6 -> Double.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int r, int c) {
        Product p = rows.get(r);
        return switch (c) {
            case 0 -> p.isActive();
            case 1 -> brandName(p);
            case 2 -> p.getSku();
            case 3 -> p.getName();
            case 4 -> p.getNameingAsProgram();
            case 5 -> p.getCategoryLabel();
            case 6 -> p.getPrice();
            case 7 -> p.getPacking();
            case 8 -> p.getEnHasQuantity().getLabel();
            default -> "";
        };
    }
}
