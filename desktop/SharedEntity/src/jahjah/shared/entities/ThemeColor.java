package jahjah.shared.entities;

/** لون من ألوان الواجهة في config.json → theme. */
public class ThemeColor {
    /** المفتاح في config (مثل primary). */
    private String key;
    /** اسم مختصر للعرض في البرنامج. */
    private String label;
    /** #RRGGBB */
    private String color;
    /** أين يظهر هذا اللون في الواجهة. */
    private String description;

    public ThemeColor() { }

    public ThemeColor(String key, String label, String color, String description) {
        this.key = key;
        this.label = label;
        this.color = color;
        this.description = description;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ThemeColor copy() { return new ThemeColor(key, label, color, description); }
}
