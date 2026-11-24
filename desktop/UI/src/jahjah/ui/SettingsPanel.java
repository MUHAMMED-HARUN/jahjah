package jahjah.ui;

import jahjah.bal.ConfigService;
import jahjah.bal.Services;
import jahjah.shared.entities.AppConfig;
import jahjah.shared.enums.EnMessageType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/** تعديل config.json: إعدادات خدمة الطلب + ألوان الواجهة مع معاينة حيّة. */
class SettingsPanel extends JPanel {
    private final Services services;
    private final JTextField number = new JTextField(22);
    private final JTextField ttlDays = new JTextField(6);
    private final JComboBox<EnMessageType> messageType = new JComboBox<>(EnMessageType.values());
    private final ThemePreview preview = new ThemePreview();
    private final ThemeEditor themeEditor = new ThemeEditor(preview);

    SettingsPanel(Services services) {
        super(new BorderLayout(8, 8));
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- إعدادات الطلب
        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        row = addRow(form, row, "رقم واتساب لاستقبال الطلبات *", number,
                "بالصيغة الدولية مع رمز الدولة، مثل: 963912345678");
        row = addRow(form, row, "مدة بقاء عناصر السلة (بالأيام) *", ttlDays,
                "بعدها تُحذف العناصر من سلة المتصفح تلقائياً (" + ConfigService.MIN_TTL_DAYS + "–" + ConfigService.MAX_TTL_DAYS + ").");
        addRow(form, row, "نوع رسالة الطلب", messageType,
                "صورة طويلة: صف لكل منتج والملاحظات في آخرها. رسالة نصية: نفس البيانات كنص.");
        form.setBorder(BorderFactory.createTitledBorder("خدمة الطلب"));

        // ---- الألوان: القائمة + المعاينة
        JScrollPane editorScroll = new JScrollPane(themeEditor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        editorScroll.getVerticalScrollBar().setUnitIncrement(18);
        editorScroll.setBorder(BorderFactory.createTitledBorder("ألوان الواجهة (حدّد لوناً لإبراز مكانه في المعاينة)"));

        JScrollPane previewScroll = new JScrollPane(preview);
        previewScroll.setBorder(BorderFactory.createTitledBorder("معاينة"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScroll, previewScroll);
        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);

        // ---- الأزرار
        JButton save = new JButton("حفظ الإعدادات");
        JButton reload = new JButton("إعادة تحميل");
        JButton resetColors = new JButton("استعادة ألوان العلامة");
        save.setFont(save.getFont().deriveFont(Font.BOLD));
        save.addActionListener(e -> onSave());
        reload.addActionListener(e -> reload());
        resetColors.addActionListener(e -> themeEditor.setTheme(services.config().defaultTheme()));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(reload);
        buttons.add(resetColors);
        JLabel hint = new JLabel("التغييرات تُطبَّق في صفحة المنتجات بعد الحفظ وإعادة تحميل الصفحة.");
        hint.setForeground(Color.GRAY);
        buttons.add(hint);

        add(form, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reload();
    }

    private static int addRow(JPanel form, int row, String label, JComponent field, String hint) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 0, 4);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 2;
        c.weightx = 1;
        JLabel h = new JLabel(hint);
        h.setForeground(Color.GRAY);
        form.add(h, c);
        return row + 1;
    }

    void reload() {
        Ui.run(this, () -> {
            AppConfig c = services.config().get();
            number.setText(c.getWhatsappNumber());
            ttlDays.setText(String.valueOf(c.getCartTtlDays()));
            messageType.setSelectedItem(c.getMessageType());
            themeEditor.setTheme(c.getTheme());
            applyComponentOrientation(Ui.RTL);
        });
    }

    /** Data validation ثم الحفظ عبر BAL (الرقم، المدة، النوع، والألوان معاً). */
    private void onSave() {
        List<String> errors = new ArrayList<>();
        String n = Ui.toLatinDigits(number.getText().trim());
        if (n.isEmpty()) errors.add("رقم واتساب مطلوب.");
        else if (!n.matches("[+0-9 ()-]+")) errors.add("رقم واتساب يجب أن يحتوي على أرقام فقط (ويُسمح بـ + والمسافات والشرطات).");

        Integer days = null;
        String d = Ui.toLatinDigits(ttlDays.getText().trim());
        if (!d.matches("\\d{1,4}")) errors.add("مدة البقاء يجب أن تكون عدداً صحيحاً من الأيام.");
        else days = Integer.parseInt(d);

        errors.addAll(themeEditor.validateInput());

        if (!errors.isEmpty()) {
            Ui.error(this, String.join("\n", errors));
            return;
        }

        AppConfig c = new AppConfig(n, days, (EnMessageType) messageType.getSelectedItem());
        c.setTheme(themeEditor.getTheme());
        AppConfig[] saved = new AppConfig[1];
        if (Ui.run(this, () -> saved[0] = services.config().update(c))) {
            number.setText(saved[0].getWhatsappNumber());
            themeEditor.setTheme(saved[0].getTheme());
            Ui.info(this, "تم حفظ الإعدادات والألوان في config.json.\nستُطبَّق في صفحة المنتجات عند إعادة تحميلها.");
        }
    }
}
