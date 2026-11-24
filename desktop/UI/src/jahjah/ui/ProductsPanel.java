package jahjah.ui;

import jahjah.bal.Services;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;
import jahjah.shared.entities.SyncResult;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** جدول المنتجات (اختيار متعدد) مع الدوال: إضافة، تعديل، تفعيل، إضافة تصنيف، تحديث الأسعار، تحديث التوفر. */
class ProductsPanel extends JPanel {
    private static final String ALL_BRANDS = "كل الماركات";

    private final Services services;
    private final ProductTableModel model = new ProductTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<ProductTableModel> sorter = new TableRowSorter<>(model);
    private final JComboBox<Object> brandFilter = new JComboBox<>();
    private final JTextField search = new JTextField(22);
    private final JLabel status = new JLabel(" ");
    private final JLabel preview = new JLabel("لا توجد صورة", SwingConstants.CENTER);

    ProductsPanel(Services services) {
        super(new BorderLayout(6, 6));
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ---- شريط الفلاتر
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filters.add(new JLabel("الماركة:"));
        filters.add(brandFilter);
        filters.add(new JLabel("بحث:"));
        filters.add(search);
        JButton refresh = new JButton("تحديث العرض");
        filters.add(refresh);

        // ---- شريط الدوال
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton add = new JButton("إضافة منتج");
        JButton edit = new JButton("تعديل");
        JButton activate = new JButton("تفعيل");
        JButton deactivate = new JButton("إيقاف");
        JButton addCategory = new JButton("إضافة تصنيف");
        JButton pricing = new JButton("تحديث الأسعار (JSON)");
        JButton visibility = new JButton("تحديث التوفر (JSON)");
        JButton newBrand = new JButton("ماركة جديدة");
        for (JButton b : new JButton[]{add, edit, activate, deactivate, addCategory, pricing, visibility, newBrand}) {
            actions.add(b);
        }

        JPanel north = new JPanel(new BorderLayout());
        north.add(filters, BorderLayout.NORTH);
        north.add(actions, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        // ---- الجدول
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(false);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(6).setMaxWidth(90);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onSelectionChanged(); });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) onEdit(); }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ---- معاينة الصورة
        preview.setPreferredSize(new Dimension(240, 240));
        preview.setBorder(BorderFactory.createTitledBorder("الصورة"));
        add(preview, BorderLayout.LINE_END);
        add(status, BorderLayout.SOUTH);

        // ---- الأحداث
        brandFilter.addActionListener(e -> applyFilter());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        refresh.addActionListener(e -> reload());
        add.addActionListener(e -> onAdd());
        edit.addActionListener(e -> onEdit());
        activate.addActionListener(e -> onSetActive(true));
        deactivate.addActionListener(e -> onSetActive(false));
        addCategory.addActionListener(e -> onAddCategory());
        pricing.addActionListener(e -> onSync(true));
        visibility.addActionListener(e -> onSync(false));
        newBrand.addActionListener(e -> onNewBrand());

        reload();
    }

    void reload() {
        List<String> keep = selectedGuids();
        Object selectedBrand = brandFilter.getSelectedItem();

        Map<String, String> names = new LinkedHashMap<>();
        DefaultComboBoxModel<Object> brands = new DefaultComboBoxModel<>();
        brands.addElement(ALL_BRANDS);
        for (Brand b : services.products().getBrands()) {
            names.put(b.getFileName(), b.getName());
            brands.addElement(b);
        }
        brandFilter.setModel(brands);
        if (selectedBrand instanceof Brand sb) {
            for (int i = 0; i < brands.getSize(); i++) {
                if (brands.getElementAt(i) instanceof Brand b && b.getFileName().equals(sb.getFileName())) brandFilter.setSelectedIndex(i);
            }
        }

        model.setData(services.products().getAll(), names);
        applyFilter();

        for (int i = 0; i < model.getRowCount(); i++) {
            if (keep.contains(model.at(i).getGuid())) {
                int v = table.convertRowIndexToView(i);
                if (v >= 0) table.getSelectionModel().addSelectionInterval(v, v);
            }
        }
    }

    private void applyFilter() {
        Object brand = brandFilter.getSelectedItem();
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ProductTableModel, ? extends Integer> entry) {
                Product p = model.at(entry.getIdentifier());
                if (brand instanceof Brand b && !b.getFileName().equals(p.getBrandFile())) return false;
                if (q.isEmpty()) return true;
                return (p.getName() + " " + p.getSku() + " " + p.getNameingAsProgram() + " " + p.getCategoryLabel())
                        .toLowerCase(Locale.ROOT).contains(q);
            }
        });
        updateStatus();
    }

    private List<String> selectedGuids() {
        List<String> guids = new ArrayList<>();
        for (int v : table.getSelectedRows()) guids.add(model.at(table.convertRowIndexToModel(v)).getGuid());
        return guids;
    }

    private void updateStatus() {
        status.setText("المعروض: " + table.getRowCount() + " من " + model.getRowCount()
                + "   |   المحدد: " + table.getSelectedRowCount());
    }

    private void onSelectionChanged() {
        updateStatus();
        int[] rows = table.getSelectedRows();
        preview.setIcon(null);
        preview.setText("لا توجد صورة");
        if (rows.length != 1) return;
        Product p = model.at(table.convertRowIndexToModel(rows[0]));
        Path img = services.products().resolveImage(p.getImagePath());
        if (img == null) return;
        ImageIcon icon = new ImageIcon(img.toString());
        if (icon.getIconWidth() <= 0) return;
        double scale = Math.min(220.0 / icon.getIconWidth(), 210.0 / icon.getIconHeight());
        Image scaled = icon.getImage().getScaledInstance(
                Math.max(1, (int) (icon.getIconWidth() * scale)), Math.max(1, (int) (icon.getIconHeight() * scale)), Image.SCALE_SMOOTH);
        preview.setText(null);
        preview.setIcon(new ImageIcon(scaled));
    }

    // ------------------------------------------------------------------ الدوال

    private void onAdd() {
        Brand preset = brandFilter.getSelectedItem() instanceof Brand b ? b : null;
        if (services.products().getBrands().isEmpty()) {
            Ui.error(this, "لا توجد ماركات. أضف ماركة أولاً.");
            return;
        }
        if (ProductDialog.showAdd(this, services, preset)) reload();
    }

    private void onEdit() {
        List<String> guids = selectedGuids();
        if (guids.size() != 1) {
            Ui.error(this, "حدد منتجاً واحداً للتعديل.");
            return;
        }
        if (ProductDialog.showEdit(this, services, services.products().get(guids.get(0)))) reload();
    }

    private void onSetActive(boolean active) {
        List<String> guids = selectedGuids();
        if (guids.isEmpty()) {
            Ui.error(this, "حدد عنصراً واحداً على الأقل.");
            return;
        }
        int[] changed = new int[1];
        if (Ui.run(this, () -> changed[0] = services.products().setActive(guids, active))) {
            reload();
            Ui.info(this, "تم " + (active ? "تفعيل " : "إيقاف ") + changed[0] + " عنصراً.");
        }
    }

    private void onAddCategory() {
        List<String> guids = selectedGuids();
        if (guids.isEmpty()) {
            Ui.error(this, "حدد عنصراً واحداً على الأقل.");
            return;
        }
        List<Category> leaves = services.categories().getLeaves();
        if (leaves.isEmpty()) {
            Ui.error(this, "لا توجد تصنيفات. أضف تصنيفاً من تبويب التصنيفات.");
            return;
        }
        JComboBox<Category> combo = new JComboBox<>(leaves.toArray(new Category[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Category c) setText(services.categories().getPath(c.getCategoryId()));
                return this;
            }
        });
        combo.applyComponentOrientation(Ui.RTL);
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"إضافة التصنيف إلى " + guids.size() + " عنصراً:", combo},
                "إضافة تصنيف", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        if (!(combo.getSelectedItem() instanceof Category chosen)) {
            Ui.error(this, "اختر تصنيفاً.");
            return;
        }
        int[] added = new int[1];
        if (Ui.run(this, () -> added[0] = services.products().addCategory(guids, chosen.getCategoryId()))) {
            reload();
            int skipped = guids.size() - added[0];
            Ui.info(this, "أُضيف التصنيف إلى " + added[0] + " عنصراً."
                    + (skipped > 0 ? "\n" + skipped + " عنصراً يحتوي التصنيف مسبقاً فلم يُضف." : ""));
        }
    }

    private void onSync(boolean pricing) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(pricing ? "اختر ملف الأسعار (JSON)" : "اختر ملف الكميات (JSON)");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        String err = Ui.jsonFile(file);
        if (err != null) {
            Ui.error(this, err);
            return;
        }

        SyncResult[] result = new SyncResult[1];
        boolean ok = Ui.run(this, () -> result[0] = pricing
                ? services.sync().updatePricing(file)
                : services.sync().updateVisibility(file));
        if (!ok) return;
        reload();

        SyncResult r = result[0];
        StringBuilder msg = new StringBuilder(r.operation().getLabel()).append(" — اكتملت العملية\n\n")
                .append("عناصر تم تحديثها: ").append(r.updatedCount()).append('\n')
                .append("موجودة في الملف ومفقودة من البرنامج: ").append(r.missingInProgram()).append('\n')
                .append("موجودة في البرنامج ومفقودة من الملف: ").append(r.missingInFile()).append('\n')
                .append("أسطر غير صالحة/مكررة في الملف: ").append(r.invalidLines()).append('\n');
        if (r.reportPath() != null) msg.append("\nالمواد المعزولة كُتبت في:\n").append(r.reportPath());
        Ui.info(this, msg.toString());
    }

    private void onNewBrand() {
        String name = JOptionPane.showInputDialog(this, "اسم الماركة الجديدة:", "ماركة جديدة", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        String err = Ui.brandName(name);
        if (err != null) {
            Ui.error(this, err);
            return;
        }
        if (Ui.run(this, () -> services.brands().add(name.trim()))) reload();
    }
}
