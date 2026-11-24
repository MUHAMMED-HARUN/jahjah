/*
 * خدمة الطلب: الإعدادات (config.json) + السلة في localStorage + الإرسال إلى واتساب.
 */
const CART_KEY = "jahjah.cart.v1";
const DEFAULT_CONFIG = { whatsappNumber: "", cartTtlDays: 7, messageType: "image" };
const UNITS = {
  carton: { label: "طرد" },
  piece: { label: "قطعة" }
};

let appConfig = { ...DEFAULT_CONFIG };

async function loadConfig() {
  try {
    const response = await fetch("config.json", { cache: "no-store" });
    if (response.ok) appConfig = { ...DEFAULT_CONFIG, ...(await response.json()) };
  } catch {
    // نكمل بالقيم الافتراضية
  }
  const days = Number(appConfig.cartTtlDays);
  appConfig.cartTtlDays = Number.isFinite(days) && days > 0 ? days : DEFAULT_CONFIG.cartTtlDays;
  appConfig.messageType = appConfig.messageType === "text" ? "text" : "image";
  appConfig.whatsappNumber = String(appConfig.whatsappNumber || "").replace(/\D/g, "").replace(/^00/, "");
  return appConfig;
}

// ------------------------------------------------------------------ التخزين

function emptyCart() {
  return { items: [], notes: "", updatedAt: Date.now() };
}

/* يقرأ السلة ويحذف العناصر التي تجاوزت مدة البقاء المحددة في config */
function readCart() {
  let cart;
  try {
    cart = JSON.parse(localStorage.getItem(CART_KEY) || "null");
  } catch {
    cart = null;
  }
  if (!cart || !Array.isArray(cart.items)) return emptyCart();

  const ttl = appConfig.cartTtlDays * 24 * 60 * 60 * 1000;
  const now = Date.now();
  const fresh = cart.items.filter((it) => it && it.guid && now - Number(it.addedAt || 0) < ttl);
  if (fresh.length !== cart.items.length) {
    cart.items = fresh;
    if (!fresh.length) cart.notes = "";
    writeCart(cart);
  }
  return cart;
}

function writeCart(cart) {
  cart.updatedAt = Date.now();
  try {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
  } catch {
    showToast("تعذّر حفظ السلة في المتصفح");
  }
  document.dispatchEvent(new CustomEvent("cart:changed"));
}

function cartItems() { return readCart().items; }

function cartCount() { return readCart().items.length; }

function isInCart(guid) { return readCart().items.some((it) => it.guid === guid); }

/* يُخزَّن السجل الكامل للمنتج مع الكمية ونوع التعبئة. العنصر الموجود لا يُضاف مرة أخرى. */
function addToCart(product, qty, unit) {
  const cart = readCart();
  if (cart.items.some((it) => it.guid === product.guid)) return false;
  cart.items.push({ guid: product.guid, product: { ...product }, qty, unit, addedAt: Date.now() });
  writeCart(cart);
  return true;
}

function updateCartItem(guid, changes) {
  const cart = readCart();
  const item = cart.items.find((it) => it.guid === guid);
  if (!item) return;
  Object.assign(item, changes);
  writeCart(cart);
}

function removeFromCart(guid) {
  const cart = readCart();
  cart.items = cart.items.filter((it) => it.guid !== guid);
  if (!cart.items.length) cart.notes = "";
  writeCart(cart);
}

function setCartNotes(notes) {
  const cart = readCart();
  cart.notes = notes;
  writeCart(cart);
}

function clearCart() {
  writeCart(emptyCart());
}

/* التحقق من الكمية (Data validation): عدد صحيح بين 1 و 99999 */
function parseQty(value) {
  const text = String(value ?? "").trim()
    .replace(/[٠-٩]/g, (d) => "٠١٢٣٤٥٦٧٨٩".indexOf(d))
    .replace(/[۰-۹]/g, (d) => "۰۱۲۳۴۵۶۷۸۹".indexOf(d));
  if (!/^\d+$/.test(text)) return null;
  const n = Number(text);
  return n >= 1 && n <= 99999 ? n : null;
}

