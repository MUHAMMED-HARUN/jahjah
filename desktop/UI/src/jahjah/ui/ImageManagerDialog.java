package jahjah.ui;

import jahjah.shared.entities.ImageInput;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** نافذة مستقلة كبيرة لإدارة صور المنتج. التغييرات تُعتمد فقط عند الضغط على "اعتماد". */
final class ImageManagerDialog extends JDialog {
    private final ImageManager manager;
    private List<ImageInput> result;

    private ImageManagerDialog(Window owner, String productName, Function<String, Path> resolver, List<ImageInput> current) {
        super(owner, "صور المنتج" + (productName == null || productName.isBlank() ? "" : " — " + productName),
                ModalityType.APPLICATION_MODAL);

        manager = new ImageManager(resolver, true);
        manager.setInputs(current);
        manager.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));

        JButton ok = new JButton("اعتماد");
        JButton cancel = new JButton("إلغاء");
        ok.addActionListener(e -> {
            result = manager.getInputs();
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(ok);
        buttons.add(cancel);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(manager, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        applyComponentOrientation(Ui.RTL);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(900, screen.width - 80), Math.min(700, screen.height - 100));
        setMinimumSize(new Dimension(520, 420));
        setLocationRelativeTo(owner);
    }

    /** يعيد القائمة الجديدة بالترتيب، أو null عند الإلغاء. */
    static List<ImageInput> edit(Component parent, String productName, Function<String, Path> resolver, List<ImageInput> current) {
        ImageManagerDialog d = new ImageManagerDialog(SwingUtilities.getWindowAncestor(parent), productName, resolver, current);
        d.setVisible(true);
        return d.result;
    }
}
