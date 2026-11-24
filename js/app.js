/* الحالة، الرسم، والأحداث */
const PAGE_SIZE = 24;
const CART_VIEW_KEY = "jahjah.cartView";
const state = { brand: "", category: "", name: "", sku: "", screen: "products", cartView: "table" };
const paging = { offset: 0, total: 0, hasMore: false, loading: false, token: 0 };

const app = document.getElementById("app");
const toast = document.getElementById("toast");
let toastTimer;
let observer = null;
let lastFocus = null;

function showToast(message, ms = 1800) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("is-visible"), ms);
}

function filters() {
  return { brand: state.brand, category: state.category, name: state.name, sku: state.sku };
}

/* صورة تالفة/مفقودة ← بديل بالحرف الأول (مع الحفاظ على مكانها داخل العارض) */
function attachImageFallbacks(root) {
  root.querySelectorAll("img[data-image]:not([data-fallback])").forEach((image) => {
    image.dataset.fallback = "1";
    image.addEventListener("error", () => {
      const fallback = document.createElement("div");
      fallback.className = "thumb-fallback" + (image.classList.contains("slide") ? " slide" : "")
        + (image.classList.contains("is-active") ? " is-active" : "");
      fallback.setAttribute("aria-label", image.alt);
      fallback.textContent = (image.alt || "؟").trim().charAt(0);
      image.replaceWith(fallback);
    });
  });
}

// ------------------------------------------------------------------ الشاشات

function showScreen(screen) {
  state.screen = screen === "cart" ? "cart" : "products";
  document.getElementById("productsScreen").hidden = state.screen !== "products";
  document.getElementById("cartScreen").hidden = state.screen !== "cart";
  document.querySelectorAll("[data-screen]").forEach((b) => {
    if (b.classList.contains("view-btn")) b.classList.toggle("is-active", b.dataset.screen === state.screen);
  });
  const title = document.getElementById("pageTitle");
  if (title) title.textContent = state.screen === "cart" ? "السلة" : "المنتجات";
  if (state.screen === "cart") renderCart();
  const hash = state.screen === "cart" ? "#cart" : "#products";
  if (location.hash !== hash) history.replaceState(null, "", hash);
  window.scrollTo(0, 0);
}

function updateCartBadge() {
  const n = cartCount();
  document.querySelectorAll("[data-cart-count]").forEach((el) => {
    el.textContent = n;
    el.hidden = n === 0;
  });
}

/* يحدّث كل OrderInputCom حسب وجود المنتج في السلة */
function syncOrderInputs() {
  const inCart = new Set(cartItems().map((it) => it.guid));
  document.querySelectorAll("[data-order]").forEach((box) => {
    const added = inCart.has(box.dataset.order);
    box.classList.toggle("is-in-cart", added);
    box.querySelectorAll("input, .qty-step").forEach((i) => { i.disabled = added; });
    const button = box.querySelector('[data-action="add-to-cart"]');
    button.disabled = added;
    button.textContent = added ? "في السلة ✓" : "أضف إلى السلة";
  });
}

// ------------------------------------------------------------------ المنتجات (كروت) والتحميل على دفعات

function renderResults() {
  const results = document.getElementById("results");
  if (!results) return;

  paging.token++;
  paging.offset = 0;
  paging.total = 0;
  paging.hasMore = true;
  paging.loading = false;

  results.innerHTML = `
    <div class="results-head"><div class="count" data-count>جارٍ التحميل...</div></div>
    <div class="cards-grid" data-list></div>
    <div class="load-more">
      <div class="sentinel" data-sentinel aria-hidden="true"></div>
      <button type="button" class="btn-more" data-action="more" hidden>المزيد</button>
    </div>`;

  watchSentinel(results.querySelector("[data-sentinel]"));
  loadMore();
}

async function loadMore() {
  if (paging.loading || !paging.hasMore) return;
  paging.loading = true;
  const token = paging.token;
  const results = document.getElementById("results");
  const moreButton = results.querySelector('[data-action="more"]');
  moreButton.disabled = true;
  moreButton.textContent = "جارٍ التحميل...";

  let page;
  try {
    page = await queryProducts(filters(), paging.offset, PAGE_SIZE);
  } catch (error) {
    paging.loading = false;
    moreButton.disabled = false;
    moreButton.textContent = "المزيد";
    moreButton.hidden = false;
    showToast("تعذّر تحميل المزيد");
    return;
  }
  if (token !== paging.token) return; // تغيّرت الفلاتر أثناء الطلب

  const list = results.querySelector("[data-list]");
  list.insertAdjacentHTML("beforeend", page.items.map(ProductCard).join(""));
  attachImageFallbacks(list);

  paging.offset += page.items.length;
  paging.total = page.total;
  paging.hasMore = page.hasMore;
  paging.loading = false;

  if (!page.total) {
    list.outerHTML = `<div class="state-panel">لا توجد منتجات مطابقة للفلاتر الحالية.</div>`;
  }
  results.querySelector("[data-count]").innerHTML =
    `عرض <strong>${paging.offset}</strong> من <strong>${paging.total}</strong>`;
  moreButton.disabled = false;
  moreButton.textContent = "المزيد";
  moreButton.hidden = !paging.hasMore;

  // إذا بقي آخر عنصر ظاهراً (شاشة كبيرة) نطلب الدفعة التالية مباشرة
  const sentinel = results.querySelector("[data-sentinel]");
  if (paging.hasMore && state.screen === "products" && sentinel
      && sentinel.getBoundingClientRect().top < window.innerHeight + 300) {
    requestAnimationFrame(loadMore);
  }
}