// ------------------------------------------------------------------ الرسالة

/* البيانات المرسلة فقط: الرمز، الاسم كما في البرنامج، اسم المنتج، الكمية، نوع التعبئة */
function orderRows(cart) {
  return cart.items.map((it) => ({
    sku: it.product.sku || "—",
    programName: it.product.nameingAsProgram || "—",
    name: it.product.name || "—",
    qty: it.qty,
    unit: (UNITS[it.unit] || UNITS.carton).label
  }));
}

function orderDate() {
  return new Date().toLocaleString("en-GB", { dateStyle: "short", timeStyle: "short" });
}

function orderText(cart) {
  const lines = [`طلب جديد — ${orderDate()}`, `عدد الأصناف: ${cart.items.length}`, ""];
  orderRows(cart).forEach((r, i) => {
    lines.push(`${i + 1}) الرمز: ${r.sku}`);
    lines.push(`   كما في البرنامج: ${r.programName}`);
    lines.push(`   المنتج: ${r.name}`);
    lines.push(`   الكمية: ${r.qty} ${r.unit}`);
  });
  if (cart.notes && cart.notes.trim()) {
    lines.push("", "ملاحظات:", cart.notes.trim());
  }
  return lines.join("\n");
}

/* صورة طويلة: صف لكل منتج، والملاحظات في آخرها */
async function orderImage(cart) {
  const W = 1080;
  const PAD = 32;
  const FONT = '"Segoe UI", Tahoma, Arial, sans-serif';
  const cols = [
    { key: "index", title: "#", w: 50 },
    { key: "sku", title: "رمز المنتج", w: 150 },
    { key: "programName", title: "الاسم كما في البرنامج", w: 270 },
    { key: "name", title: "اسم المنتج", w: 300 },
    { key: "qty", title: "الكمية", w: 120 },
    { key: "unit", title: "التعبئة", w: 126 }
  ];
  const rows = orderRows(cart).map((r, i) => ({ ...r, index: String(i + 1), qty: String(r.qty) }));

  const measure = document.createElement("canvas").getContext("2d");
  const wrap = (text, width, font) => {
    measure.font = font;
    const words = String(text).split(/\s+/);
    const lines = [];
    let line = "";
    words.forEach((word) => {
      const test = line ? `${line} ${word}` : word;
      if (measure.measureText(test).width > width && line) {
        lines.push(line);
        line = word;
      } else {
        line = test;
      }
    });
    lines.push(line);
    return lines;
  };

  const cellFont = `20px ${FONT}`;
  const lineH = 28;
  const laidOut = rows.map((r) => {
    const cells = cols.map((c) => wrap(r[c.key], c.w - 16, cellFont));
    return { cells, h: Math.max(...cells.map((l) => l.length)) * lineH + 20 };
  });
  const notes = (cart.notes || "").trim();
  const noteLines = notes ? notes.split(/\n/).flatMap((p) => wrap(p, W - PAD * 2 - 24, cellFont)) : [];

  const headerH = 120;
  const tableHeadH = 52;
  const notesH = noteLines.length ? noteLines.length * lineH + 70 : 0;
  const H = headerH + tableHeadH + laidOut.reduce((s, r) => s + r.h, 0) + notesH + PAD * 2;

  const scale = 1;
  const canvas = document.createElement("canvas");
  canvas.width = W * scale;
  canvas.height = H * scale;
  const ctx = canvas.getContext("2d");
  ctx.scale(scale, scale);
  ctx.direction = "rtl";
  ctx.textBaseline = "top";

  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, W, H);

  // الرأس
  ctx.fillStyle = "#4f46e5";
  ctx.fillRect(0, 0, W, headerH);
  ctx.fillStyle = "#ffffff";
  ctx.textAlign = "right";
  ctx.font = `bold 34px ${FONT}`;
  ctx.fillText("طلب جديد", W - PAD, 26);
  ctx.font = `20px ${FONT}`;
  ctx.fillText(`${orderDate()}   •   عدد الأصناف: ${rows.length}`, W - PAD, 74);

  // رأس الجدول (من اليمين لليسار)
  let y = headerH + PAD;
  ctx.fillStyle = "#eef2ff";
  ctx.fillRect(PAD, y, W - PAD * 2, tableHeadH);
  ctx.fillStyle = "#3730a3";
  ctx.font = `bold 19px ${FONT}`;
  let x = W - PAD;
  cols.forEach((c) => {
    ctx.fillText(c.title, x - 8, y + 15);
    x -= c.w;
  });
  y += tableHeadH;

  // الصفوف
  laidOut.forEach((row, i) => {
    ctx.fillStyle = i % 2 ? "#f8fafc" : "#ffffff";
    ctx.fillRect(PAD, y, W - PAD * 2, row.h);
    ctx.fillStyle = "#111827";
    ctx.font = cellFont;
    x = W - PAD;
    cols.forEach((c, ci) => {
      row.cells[ci].forEach((text, li) => ctx.fillText(text, x - 8, y + 10 + li * lineH));
      x -= c.w;
    });
    ctx.strokeStyle = "#e5e7eb";
    ctx.beginPath();
    ctx.moveTo(PAD, y + row.h);
    ctx.lineTo(W - PAD, y + row.h);
    ctx.stroke();
    y += row.h;
  });

  // الملاحظات في آخر الصورة
  if (noteLines.length) {
    y += 24;
    ctx.fillStyle = "#fffbeb";
    ctx.fillRect(PAD, y, W - PAD * 2, notesH - 24);
    ctx.fillStyle = "#92400e";
    ctx.font = `bold 20px ${FONT}`;
    ctx.fillText("ملاحظات:", W - PAD - 12, y + 12);
    ctx.fillStyle = "#111827";
    ctx.font = cellFont;
    noteLines.forEach((text, li) => ctx.fillText(text, W - PAD - 12, y + 46 + li * lineH));
  }

  return new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
}

