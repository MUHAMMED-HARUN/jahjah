package jahjah.bal;

import jahjah.dal.ImportRepository;
import jahjah.dal.ReportRepository;
import jahjah.shared.entities.PriceEntry;
import jahjah.shared.entities.Product;
import jahjah.shared.entities.StockEntry;
import jahjah.shared.entities.SyncReportItem;
import jahjah.shared.entities.SyncResult;
import jahjah.shared.enums.EnHasQuantity;
import jahjah.shared.enums.EnMissingSide;
import jahjah.shared.enums.EnOperation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UpdatePricing / UpdateVisability(all).
 * المطابقة عبر nameingAsProgram. المادة الموجودة في طرف واحد فقط تُعزل
 * وتُكتب في ملف JSON داخل "data base/reports" مع ملاحظة بالطرف المفقود والعملية.
 */
public class SyncService {
    /** حتى هذا العدد من الطرود الكاملة يُعتبر "قليل". */
    public static final int LITTLE_MAX_PACKS = 2;
    /** حتى هذا العدد من الطرود الكاملة يُعتبر "متوسط"، وما فوقه "كثير". */
    public static final int MIDDLE_MAX_PACKS = 9;

    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+");

    private final CatalogStore store;
    private final ImportRepository importRepo;
    private final ReportRepository reportRepo;

    SyncService(CatalogStore store, ImportRepository importRepo, ReportRepository reportRepo) {
        this.store = store;
        this.importRepo = importRepo;
        this.reportRepo = reportRepo;
    }

    /** UpdatePricing: ملف فيه السعر والاسم كما في البرنامج. */
    public SyncResult updatePricing(Path file) {
        EnOperation op = EnOperation.UPDATE_PRICING;
        List<SyncReportItem> isolated = new ArrayList<>();
        Map<String, Double> entries = new LinkedHashMap<>();
        Map<String, String> originalNames = new LinkedHashMap<>();
        int invalid = 0;

        for (PriceEntry e : importRepo.readPriceEntries(file)) {
            String key = Names.key(e.nameingAsProgram());
            String problem = key.isEmpty() ? "الاسم كما في البرنامج فارغ"
                    : e.price() == null ? "السعر مفقود أو ليس رقماً"
                    : e.price() < 0 ? "السعر سالب"
                    : entries.containsKey(key) ? "الاسم مكرر في الملف (تم اعتماد أول ظهور)"
                    : null;
            if (problem != null) {
                invalid++;
                isolated.add(new SyncReportItem(op, EnMissingSide.INVALID, e.nameingAsProgram(), null, null, null,
                        e.price(), problem + " — عملية " + op.getJsonValue()));
                continue;
            }
            entries.put(key, e.price());
            originalNames.put(key, e.nameingAsProgram());
        }

        Counters c = match(op, entries.keySet(), originalNames, isolated, (p, key) -> {
            double price = entries.get(key);
            if (p.getPrice() == price) return false;
            p.setPrice(price);
            return true;
        }, (p) -> false, entries);

        return finish(op, file, c, invalid, isolated);
    }

    /**
     * UpdateVisability(all): ملف فيه الاسم كما في البرنامج والكمية الموجودة.
     * يُعتبر المنتج متوفراً فقط إذا وُجد طرد كامل على الأقل (الكمية ≥ عدد التعبئة).
     * المنتجات غير الموجودة في الملف تُعيَّن "غير متوفر".
     */
    public SyncResult updateVisibility(Path file) {
        EnOperation op = EnOperation.UPDATE_VISIBILITY;
        List<SyncReportItem> isolated = new ArrayList<>();
        Map<String, Double> entries = new LinkedHashMap<>();
        Map<String, String> originalNames = new LinkedHashMap<>();
        int invalid = 0;

        for (StockEntry e : importRepo.readStockEntries(file)) {
            String key = Names.key(e.nameingAsProgram());
            String problem = key.isEmpty() ? "الاسم كما في البرنامج فارغ"
                    : e.quantity() == null ? "الكمية مفقودة أو ليست رقماً"
                    : e.quantity() < 0 ? "الكمية سالبة"
                    : entries.containsKey(key) ? "الاسم مكرر في الملف (تم اعتماد أول ظهور)"
                    : null;
            if (problem != null) {
                invalid++;
                isolated.add(new SyncReportItem(op, EnMissingSide.INVALID, e.nameingAsProgram(), null, null, null,
                        e.quantity(), problem + " — عملية " + op.getJsonValue()));
                continue;
            }
            entries.put(key, e.quantity());
            originalNames.put(key, e.nameingAsProgram());
        }

        Counters c = match(op, entries.keySet(), originalNames, isolated, (p, key) -> {
            EnHasQuantity level = levelFor(entries.get(key), packSize(p.getPacking()));
            if (p.getEnHasQuantity() == level) return false;
            p.setEnHasQuantity(level);
            return true;
        }, (p) -> {
            if (p.getEnHasQuantity() == EnHasQuantity.NOTHING) return false;
            p.setEnHasQuantity(EnHasQuantity.NOTHING);
            return true;
        }, entries);

        return finish(op, file, c, invalid, isolated);
    }

