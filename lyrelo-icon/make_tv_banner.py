"""Draw the Android TV banner: the Lyrelo mark beside the wordmark."""
import sys

from PIL import Image, ImageDraw, ImageFont

BG = (20, 12, 36)
G_START = (139, 92, 246)
G_END = (236, 72, 153)
BAR_W, GAP = 7.5, 3.0
HEIGHTS = [42, 15, 10, 18, 12]
SCALE = 4
W, H = 1024 * SCALE, 576 * SCALE


def main(font_path, out_paths):
    image = Image.new("RGB", (W, H), BG)
    font = ImageFont.truetype(font_path, 190 * SCALE)
    draw = ImageDraw.Draw(image)
    text = "Lyrelo"
    box = draw.textbbox((0, 0), text, font=font)
    text_w, text_h = box[2] - box[0], box[3] - box[1]

    unit = 5.0 * SCALE
    mark_w = (len(HEIGHTS) * BAR_W + (len(HEIGHTS) - 1) * GAP) * unit
    mark_h = max(HEIGHTS) * unit
    gap = 56 * SCALE
    left = (W - (mark_w + gap + text_w)) / 2
    top = (H - mark_h) / 2

    mask = Image.new("L", (W, H), 0)
    mask_draw = ImageDraw.Draw(mask)
    for i, height in enumerate(HEIGHTS):
        x = left + i * (BAR_W + GAP) * unit
        y = top + mark_h - height * unit
        mask_draw.rounded_rectangle(
            (x, y, x + BAR_W * unit, top + mark_h), radius=BAR_W * unit / 2, fill=255
        )
    gradient = Image.new("RGB", (W, H))
    pixels = gradient.load()
    for py in range(H):
        for px in range(W):
            t = ((px - left) / mark_w + (py - top) / mark_h) / 2
            t = max(0.0, min(1.0, t))
            pixels[px, py] = tuple(int(a + (b - a) * t) for a, b in zip(G_START, G_END))
    image.paste(gradient, (0, 0), mask)

    text_x = left + mark_w + gap - box[0]
    text_y = (H - text_h) / 2 - box[1]
    draw.text((text_x, text_y), text, font=font, fill=(255, 255, 255))

    final = image.resize((W // SCALE, H // SCALE), Image.LANCZOS)
    for path in out_paths:
        final.save(path, optimize=True)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2:])