/* عند وصول المستخدم لآخر عنصر معروض تُطلب العناصر الجديدة */
function watchSentinel(sentinel) {
  if (observer) observer.disconnect();
  if (!("IntersectionObserver" in window) || !sentinel) return; // يبقى زر "المزيد"
  observer = new IntersectionObserver((entries) => {
    if (state.screen === "products" && entries.some((e) => e.isIntersecting)) loadMore();
  }, { rootMargin: "300px 0px" });
  observer.observe(sentinel);
}

function renderShell() {
  app.innerHTML = `
    <section id="productsScreen">
      ${Filter({ brands: getBrands(), categories: getParentCategories(), state })}
      <section id="results" aria-live="polite"></section>
    </section>
    <section id="cartScreen" hidden></section>`;
  renderResults();
}

function resetFilters() {
  state.brand = "";
  state.category = "";
  state.name = "";
  state.sku = "";
  app.querySelectorAll("[data-filter]").forEach((field) => { field.value = ""; });
  renderResults();
}

// ------------------------------------------------------------------ السلة

function renderCart() {
  const screen = document.getElementById("cartScreen");
  if (!screen) return;
  screen.innerHTML = CartScreen(readCart(), state.cartView);
  attachImageFallbacks(screen);
}

async function onAddToCart(button) {
  const box = button.closest("[data-order]");
  const guid = box.dataset.order;
  const qtyInput = box.querySelector("[data-order-qty]");
  const unit = box.querySelector("[data-order-unit]:checked")?.value || "carton";
  const qty = parseQty(qtyInput.value);

  if (qty == null) {
    qtyInput.classList.add("is-invalid");
    qtyInput.focus();
    showToast("الكمية يجب أن تكون عدداً صحيحاً من 1 إلى 99999", 2600);
    return;
  }
  qtyInput.classList.remove("is-invalid");

  if (isInCart(guid)) {
    showToast("المنتج موجود في السلة مسبقاً");
    return;
  }
  const product = await getProduct(guid);
  if (!product) return;
  addToCart(product, qty, unit);
  showToast(`أُضيف إلى السلة: ${qty} ${UNITS[unit].label}`);
}

function onCartQtyInput(input) {
  const qty = parseQty(input.value);
  input.classList.toggle("is-invalid", qty == null);
  if (qty != null) updateCartItem(input.dataset.cartQty, { qty });
}

function openConfirmClear() {
  closeConfirm();
  lastFocus = document.activeElement;
  document.body.insertAdjacentHTML("beforeend", ConfirmClearDialog(cartCount()));
  document.body.classList.add("modal-open");
  document.querySelector('[data-confirm] [data-action="cancel-confirm"]').focus(); // الإلغاء هو الافتراضي
}

function closeConfirm() {
  const dialog = document.querySelector("[data-confirm]");
  if (!dialog) return;
  dialog.remove();
  if (!document.querySelector("[data-modal]")) document.body.classList.remove("modal-open");
  if (lastFocus && document.contains(lastFocus)) lastFocus.focus();
}

async function onSendOrder(button) {
  const invalid = document.querySelector("#cartScreen .qty-input.is-invalid");
  if (invalid) {
    invalid.focus();
    showToast("صحّح الكميات غير الصالحة قبل الإرسال", 2600);
    return;
  }
  button.disabled = true;
  const original = button.textContent;
  button.textContent = "جارٍ التجهيز...";
  try {
    const result = await sendOrder();
    showToast(result.message, result.ok ? 4500 : 3000);
  } catch (error) {
    showToast("تعذّر تجهيز الطلب: " + error.message, 4000);
  } finally {
    button.disabled = false;
    button.textContent = original;
  }
}

// ------------------------------------------------------------------ عارض الصور

