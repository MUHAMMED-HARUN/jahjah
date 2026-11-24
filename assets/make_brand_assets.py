"""
يستخرج من صورة الهوية (913213015_...jpg) أصول الواجهة بخلفية شفافة:
  assets/logo.png     شعار الجحجاح (العلامة + JAHJAH + الشعار النصي)
  assets/mark.png     العلامة فقط (أيقونة المتصفح)
  assets/dcel.png, assets/dsp.png, assets/lapon.png   أسماء الماركات كما في الصورة
  assets/pattern.png  بلاطة النقوش للخلفية (تُلوَّن في CSS بلون config عبر mask)

التشغيل:  py -3 assets/make_brand_assets.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "913213015_1694930188937942_4781244432167111891_n.jpg"
OUT = ROOT / "assets"
SCALE = 3  # تكبير قبل الاستخراج لحواف أنعم

BG = np.array([253, 248, 229], dtype=float)       # الكريمي
INKS = np.array([[103, 22, 23], [177, 145, 84]], dtype=float)  # العنابي، الذهبي

img = Image.open(SOURCE).convert("RGB")


def extract(box, pad=4):
    """قص + تكبير + فصل الخلفية: لكل بكسل نختار لون الحبر الأقرب ونحسب نسبة تغطيته."""
    x0, y0, x1, y1 = box
    crop = img.crop((x0 - pad, y0 - pad, x1 + pad, y1 + pad))
    crop = crop.resize((crop.width * SCALE, crop.height * SCALE), Image.LANCZOS)
    p = np.asarray(crop).astype(float)

    best_alpha = np.zeros(p.shape[:2])
    best_err = np.full(p.shape[:2], np.inf)
    best_ink = np.zeros(p.shape[:2], dtype=int)
    for i, ink in enumerate(INKS):
        v = ink - BG
        a = ((p - BG) @ v) / (v @ v)          # إسقاط على خط (الخلفية → الحبر)
        a = np.clip(a, 0, 1)
        err = np.linalg.norm(p - (BG + a[..., None] * v), axis=2)
        better = err < best_err
        best_err[better], best_alpha[better], best_ink[better] = err[better], a[better], i

    alpha = np.clip((best_alpha - 0.06) / 0.94, 0, 1)  # إزالة ضوضاء الخلفية الخفيفة
    rgba = np.zeros(p.shape[:2] + (4,), dtype=np.uint8)
    rgba[..., :3] = INKS[best_ink].astype(np.uint8)
    rgba[..., 3] = (alpha * 255).round().astype(np.uint8)
    out = Image.fromarray(rgba, "RGBA")
    return out.crop(out.getbbox())


OUT.mkdir(exist_ok=True)

# الشعار: العلامة + الاسم (377,195)-(585,301) والشعار النصي بدون الخطوط الطويلة (353,312)-(609,323)
mark_name = extract((377, 195, 585, 301))
tagline = extract((352, 311, 610, 324), pad=1)
gap = 8 * SCALE
logo = Image.new("RGBA", (max(mark_name.width, tagline.width), mark_name.height + gap + tagline.height), (0, 0, 0, 0))
logo.alpha_composite(mark_name, ((logo.width - mark_name.width) // 2, 0))
logo.alpha_composite(tagline, ((logo.width - tagline.width) // 2, mark_name.height + gap))
logo.save(OUT / "logo.png", optimize=True)

mark = extract((436, 195, 532, 258))
mark.save(OUT / "mark.png", optimize=True)

brands = {
    "dcel": extract((250, 347, 370, 371)),
    "dsp": extract((435, 340, 532, 378)),
    "lapon": extract((600, 341, 715, 371)),
}
for name, im in brands.items():
    im.save(OUT / f"{name}.png", optimize=True)

# بلاطة النقوش: صفوف متداخلة (مثل الطوب) حتى يتكرر النقش بدون فواصل ظاهرة
TILE_W, TILE_H = 1500, 720
tile = Image.new("RGBA", (TILE_W, TILE_H), (0, 0, 0, 0))
order = ["dcel", "dsp", "lapon"]
rows = [(90, 0), (450, 250)]  # (y, إزاحة x)
step = TILE_W // 3
for r, (y, shift) in enumerate(rows):
    for c in range(3):
        name = order[(c + r) % 3]
        im = brands[name]
        x = (shift + c * step + (step - im.width) // 2) % TILE_W
        if x + im.width > TILE_W:   # تقسيم عند حافة البلاطة ليبقى التكرار متصلاً
            tile.alpha_composite(im.crop((0, 0, TILE_W - x, im.height)), (x, y))
            tile.alpha_composite(im.crop((TILE_W - x, 0, im.width, im.height)), (0, y))
        else:
            tile.alpha_composite(im, (x, y))
tile.save(OUT / "pattern.png", optimize=True)

for f in ["logo.png", "mark.png", "dcel.png", "dsp.png", "lapon.png", "pattern.png"]:
    with Image.open(OUT / f) as im:
        print(f"{f:12} {im.width}x{im.height}  {(OUT / f).stat().st_size // 1024} KB")
