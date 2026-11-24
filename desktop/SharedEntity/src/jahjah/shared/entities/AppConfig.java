package jahjah.shared.entities;

import jahjah.shared.enums.EnMessageType;

/** إعدادات الواجهة (config.json): رقم واتساب، مدة بقاء السلة بالأيام، ونوع الرسالة. */
public class AppConfig {
    private String whatsappNumber = "";
    private int cartTtlDays = 7;
    private EnMessageType messageType = EnMessageType.IMAGE;

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

    public AppConfig copy() { return new AppConfig(whatsappNumber, cartTtlDays, messageType); }
}
