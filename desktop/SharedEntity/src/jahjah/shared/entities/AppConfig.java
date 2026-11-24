package jahjah.shared.entities;

import jahjah.shared.enums.EnMessageType;

import java.util.ArrayList;
import java.util.List;

/** إعدادات الواجهة (config.json): رقم واتساب، مدة بقاء السلة بالأيام، نوع الرسالة، والألوان. */
public class AppConfig {
    private String whatsappNumber = "";
    private int cartTtlDays = 7;
    private EnMessageType messageType = EnMessageType.IMAGE;
    /** ألوان الواجهة بالترتيب. */
    private List<ThemeColor> theme = new ArrayList<>();

    public AppConfig() { }

    public AppConfig(String whatsappNumber, int cartTtlDays, EnMessageType messageType) {
        this.whatsappNumber = whatsappNumber;
        this.cartTtlDays = cartTtlDays;
        this.messageType = messageType;
    }

    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber == null ? "" : whatsappNumber; }

    public int getCartTtlDays() { return cartTtlDays; }
    public void setCartTtlDays(int cartTtlDays) { this.cartTtlDays = cartTtlDays; }

    public EnMessageType getMessageType() { return messageType; }
    public void setMessageType(EnMessageType messageType) { this.messageType = messageType == null ? EnMessageType.IMAGE : messageType; }

    public List<ThemeColor> getTheme() { return theme; }
    public void setTheme(List<ThemeColor> theme) {
        this.theme = new ArrayList<>();
        if (theme != null) for (ThemeColor c : theme) this.theme.add(c.copy());
    }

    public AppConfig copy() {
        AppConfig c = new AppConfig(whatsappNumber, cartTtlDays, messageType);
        c.setTheme(theme);
        return c;
    }
}
