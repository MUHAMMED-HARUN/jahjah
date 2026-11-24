package jahjah.shared.entities;

import java.nio.file.Path;

/**
 * عنصر في القائمة النهائية لصور المنتج كما يرسلها الـ UI (بالترتيب المطلوب):
 * إما صورة موجودة مسبقاً (existingPath) أو ملف جديد يُنسخ (newSource).
 */
public record ImageInput(String existingPath, Path newSource) {

    public static ImageInput existing(String path) { return new ImageInput(path, null); }

    public static ImageInput fromFile(Path source) { return new ImageInput(null, source); }

    public boolean isNew() { return newSource != null; }
}
