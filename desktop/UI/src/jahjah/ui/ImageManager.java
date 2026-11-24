package jahjah.ui;

import jahjah.shared.entities.ImageInput;
import jahjah.shared.entities.ProductImage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * كومبوننت صور المنتج: معاينة كبيرة تتمدد مع النافذة + شريط مصغرات + تنقل.
 * وضع العرض فقط (للمعاينة) أو وضع التحرير (إضافة صور متعددة، حذف، تقديم/تأخير، جعلها رئيسية).
 * الترتيب في الشريط = order (الأولى رئيسية).
 */
class ImageManager extends JPanel {

    /** صورة: موجودة (existingPath) أو جديدة لم تُحفظ بعد (file فقط). */
    private record Item(String existingPath, Path file) {
        boolean isNew() { return existingPath == null; }

        String title() { return isNew() ? file.getFileName().toString() : existingPath; }
    }

    private static final int THUMB = 72;

    private final Function<String, Path> resolver;
    private final boolean editable;
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> strip = new JList<>(model);
    private final Preview preview = new Preview();
    private final JLabel info = new JLabel(" ", SwingConstants.CENTER);
    private final JButton prev = new JButton("›");
    private final JButton next = new JButton("‹");
    private final Map<Path, Image> images = new HashMap<>();
    private final Map<Path, ImageIcon> thumbs = new HashMap<>();
    private Runnable onChange = () -> { };

