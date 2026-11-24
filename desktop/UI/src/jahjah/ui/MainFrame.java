package jahjah.ui;

import jahjah.bal.Services;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.nio.file.Path;

class MainFrame extends JFrame {

    MainFrame(Services services, Path root) {
        super("إدارة المنتجات — " + root);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        ProductsPanel products = new ProductsPanel(services);
        CategoriesPanel categories = new CategoriesPanel(services, products::reload);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("المنتجات", products);
        tabs.addTab("التصنيفات", categories);
        SettingsPanel settings = new SettingsPanel(services);
        tabs.addTab("الإعدادات", settings);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == products) products.reload();
            else if (tabs.getSelectedComponent() == categories) categories.reload();
            else settings.reload();
        });
        setContentPane(tabs);

        applyComponentOrientation(Ui.RTL);
        setSize(1350, 780);
        setLocationRelativeTo(null);
    }
}
