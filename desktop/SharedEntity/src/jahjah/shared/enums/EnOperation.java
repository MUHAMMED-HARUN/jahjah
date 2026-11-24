package jahjah.shared.enums;

/** العمليات التي تعتمد على ملف JSON خارجي. */
public enum EnOperation {
    UPDATE_PRICING("UpdatePricing", "تحديث الأسعار"),
    UPDATE_VISIBILITY("UpdateVisibility", "تحديث التوفر");

    private final String jsonValue;
    private final String label;

    EnOperation(String jsonValue, String label) {
        this.jsonValue = jsonValue;
        this.label = label;
    }

    public String getJsonValue() { return jsonValue; }

    public String getLabel() { return label; }
}
