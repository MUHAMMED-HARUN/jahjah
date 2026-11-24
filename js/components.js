/* مكوّنات الواجهة: كل مكوّن دالة تُعيد نص HTML */

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  })[char]);
}

/* السعر بعلامة $ فقط وأرقام لاتينية */
function formatPrice(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return "—";
  const text = number.toLocaleString("en-US", { maximumFractionDigits: 2 });
  return `<bdi dir="ltr">$${text}</bdi>`;
}

const STOCK = {
  lot: { cls: "is-lot", label: "كمية كثيرة" },
  middle: { cls: "is-middle", label: "كمية متوسطة" },
  little: { cls: "is-little", label: "كمية قليلة" },
  nothing: { cls: "is-nothing", label: "منتهي" }
};

/* دائرة التوفر: أخضر كثير، أصفر متوسط، أحمر قليل، رمادي منتهي */
function StockDot(level, withLabel = false) {
  const s = STOCK[level] || STOCK.nothing;
  return `<span class="stock ${s.cls}" title="${s.label}"><span class="stock-dot" aria-hidden="true"></span>${withLabel ? `<span class="stock-label">${s.label}</span>` : `<span class="sr-only">${s.label}</span>`}</span>`;
}

function categoryText(product) {
  return product.parents && product.parents.length ? product.parents.join("، ") : "—";
}

function CopyChip(sku) {
  return `<button type="button" class="code-chip" data-copy="${escapeHtml(sku || "")}" title="نسخ الرمز">${escapeHtml(sku || "—")}</button>`;
}

function imageTag(src, alt, className) {
  return `<img class="${className}" src="${escapeHtml(src || "")}" alt="${escapeHtml(alt || "")}" data-image loading="lazy">`;
}

/*
 * عارض الصور: يعرض الصورة الحالية + نقاط أسفلها + أسهم للكمبيوتر.
 * images: الصور المعروفة الآن. إذا كانت count أكبر من عدد الصور المحمّلة
 * فالباقي يُطلب عند أول تنقل (data-loaded="0").
 */
function ImageSlider({ guid, name, images = [], count = images.length, loaded = false }) {
  const total = Math.max(count, images.length);
  const slides = images.length
    ? images.map((img, i) => `<img class="slide${i === 0 ? " is-active" : ""}" ${i === 0 ? "src" : "data-src"}="${escapeHtml(img.path)}" alt="${escapeHtml(name)} — صورة ${i + 1}" data-image draggable="false">`).join("")
    : `<div class="thumb-fallback">${escapeHtml((name || "؟").trim().charAt(0))}</div>`;

  const controls = total > 1 ? `
      <button type="button" class="slider-arrow is-prev" data-slide="prev" aria-label="الصورة السابقة">›</button>
      <button type="button" class="slider-arrow is-next" data-slide="next" aria-label="الصورة التالية">‹</button>
      <div class="slider-dots" role="tablist" aria-label="صور المنتج">
        ${Array.from({ length: total }, (_, i) => `<button type="button" class="dot${i === 0 ? " is-active" : ""}" data-slide-to="${i}" aria-label="الصورة ${i + 1}"></button>`).join("")}
      </div>` : "";

  return `
    <div class="slider" data-slider data-guid="${escapeHtml(guid)}" data-index="0" data-count="${total}" data-loaded="${loaded || total <= images.length ? 1 : 0}" tabindex="0">
      <div class="slides">${slides}</div>
      ${controls}
    </div>`;
}

/* عنصر واحد من عناصر الفلتر (خيار select) */
function FilterItem({ value = "", label = "", selected = false }) {
  return `<option value="${escapeHtml(value)}"${selected ? " selected" : ""}>${escapeHtml(label)}</option>`;
}

