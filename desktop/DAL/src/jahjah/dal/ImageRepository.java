package jahjah.dal;

import jahjah.shared.exceptions.DataAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** حفظ/حذف الصور في images/<الماركة>/. */
public class ImageRepository {
    private final DataPaths paths;

    public ImageRepository(DataPaths paths) {
        this.paths = paths;
    }

    /** ينسخ الصورة باسم fileBaseName + الامتداد الأصلي، ويعيد المسار النسبي (images/Brand/name.png). */
    public String save(Path source, String brandFolder, String fileBaseName) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
        Path target = paths.imagesDir().resolve(brandFolder).resolve(fileBaseName + ext);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new DataAccessException("تعذّر حفظ الصورة: " + target, ex);
        }
        return "images/" + brandFolder + "/" + fileBaseName + ext;
    }

    /** هل يوجد ملف بهذا الاسم الأساسي (بأي امتداد) داخل مجلد الماركة؟ */
    public boolean baseNameExists(String brandFolder, String fileBaseName) {
        Path dir = paths.imagesDir().resolve(brandFolder);
        if (!Files.isDirectory(dir)) return false;
        try (var files = Files.list(dir)) {
            return files.anyMatch(f -> {
                String n = f.getFileName().toString();
                int dot = n.lastIndexOf('.');
                return (dot >= 0 ? n.substring(0, dot) : n).equalsIgnoreCase(fileBaseName);
            });
        } catch (IOException ex) {
            throw new DataAccessException("تعذّرت قراءة المجلد: " + dir, ex);
        }
    }

    /** يحذف ملف صورة (فقط إن كان داخل مجلد images). */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        Path p = paths.root().resolve(relativePath).normalize();
        if (!p.startsWith(paths.imagesDir())) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException ex) {
            throw new DataAccessException("تعذّر حذف الصورة: " + p, ex);
        }
    }

    /** المسار الكامل لصورة من مسارها النسبي (أو null إن لم توجد). */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        Path p = paths.root().resolve(relativePath).normalize();
        return Files.exists(p) ? p : null;
    }
}
