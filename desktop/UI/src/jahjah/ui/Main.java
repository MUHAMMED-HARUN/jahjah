package jahjah.ui;

import jahjah.bal.Services;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

public final class Main {
    private Main() { }

    /** الوسيط الأول (اختياري): مجلد المشروع الذي يحوي "data base" و "images". */
    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0] : System.getProperty("user.dir")).toAbsolutePath().normalize();

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // نكمل بالمظهر الافتراضي
            }
            setDefaultFont(new Font("Segoe UI", Font.PLAIN, 14));

            if (!Files.isDirectory(root.resolve("data base"))) {
                Ui.error(null, "لم يتم العثور على مجلد \"data base\" داخل:\n" + root
                        + "\n\nشغّل البرنامج من مجلد المشروع أو مرّر مساره كوسيط.");
                System.exit(1);
            }

            final Services[] services = new Services[1];
            if (!Ui.run(null, () -> services[0] = Services.create(root))) System.exit(1);

            MainFrame frame = new MainFrame(services[0], root);
            frame.setVisible(true);
            if (services[0].migratedProducts() > 0) {
                Ui.info(frame, "تم تحويل التصنيف النصي القديم لـ " + services[0].migratedProducts()
                        + " منتجاً إلى جدول التصنيفات.");
            }
        });
    }

    private static void setDefaultFont(Font font) {
        FontUIResource f = new FontUIResource(font);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, f);
        }
    }
}
