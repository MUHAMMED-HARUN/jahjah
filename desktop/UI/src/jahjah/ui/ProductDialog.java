package jahjah.ui;

import jahjah.bal.Services;
import jahjah.shared.entities.Brand;
import jahjah.shared.entities.Category;
import jahjah.shared.entities.Product;
import jahjah.shared.enums.EnHasQuantity;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** نافذة إضافة/تعديل منتج. التحقق من البيانات هنا، والقواعد المنطقية في BAL. */
class ProductDialog extends JDialog {
    private final Services services;
    private final Product existing;
    private boolean saved;
    private Path imageSource;

    private final JComboBox<Brand> brand = new JComboBox<>();
    private final JTextField sku = new JTextField(28);
    private final JTextField name = new JTextField(28);
    private final JTextField nameingAsProgram = new JTextField(28);
    private final JTextField price = new JTextField(10);
    private final JTextField packing = new JTextField(20);
    private final JTextArea description = new JTextArea(5, 28);
    private final JCheckBox isActive = new JCheckBox("فعال", true);
    private final JComboBox<EnHasQuantity> quantity = new JComboBox<>(EnHasQuantity.values());
    private final JList<Category> categories = new JList<>();
    private final JLabel imageLabel = new JLabel("بدون صورة");

    static boolean showAdd(Component parent, Services services, Brand preset) {
        ProductDialog d = new ProductDialog(SwingUtilities.getWindowAncestor(parent), services, null, preset);
        d.setVisible(true);
        return d.saved;
    }

    static boolean showEdit(Component parent, Services services, Product product) {
        ProductDialog d = new ProductDialog(SwingUtilities.getWindowAncestor(parent), services, product, null);
        d.setVisible(true);
        return d.saved;
    }

    private ProductDialog(Window owner, Services services, Product existing, Brand preset) {
        super(owner, existing == null ? "إضافة منتج" : "تعديل منتج", ModalityType.APPLICATION_MODAL);
        this.services = services;
        this.existing = existing;

        for (Brand b : services.products().getBrands()) brand.addItem(b);
        List<Category> leaves = services.categories().getLeaves();
        categories.setListData(leaves.toArray(new Category[0]));
        categories.setVisibleRowCount(6);
        categories.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Category c) setText(services.categories().getPath(c.getCategoryId()));
                return this;
            }
        });
        description.setLineWrap(true);
        description.setWrapStyleWord(true);

        JButton chooseImage = new JButton("اختيار صورة...");
        chooseImage.addActionListener(e -> chooseImage());
        JPanel imageRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        imageRow.add(chooseImage);
        imageRow.add(new JLabel("  "));
        imageRow.add(imageLabel);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        int row = 0;
        row = addRow(form, row, "الماركة *", brand);
        row = addRow(form, row, "الاسم *", name);
        row = addRow(form, row, "الاسم كما في البرنامج", nameingAsProgram);
        row = addRow(form, row, "الرمز (SKU)", sku);
        row = addRow(form, row, "السعر *", price);
        row = addRow(form, row, "التعبئة في الطرد *", packing);
        row = addRow(form, row, "التصنيفات (أوراق)", new JScrollPane(categories));
        row = addRow(form, row, "الوصف (سطر لكل مواصفة)", new JScrollPane(description));
        row = addRow(form, row, "التوفر", quantity);
        row = addRow(form, row, "الحالة", isActive);
        addRow(form, row, "الصورة", imageRow);

        JButton ok = new JButton("حفظ");
        JButton cancel = new JButton("إلغاء");
        ok.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(ok);
        buttons.add(cancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        if (existing != null) fill(existing, leaves);
        else if (preset != null) selectBrand(preset.getFileName());

        applyComponentOrientation(Ui.RTL);
        pack();
        setLocationRelativeTo(owner);
    }

    private static int addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
        return row + 1;
    }

    private void fill(Product p, List<Category> leaves) {
        selectBrand(p.getBrandFile());
        brand.setEnabled(false);
        sku.setText(p.getSku());
        name.setText(p.getName());
        nameingAsProgram.setText(p.getNameingAsProgram());
        price.setText(p.getPrice() == Math.rint(p.getPrice()) ? String.valueOf((long) p.getPrice()) : String.valueOf(p.getPrice()));
        packing.setText(p.getPacking());
        description.setText(String.join("\n", p.getDescription()));
        isActive.setSelected(p.isActive());
        quantity.setSelectedItem(p.getEnHasQuantity());
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i++) if (p.getCategory().contains(leaves.get(i).getCategoryId())) idx.add(i);
        categories.setSelectedIndices(idx.stream().mapToInt(Integer::intValue).toArray());
        imageLabel.setText(p.getImagePath() == null ? "بدون صورة" : p.getImagePath());
    }

    private void selectBrand(String fileName) {
        for (int i = 0; i < brand.getItemCount(); i++) {
            if (brand.getItemAt(i).getFileName().equals(fileName)) brand.setSelectedIndex(i);
        }
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("صور", "png", "jpg", "jpeg", "webp", "gif"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = chooser.getSelectedFile().toPath();
        String err = Ui.imageFile(p);
        if (err != null) {
            Ui.error(this, err);
            return;
        }
        imageSource = p;
        imageLabel.setText(p.getFileName().toString());
    }

    /** Data validation ثم استدعاء BAL. */
    private void onSave() {
        List<String> errors = new ArrayList<>();
        if (brand.getSelectedItem() == null) errors.add("اختر الماركة.");
        addIfError(errors, Ui.required(name.getText(), "الاسم", 150));
        addIfError(errors, Ui.optional(nameingAsProgram.getText(), "الاسم كما في البرنامج", 200));
        addIfError(errors, Ui.optional(sku.getText(), "الرمز", 50));
        addIfError(errors, Ui.required(packing.getText(), "التعبئة في الطرد", 60));
        Double priceValue = Ui.nonNegativeNumber(price.getText());
        if (priceValue == null) errors.add("السعر يجب أن يكون رقماً أكبر من أو يساوي صفر.");
        addIfError(errors, Ui.imageFile(imageSource));
        List<String> lines = new ArrayList<>();
        for (String line : description.getText().split("\\R")) if (!line.isBlank()) lines.add(line.trim());

        if (!errors.isEmpty()) {
            Ui.error(this, String.join("\n", errors));
            return;
        }

        Product p = existing == null ? new Product() : existing.copy();
        p.setSku(sku.getText());
        p.setName(name.getText());
        p.setNameingAsProgram(nameingAsProgram.getText());
        p.setPrice(priceValue);
        p.setPacking(packing.getText());
        p.setDescription(lines);
        p.setActive(isActive.isSelected());
        p.setEnHasQuantity((EnHasQuantity) quantity.getSelectedItem());
        List<Integer> ids = new ArrayList<>();
        for (Category c : categories.getSelectedValuesList()) ids.add(c.getCategoryId());
        p.setCategory(ids);

        Brand b = (Brand) brand.getSelectedItem();
        boolean ok = Ui.run(this, () -> {
            if (existing == null) services.products().add(p, b.getFileName(), imageSource);
            else services.products().update(p, imageSource);
        });
        if (ok) {
            saved = true;
            dispose();
        }
    }

    private static void addIfError(List<String> errors, String err) {
        if (err != null) errors.add(err);
    }
}
