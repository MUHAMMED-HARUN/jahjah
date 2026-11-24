/* تحميل ملفات JSON لكل ماركة وتجميعها في متغير واحد */
const DATA_DIR = "data base/";
let DB = { brands: [], products: [] };

async function fetchJson(path) {
  const response = await fetch(encodeURI(path), { cache: "no-store" });
  if (!response.ok) {
    throw new Error(`تعذّر قراءة ${path} (الحالة: ${response.status})`);
  }
  return response.json();
}

async function loadDatabase() {
  const fileNames = await fetchJson(`${DATA_DIR}manifest.json`);
  if (!Array.isArray(fileNames)) {
    throw new Error("صيغة manifest.json غير صحيحة؛ يجب أن تكون مصفوفة أسماء ملفات.");
  }

  const brandFiles = await Promise.all(
    fileNames.map((name) => fetchJson(`${DATA_DIR}${name}`))
  );

  const brands = brandFiles.map((file) => file.brand);
  const products = brandFiles.flatMap((file) =>
    (file.products || []).map((product) => ({
      ...product,
      brandId: file.brand.id,
      brandName: file.brand.name
    }))
  );

  DB = { brands, products };
  window.DB = DB;
  return DB;
}
