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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/** تعديل config.json الخاص بخدمة الطلب في الواجهة. */
class SettingsPanel extends JPanel {
    private final Services services;
    private final JTextField number = new JTextField(22);
    private final JTextField ttlDays = new JTextField(6);
    private final JComboBox<EnMessageType> messageType = new JComboBox<>(EnMessageType.values());

    SettingsPanel(Services services) {
        super(new BorderLayout());
        this.services = services;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        row = addRow(form, row, "رقم واتساب لاستقبال الطلبات *", number,
                "بالصيغة الدولية مع رمز الدولة، مثل: 963912345678");
        row = addRow(form, row, "مدة بقاء عناصر السلة (بالأيام) *", ttlDays,
                "بعدها تُحذف العناصر من سلة المتصفح تلقائياً (" + ConfigService.MIN_TTL_DAYS + "–" + ConfigService.MAX_TTL_DAYS + ").");
        addRow(form, row, "نوع رسالة الطلب", messageType,
                "صورة طويلة: صف لكل منتج والملاحظات في آخرها. رسالة نصية: نفس البيانات كنص.");

        JButton save = new JButton("حفظ الإعدادات");
        JButton reload = new JButton("إعادة تحميل");
        save.addActionListener(e -> onSave());
        reload.addActionListener(e -> reload());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(reload);

        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("إعدادات خدمة الطلب (config.json)");
        title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() + 3f));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        top.add(title, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        reload();
    }

    private static int addRow(JPanel form, int row, String label, JComponent field, String hint) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 0, 4);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.LINE_START;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
        c.gridx = 1;
        c.gridy = row + 1;
        c.insets = new Insets(0, 4, 6, 4);
        JLabel h = new JLabel(hint);
        h.setForeground(Color.GRAY);
        form.add(h, c);
        return row + 2;
    }

    void reload() {
        Ui.run(this, () -> {
            AppConfig c = services.config().get();
            number.setText(c.getWhatsappNumber());
            ttlDays.setText(String.valueOf(c.getCartTtlDays()));
            messageType.setSelectedItem(c.getMessageType());
        });
    }

    /** Data validation ثم الحفظ عبر BAL. */
    private void onSave() {
        List<String> errors = new ArrayList<>();
        String n = Ui.toLatinDigits(number.getText().trim());
        if (n.isEmpty()) errors.add("رقم واتساب مطلوب.");
        else if (!n.matches("[+0-9 ()-]+")) errors.add("رقم واتساب يجب أن يحتوي على أرقام فقط (ويُسمح بـ + والمسافات والشرطات).");

        Integer days = null;
        String d = Ui.toLatinDigits(ttlDays.getText().trim());
        if (!d.matches("\\d{1,4}")) errors.add("مدة البقاء يجب أن تكون عدداً صحيحاً من الأيام.");
        else days = Integer.parseInt(d);

        if (!errors.isEmpty()) {
            Ui.error(this, String.join("\n", errors));
            return;
        }

        AppConfig c = new AppConfig(n, days, (EnMessageType) messageType.getSelectedItem());
        AppConfig[] saved = new AppConfig[1];
        if (Ui.run(this, () -> saved[0] = services.config().update(c))) {
            number.setText(saved[0].getWhatsappNumber());
            Ui.info(this, "تم حفظ الإعدادات في config.json.\nستُطبَّق في صفحة المنتجات عند إعادة تحميلها.");
        }
    }
}
