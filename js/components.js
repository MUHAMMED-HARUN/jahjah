/* مكوّنات الواجهة: كل مكوّن دالة تُعيد نص HTML */
const priceFormatter = new Intl.NumberFormat("ar-EG", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0
});

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  })[char]);
}

function formatPrice(value) {
  const number = Number(value);
  return Number.isFinite(number) ? priceFormatter.format(number) : "—";
}

function imageTag(product, className) {
  return `<img class="${className}" src="${escapeHtml(product.imagePath || "")}" alt="${escapeHtml(product.name || "")}" data-image loading="lazy">`;
}

/* عنصر واحد من عناصر الفلتر (خيار select) */
function FilterItem({ value = "", label = "", selected = false }) {
  return `<option value="${escapeHtml(value)}"${selected ? " selected" : ""}>${escapeHtml(label)}</option>`;
}

/* لوحة الفلاتر: ماركة + تصنيف + اسم + الرمز */
function Filter({ brands = [], categories = [], state = {} }) {
  const brandOptions = [
    FilterItem({ value: "", label: "كل الماركات", selected: !state.brand }),
    ...brands.map((brand) => FilterItem({
      value: brand.id,
      label: brand.name,
      selected: state.brand === brand.id
    }))
  ].join("");

  const categoryOptions = [
    FilterItem({ value: "", label: "كل التصنيفات", selected: !state.category }),
    ...categories.map((category) => FilterItem({
      value: category.value,
      label: category.label,
      selected: state.category === category.value
    }))
  ].join("");

  return `
    <section class="filters" aria-label="الفلاتر">
      <div class="filter-field">
        <label for="brandFilter">الماركة</label>
        <select id="brandFilter" class="control" data-filter="brand">${brandOptions}</select>
      </div>
      <div class="filter-field">
        <label for="categoryFilter">تصنيف المنتج</label>
        <select id="categoryFilter" class="control" data-filter="category">${categoryOptions}</select>
      </div>
      <div class="filter-field">
        <label for="nameFilter">الاسم</label>
        <input id="nameFilter" class="control" type="search" placeholder="ابحث بالاسم..." value="${escapeHtml(state.name || "")}" data-filter="name">
      </div>
      <div class="filter-field">
        <label for="skuFilter">الرمز</label>
        <input id="skuFilter" class="control" type="search" placeholder="ابحث بالرمز..." value="${escapeHtml(state.sku || "")}" data-filter="sku">
      </div>
      <button type="button" class="btn-reset" data-action="reset">إعادة ضبط</button>
    </section>`;
}

/* صف واحد في الجدول */
function TableItem(product) {
  return `
    <tr>
      <td><button type="button" class="code-chip" data-copy="${escapeHtml(product.sku || "")}" title="نسخ الرمز">${escapeHtml(product.sku || "—")}</button></td>
      <td>
        <span class="cell-name">${escapeHtml(product.name || "—")}</span>
        <span class="cell-category">${escapeHtml(product.categoryLabel || product.category || "—")}</span>
      </td>
      <td><span class="brand-tag">${escapeHtml(product.brandName || "—")}</span></td>
      <td><span class="price">${formatPrice(product.price)}</span></td>
      <td>${imageTag(product, "thumb")}</td>
      <td>${escapeHtml(product.packing || "—")}</td>
    </tr>`;
}

/* الجدول كامل */
function Table({ products = [] }) {
  return `
    <div class="table-wrap">
      <table class="product-table">
        <thead>
          <tr>
            <th>الرمز</th>
            <th>اسم</th>
            <th>ماركة</th>
            <th>سعر</th>
            <th>صورة</th>
            <th>التعبئة في الطرد</th>
          </tr>
        </thead>
        <tbody>${products.map(TableItem).join("")}</tbody>
      </table>
    </div>`;
}

/* بطاقة منتج: المواصفات من description[] */
function ProductCard(product) {
  const specs = Array.isArray(product.description) && product.description.length
    ? `<p class="specs-title">المواصفات</p><ul class="specs">${product.description.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`
    : "";

  return `
    <article class="product-card">
      <div class="card-media">${imageTag(product, "card-image")}</div>
      <div class="card-body">
        <div class="card-top">
          <span class="brand-tag">${escapeHtml(product.brandName || "—")}</span>
          <button type="button" class="code-chip" data-copy="${escapeHtml(product.sku || "")}" title="نسخ الرمز">${escapeHtml(product.sku || "—")}</button>
        </div>
        <h3>${escapeHtml(product.name || "—")}</h3>
        <span class="card-category">${escapeHtml(product.categoryLabel || product.category || "—")}</span>
        <p class="card-price">${formatPrice(product.price)}</p>
        ${specs}
      </div>
    </article>`;
}
