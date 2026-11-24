package jahjah.shared.enums;

/** مستوى توفر الكمية للمنتج. */
public enum EnHasQuantity {
    LOT("lot", "كثير"),
    MIDDLE("middle", "متوسط"),
    LITTLE("little", "قليل"),
    NOTHING("nothing", "غير متوفر");

    private final String jsonValue;
    private final String label;

    EnHasQuantity(String jsonValue, String label) {
        this.jsonValue = jsonValue;
        this.label = label;
    }

    public String getJsonValue() { return jsonValue; }

    public String getLabel() { return label; }

    public static EnHasQuantity fromJson(String value) {
        if (value != null) {
            for (EnHasQuantity q : values()) {
                if (q.jsonValue.equalsIgnoreCase(value.trim())) return q;
            }
        }
        return NOTHING;
    }

    @Override
    public String toString() { return label; }
}