    ImageManager(Function<String, Path> resolver, boolean editable) {
        super(new BorderLayout(6, 6));
        this.resolver = resolver;
        this.editable = editable;

        // ---- المعاينة الكبيرة مع أزرار التنقل
        prev.setToolTipText("السابقة");
        next.setToolTipText("التالية");
        prev.addActionListener(e -> step(-1));
        next.addActionListener(e -> step(1));
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.add(prev, BorderLayout.LINE_START);
        center.add(preview, BorderLayout.CENTER);
        center.add(next, BorderLayout.LINE_END);
        info.setFont(info.getFont().deriveFont(Font.BOLD));
        center.add(info, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // ---- شريط المصغرات (صف أفقي واحد مع تمرير)
        strip.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        strip.setVisibleRowCount(1);
        strip.setFixedCellWidth(THUMB + 14);
        strip.setFixedCellHeight(THUMB + 26);
        strip.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        strip.setCellRenderer(new ThumbRenderer());
        strip.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) refreshPreview(); });
        JScrollPane stripScroll = new JScrollPane(strip,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        stripScroll.setPreferredSize(new Dimension(100, THUMB + 26 + 22));
        add(stripScroll, BorderLayout.SOUTH);

        preview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showFullSize();
            }
        });

        if (editable) add(toolbar(), BorderLayout.NORTH);
        refreshPreview();
    }

    private JPanel toolbar() {
        JButton add = new JButton("إضافة صور...");
        JButton remove = new JButton("حذف");
        JButton earlier = new JButton("تقديم");
        JButton later = new JButton("تأخير");
        JButton main = new JButton("جعلها رئيسية");
        add.addActionListener(e -> onAdd());
        remove.addActionListener(e -> onRemove());
        earlier.addActionListener(e -> move(-1));
        later.addActionListener(e -> move(1));
        main.addActionListener(e -> makeMain());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        for (JButton b : new JButton[]{add, remove, earlier, later, main}) bar.add(b);
        return bar;
    }

    // ------------------------------------------------------------------ البيانات

    void setOnChange(Runnable onChange) { this.onChange = onChange == null ? () -> { } : onChange; }

    /** صور منتج محفوظة (تُرتب حسب order). */
    void setImages(List<ProductImage> list) {
        List<ImageInput> inputs = new ArrayList<>();
        if (list != null) {
            List<ProductImage> sorted = new ArrayList<>(list);
            sorted.sort(Comparator.comparingInt(ProductImage::getOrder));
            for (ProductImage i : sorted) inputs.add(ImageInput.existing(i.getPath()));
        }
        setInputs(inputs);
    }

    /** قائمة بالترتيب (صور موجودة و/أو ملفات جديدة). */
    void setInputs(List<ImageInput> inputs) {
        model.clear();
        if (inputs != null) {
            for (ImageInput in : inputs) {
                model.addElement(in.isNew() ? new Item(null, in.newSource()) : new Item(in.existingPath(), resolver.apply(in.existingPath())));
            }
        }
        if (!model.isEmpty()) strip.setSelectedIndex(0);
        refreshPreview();
    }

    /** القائمة النهائية بالترتيب لإرسالها إلى BAL. */
    List<ImageInput> getInputs() {
        List<ImageInput> inputs = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            Item it = model.get(i);
            inputs.add(it.isNew() ? ImageInput.fromFile(it.file()) : ImageInput.existing(it.existingPath()));
        }
        return inputs;
    }

    int count() { return model.size(); }

    // ------------------------------------------------------------------ التحرير

    private void onAdd() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("صور", "png", "jpg", "jpeg", "webp", "gif"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        List<String> errors = new ArrayList<>();
        int firstAdded = -1;
        for (File f : chooser.getSelectedFiles()) {
            Path p = f.toPath().toAbsolutePath().normalize();
            String err = Ui.imageFile(p);
            if (err != null) { errors.add(f.getName() + ": " + err); continue; }
            if (containsFile(p)) { errors.add(f.getName() + ": مضافة مسبقاً."); continue; }
            model.addElement(new Item(null, p));
            if (firstAdded < 0) firstAdded = model.size() - 1;
        }
        if (firstAdded >= 0) select(firstAdded);
        changed();
        if (!errors.isEmpty()) Ui.error(this, String.join("\n", errors));
    }

    private boolean containsFile(Path p) {
        for (int i = 0; i < model.size(); i++) if (p.equals(model.get(i).file())) return true;
        return false;
    }

    private void onRemove() {
        int i = strip.getSelectedIndex();
        if (i < 0) { Ui.error(this, "حدد صورة للحذف."); return; }
        model.remove(i);
        if (!model.isEmpty()) select(Math.min(i, model.size() - 1));
        changed();
    }

    private void move(int delta) {
        int i = strip.getSelectedIndex();
        if (i < 0) { Ui.error(this, "حدد صورة."); return; }
        int j = i + delta;
        if (j < 0 || j >= model.size()) return;
        model.add(j, model.remove(i));
        select(j);
        changed();
    }

    private void makeMain() {
        int i = strip.getSelectedIndex();
        if (i < 0) { Ui.error(this, "حدد صورة."); return; }
        if (i == 0) return;
        model.add(0, model.remove(i));
        select(0);
        changed();
    }

    private void changed() {
        strip.repaint();
        refreshPreview();
        onChange.run();
    }

    // ------------------------------------------------------------------ العرض

    private void step(int delta) {
        if (model.isEmpty()) return;
        int i = strip.getSelectedIndex();
        select(((i < 0 ? 0 : i) + delta + model.size()) % model.size());
    }

    private void select(int i) {
        strip.setSelectedIndex(i);
        strip.ensureIndexIsVisible(i);
    }

    private void refreshPreview() {
        int i = strip.getSelectedIndex();
        Item it = i < 0 ? null : model.get(i);
        preview.setImage(it == null ? null : image(it.file()));
        preview.setEmptyText(model.isEmpty() ? (editable ? "لا توجد صور — اضغط \"إضافة صور...\"" : "لا توجد صور") : "ملف الصورة مفقود");
        prev.setEnabled(model.size() > 1);
        next.setEnabled(model.size() > 1);
        if (it == null) {
            info.setText(model.isEmpty() ? " " : model.size() + " صور");
            preview.setToolTipText(null);
        } else {
            info.setText("صورة " + (i + 1) + " من " + model.size()
                    + (i == 0 ? " — رئيسية" : "") + (it.isNew() ? " — جديدة" : ""));
            preview.setToolTipText(it.title() + "  (نقر مزدوج للحجم الكامل)");
        }
    }

    private Image image(Path file) {
        if (file == null) return null;
        return images.computeIfAbsent(file, f -> {
            ImageIcon icon = new ImageIcon(f.toString());
            return icon.getIconWidth() > 0 ? icon.getImage() : null;
        });
    }

    private ImageIcon thumb(Path file) {
        if (file == null) return null;
        return thumbs.computeIfAbsent(file, f -> {
            Image img = image(f);
            if (img == null) return null;
            int w = img.getWidth(null), h = img.getHeight(null);
            double s = Math.min((double) THUMB / w, (double) THUMB / h);
            return new ImageIcon(img.getScaledInstance(Math.max(1, (int) (w * s)), Math.max(1, (int) (h * s)), Image.SCALE_SMOOTH));
        });
    }

    private void showFullSize() {
        int i = strip.getSelectedIndex();
        if (i < 0) return;
        Item it = model.get(i);
        Image img = image(it.file());
        if (img == null) { Ui.error(this, "تعذّر عرض الصورة."); return; }
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog d = new JDialog(owner, it.title());
        Preview big = new Preview();
        big.setImage(img);
        big.setPreferredSize(new Dimension(Math.min(1100, img.getWidth(null) + 20), Math.min(800, img.getHeight(null) + 20)));
        d.add(big);
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    /** يرسم الصورة بحجم يناسب المساحة المتاحة (يتغير مع تكبير النافذة). */
    private static class Preview extends JComponent {
        private Image image;
        private String emptyText = "";

        Preview() {
            setPreferredSize(new Dimension(320, 260));
            setMinimumSize(new Dimension(160, 140));
            setBorder(BorderFactory.createLineBorder(new Color(0xE5E7EB)));
        }

        void setImage(Image image) { this.image = image; repaint(); }

        void setEmptyText(String text) { this.emptyText = text; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (image == null) {
                g2.setColor(Color.GRAY);
                int tw = g2.getFontMetrics().stringWidth(emptyText);
                g2.drawString(emptyText, (getWidth() - tw) / 2, getHeight() / 2);
            } else {
                int iw = image.getWidth(null), ih = image.getHeight(null);
                double s = Math.min(1.0, Math.min((getWidth() - 12.0) / iw, (getHeight() - 12.0) / ih));
                int w = (int) (iw * s), h = (int) (ih * s);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            }
            g2.dispose();
        }
    }

    private class ThumbRenderer extends JLabel implements ListCellRenderer<Item> {
        ThumbRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> l, Item it, int index, boolean sel, boolean focus) {
            ImageIcon icon = thumb(it.file());
            setIcon(icon);
            setText((index + 1) + (index == 0 ? " ★" : "") + (icon == null ? " ✕" : ""));
            setToolTipText(it.title());
            setBackground(sel ? UIManager.getColor("List.selectionBackground") : l.getBackground());
            setForeground(sel ? UIManager.getColor("List.selectionForeground") : (icon == null ? Color.RED.darker() : l.getForeground()));
            return this;
        }
    }
}
