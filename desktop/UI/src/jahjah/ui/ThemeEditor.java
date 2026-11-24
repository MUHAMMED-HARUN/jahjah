package jahjah.ui;

import jahjah.shared.entities.ThemeColor;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** قائمة ألوان الواجهة: مربع اللون (يفتح منتقي الألوان) + الاسم والوصف + حقل #RRGGBB. */
class ThemeEditor extends JPanel {
    private final ThemePreview preview;
    private final List<Row> rows = new ArrayList<>();

    private final class Row {
        final ThemeColor color;
        final JButton swatch = new JButton();
        final JTextField hex = new JTextField(8);

        Row(ThemeColor color) {
            this.color = color.copy();
        }
    }

    ThemeEditor(ThemePreview preview) {
        super(new GridBagLayout());
        this.preview = preview;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    /** حقول الألوان تبقى من اليسار لليمين (#RRGGBB) حتى داخل واجهة RTL. */
    @Override
    public void applyComponentOrientation(java.awt.ComponentOrientation o) {
        super.applyComponentOrientation(o);
        for (Row r : rows) r.hex.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
    }

    void setTheme(List<ThemeColor> theme) {
        removeAll();
        rows.clear();
        int y = 0;
        for (ThemeColor t : theme) {
            Row row = new Row(t);
            rows.add(row);
            addRow(row, y++);
            apply(row, t.getColor());
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = y;
        filler.weighty = 1;
        add(new JPanel(), filler);
        applyComponentOrientation(Ui.RTL);
        revalidate();
        repaint();
    }

    private void addRow(Row row, int y) {
        row.swatch.setPreferredSize(new Dimension(44, 32));
        row.swatch.setToolTipText("اختيار اللون");
        row.swatch.addActionListener(e -> choose(row));

        row.hex.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        row.hex.setHorizontalAlignment(JTextField.CENTER);
        row.hex.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onHexTyped(row); }
            @Override public void removeUpdate(DocumentEvent e) { onHexTyped(row); }
            @Override public void changedUpdate(DocumentEvent e) { onHexTyped(row); }
        });

        JLabel title = new JLabel("<html><div style='width:260px'><b>" + escape(row.color.getLabel()) + "</b></div></html>");
        JLabel desc = new JLabel("<html><div style='width:260px'>" + escape(row.color.getDescription()) + "</div></html>");
        desc.setForeground(Color.GRAY);
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(desc, BorderLayout.CENTER);

        // تحديد الصف يُبرز العناصر المتأثرة في المعاينة
        FocusAdapter focus = new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { preview.setHighlight(row.color.getKey()); }
        };
        row.hex.addFocusListener(focus);
        row.swatch.addFocusListener(focus);
        text.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { row.hex.requestFocusInWindow(); }
        });

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.insets = new Insets(6, 4, 6, 4);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        add(row.swatch, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(text, c);
        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        add(row.hex, c);
    }

    private void choose(Row row) {
        preview.setHighlight(row.color.getKey());
        Color current = parse(row.hex.getText());
        Color chosen = JColorChooser.showDialog(this, "اختيار لون: " + row.color.getLabel(), current == null ? Color.WHITE : current);
        if (chosen != null) apply(row, toHex(chosen));
    }

    private void apply(Row row, String hex) {
        if (!row.hex.getText().equalsIgnoreCase(hex)) row.hex.setText(hex);
        onHexTyped(row);
    }

    private void onHexTyped(Row row) {
        Color color = parse(row.hex.getText());
        boolean ok = color != null;
        row.hex.setForeground(ok ? Color.BLACK : new Color(0xb42318));
        row.hex.setBorder(ok ? new JTextField().getBorder() : BorderFactory.createLineBorder(new Color(0xb42318), 2));
        if (!ok) return;
        row.swatch.setIcon(new SwatchIcon(color));
        preview.setColor(row.color.getKey(), color);
    }

    /** Data validation: كل الحقول بصيغة #RRGGBB. يعيد قائمة الأخطاء. */
    List<String> validateInput() {
        List<String> errors = new ArrayList<>();
        for (Row r : rows) {
            if (parse(r.hex.getText()) == null) {
                errors.add("لون \"" + r.color.getLabel() + "\" غير صالح: " + r.hex.getText().trim() + " (الصيغة #RRGGBB)");
            }
        }
        return errors;
    }

    List<ThemeColor> getTheme() {
        List<ThemeColor> list = new ArrayList<>();
        for (Row r : rows) {
            ThemeColor t = r.color.copy();
            t.setColor(r.hex.getText().trim());
            list.add(t);
        }
        return list;
    }

    static Color parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (!s.startsWith("#")) s = "#" + s;
        if (!s.matches("#[0-9a-fA-F]{6}")) return null;
        return new Color(Integer.parseInt(s.substring(1), 16));
    }

    static String toHex(Color c) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class SwatchIcon implements Icon {
        private final Color color;

        SwatchIcon(Color color) { this.color = color; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRoundRect(x, y, getIconWidth(), getIconHeight(), 6, 6);
            g.setColor(Color.GRAY);
            g.drawRoundRect(x, y, getIconWidth() - 1, getIconHeight() - 1, 6, 6);
        }

        @Override public int getIconWidth() { return 28; }

        @Override public int getIconHeight() { return 18; }
    }
}