function whatsappUrl(text) {
  return `https://wa.me/${appConfig.whatsappNumber}?text=${encodeURIComponent(text)}`;
}

function downloadBlob(blob, fileName) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

/*
 * الإرسال حسب messageType في config:
 *  text  → يفتح محادثة واتساب مع الرقم والرسالة جاهزة.
 *  image → على الهاتف: مشاركة الصورة مباشرة (يختار المستخدم واتساب).
 *          على الكمبيوتر: تنزيل الصورة ونسخها للحافظة، ثم فتح محادثة الرقم للصقها.
 */
async function sendOrder() {
  const cart = readCart();
  if (!cart.items.length) return { ok: false, message: "السلة فارغة." };
  if (!appConfig.whatsappNumber) return { ok: false, message: "لم يُضبط رقم واتساب في الإعدادات (config.json)." };

  if (appConfig.messageType === "text") {
    window.open(whatsappUrl(orderText(cart)), "_blank", "noopener");
    return { ok: true, message: "تم فتح واتساب مع الرسالة." };
  }

  const blob = await orderImage(cart);
  const fileName = `order-${new Date().toISOString().slice(0, 16).replace(/[:T]/g, "-")}.png`;
  const file = new File([blob], fileName, { type: "image/png" });
  const caption = `طلب جديد — ${cart.items.length} أصناف`;

  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try {
      await navigator.share({ files: [file], text: caption });
      return { ok: true, message: "تمت مشاركة صورة الطلب." };
    } catch (error) {
      if (error && error.name === "AbortError") return { ok: false, message: "تم إلغاء المشاركة." };
    }
  }

  downloadBlob(blob, fileName);
  let copied = false;
  try {
    if (navigator.clipboard && window.ClipboardItem) {
      await navigator.clipboard.write([new ClipboardItem({ "image/png": blob })]);
      copied = true;
    }
  } catch {
    copied = false;
  }
  window.open(whatsappUrl(caption), "_blank", "noopener");
  return {
    ok: true,
    message: copied
      ? "تم تنزيل صورة الطلب ونسخها — الصقها (Ctrl+V) في محادثة واتساب."
      : "تم تنزيل صورة الطلب — أرفقها في محادثة واتساب."
  };
}
