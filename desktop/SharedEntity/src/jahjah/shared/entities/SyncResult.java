package jahjah.shared.entities;

import jahjah.shared.enums.EnOperation;

import java.util.List;

/** نتيجة عملية UpdatePricing / UpdateVisibility. */
public record SyncResult(
        EnOperation operation,
        int updatedCount,
        int missingInProgram,
        int missingInFile,
        int invalidLines,
        List<SyncReportItem> isolatedItems,
        String reportPath) { }
