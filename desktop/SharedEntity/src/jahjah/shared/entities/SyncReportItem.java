package jahjah.shared.entities;

import jahjah.shared.enums.EnMissingSide;
import jahjah.shared.enums.EnOperation;

/** مادة معزولة: موجودة في أحد الطرفين فقط (أو سطر غير صالح). */
public record SyncReportItem(
        EnOperation operation,
        EnMissingSide missingFrom,
        String nameingAsProgram,
        String productName,
        String brand,
        String guid,
        Double value,
        String note) { }
