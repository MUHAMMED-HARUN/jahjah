/* الحالة، الرسم، والأحداث */
const state = { brand: "", category: "", name: "", sku: "", view: "table" };
let database = { brands: [], products: [] };
let categories = [];

const app = document.getElementById("app");
const toast = document.getElementById("toast");
let toastTimer;

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("is-visible"), 1800);
}

function uniqueCategories(products) {
  const map = new Map();
  products.forEach((product) => {
    if (product.category && !map.has(product.category)) {
      map.set(product.category, product.category);
    }
  });
  return Array.from(map, ([value, label]) => ({ value, label }));
}

function filteredProducts() {
  const name = state.name.trim().toLocaleLowerCase("ar");
  const sku = state.sku.trim().toLocaleLowerCase("ar");

  return database.products.filter((product) => {
    const matchesBrand = !state.brand || product.brandId === state.brand;
    const matchesCategory = !state.category || product.category === state.category;
    const matchesName = !name || (product.name || "").toLocaleLowerCase("ar").includes(name);
    const matchesSku = !sku || (product.sku || "").toLocaleLowerCase("ar").includes(sku);
    return matchesBrand && matchesCategory && matchesName && matchesSku;
  });
}

function attachImageFallbacks(root) {
  root.querySelectorAll("img[data-image]").forEach((image) => {
    image.addEventListener("error", () => {
      const fallback = document.createElement("div");
      fallback.className = "thumb-fallback";
      fallback.setAttribute("aria-label", image.alt);
      fallback.textContent = (image.alt || "؟").trim().charAt(0);
      image.replaceWith(fallback);
    });
  });
}

function renderResults() {
  const results = document.getElementById("results");
  if (!results) return;

  const products = filteredProducts();
  const summary = `<div class="results-head"><div class="count">عدد النتائج: <strong>${products.length}</strong></div></div>`;

  let content;
  if (!products.length) {
    content = `<div class="state-panel">لا توجد منتجات مطابقة للفلاتر الحالية.</div>`;
  } else if (state.view === "cards") {
    content = `<div class="cards-grid">${products.map(ProductCard).join("")}</div>`;
  } else {
    content = Table({ products });
  }

  results.innerHTML = summary + content;
  attachImageFallbacks(results);
}

function renderShell() {
  app.innerHTML =
    Filter({ brands: database.brands, categories, state }) +
    `<section id="results" aria-live="polite"></section>`;
  renderResults();
}

function resetFilters() {
  state.brand = "";
  state.category = "";
  state.name = "";
  state.sku = "";
  app.querySelector('[data-filter="brand"]').value = "";
  app.querySelector('[data-filter="category"]').value = "";
  app.querySelector('[data-filter="name"]').value = "";
  app.querySelector('[data-filter="sku"]').value = "";
  renderResults();
}

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

document.addEventListener("input", (event) => {
  const field = event.target.closest("input[data-filter]");
  if (!field) return;
  state[field.dataset.filter] = field.value;
  renderResults();
});

document.addEventListener("change", (event) => {
  const field = event.target.closest("select[data-filter]");
  if (!field) return;
  state[field.dataset.filter] = field.value;
  renderResults();
});

document.addEventListener("click", (event) => {
  const viewButton = event.target.closest("[data-view]");
  if (viewButton) {
    state.view = viewButton.dataset.view;
    document.querySelectorAll("[data-view]").forEach((button) => {
      button.classList.toggle("is-active", button === viewButton);
    });
    renderResults();
    return;
  }

  const copyButton = event.target.closest("[data-copy]");
  if (copyButton) {
    copyText(copyButton.dataset.copy || "");
    return;
  }

  if (event.target.closest('[data-action="reset"]')) {
    resetFilters();
  }
});

async function init() {
  if (location.protocol === "file:") {
    app.innerHTML = `<div class="state-panel is-error">
      لا يمكن قراءة ملفات JSON عند فتح الصفحة مباشرة。<br>
      شغّل سيرفر محلياً داخل المجلد ثم افتح الصفحة، مثلاً:
      <code>python -m http.server</code> أو عبر Live Server.
    </div>`;
    return;
  }

  app.innerHTML = `<div class="state-panel">جارٍ تحميل البيانات...</div>`;

  try {
    database = await loadDatabase();
    categories = uniqueCategories(database.products);
    renderShell();
  } catch (error) {
    app.innerHTML = `<div class="state-panel is-error">تعذّر تحميل البيانات:<br>${escapeHtml(error.message)}</div>`;
  }
}

init();
