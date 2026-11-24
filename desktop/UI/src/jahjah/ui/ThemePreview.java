package jahjah.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * معاينة حيّة لعينة بسيطة من الواجهة تتأثر بكل الألوان:
 * رأس الصفحة، أزرار الشاشات مع عداد السلة، كارد منتج (شارة الماركة، الرمز، السعر، التعبئة،
 * التوفر، زر الإضافة)، حالة "في السلة"، أزرار السلة (إرسال، إزالة، تفريغ)، ودوائر التوفر.
 */
class ThemePreview extends JComponent {
    private final Map<String, Color> c = new HashMap<>();
    private String highlight;

    ThemePreview() {
        setPreferredSize(new Dimension(560, 620));
    }

    void setColor(String key, Color color) {
        c.put(key, color);
        repaint();
    }

    /** يُبرز العناصر التي يؤثر عليها لون معيّن (عند تحديد صفّه). */
    void setHighlight(String key) {
        this.highlight = key;
        repaint();
    }

    private Color col(String key) {
        return c.getOrDefault(key, Color.MAGENTA);
    }

    private static Color mix(Color a, Color b, double wa) {
        double wb = 1 - wa;
        return new Color((int) Math.round(a.getRed() * wa + b.getRed() * wb),
                (int) Math.round(a.getGreen() * wa + b.getGreen() * wb),
                (int) Math.round(a.getBlue() * wa + b.getBlue() * wb));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        Font bold = base.deriveFont(Font.BOLD);
        int W = getWidth();
        int R = W - 20; // الحافة اليمنى (RTL)

        Color white = Color.WHITE;
        Color accentStrong = mix(col("accent"), Color.BLACK, 0.72);
        Color successSoft = mix(col("success"), col("surface"), 0.14);
        Color dangerBorder = mix(col("danger"), col("surface"), 0.35);

        // خلفية الصفحة
        g.setColor(col("background"));
        g.fillRect(0, 0, W, getHeight());
        mark(g, "background", 2, 2, W - 4, getHeight() - 4);

        // نقوش الخلفية الشفافة (DCEL / dsp / LAPÔN)
        Color p = col("pattern");
        Color faint = new Color(p.getRed(), p.getGreen(), p.getBlue(), 26);
        Font patternFont = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 22);
        String[] words = {"DCEL", "dsp", "LAPÔN"};
        for (int row = 0, yy = 110; yy < getHeight(); row++, yy += 120) {
            for (int col = 0, xx = (row % 2) * 90 - 40; xx < W; col++, xx += 190) {
                text(g, words[(col + row) % 3], xx + 90, yy, patternFont, faint);
            }
        }
        mark(g, "pattern", 6, 92, W - 12, getHeight() - 120);

        // رأس الصفحة
        text(g, "كتالوج المنتجات", R, 30, bold.deriveFont(11f), col("accent"));
        mark(g, "accent", R - 100, 16, 100, 20);
        text(g, "المنتجات", R, 58, bold.deriveFont(22f), col("text"));
        text(g, "تصفّح حسب الماركة وأضف ما تريد إلى السلة.", R, 80, base.deriveFont(12f), col("mutedText"));
        mark(g, "mutedText", R - 250, 66, 250, 20);

        // أزرار الشاشات + عداد السلة
        pill(g, 20, 30, 190, 40, col("surface"), col("border"));
        pill(g, 110, 35, 95, 30, col("primary"), null);
        text(g, "المنتجات", 195, 55, bold, white);
        text(g, "السلة", 95, 55, bold, col("mutedText"));
        g.setColor(col("accent"));
        g.fillOval(30, 39, 22, 22);
        textCenter(g, "2", 41, 55, bold.deriveFont(11f), white);
        mark(g, "primary", 108, 33, 99, 34);

        // كارد منتج
        int cx = 20, cy = 100, cw = 300, ch = 395;
        round(g, cx, cy, cw, ch, 16, col("surface"), col("border"));
        mark(g, "surface", cx, cy, cw, ch);
        mark(g, "border", cx - 2, cy - 2, cw + 4, ch + 4);
        // الصورة البديلة
        g.setColor(col("accentSoft"));
        g.fillRect(cx + 1, cy + 1, cw - 2, 130);
        textCenter(g, "م", cx + cw / 2, cy + 80, bold.deriveFont(40f), col("primary"));
        mark(g, "accentSoft", cx + 1, cy + 1, cw - 2, 130);
        // دائرة التوفر
        g.setColor(col("stockLot"));
        g.fillOval(cx + 14, cy + 14, 14, 14);
        int y = cy + 145;
        // شارة الماركة + الرمز
        pill(g, cx + cw - 60, y, 46, 24, col("accentSoft"), null);
        textCenter(g, "DSP", cx + cw - 37, y + 17, bold.deriveFont(11f), accentStrong);
        round(g, cx + 14, y, 70, 24, 8, col("primarySoft"), null);
        textCenter(g, "KA3002", cx + 49, y + 17, bold.deriveFont(11f), col("primary"));
        mark(g, "primarySoft", cx + 12, y - 2, 74, 28);
        y += 46;
        text(g, "مطحنة قهوة", cx + cw - 14, y, bold.deriveFont(15f), col("text"));
        mark(g, "text", cx + cw - 110, y - 16, 98, 22);
        text(g, "تحضير القهوة", cx + cw - 14, y + 20, base.deriveFont(11f), col("mutedText"));
        text(g, "طاحونة كهربائية متينة لطحن البن...", cx + cw - 14, y + 44, base.deriveFont(12f), mix(col("text"), col("surface"), 0.85));
        text(g, "المزيد...", cx + cw - 14, y + 64, bold.deriveFont(12f), col("primary"));
        y += 82;
        g.setColor(mix(col("border"), col("surface"), 0.55));
        g.drawLine(cx + 14, y, cx + cw - 14, y);
        text(g, "$11", cx + cw - 14, y + 26, bold.deriveFont(18f), col("primary"));
        mark(g, "primary", cx + cw - 56, y + 6, 44, 26);
        text(g, "24 / كرتون", cx + 110, y + 24, bold.deriveFont(12f), col("mutedText"));
        y += 40;
        // نوع التعبئة
        round(g, cx + cw - 124, y, 110, 32, 10, mix(col("background"), col("surface"), 0.45), col("border"));
        round(g, cx + cw - 68, y + 3, 52, 26, 8, col("primary"), null);
        textCenter(g, "طرد", cx + cw - 42, y + 21, bold.deriveFont(12f), white);
        mark(g, "primary", cx + cw - 70, y + 1, 56, 30);
        textCenter(g, "قطعة", cx + cw - 96, y + 21, bold.deriveFont(12f), col("mutedText"));
        // الكمية
        round(g, cx + 14, y, 140, 32, 10, col("surface"), col("border"));
        textCenter(g, "−", cx + 30, y + 22, bold.deriveFont(15f), col("primary"));
        textCenter(g, "1", cx + 84, y + 21, bold, col("text"));
        textCenter(g, "+", cx + 138, y + 22, bold.deriveFont(15f), col("primary"));
        y += 42;
        // زر الإضافة (عادي + عند المرور)
        round(g, cx + 14, y, cw - 28, 34, 10, col("primary"), null);
        textCenter(g, "أضف إلى السلة", cx + cw / 2, y + 22, bold, white);
        mark(g, "primary", cx + 12, y - 2, cw - 24, 38);