    /** عدد الوحدات في الطرد: أول رقم في حقل التعبئة، وإلا 1. */
    public static int packSize(String packing) {
        if (packing == null) return 1;
        Matcher m = FIRST_NUMBER.matcher(packing);
        if (!m.find()) return 1;
        try {
            return Math.max(1, Integer.parseInt(m.group()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public static EnHasQuantity levelFor(double quantity, int packSize) {
        long packs = (long) Math.floor(quantity / Math.max(1, packSize));
        if (packs < 1) return EnHasQuantity.NOTHING;
        if (packs <= LITTLE_MAX_PACKS) return EnHasQuantity.LITTLE;
        if (packs <= MIDDLE_MAX_PACKS) return EnHasQuantity.MIDDLE;
        return EnHasQuantity.LOT;
    }

    // ------------------------------------------------------------------ مشترك

    private interface Apply { boolean apply(Product p, String key); }

    private interface Missing { boolean apply(Product p); }

    private record Counters(int updated, int missingInProgram, int missingInFile, Set<String> dirty) { }

    private Counters match(EnOperation op, Set<String> fileKeys, Map<String, String> originalNames,
                           List<SyncReportItem> isolated, Apply onMatch, Missing onMissingInFile,
                           Map<String, Double> values) {
        Set<String> matchedKeys = new LinkedHashSet<>();
        Set<String> dirty = new LinkedHashSet<>();
        int updated = 0;
        int missingInFile = 0;

        for (Product p : store.allProducts()) {
            String key = Names.key(p.getNameingAsProgram());
            String brand = store.catalog(p.getBrandFile()).getBrand().getName();
            if (!key.isEmpty() && fileKeys.contains(key)) {
                matchedKeys.add(key);
                if (onMatch.apply(p, key)) {
                    dirty.add(p.getBrandFile());
                    updated++;
                }
            } else {
                missingInFile++;
                String note = key.isEmpty()
                        ? "المنتج لا يملك اسماً كما في البرنامج (nameingAsProgram) — مفقود من ملف " + op.getJsonValue()
                        : "موجود في البرنامج ومفقود من ملف " + op.getJsonValue();
                if (onMissingInFile.apply(p)) {
                    dirty.add(p.getBrandFile());
                    note += " — تم تعيينه غير متوفر";
                }
                isolated.add(new SyncReportItem(op, EnMissingSide.FILE, p.getNameingAsProgram(), p.getName(), brand,
                        p.getGuid(), null, note));
            }
        }

        int missingInProgram = 0;
        for (String key : fileKeys) {
            if (matchedKeys.contains(key)) continue;
            missingInProgram++;
            isolated.add(new SyncReportItem(op, EnMissingSide.PROGRAM, originalNames.get(key), null, null, null,
                    values.get(key), "موجود في ملف " + op.getJsonValue() + " ومفقود من البرنامج"));
        }
        return new Counters(updated, missingInProgram, missingInFile, dirty);
    }

    private SyncResult finish(EnOperation op, Path file, Counters c, int invalid, List<SyncReportItem> isolated) {
        store.saveBrands(c.dirty());
        String report = isolated.isEmpty() ? null : reportRepo.write(op.getJsonValue(), file.toString(), isolated);
        return new SyncResult(op, c.updated(), c.missingInProgram(), c.missingInFile(), invalid, isolated, report);
    }
}
