package jahjah.dal;

import jahjah.shared.exceptions.DataAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** قراءة وكتابة الملفات النصية (UTF-8) مع كتابة آمنة عبر ملف مؤقت. */
final class FileIO {
    private FileIO() { }

    static Object readJson(Path path) {
        try {
            return Json.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new DataAccessException("تعذّرت قراءة الملف: " + path, ex);
        } catch (DataAccessException ex) {
            throw new DataAccessException(path.getFileName() + " — " + ex.getMessage(), ex);
        }
    }

    static void writeJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, Json.write(value), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new DataAccessException("تعذّرت كتابة الملف: " + path, ex);
        }
    }
}
