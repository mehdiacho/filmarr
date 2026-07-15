"""Generate Play Store graphics for Filmarr (app icon 512x512 + feature graphic 1024x500).

Derived from templates/android/store-assets/generate_play_assets.py. Brand: Filmarr's Rose ->
Violet diagonal gradient with a white heart glyph (the app's signature "films you love" mark).

    pip install pillow
    python generate_play_assets.py
"""
from PIL import Image, ImageDraw, ImageFont
import os

# ---------------------------------------------------------------- CONFIG (Filmarr)
OUT = os.path.join(os.path.dirname(__file__), "out")
GRADIENT = [(0xFB, 0x71, 0x85), (0xF4, 0x3F, 0x5E), (0xD9, 0x46, 0xEF)]  # rose-300 -> rose -> fuchsia
WORDMARK = "Filmarr"
TAGLINE = "Films you'll love, found by feel."
# ----------------------------------------------------------------

os.makedirs(OUT, exist_ok=True)
C1, C2, C3 = GRADIENT


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(w, h):
    """Diagonal 3-stop gradient (top-left -> bottom-right)."""
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / w + y / h) / 2
            c = lerp(C1, C2, t / 0.55) if t < 0.55 else lerp(C2, C3, (t - 0.55) / 0.45)
            px[x, y] = c
    return img


def font(size, bold=True):
    for name in (["segoeuib.ttf", "arialbd.ttf", "DejaVuSans-Bold.ttf"] if bold
                 else ["segoeui.ttf", "arial.ttf", "DejaVuSans.ttf"]):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_glyph(base, cx, cy, scale):
    """A white heart centered at (cx, cy), drawn on a transparent overlay then composited so its
    edges anti-alias against the gradient. `scale` ~= heart half-width in px."""
    ov = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    r = scale * 0.55                     # lobe radius
    ly = cy - scale * 0.18               # lobe center y
    white = (255, 255, 255, 255)
    # two top lobes
    d.ellipse([cx - 2 * r, ly - r, cx, ly + r], fill=white)
    d.ellipse([cx, ly - r, cx + 2 * r, ly + r], fill=white)
    # bottom point (triangle meeting the lobes)
    d.polygon([(cx - 2 * r, ly), (cx + 2 * r, ly), (cx, cy + scale * 1.15)], fill=white)
    return Image.alpha_composite(base, ov)


# ---------- App icon 512x512 (full-bleed square; Play masks corners itself) ----------
icon = gradient(512, 512).convert("RGBA")
hl = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
ImageDraw.Draw(hl).ellipse([-180, -220, 420, 240], fill=(255, 255, 255, 26))  # soft top highlight
icon = Image.alpha_composite(icon, hl)
icon = draw_glyph(icon, 256, 250, scale=150)
icon.convert("RGB").save(os.path.join(OUT, "icon-512.png"))

# ---------- Feature graphic 1024x500 ----------
fg = gradient(1024, 500).convert("RGBA")
hl = Image.new("RGBA", (1024, 500), (0, 0, 0, 0))
ImageDraw.Draw(hl).ellipse([-300, -320, 700, 260], fill=(255, 255, 255, 22))
fg = Image.alpha_composite(fg, hl)

# Heart tile on the left, on a translucent rounded tile.
tile_cx, tile_cy, tile_half = 200, 250, 120
ov = Image.new("RGBA", fg.size, (0, 0, 0, 0))
ImageDraw.Draw(ov).rounded_rectangle(
    [tile_cx - tile_half, tile_cy - tile_half, tile_cx + tile_half, tile_cy + tile_half],
    radius=56, fill=(255, 255, 255, 34))
fg = Image.alpha_composite(fg, ov)
fg = draw_glyph(fg, tile_cx, tile_cy - 6, scale=78)
d = ImageDraw.Draw(fg)
d.text((380, 155), WORDMARK, font=font(120), fill=(255, 255, 255, 255))
d.text((384, 300), TAGLINE, font=font(40, bold=False), fill=(255, 245, 250, 235))
fg.convert("RGB").save(os.path.join(OUT, "feature-1024x500.png"))

print("assets written to", OUT, "->", os.listdir(OUT))
