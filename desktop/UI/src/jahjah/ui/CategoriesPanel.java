package jahjah.ui;

import jahjah.bal.Services;
import jahjah.shared.entities.Category;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** شجرة التصنيفات مع الدوال: add item، addChild، update. */
class CategoriesPanel extends JPanel {
    private final Services services;
    private final Runnable onChanged;
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("التصنيفات");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree tree = new JTree(treeModel);

    /** عنصر الشجرة. */
    private record Node(Category category, boolean used) {
        @Override
        public String toString() {
            String suffix = category.isLeef() ? (used ? "  🍃 (مستخدمة)" : "  🍃") : "";
            return category.getCategoryName() + suffix;
        }
    }

    CategoriesPanel(Services services, Runnable onChanged) {
        super(new BorderLayout(6, 6));
        this.services = services;
        this.onChanged = onChanged;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton addRoot = new JButton("إضافة تصنيف رئيسي");
        JButton addChild = new JButton("إضافة ابن");
        JButton rename = new JButton("تعديل الاسم");
        JButton refresh = new JButton("تحديث العرض");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(addRoot);
        actions.add(addChild);
        actions.add(rename);
        actions.add(refresh);
        add(actions, BorderLayout.NORTH);

        tree.setRowHeight(24);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(new JLabel("🍃 = ورقة (يمكن ربط المنتجات بها). الورقة المستخدمة لا يمكن إضافة أبناء لها."), BorderLayout.SOUTH);

        addRoot.addActionListener(e -> onAdd(null));
        addChild.addActionListener(e -> {
            Category selected = selected();
            if (selected == null) Ui.error(this, "حدد التصنيف الأب أولاً.");
            else onAdd(selected);
        });
        rename.addActionListener(e -> onRename());
        refresh.addActionListener(e -> reload());

        reload();
    }

    void reload() {
        Integer keep = selected() == null ? null : selected().getCategoryId();
        List<Category> all = services.categories().getAll();
        all.sort(Comparator.comparing(Category::getCategoryName));

        rootNode.removeAllChildren();
        Map<Integer, DefaultMutableTreeNode> nodes = new HashMap<>();
        for (Category c : all) {
            boolean used = c.isLeef() && services.categories().isUsed(c.getCategoryId());
            nodes.put(c.getCategoryId(), new DefaultMutableTreeNode(new Node(c, used)));
        }
        for (Category c : all) {
            DefaultMutableTreeNode parent = c.getCategoryFather() == null ? rootNode : nodes.getOrDefault(c.getCategoryFather(), rootNode);
            parent.add(nodes.get(c.getCategoryId()));
        }
        treeModel.reload();
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);

        if (keep != null && nodes.containsKey(keep)) {
            TreePath path = new TreePath(nodes.get(keep).getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }

    private Category selected() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        Object user = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        return user instanceof Node n ? n.category() : null;
    }

    private void onAdd(Category father) {
        String title = father == null ? "تصنيف رئيسي جديد" : "ابن جديد لـ: " + services.categories().getPath(father.getCategoryId());
        String name = askName(title, "");
        if (name == null) return;
        boolean ok = Ui.run(this, () -> {
            if (father == null) services.categories().addItem(name, null);
            else services.categories().addChild(father.getCategoryId(), name);
        });
        if (ok) changed();
    }

    private void onRename() {
        Category c = selected();
        if (c == null) {
            Ui.error(this, "حدد تصنيفاً للتعديل.");
            return;
        }
        String name = askName("تعديل اسم: " + services.categories().getPath(c.getCategoryId()), c.getCategoryName());
        if (name == null || Objects.equals(name, c.getCategoryName())) return;
        if (Ui.run(this, () -> services.categories().update(c.getCategoryId(), name))) changed();
    }

    /** Data validation لاسم التصنيف. يعيد null عند الإلغاء أو الخطأ. */
    private String askName(String title, String initial) {
        Object value = JOptionPane.showInputDialog(this, "اسم التصنيف:", title, JOptionPane.PLAIN_MESSAGE, null, null, initial);
        if (value == null) return null;
        String name = value.toString().trim();
        String err = Ui.required(name, "اسم التصنيف", 80);
        if (err != null) {
            Ui.error(this, err);
            return null;
        }
        return name;
    }

    private void changed() {
        reload();
        onChanged.run();
    }
}