/* لوحة الفلاتر: ماركة + التصنيف الأب + اسم + الرمز */
function Filter({ brands = [], categories = [], state = {} }) {
  const brandOptions = [
    FilterItem({ value: "", label: "كل الماركات", selected: !state.brand }),
    ...brands.map((brand) => FilterItem({ value: brand.id, label: brand.name, selected: state.brand === brand.id }))
  ].join("");

  const categoryOptions = [
    FilterItem({ value: "", label: "كل التصنيفات", selected: !state.category }),
    ...categories.map((name) => FilterItem({ value: name, label: name, selected: state.category === name }))
  ].join("");

  return `
    <section class="filters" aria-label="الفلاتر">
      <div class="filter-field">
        <label for="brandFilter">الماركة</label>
        <select id="brandFilter" class="control" data-filter="brand">${brandOptions}</select>
      </div>
      <div class="filter-field">
        <label for="categoryFilter">التصنيف</label>
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

/* بطاقة منتج في الشبكة: سطر واحد من المواصفات + "المزيد..." */
function ProductCard(product) {
  const desc = Array.isArray(product.description) ? product.description.filter(Boolean) : [];
  const more = desc.length > 1 || (desc[0] || "").length > 90;
  const specs = desc.length
    ? `<p class="card-spec">${escapeHtml(desc[0])}</p>${more ? `<button type="button" class="more-link" data-open="${escapeHtml(product.guid)}">المزيد...</button>` : ""}`
    : `<p class="card-spec is-empty">لا توجد مواصفات</p>`;

  return `
    <article class="product-card" data-open="${escapeHtml(product.guid)}">
      <div class="card-media">
        ${ImageSlider({ guid: product.guid, name: product.name, images: product.mainImage ? [{ path: product.mainImage }] : [], count: product.imageCount })}
        ${StockDot(product.enHasQuantity)}
      </div>
      <div class="card-body">
        <div class="card-top">
          <span class="brand-tag">${escapeHtml(product.brandName || "—")}</span>
          ${CopyChip(product.sku)}
        </div>
        <h3 class="card-title">${escapeHtml(product.name || "—")}</h3>
        <span class="card-category">${escapeHtml(categoryText(product))}</span>
        <div class="card-specs">${specs}</div>
        <div class="card-foot">
          <span class="card-price">${formatPrice(product.price)}</span>
          <span class="card-packing" title="التعبئة في الطرد">📦 ${escapeHtml(product.packing || "—")}</span>
        </div>
        ${OrderInputCom(product, "card")}
      </div>
    </article>`;
}

/* الكارد الكامل داخل النافذة المنبثقة: كل الصور بالترتيب + كل المواصفات */
function ProductModal(product, images) {
  const desc = Array.isArray(product.description) ? product.description.filter(Boolean) : [];
  return `
    <div class="modal-backdrop" data-modal>
      <article class="modal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <button type="button" class="modal-close" data-action="close-modal" aria-label="إغلاق">×</button>
        <div class="modal-media">
          ${ImageSlider({ guid: product.guid, name: product.name, images, loaded: true })}
        </div>
        <div class="modal-body">
          <div class="card-top">
            <span class="brand-tag">${escapeHtml(product.brandName || "—")}</span>
            ${CopyChip(product.sku)}
          </div>
          <h2 id="modalTitle">${escapeHtml(product.name || "—")}</h2>
          <span class="card-category">${escapeHtml(categoryText(product))}</span>
          <div class="modal-facts">
            <span class="card-price">${formatPrice(product.price)}</span>
            <span class="card-packing">📦 ${escapeHtml(product.packing || "—")}</span>
            ${StockDot(product.enHasQuantity, true)}
          </div>
          ${OrderInputCom(product, "modal")}
          ${desc.length ? `<p class="specs-title">المواصفات</p><ul class="specs">${desc.map((d) => `<li>${escapeHtml(d)}</li>`).join("")}</ul>` : ""}
        </div>
      </article>
    </div>`;
}

// ================================================================== الطلب والسلة

/* اختيار نوع التعبئة: طرد أو قطعة */
function UnitToggle(name, selected, dataAttr, disabled = false) {
  return `
    <div class="unit-toggle" role="radiogroup" aria-label="نوع التعبئة">
      ${Object.entries(UNITS).map(([value, u]) => `
        <label class="unit-option">
          <input type="radio" name="${escapeHtml(name)}" value="${value}" ${dataAttr}${selected === value ? " checked" : ""}${disabled ? " disabled" : ""}>
          <span>${u.label}</span>
        </label>`).join("")}
    </div>`;
}

/*
 * OrderInputCom: نوع التعبئة + الكمية + زر الإضافة للسلة.
 * المنتج الموجود في السلة لا يمكن إضافته مرة أخرى.
 */
function OrderInputCom(product, scope) {
  const guid = escapeHtml(product.guid);
  const inCart = isInCart(product.guid);
  return `
    <div class="order-input${inCart ? " is-in-cart" : ""}" data-order="${guid}">
      ${UnitToggle(`unit-${scope}-${product.guid}`, "carton", "data-order-unit", inCart)}
      ${QtyInput(1, "data-order-qty", inCart)}
      <button type="button" class="btn-add" data-action="add-to-cart" data-guid="${guid}"${inCart ? " disabled" : ""}>
        ${inCart ? "في السلة ✓" : "أضف إلى السلة"}
      </button>
    </div>`;
}

/* حقل الكمية: أرقام لاتينية + زرّا − و + (عدد صحيح 1–99999) */
function QtyInput(value, dataAttr, disabled = false) {
  const dis = disabled ? " disabled" : "";
  return `
    <div class="qty-stepper" dir="ltr">
      <button type="button" class="qty-step" data-qty-step="-1" aria-label="إنقاص"${dis}>−</button>
      <input type="text" class="qty-input" inputmode="numeric" autocomplete="off" maxlength="5" value="${escapeHtml(value)}" ${dataAttr} aria-label="الكمية"${dis}>
      <button type="button" class="qty-step" data-qty-step="1" aria-label="زيادة"${dis}>+</button>
    </div>`;
}

function CartQtyInput(item) {
  return QtyInput(item.qty, `data-cart-qty="${escapeHtml(item.guid)}"`);
}

function CartRemoveButton(item) {
  return `<button type="button" class="btn-remove" data-action="remove-from-cart" data-guid="${escapeHtml(item.guid)}" title="إزالة من السلة">إزالة</button>`;
}

function cartThumb(item) {
  const p = item.product;
  return p.mainImage
    ? imageTag(p.mainImage, p.name, "thumb")
    : `<div class="thumb-fallback">${escapeHtml((p.name || "؟").charAt(0))}</div>`;
}

function CartTable(items) {
  return `
    <div class="table-wrap">
      <table class="product-table cart-table">
        <thead>
          <tr>
            <th>صورة</th>
            <th>الرمز</th>
            <th>اسم المنتج</th>
            <th>التعبئة</th>
            <th>الكمية</th>
            <th><span class="sr-only">إزالة</span></th>
          </tr>
        </thead>
        <tbody>
          ${items.map((it) => `
            <tr data-cart-row="${escapeHtml(it.guid)}">
              <td>${cartThumb(it)}</td>
              <td><span class="code-chip">${escapeHtml(it.product.sku || "—")}</span></td>
              <td>
                <span class="cell-name">${escapeHtml(it.product.name || "—")}</span>
                <span class="cell-category">${escapeHtml(it.product.nameingAsProgram || "")}</span>
              </td>
              <td>${UnitToggle(`cart-unit-${it.guid}`, it.unit, `data-cart-unit="${escapeHtml(it.guid)}"`)}</td>
              <td>${CartQtyInput(it)}</td>
              <td>${CartRemoveButton(it)}</td>
            </tr>`).join("")}
        </tbody>
      </table>
    </div>`;
}

function CartCards(items) {
  return `
    <div class="cart-cards">
      ${items.map((it) => `
        <article class="cart-card" data-cart-row="${escapeHtml(it.guid)}">
          <div class="cart-card-media">${cartThumb(it)}</div>
          <div class="cart-card-body">
            <div class="card-top">
              <span class="code-chip">${escapeHtml(it.product.sku || "—")}</span>
              ${CartRemoveButton(it)}
            </div>
            <h3 class="card-title">${escapeHtml(it.product.name || "—")}</h3>
            <span class="card-category">${escapeHtml(it.product.nameingAsProgram || "")}</span>
            <div class="cart-card-order">
              ${UnitToggle(`cart-unit-${it.guid}`, it.unit, `data-cart-unit="${escapeHtml(it.guid)}"`)}
              ${CartQtyInput(it)}
            </div>
          </div>
        </article>`).join("")}
    </div>`;
}

function CartScreen(cart, view) {
  const items = cart.items;
  const head = `
    <div class="cart-head">
      <p class="count">عدد الأصناف: <strong>${items.length}</strong></p>
      ${items.length ? `
      <div class="view-switch is-small" role="group" aria-label="طريقة عرض السلة">
        <button type="button" class="view-btn${view === "table" ? " is-active" : ""}" data-cart-view="table">جدول</button>
        <button type="button" class="view-btn${view === "cards" ? " is-active" : ""}" data-cart-view="cards">كروت</button>
      </div>` : ""}
    </div>`;

  if (!items.length) {
    return `${head}<div class="state-panel">السلة فارغة.<br><button type="button" class="btn-more" data-screen="products">تصفّح المنتجات</button></div>`;
  }

  return `
    ${head}
    ${view === "cards" ? CartCards(items) : CartTable(items)}
    <section class="cart-notes">
      <label for="cartNotes">ملاحظات</label>
      <textarea id="cartNotes" class="control" rows="4" maxlength="1000" placeholder="أضف ملاحظات للطلب..." data-cart-notes>${escapeHtml(cart.notes || "")}</textarea>
    </section>
    <div class="cart-actions">
      <button type="button" class="btn-send" data-action="send-order">إرسال إلى واتساب</button>
      <button type="button" class="btn-danger-outline" data-action="ask-clear-cart">تفريغ السلة</button>
    </div>`;
}

/* تحذير واضح قبل تفريغ السلة */
function ConfirmClearDialog(count) {
  return `
    <div class="modal-backdrop" data-confirm>
      <div class="confirm" role="alertdialog" aria-modal="true" aria-labelledby="confirmTitle" aria-describedby="confirmText">
        <div class="confirm-icon" aria-hidden="true">!</div>
        <h2 id="confirmTitle">تفريغ السلة؟</h2>
        <p id="confirmText">سيتم حذف <strong>${count}</strong> صنفاً مع الملاحظات من السلة نهائياً. <strong>لا يمكن التراجع عن هذه العملية.</strong></p>
        <div class="confirm-actions">
          <button type="button" class="btn-danger" data-action="confirm-clear-cart">نعم، فرّغ السلة</button>
          <button type="button" class="btn-reset" data-action="cancel-confirm">إلغاء</button>
        </div>
      </div>
    </div>`;
}
