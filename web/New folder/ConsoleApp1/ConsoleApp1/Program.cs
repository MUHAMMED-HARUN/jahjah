using System.Globalization;
using System.Text.Encodings.Web;
using System.Text.Json;
using DocumentFormat.OpenXml.Drawing.Spreadsheet;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Spreadsheet;

string excelPath = @"C:\Users\مستخدم 1 ديل\Downloads\Documents\New folder\10-10نشرة ديسل شاملة.xlsx";
string outputDir = @"C:\Users\مستخدم 1 ديل\Downloads\Documents\New folder\";
string imagesDir = Path.Combine(outputDir, "images");

var brand = new BrandDto(Id: "brand-id", Name: "اسم العلامة التجارية");
var products = ExcelCatalogExtractor.ExtractProducts(
    excelPath: excelPath,
    outputImagesDir: imagesDir,
    category: "general",
    categoryLabel: "عام"
);

var catalogDocument = new CatalogDocumentDto(
    Brand: brand,
    Products: products
);

Directory.CreateDirectory(outputDir);
string jsonPath = Path.Combine(outputDir, "products.json");

var jsonOptions = new JsonSerializerOptions(JsonSerializerDefaults.Web)
{
    WriteIndented = true,
    Encoder = JavaScriptEncoder.UnsafeRelaxedJsonEscaping
};

File.WriteAllText(jsonPath, JsonSerializer.Serialize(catalogDocument, jsonOptions));
Console.WriteLine($"Exported {products.Count} products to {jsonPath}");

public sealed record BrandDto(
    string Id,
    string Name
);

public sealed record ProductDto(
    Guid Guid,
    string Sku,
    string Name,
    string Category,
    string CategoryLabel,
    decimal Price,
    string? ImagePath,
    string Packing,
    string[] Description
);

public sealed record CatalogDocumentDto(
    BrandDto Brand,
    List<ProductDto> Products
);

public static class ExcelCatalogExtractor
{
    public static List<ProductDto> ExtractProducts(
        string excelPath,
        string outputImagesDir,
        string category,
        string categoryLabel)
    {
        Directory.CreateDirectory(outputImagesDir);

        using var doc = SpreadsheetDocument.Open(excelPath, isEditable: false);
        var workbookPart = doc.WorkbookPart ?? throw new InvalidOperationException("Invalid workbook.");
        var worksheetPart = workbookPart.WorksheetParts.First();

        var imagesByRow = ExtractFloatingImagesByRow(worksheetPart);
        var sharedStrings = workbookPart.SharedStringTablePart?.SharedStringTable;
        var rows = worksheetPart.Worksheet.GetFirstChild<SheetData>()?.Elements<Row>().ToList() ?? [];

        var products = new List<ProductDto>();

        // تخطّي الصف الأول (العناوين) والبدء من الصف 2
        foreach (var row in rows.Skip(1))
        {
            int rowNumber = (int)(row.RowIndex?.Value ?? 0);
            if (rowNumber == 0) continue;

            // الأعمدة حسب الجدول: B = الصنف، C = مواصفات، D = السعر
            string name = GetCellValue(row, "B", sharedStrings).Trim();
            string rawSpecs = GetCellValue(row, "C", sharedStrings);
            string rawPrice = GetCellValue(row, "D", sharedStrings).Trim();

            if (string.IsNullOrWhiteSpace(name) &&
                string.IsNullOrWhiteSpace(rawSpecs) &&
                string.IsNullOrWhiteSpace(rawPrice))
            {
                continue;
            }

            string[] descriptionArray = rawSpecs
                .Split(["\r\n", "\n", "\r"], StringSplitOptions.None)
                .Select(line => line.Trim())
                .Where(line => !string.IsNullOrWhiteSpace(line))
                .ToArray();

            Guid guid = Guid.NewGuid();
            string? savedImageRelPath = null;

            // حفظ الصورة باسم الـ Guid لأن الـ SKU فارغ
            if (imagesByRow.TryGetValue(rowNumber, out byte[]? imageBytes))
            {
                string fileName = $"{guid}.png";
                string fullPath = Path.Combine(outputImagesDir, fileName);
                File.WriteAllBytes(fullPath, imageBytes);
                savedImageRelPath = $"images/{fileName}";
            }

            products.Add(new ProductDto(
                Guid: guid,
                Sku: string.Empty,
                Name: name,
                Category: category,
                CategoryLabel: categoryLabel,
                Price: ParsePrice(rawPrice),
                ImagePath: savedImageRelPath,
                Packing: "1",
                Description: descriptionArray
            ));
        }

        return products;
    }

    private static decimal ParsePrice(string rawPrice)
    {
        string cleaned = new string(rawPrice.Where(c => char.IsDigit(c) || c == '.' || c == ',').ToArray())
            .Replace(',', '.');

        return decimal.TryParse(cleaned, NumberStyles.Any, CultureInfo.InvariantCulture, out decimal value)
            ? value
            : 0m;
    }

    private static string GetCellValue(Row row, string columnLetter, SharedStringTable? sharedStrings)
    {
        var cell = row.Elements<Cell>().FirstOrDefault(c =>
        {
            string? cellRef = c.CellReference?.Value;
            if (string.IsNullOrEmpty(cellRef)) return false;
            string letters = new string(cellRef.TakeWhile(char.IsLetter).ToArray());
            return string.Equals(letters, columnLetter, StringComparison.OrdinalIgnoreCase);
        });

        if (cell is null) return string.Empty;

        string rawValue = cell.DataType?.Value == CellValues.InlineString
            ? (cell.InlineString?.InnerText ?? string.Empty)
            : (cell.CellValue?.Text ?? string.Empty);

        if (cell.DataType?.Value == CellValues.SharedString &&
            int.TryParse(rawValue, out int sstIndex) &&
            sharedStrings is not null)
        {
            return sharedStrings.ElementAtOrDefault(sstIndex)?.InnerText ?? string.Empty;
        }

        return rawValue;
    }

    private static Dictionary<int, byte[]> ExtractFloatingImagesByRow(WorksheetPart worksheetPart)
    {
        var result = new Dictionary<int, byte[]>();
        var drawingsPart = worksheetPart.DrawingsPart;

        if (drawingsPart?.WorksheetDrawing is null)
            return result;

        foreach (var anchor in drawingsPart.WorksheetDrawing.ChildElements)
        {
            DocumentFormat.OpenXml.Drawing.Spreadsheet.FromMarker? fromMarker = anchor switch
            {
                TwoCellAnchor twoCell => twoCell.FromMarker,
                OneCellAnchor oneCell => oneCell.FromMarker,
                _ => null
            };

            string? rowIdText = fromMarker?.RowId?.Text;
            if (string.IsNullOrWhiteSpace(rowIdText) || !int.TryParse(rowIdText, out int rowIdx0Based))
                continue;

            int rowNumber = rowIdx0Based + 1;

            var blip = anchor.Descendants<DocumentFormat.OpenXml.Drawing.Blip>().FirstOrDefault();
            string? embedId = blip?.Embed?.Value;

            if (string.IsNullOrWhiteSpace(embedId))
                continue;

            if (drawingsPart.TryGetPartById(embedId, out var part) && part is ImagePart imagePart)
            {
                using var stream = imagePart.GetStream();
                using var ms = new MemoryStream();
                stream.CopyTo(ms);
                result[rowNumber] = ms.ToArray();
            }
        }

        return result;
    }
}