async function ensureAllImages(slider) {
  if (slider.dataset.loaded === "1") return;
  if (slider._loading) return slider._loading;
  slider._loading = (async () => {
    const images = await getProductImages(slider.dataset.guid);
    const slides = slider.querySelector(".slides");
    const name = (slider.querySelector(".slide")?.getAttribute("alt") || "").split(" — ")[0];
    slides.innerHTML = images.map((img, i) =>
      `<img class="slide${i === 0 ? " is-active" : ""}" ${i === 0 ? "src" : "data-src"}="${escapeHtml(img.path)}" alt="${escapeHtml(name)} — صورة ${i + 1}" data-image draggable="false">`
    ).join("");
    attachImageFallbacks(slides);
    slider.dataset.count = String(images.length);
    slider.dataset.loaded = "1";
  })();
  return slider._loading;
}

async function goToSlide(slider, index) {
  if (!slider || Number(slider.dataset.count) < 2) return;
  await ensureAllImages(slider);
  const slides = [...slider.querySelectorAll(".slide")];
  const count = slides.length;
  if (count < 2) return;
  const target = ((index % count) + count) % count;

  slides.forEach((slide, i) => {
    if (i === target && slide.dataset.src) {
      slide.src = slide.dataset.src; // الصورة تُطلب فقط عند عرضها
      delete slide.dataset.src;
    }
    slide.classList.toggle("is-active", i === target);
  });
  slider.querySelectorAll(".dot").forEach((dot, i) => dot.classList.toggle("is-active", i === target));
  slider.dataset.index = String(target);
}

function stepSlide(slider, delta) {
  goToSlide(slider, Number(slider.dataset.index || 0) + delta);
}

/* السحب على الهاتف (RTL: السحب لليمين = التالي) */
let touch = null;
document.addEventListener("touchstart", (event) => {
  const slider = event.target.closest("[data-slider]");
  if (!slider || event.touches.length !== 1) { touch = null; return; }
  touch = { slider, x: event.touches[0].clientX, y: event.touches[0].clientY };
}, { passive: true });

document.addEventListener("touchend", (event) => {
  if (!touch) return;
  const t = event.changedTouches[0];
  const dx = t.clientX - touch.x;
  const dy = t.clientY - touch.y;
  if (Math.abs(dx) > 40 && Math.abs(dx) > Math.abs(dy)) {
    touch.slider.dataset.swipedAt = String(Date.now());
    stepSlide(touch.slider, dx > 0 ? 1 : -1);
  }
  touch = null;
}, { passive: true });

// ------------------------------------------------------------------ النافذة المنبثقة

async function openModal(guid) {
  const product = await getProduct(guid);
  if (!product) return;
  const images = await getProductImages(guid); // باقي الصور تُجلب هنا مع الحفاظ على الترتيب
  closeModal();
  lastFocus = document.activeElement;
  document.body.insertAdjacentHTML("beforeend", ProductModal(product, images));
  document.body.classList.add("modal-open");
  const modal = document.querySelector("[data-modal]");
  attachImageFallbacks(modal);
  modal.querySelector(".modal-close").focus();
}

function closeModal() {
  const modal = document.querySelector("[data-modal]");
  if (!modal) return;
  modal.remove();
  document.body.classList.remove("modal-open");
  if (lastFocus && document.contains(lastFocus)) lastFocus.focus();
}

// ------------------------------------------------------------------ الأحداث

async function copyText(text) {
  try {
    if (!navigator.clipboard) throw new Error("clipboard unavailable");
    await navigator.clipboard.writeText(text);
  } catch {
    const input = document.createElement("textarea");
    input.value = text;
    document.body.appendChild(input);
    input.select();
    document.execCommand("copy");
    input.remove();
  }
  showToast("تم نسخ الرمز");
}

document.addEventListener("cart:changed", () => {
  updateCartBadge();
  syncOrderInputs();
});

document.addEventListener("input", (event) => {
  const target = event.target;
  if (target.matches("input[data-filter]")) {
    state[target.dataset.filter] = target.value;
    renderResults();
  } else if (target.matches("[data-cart-qty]")) {
    onCartQtyInput(target);
  } else if (target.matches("[data-cart-notes]")) {
    setCartNotes(target.value);
  } else if (target.matches("[data-order-qty]")) {
    target.classList.toggle("is-invalid", parseQty(target.value) == null);
  }
});

document.addEventListener("change", (event) => {
  const target = event.target;
  if (target.matches("select[data-filter]")) {
    state[target.dataset.filter] = target.value;
    renderResults();
  } else if (target.matches("[data-cart-unit]")) {
    updateCartItem(target.dataset.cartUnit, { unit: target.value });
  }
});

