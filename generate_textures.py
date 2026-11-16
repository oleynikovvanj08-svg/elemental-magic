#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_textures.py - 32x32 pixel-art item textures for the elemental-magic mod.

For every element it draws 4 items (20 elements x 4 = 80 PNG, 32x32, RGBA):
    <id>spellbook.png    -> item id  <id>spellbook
    <id>staff.png        -> item id  <id>staff
    <id>amulet.png       -> item id  <id>amulet
    <id>ring.png         -> item id  <id>ring

WHERE TO PUT THIS FILE
    <repo root>/generate_textures.py     (next to build.gradle and gradlew)
    Do NOT put it into src/main/resources - it would be packed into the jar
    as a game resource. From the repo root the script finds the texture folder
    by itself (it also works from any other working directory).

WHERE THE PNGs GO
    <repo root>/src/main/resources/assets/elementalmagic/textures/item/
    Missing item models are also created in .../assets/elementalmagic/models/item/

USAGE
    py generate_textures.py                     # create only missing files
    py generate_textures.py --overwrite         # replace existing files too
    py generate_textures.py --only fire ice     # only these elements
    py generate_textures.py --out some/dir      # custom texture folder
    py generate_textures.py --check             # just show detected paths

No third-party libraries needed: the PNG is written with zlib + struct only,
so Pillow is not required.

