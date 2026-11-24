package jahjah.dal;

import java.nio.file.Path;

/** مسارات التخزين انطلاقاً من جذر المشروع. */
public final class DataPaths {
    private final Path root;

    public DataPaths(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() { return root; }

    public Path dataDir() { return root.resolve("data base"); }

    public Path manifest() { return dataDir().resolve("manifest.json"); }

    public Path categories() { return dataDir().resolve("categories.json"); }

    public Path reportsDir() { return dataDir().resolve("reports"); }

    public Path imagesDir() { return root.resolve("images"); }

    /** إعدادات الواجهة (يقرؤها index.html). */
    public Path config() { return root.resolve("config.json"); }

    public Path brandFile(String fileName) { return dataDir().resolve(fileName); }
}
