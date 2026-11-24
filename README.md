# كتالوج المنتجات

صفحة ويب بسيطة (HTML / CSS / JS بدون مكتبات) تعرض المنتجات إما كجدول أو كبطاقات، مع فلاتر للماركة والتصنيف والاسم والرمز.

## التشغيل

`fetch` لا يعمل مع `file://`، لذا شغّل سيرفر محلياً داخل هذا المجلد:

```bash
python -m http.server 8000
```

ثم افتح `http://localhost:8000`. يمكن أيضاً استخدام Live Server في VS Code.

## الملفات

- `index.html` — هيكل الصفحة وزر تبديل العرض.
- `css/styles.css` — التنسيق والألوان والتخطيط المتجاوب وRTL.
- `js/components.js` — المكوّنات: `Filter`، `FilterItem`، `Table`، `TableItem`، `ProductCard`.
- `js/data.js` — يقرأ `data base/manifest.json` ثم ملفات الماركات ويجمعها في `DB`.
- `js/app.js` — الحالة والفلاتر وإعادة الرسم.
- `data base/` — ملف JSON لكل ماركة.
- `images/` — الصور بترتيب: `images/<الماركة>/<التصنيف>/<guid>.jpg`.

## إضافة ماركة جديدة

1. أنشئ `data base/<brand-id>.json` بالشكل:

```json
{
  "brand": { "id": "brand-id", "name": "اسم الماركة" },
  "products": [
    {
      "guid": "guid-فريد",
      "sku": "الرمز المعروض",
      "name": "اسم المنتج",
      "category": "المجموعة الواسعة حسب المهمة",
      "categoryLabel": "تصنيف أدق داخل المجموعة",
      "price": 100,
      "imagePath": "images/brand-id/shoes/guid-فريد.jpg",
      "packing": "12 وحدة / كرتون",
      "description": ["مواصفة 1", "مواصفة 2"]
    }
  ]
}
```

2. أضف اسم الملف إلى `data base/manifest.json`.
3. أنشئ المجلدات `images/<brand-id>/<category>/` وضع الصورة باسم `guid.jpg`.

الجدول يعرض: الرمز، الاسم، الماركة، السعر، الصورة، التعبئة في الطرد.
الكارت يعرض المواصفات من `description[]`. إذا لم توجد الصورة يظهر بديل تلقائي.

قواعد الحقول: `category` يجمع كل ما يؤدي المهمة نفسها (مثل «تحضير القهوة»)، و`categoryLabel` يخصصها أكثر (مثل «مطحنة قهوة وتوابل»)، و`name` اسم قصير بلا رموز أو موديلات (مثل «مطحنة قهوة»). فلتر «تصنيف المنتج» يستخدم `category`.