Armor is NOT generated here: armor uses layer textures, not item textures
(models/armor/<id>_layer_1.png and _layer_2.png).
"""

import argparse
import json
import os
import struct
import sys
import zlib
from pathlib import Path

SIZE = 32
CLEAR = (0, 0, 0, 0)

# id -> base color. Copied from Element.java (no underscores in ids!).
ELEMENTS = [
    ("lightning", 0x7FDBFF),
    ("light", 0xFFF3A0),
    ("darkness", 0x8A2BE2),
    ("water", 0x1E90FF),
    ("earth", 0x8B5A2B),
    ("air", 0xC8F7F7),
    ("fire", 0xFF4500),
    ("nature", 0x3CB043),
    ("ash", 0x9E9E9E),
    ("explosion", 0xFF8C00),
    ("elemental", 0x40E0D0),
    ("ice", 0xAFEEEE),
    ("gravity", 0x6A5ACD),
    ("poison", 0x7CFC00),
    ("summoning", 0xDA70D6),
    ("time", 0xD4AF37),
    ("stars", 0xB0C4FF),
    ("darknature", 0x556B2F),
    ("shadow", 0x4B4B6E),
    ("lava", 0xFF6A00),
]

# Shared materials - identical for every element.
GOLD = (233, 199, 101)
GOLDD = (150, 112, 38)
PARCH = (241, 226, 192)
PARCHD = (198, 172, 128)
WOOD = (122, 86, 54)
WOODD = (78, 54, 32)
WOODL = (160, 118, 78)


# ----------------------------------------------------------------- helpers
def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    out = []
    for i in range(3):
        out.append(int(a[i] + (b[i] - a[i]) * t + 0.5))
    return tuple(out)


def rgba(c):
    return (c[0], c[1], c[2], 255)


def palette(base_hex):
    b = rgb(base_hex)
    return {
        "out": rgba(mix(b, (0, 0, 0), 0.78)),        # outline
        "dark": rgba(mix(b, (0, 0, 0), 0.42)),       # shadow of element color
        "base": rgba(b),                             # element color
        "light": rgba(mix(b, (255, 255, 255), 0.38)),
        "glow": rgba(mix(b, (255, 255, 255), 0.80)),
        "gold": rgba(GOLD),
        "goldd": rgba(GOLDD),
        "parch": rgba(PARCH),
        "parchd": rgba(PARCHD),
        "wood": rgba(WOOD),
        "woodd": rgba(WOODD),
        "woodl": rgba(WOODL),
    }


def blank():
    return [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]


def put(g, x, y, c):
    xi = int(x + 0.5)
    yi = int(y + 0.5)
    if 0 <= xi < SIZE and 0 <= yi < SIZE and c is not None:
        g[yi][xi] = c


def rect(g, x, y, w, h, c):
    for j in range(y, y + h):
        for i in range(x, x + w):
            put(g, i, j, c)


def hline(g, x0, x1, y, c):
    for x in range(x0, x1 + 1):
        put(g, x, y, c)


def vline(g, x, y0, y1, c):
    for y in range(y0, y1 + 1):
        put(g, x, y, c)


def disc(g, cx, cy, r, c):
    r2 = r * r
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            if dx * dx + dy * dy <= r2:
                g[y][x] = c


def ring(g, cx, cy, ri, ro, c):
    ri2 = ri * ri
    ro2 = ro * ro
    for y in range(SIZE):
        for x in range(SIZE):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            d2 = dx * dx + dy * dy
            if ri2 <= d2 <= ro2:
                g[y][x] = c


def diamond(g, cx, cy, r, c):
    for y in range(SIZE):
        for x in range(SIZE):
            if abs(x + 0.5 - cx) + abs(y + 0.5 - cy) <= r:
                g[y][x] = c


def shade_lower(g, color, new_color, y_from):
    for y in range(y_from, SIZE):
        for x in range(SIZE):
            if g[y][x] == color:
                g[y][x] = new_color


# ---------------------------------------------------------------- sprites
def draw_spellbook(p):
    """Book cover with gold frame, spine, page block and a glowing rune orb."""
    g = blank()
    rect(g, 3, 4, 26, 24, p["out"])          # dark outline
    rect(g, 4, 5, 24, 22, p["gold"])         # gold frame
    rect(g, 5, 6, 20, 20, p["base"])         # cover
    rect(g, 25, 7, 3, 18, p["parch"])        # page block on the right
    vline(g, 27, 7, 24, p["parchd"])
    rect(g, 5, 6, 3, 20, p["dark"])          # spine
    for y in (9, 15, 21):                    # gold bands on the spine
        hline(g, 5, 7, y, p["gold"])
        hline(g, 5, 7, y + 1, p["goldd"])
    hline(g, 5, 24, 6, p["light"])           # top bevel
    hline(g, 5, 24, 24, p["dark"])           # bottom shade
    ring(g, 15.5, 16, 5.0, 6.1, p["out"])    # rune orb
    disc(g, 15.5, 16, 5.0, p["light"])
    disc(g, 15.5, 16, 2.8, p["glow"])
    for sx, sy in ((6, 9), (21, 9), (6, 20), (21, 20)):   # corner studs
        rect(g, sx, sy, 2, 2, p["gold"])
        put(g, sx + 1, sy + 1, p["goldd"])
    rect(g, 26, 15, 3, 5, p["gold"])         # clasp
    vline(g, 27, 16, 19, p["goldd"])
    return g


def draw_staff(p):
    """Diagonal wooden shaft, gold bindings, ferrule and a crystal orb."""
    g = blank()
    pts = []
    for i in range(18):
        pts.append((7 + i, 27 - i))          # (7,27) -> (24,10)
    for (x, y) in pts:
        put(g, x, y, p["woodl"])
        put(g, x + 1, y, p["wood"])
        put(g, x + 1, y + 1, p["woodd"])
    for i in (4, 9, 14):                     # gold bindings on the shaft
        x, y = pts[i]
        put(g, x, y, p["gold"])
        put(g, x + 1, y, p["gold"])
        put(g, x + 1, y + 1, p["goldd"])
    rect(g, 6, 26, 4, 3, p["gold"])          # ferrule on the bottom
    hline(g, 6, 9, 29, p["goldd"])
    rect(g, 22, 9, 5, 2, p["gold"])          # collar under the orb
    hline(g, 22, 26, 10, p["goldd"])
    ring(g, 24.5, 6, 4.0, 5.0, p["out"])     # crystal orb
    disc(g, 24.5, 6, 4.0, p["light"])
    disc(g, 24.5, 6, 2.0, p["glow"])
    return g


def draw_amulet(p):
    """Dotted gold chain, bail, gold setting and a faceted gem."""
    g = blank()
    steps = 14
    for k in range(steps):
        t = k / float(steps - 1)
        c = p["goldd"] if k % 2 else p["gold"]
        put(g, 7 + t * 9, 3 + t * 11, c)
        put(g, 25 - t * 9, 3 + t * 11, c)
    rect(g, 15, 13, 3, 4, p["gold"])         # bail
    put(g, 16, 14, p["goldd"])
    put(g, 16, 15, p["goldd"])
    ring(g, 16, 21, 3.6, 5.5, p["gold"])     # gold setting
    shade_lower(g, p["gold"], p["goldd"], 22)
    diamond(g, 16, 20, 4.4, p["out"])        # gem
    diamond(g, 16, 20, 3.4, p["light"])
    diamond(g, 16, 20, 1.6, p["glow"])
    put(g, 15, 20, p["glow"])
    return g


def draw_ring(p):
    """Gold band with rim highlights, setting and a large faceted gem."""
    g = blank()
    ring(g, 16, 22, 4.4, 6.4, p["gold"])
    shade_lower(g, p["gold"], p["goldd"], 23)
    put(g, 11, 18, p["glow"])                # rim highlights
    put(g, 12, 16, p["glow"])
    put(g, 10, 20, p["glow"])
    rect(g, 13, 15, 7, 3, p["gold"])         # setting
    hline(g, 13, 19, 16, p["goldd"])
    diamond(g, 16, 10, 5.2, p["out"])        # gem
    diamond(g, 16, 10, 4.0, p["light"])
    diamond(g, 16, 10, 1.8, p["glow"])
    return g


KINDS = [
    ("spellbook", draw_spellbook),
    ("staff", draw_staff),
    ("amulet", draw_amulet),
    ("ring", draw_ring),
]


# --------------------------------------------------------------- png write
def write_png(path, grid):
    raw = bytearray()
    for y in range(SIZE):
        raw.append(0)                        # filter type: none
        for x in range(SIZE):
            raw += bytes(grid[y][x])

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(str(path), "wb") as f:
        f.write(png)


def write_model(path, item_id):
    data = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "elementalmagic:item/" + item_id},
    }
    with open(str(path), "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


# ------------------------------------------------------------ path search
def find_item_dir(explicit=None):
    if explicit:
        return Path(explicit).expanduser().resolve()
    rel = Path("src") / "main" / "resources" / "assets" / "elementalmagic" / "textures" / "item"
    starts = [Path(os.getcwd()).resolve()]
    try:
        starts.append(Path(os.path.abspath(sys.argv[0])).resolve().parent)
    except Exception:
        pass
    seen = set()
    for start in starts:
        chain = [start]
        chain.extend(list(start.parents)[:7])
        for base in chain:
            if base in seen:
                continue
            seen.add(base)
            if (base / rel).is_dir():
                return base / rel
            if (base / rel.parent).is_dir():
                return base / rel
    return starts[0] / rel


def main():
    ap = argparse.ArgumentParser(description="32x32 item textures for elemental-magic")
    ap.add_argument("--out", help="texture output folder (default: auto-detect)")
    ap.add_argument("--overwrite", action="store_true", help="replace existing files")
    ap.add_argument("--only", nargs="+", help="only these element ids, e.g. --only fire ice")
    ap.add_argument("--no-models", action="store_true", help="do not create item models")
    ap.add_argument("--check", action="store_true", help="only print detected paths")
    args = ap.parse_args()

    out = find_item_dir(args.out)
    print("python : " + sys.version.split()[0])
    print("script : " + os.path.abspath(sys.argv[0]))
    print("target : " + str(out))
    if args.check:
        print("exists : " + str(out.is_dir()))
        return

    out.mkdir(parents=True, exist_ok=True)

    ids = []
    for e in ELEMENTS:
        ids.append(e[0])
    if args.only:
        unknown = [n for n in args.only if n not in ids]
        if unknown:
            print("unknown element ids: " + ", ".join(unknown))
            print("known ids: " + ", ".join(ids))

    elements = []
    for e in ELEMENTS:
        if not args.only or e[0] in args.only:
            elements.append(e)
    if not elements:
        print("nothing to do")
        return

    models_dir = out.parent.parent / "models" / "item"
    if not args.no_models:
        models_dir.mkdir(parents=True, exist_ok=True)

    written = 0
    skipped = 0
    models = 0
    failed = 0

    for eid, color in elements:
        p = palette(color)
        for suffix, draw in KINDS:
            item_id = eid + suffix
            path = out / (item_id + ".png")
            if path.exists() and not args.overwrite:
                skipped += 1
                print("skip    " + path.name)
            else:
                write_png(path, draw(p))
                if path.exists() and path.stat().st_size > 100:
                    written += 1
                    print("write   " + path.name)
                else:
                    failed += 1
                    print("FAILED  " + path.name)
            if not args.no_models:
                mpath = models_dir / (item_id + ".json")
                if not mpath.exists() or args.overwrite:
                    write_model(mpath, item_id)
                    models += 1

    pngs = list(out.glob("*.png"))
    print("")
    print("written: %d   skipped: %d   failed: %d" % (written, skipped, failed))
    if not args.no_models:
        print("models written: %d  ->  %s" % (models, models_dir))
    print("PNG in folder now: %d" % len(pngs))
    print("folder: " + str(out))
    if written == 0 and skipped > 0:
        print("")
        print("Nothing was created because the files already exist.")
        print("Run again with --overwrite to replace them.")


if __name__ == "__main__":
    main()
