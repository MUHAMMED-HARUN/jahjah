package jahjah.shared.entities;

/** سطر من ملف الكميات: الاسم كما في البرنامج + الكمية الموجودة (بالوحدة). */
public record StockEntry(String nameingAsProgram, Double quantity) { }
