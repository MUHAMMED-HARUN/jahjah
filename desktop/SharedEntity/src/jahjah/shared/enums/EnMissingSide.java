package jahjah.shared.enums;

/** الطرف الذي فُقدت منه المادة أثناء المطابقة. */
public enum EnMissingSide {
    /** موجودة في الملف ومفقودة من البرنامج. */
    PROGRAM("program", "مفقودة من البرنامج"),
    /** موجودة في البرنامج ومفقودة من الملف. */
    FILE("file", "مفقودة من الملف"),
    /** سطر غير صالح أو مكرر في الملف. */
    INVALID("invalid", "سطر غير صالح في الملف");

    private final String jsonValue;
    private final String label;

    EnMissingSide(String jsonValue, String label) {
        this.jsonValue = jsonValue;
        this.label = label;
    }

    public String getJsonValue() { return jsonValue; }

    public String getLabel() { return label; }
}
