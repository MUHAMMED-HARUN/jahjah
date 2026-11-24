package jahjah.shared.enums;

/** شكل رسالة الطلب المرسلة إلى واتساب. */
public enum EnMessageType {
    IMAGE("image", "صورة طويلة"),
    TEXT("text", "رسالة نصية");

    private final String jsonValue;
    private final String label;

    EnMessageType(String jsonValue, String label) {
        this.jsonValue = jsonValue;
        this.label = label;
    }

    public String getJsonValue() { return jsonValue; }

    public String getLabel() { return label; }

    public static EnMessageType fromJson(String value) {
        return "text".equalsIgnoreCase(value == null ? "" : value.trim()) ? TEXT : IMAGE;
    }

    @Override
    public String toString() { return label; }
}
