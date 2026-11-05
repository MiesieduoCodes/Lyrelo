"""Generate the Lyrelo icon: an L built from equalizer bars.

Outputs Android adaptive-icon resources (foreground, background, monochrome)
plus SVG masters and PNG previews. All geometry lives on the standard
108x108 adaptive-icon canvas; the artwork stays inside the 66dp safe zone.
"""
import os

OUT = os.path.dirname(os.path.abspath(__file__))

# Brand colors
BG = "#140C24"        # deep night purple
G_START = "#8B5CF6"   # violet
G_END = "#EC4899"     # pink

# Bars: (height) — first bar is the L's upright, the rest form its foot,
# with uneven heights so the foot reads as an equalizer.
BAR_W, GAP, RADIUS = 7.5, 3.0, 3.75
HEIGHTS = [42, 15, 10, 18, 12]
BASELINE = 75.0
TOTAL_W = len(HEIGHTS) * BAR_W + (len(HEIGHTS) - 1) * GAP
X0 = (108 - TOTAL_W) / 2


def bars():
    out = []
    for i, h in enumerate(HEIGHTS):
        x = X0 + i * (BAR_W + GAP)
        out.append((x, BASELINE - h, BAR_W, h))
    return out


def rrect_path(x, y, w, h, r):
    """Path data for a fully rounded rectangle (pill ends)."""
    return (f"M{x + r:.2f},{y:.2f}"
            f"H{x + w - r:.2f}"
            f"A{r},{r} 0 0 1 {x + w:.2f},{y + r:.2f}"
            f"V{y + h - r:.2f}"
            f"A{r},{r} 0 0 1 {x + w - r:.2f},{y + h:.2f}"
            f"H{x + r:.2f}"
            f"A{r},{r} 0 0 1 {x:.2f},{y + h - r:.2f}"
            f"V{y + r:.2f}"
            f"A{r},{r} 0 0 1 {x + r:.2f},{y:.2f}Z")


PATH = " ".join(rrect_path(*b, RADIUS) for b in bars())
TOP = min(y for _, y, _, _ in bars())


def write(rel, text):
    p = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        f.write(text)
    return p


# ---------- Android adaptive icon resources ----------
write("res/drawable/ic_launcher_background.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z" android:fillColor="{BG}"/>
    <!-- soft glow behind the mark -->
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="radial"
                android:centerX="54" android:centerY="58" android:gradientRadius="52"
                android:startColor="#552D1B4D" android:endColor="#00140C24"/>
        </aapt:attr>
    </path>
</vector>
""")

write("res/drawable/ic_launcher_foreground.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="{PATH}">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear"
                android:startX="{X0:.2f}" android:startY="{TOP:.2f}"
                android:endX="{X0 + TOTAL_W:.2f}" android:endY="{BASELINE:.2f}"
                android:startColor="{G_START}" android:endColor="{G_END}"/>
        </aapt:attr>
    </path>
</vector>
""")

# Monochrome: Android 13+ themed icons tint this single-color shape.
write("res/drawable/ic_launcher_monochrome.xml", f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="{PATH}" android:fillColor="#FFFFFFFF"/>
</vector>
""")

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
"""
write("res/mipmap-anydpi-v26/ic_launcher.xml", ADAPTIVE)
write("res/mipmap-anydpi-v26/ic_launcher_round.xml", ADAPTIVE)

# ---------- SVG masters ----------
GRAD_DEFS = f"""<defs>
  <linearGradient id="g" gradientUnits="userSpaceOnUse"
      x1="{X0:.2f}" y1="{TOP:.2f}" x2="{X0 + TOTAL_W:.2f}" y2="{BASELINE:.2f}">
    <stop offset="0" stop-color="{G_START}"/><stop offset="1" stop-color="{G_END}"/>
  </linearGradient>
  <radialGradient id="glow" gradientUnits="userSpaceOnUse" cx="54" cy="58" r="52">
    <stop offset="0" stop-color="#2D1B4D" stop-opacity="0.33"/>
    <stop offset="1" stop-color="{BG}" stop-opacity="0"/>
  </radialGradient>
</defs>"""


def svg(body, size=512, view="0 0 108 108", extra_defs=""):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
            f'viewBox="{view}">{GRAD_DEFS}{extra_defs}{body}</svg>')


FULL_BG = f'<rect width="108" height="108" fill="{BG}"/><rect width="108" height="108" fill="url(#glow)"/>'
MARK = f'<path d="{PATH}" fill="url(#g)"/>'

# Square full-bleed master (Play Store 512x512 wants square, no rounding)
write("svg/lyrelo-icon-full.svg", svg(FULL_BG + MARK))
write("svg/lyrelo-mark.svg", svg(MARK))  # transparent, for docs/README/web
write("svg/lyrelo-mark-mono.svg", svg(f'<path d="{PATH}" fill="#000"/>'))

# ---------- Preview sheet: circle, squircle, themed light, themed dark ----------
def tile(cx, label, shape, bg_fill, mark):
    s = 108
    clip = (f'<circle cx="54" cy="54" r="54"/>' if shape == "circle"
            else '<path d="M54,0C92,0 108,16 108,54C108,92 92,108 54,108C16,108 0,92 0,54C0,16 16,0 54,0Z"/>')
    cid = f"c{cx}"
    return (f'<g transform="translate({cx},40)">'
            f'<clipPath id="{cid}">{clip}</clipPath>'
            f'<g clip-path="url(#{cid})">{bg_fill}{mark}</g></g>'
            f'<text x="{cx + 54}" y="175" font-family="sans-serif" font-size="11" '
            f'fill="#555" text-anchor="middle">{label}</text>')


themed_light_bg = '<rect width="108" height="108" fill="#E8DEF8"/>'
themed_light_mark = f'<path d="{PATH}" fill="#4A3B6B"/>'
themed_dark_bg = '<rect width="108" height="108" fill="#2B2733"/>'
themed_dark_mark = f'<path d="{PATH}" fill="#D7C9F5"/>'

sheet = (f'<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="420" viewBox="0 0 540 210">'
         f'{GRAD_DEFS}<rect width="540" height="210" fill="#F6F4F9"/>'
         + tile(20, "Circle", "circle", FULL_BG, MARK)
         + tile(150, "Squircle", "squircle", FULL_BG, MARK)
         + tile(280, "Themed (light)", "circle", themed_light_bg, themed_light_mark)
         + tile(410, "Themed (dark)", "circle", themed_dark_bg, themed_dark_mark)
         + '</svg>')
write("svg/preview-sheet.svg", sheet)

print("bars:", bars())
print("safe-zone check (max dist from center, must be < 33):",
      max(((x - 54) ** 2 + (y - 54) ** 2) ** 0.5
          for bx, by, bw, bh in bars()
          for x in (bx, bx + bw) for y in (by, by + bh)))