document.addEventListener("click", (event) => {
  const target = event.target;

  const copyButton = target.closest("[data-copy]");
  if (copyButton) {
    copyText(copyButton.dataset.copy || "");
    return;
  }

  const action = target.closest("[data-action]")?.dataset.action;
  switch (action) {
    case "add-to-cart": onAddToCart(target.closest("[data-action]")); return;
    case "remove-from-cart": {
      const guid = target.closest("[data-action]").dataset.guid;
      removeFromCart(guid);
      renderCart();
      showToast("تمت إزالة العنصر من السلة");
      return;
    }
    case "ask-clear-cart": openConfirmClear(); return;
    case "cancel-confirm": closeConfirm(); return;
    case "confirm-clear-cart":
      clearCart();
      closeConfirm();
      renderCart();
      showToast("تم تفريغ السلة");
      return;
    case "send-order": onSendOrder(target.closest("[data-action]")); return;
    case "close-modal": closeModal(); return;
    case "more": loadMore(); return;
    case "reset": resetFilters(); return;
    default: break;
  }

  if (target.matches("[data-confirm]")) { closeConfirm(); return; }
  if (target.matches("[data-modal]")) { closeModal(); return; }

  const screenButton = target.closest("[data-screen]");
  if (screenButton) {
    closeModal();
    showScreen(screenButton.dataset.screen);
    return;
  }

  const cartViewButton = target.closest("[data-cart-view]");
  if (cartViewButton) {
    state.cartView = cartViewButton.dataset.cartView;
    try { localStorage.setItem(CART_VIEW_KEY, state.cartView); } catch { /* تفضيل فقط */ }
    renderCart();
    return;
  }

  const stepButton = target.closest("[data-qty-step]");
  if (stepButton) {
    const input = stepButton.parentElement.querySelector(".qty-input");
    const next = Math.min(99999, Math.max(1, (parseQty(input.value) || 1) + Number(stepButton.dataset.qtyStep)));
    input.value = String(next);
    input.dispatchEvent(new Event("input", { bubbles: true }));
    return;
  }

  const slideButton = target.closest("[data-slide], [data-slide-to]");
  if (slideButton) {
    const slider = slideButton.closest("[data-slider]");
    if (slideButton.dataset.slideTo != null) goToSlide(slider, Number(slideButton.dataset.slideTo));
    else stepSlide(slider, slideButton.dataset.slide === "next" ? 1 : -1);
    return;
  }

  if (target.closest("[data-order]")) return; // التفاعل مع OrderInputCom لا يفتح الكارد

  const opener = target.closest("[data-open]");
  if (opener && !target.closest("[data-modal]")) {
    const slider = target.closest("[data-slider]");
    if (slider && Date.now() - Number(slider.dataset.swipedAt || 0) < 500) return; // كان سحباً لا نقراً
    openModal(opener.dataset.open);
  }
});

document.addEventListener("keydown", (event) => {
  const el = event.target instanceof Element ? event.target : document.body;
  const modal = document.querySelector("[data-modal]");
  if (event.key === "Escape") {
    if (document.querySelector("[data-confirm]")) closeConfirm();
    else if (modal) closeModal();
    return;
  }

  if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
    if (el.closest("input, select, textarea")) return;
    const slider = modal ? modal.querySelector("[data-slider]") : el.closest("[data-slider]");
    if (!slider) return;
    event.preventDefault();
    stepSlide(slider, event.key === "ArrowLeft" ? 1 : -1); // RTL: اليسار = التالي
  }
});

window.addEventListener("hashchange", () => showScreen(location.hash === "#cart" ? "cart" : "products"));

/* السلة تتغير من تبويب آخر */
window.addEventListener("storage", (event) => {
  if (event.key !== CART_KEY) return;
  updateCartBadge();
  syncOrderInputs();
  if (state.screen === "cart" && !document.activeElement?.matches("[data-cart-qty], [data-cart-notes]")) renderCart();
});

async function init() {
  if (location.protocol === "file:") {
    app.innerHTML = `<div class="state-panel is-error">
      لا يمكن قراءة ملفات JSON عند فتح الصفحة مباشرة.<br>
      شغّل <code>start-web.bat</code> أو سيرفر محلياً داخل المجلد ثم افتح الصفحة.
    </div>`;
    return;
  }

  try { state.cartView = localStorage.getItem(CART_VIEW_KEY) === "cards" ? "cards" : "table"; } catch { /* افتراضي */ }
  app.innerHTML = `<div class="state-panel">جارٍ تحميل البيانات...</div>`;

  try {
    await Promise.all([loadDatabase(), loadConfig()]);
    renderShell();
    updateCartBadge();
    showScreen(location.hash === "#cart" ? "cart" : "products");
  } catch (error) {
    app.innerHTML = `<div class="state-panel is-error">تعذّر تحميل البيانات:<br>${escapeHtml(error.message)}</div>`;
  }
}

init();
