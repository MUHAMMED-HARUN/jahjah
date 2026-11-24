/*
 * طبقة الوصول للبيانات في الواجهة.
 * الواجهة لا تقرأ المصفوفة كاملة؛ بل تطلب صفحات عبر queryProducts()
 * وتطلب صور المنتج الإضافية عبر getProductImages() فقط عند الحاجة.
 * (الموقع ملفات ثابتة بلا سيرفر، لذلك تُقرأ ملفات JSON مرة واحدة هنا،
 *  ويمكن لاحقاً استبدال هذه الدوال بطلبات API دون تغيير بقية الواجهة.)
 */
const DATA_DIR = "data base/";
const store = { brands: [], products: [], categories: new Map() };

async function fetchJson(path, { optional = false } = {}) {
  const response = await fetch(encodeURI(path), { cache: "no-store" });
  if (!response.ok) {
    if (optional && response.status === 404) return null;
    throw new Error(`تعذّر قراءة ${path} (الحالة: ${response.status})`);
  }
  return response.json();
}

/* الصور مرتبة حسب order (أو imagePath في الملفات القديمة) */
function sortedImages(product) {
  if (Array.isArray(product.image) && product.image.length) {
    return product.image
      .filter((img) => img && img.path)
      .map((img, i) => ({ path: img.path, order: Number.isFinite(img.order) ? img.order : i + 1 }))
      .sort((a, b) => a.order - b.order);
  }
  return product.imagePath ? [{ path: product.imagePath, order: 1 }] : [];
}

/* اسم التصنيف الأب (الجذر) لمعرّف تصنيف */
function rootCategoryName(id) {
  let node = store.categories.get(id);
  const seen = new Set();
  while (node && node.CategoryFather != null && !seen.has(node.CategoryID)) {
    seen.add(node.CategoryID);
    const parent = store.categories.get(node.CategoryFather);
    if (!parent) break;
    node = parent;
  }
  return node ? node.CategoryName : null;
}

/* التصنيفات الأب للمنتج (بدون تكرار) */
function parentCategories(product) {
  if (Array.isArray(product.category)) {
    const names = product.category.map(rootCategoryName).filter(Boolean);
    return [...new Set(names)];
  }
  return product.category ? [String(product.category)] : [];
}

async function loadDatabase() {
  const fileNames = await fetchJson(`${DATA_DIR}manifest.json`);
  if (!Array.isArray(fileNames)) {
    throw new Error("صيغة manifest.json غير صحيحة؛ يجب أن تكون مصفوفة أسماء ملفات.");
  }

  const [brandFiles, categories] = await Promise.all([
    Promise.all(fileNames.map((name) => fetchJson(`${DATA_DIR}${name}`))),
    fetchJson(`${DATA_DIR}categories.json`, { optional: true })
  ]);

  store.categories = new Map((categories || []).map((c) => [c.CategoryID, c]));
  store.brands = brandFiles.map((file) => file.brand);
  store.products = brandFiles.flatMap((file) =>
    (file.products || [])
      .filter((product) => product.isActive !== false) // غير المفعّل لا يُعرض
      .map((product) => {
        const images = sortedImages(product);
        return {
          ...product,
          brandId: file.brand.id,
          brandName: file.brand.name,
          parents: parentCategories(product),
          _images: images
        };
      })
  );
}

function getBrands() {
  return store.brands.map((b) => ({ id: b.id, name: b.name }));
}

/* التصنيفات الأب التي لها منتجات معروضة فقط */
function getParentCategories() {
  const names = new Set(store.products.flatMap((p) => p.parents));
  return [...names].sort((a, b) => a.localeCompare(b, "ar"));
}

/* نسخة خفيفة للعرض في الجدول/الكروت: الصورة ذات الترتيب 1 فقط + عدد الصور */
function toSummary(product) {
  const { _images, image, imagePath, ...rest } = product;
  return { ...rest, mainImage: _images[0] ? _images[0].path : "", imageCount: _images.length };
}

function matches(product, filters) {
  const name = (filters.name || "").trim().toLocaleLowerCase("ar");
  const sku = (filters.sku || "").trim().toLocaleLowerCase("ar");
  return (!filters.brand || product.brandId === filters.brand)
    && (!filters.category || product.parents.includes(filters.category))
    && (!name || (product.name || "").toLocaleLowerCase("ar").includes(name))
    && (!sku || (product.sku || "").toLocaleLowerCase("ar").includes(sku));
}

/* صفحة من المنتجات: { items, total, hasMore } */
async function queryProducts(filters, offset, limit) {
  const all = store.products.filter((p) => matches(p, filters));
  const items = all.slice(offset, offset + limit).map(toSummary);
  return { items, total: all.length, hasMore: offset + items.length < all.length };
}

async function getProduct(guid) {
  const product = store.products.find((p) => p.guid === guid);
  return product ? toSummary(product) : null;
}

/* كل صور المنتج مرتبة حسب order — تُطلب فقط عند فتح الكارد أو التنقل بين الصور */
async function getProductImages(guid) {
  const product = store.products.find((p) => p.guid === guid);
  return product ? product._images.map((img) => ({ ...img })) : [];
}