        // العمود الأيسر: حالات وأزرار
        int lx = 340, lw = W - lx - 20;
        int ly = 100;
        text(g, "عند المرور:", lx + lw, ly + 14, base.deriveFont(11f), col("mutedText"));
        round(g, lx, ly + 22, lw, 34, 10, col("primaryHover"), null);
        textCenter(g, "أضف إلى السلة", lx + lw / 2, ly + 44, bold, white);
        mark(g, "primaryHover", lx - 2, ly + 20, lw + 4, 38);

        ly += 72;
        text(g, "منتج في السلة:", lx + lw, ly + 14, base.deriveFont(11f), col("mutedText"));
        round(g, lx, ly + 22, lw, 34, 10, successSoft, null);
        textCenter(g, "في السلة", lx + lw / 2, ly + 44, bold, col("success"));

        ly += 72;
        text(g, "السلة:", lx + lw, ly + 14, base.deriveFont(11f), col("mutedText"));
        round(g, lx, ly + 22, lw, 38, 12, col("success"), null);
        textCenter(g, "إرسال إلى واتساب", lx + lw / 2, ly + 46, bold, white);
        mark(g, "success", lx - 2, ly + 20, lw + 4, 42);
        round(g, lx, ly + 70, lw / 2 - 4, 32, 10, col("surface"), dangerBorder);
        textCenter(g, "إزالة", lx + lw / 4, ly + 91, bold, mix(col("danger"), Color.BLACK, 0.85));
        round(g, lx + lw / 2 + 4, ly + 70, lw / 2 - 4, 32, 10, col("danger"), null);
        textCenter(g, "نعم، فرّغ", lx + lw * 3 / 4, ly + 91, bold, white);
        mark(g, "danger", lx - 2, ly + 68, lw + 4, 36);

        ly += 124;
        text(g, "دوائر التوفر:", lx + lw, ly + 14, base.deriveFont(11f), col("mutedText"));
        String[][] stock = {{"stockLot", "كثير"}, {"stockMiddle", "متوسط"}, {"stockLittle", "قليل"}, {"stockNothing", "منتهي"}};
        int sy = ly + 30;
        for (String[] s : stock) {
            g.setColor(col(s[0]));
            g.fillOval(lx + lw - 14, sy, 14, 14);
            text(g, s[1], lx + lw - 22, sy + 12, bold.deriveFont(12f), col("text"));
            mark(g, s[0], lx + lw - 70, sy - 3, 70, 20);
            sy += 24;
        }

        // تذييل
        text(g, "النقاط تحت الصورة وعداد السلة والعنوان الصغير تستخدم اللون الثانوي.", R, getHeight() - 18,
                base.deriveFont(11f), col("mutedText"));
        g.dispose();
    }

    /** إطار متقطع حول العناصر المتأثرة باللون المحدد. */
    private void mark(Graphics2D g, String key, int x, int y, int w, int h) {
        if (!key.equals(highlight)) return;
        Graphics2D m = (Graphics2D) g.create();
        m.setColor(new Color(0x2563eb));
        m.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
        m.drawRoundRect(x, y, w, h, 10, 10);
        m.dispose();
    }

    private static void text(Graphics2D g, String s, int right, int baseline, Font f, Color color) {
        g.setFont(f);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, right - fm.stringWidth(s), baseline);
    }

    private static void textCenter(Graphics2D g, String s, int cx, int baseline, Font f, Color color) {
        g.setFont(f);
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }

    private static void round(Graphics2D g, int x, int y, int w, int h, int r, Color fill, Color stroke) {
        if (fill != null) {
            g.setColor(fill);
            g.fillRoundRect(x, y, w, h, r, r);
        }
        if (stroke != null) {
            g.setColor(stroke);
            g.drawRoundRect(x, y, w, h, r, r);
        }
    }

    private static void pill(Graphics2D g, int x, int y, int w, int h, Color fill, Color stroke) {
        round(g, x, y, w, h, h, fill, stroke);
    }
}
