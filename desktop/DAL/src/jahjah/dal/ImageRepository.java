package jahjah.dal;

import jahjah.shared.exceptions.DataAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** حفظ الصور في images/<الماركة>/<guid>.<ext>. */
public class ImageRepository {
    private final DataPaths paths;

    public ImageRepository(DataPaths paths) {
        this.paths = paths;
    }

    /** ينسخ الصورة ويعيد المسار النسبي المستخدم في JSON (images/Brand/guid.png). */
    public String save(Path source, String brandFolder, String guid) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot).toLowerCase() : "";
        Path target = paths.imagesDir().resolve(brandFolder).resolve(guid + ext);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new DataAccessException("تعذّر حفظ الصورة: " + target, ex);
        }
        return "images/" + brandFolder + "/" + guid + ext;
    }

    /** المسار الكامل لصورة من مسارها النسبي (أو null إن لم توجد). */
    public Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        Path p = paths.root().resolve(relativePath).normalize();
        return Files.exists(p) ? p : null;
    }
}
