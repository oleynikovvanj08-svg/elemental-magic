#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_textures.py - 32x32 pixel-art item textures for the elemental-magic mod.

Generates 4 textures per element (20 elements -> 80 PNG files, 32x32 RGBA):
    <id>spellbook.png   (registry: <id>spellbook)
    <id>staff.png       (registry: <id>staff)
    <id>amulet.png      (registry: <id>amulet)
    <id>ring.png        (registry: <id>ring)

WHERE TO PUT THIS FILE
    <repo root>/generate_textures.py      (next to build.gradle and gradlew)
    Do NOT put it inside src/main/resources - it would be packed into the jar
    as a resource. From the repo root it finds the texture folder by itself.

WHERE THE PNGs GO
    <repo root>/src/main/resources/assets/elementalmagic/textures/item/

USAGE
    python generate_textures.py                  # only missing files, never overwrites
    python generate_textures.py --overwrite      # replace existing files too
    python generate_textures.py --only fire ice  # just a few elements
    python generate_textures.py --out some/dir   # custom output folder

No third-party libraries - the PNG is written with zlib + struct only.
Armor is NOT handled here: armor needs layer textures (<id>_layer_1.png / _layer_2.png
in textures/models/armor), which is a different format.
"""

import argparse
import math
import struct
import zlib
from pathlib import Path

SIZE = 32
CLEAR = (0, 0, 0, 0)

# id + base color, copied from Element.java (keep in sync if you add new schools)
ELEMENTS = [
    ("lightning", 0x7FDBFF), ("light", 0xFFF3A0), ("darkness", 0x8A2BE2),
    ("water", 0x1E90FF), ("earth", 0x8B5A2B), ("air", 0xC8F7F7),
    ("fire", 0xFF4500), ("nature", 0x3CB043), ("ash", 0x9E9E9E),
    ("explosion", 0xFF8C00), ("elemental", 0x40E0D0), ("ice", 0xAFEEEE),
    ("gravity", 0x6A5ACD), ("poison", 0x7CFC00), ("summoning", 0xDA70D6),
    ("time", 0xD4AF37), ("stars", 0xB0C4FF), ("darknature", 0x556B2F),
    ("shadow", 0x4B4B6E), ("lava", 0xFF6A00),
]

# shared materials (same for every element - leather, metal, parchment, wood)
GOLD = (233, 199, 101)
GOLD_D = (150, 112, 38)
PARCH = (241, 226, 192)
PARCH_D = (198, 172, 128)
WOOD = (122, 86, 54)
WOOD_D = (78, 54, 32)
WOOD_L = (160, 118, 78)


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def mix(a, b, t):
    return tuple(int(math.floor(a[i] + (b[i] - a[i]) * t + 0.5)) for i in range(3))


def rgba(c):
    return (c[0], c[1], c[2], 255)


def palette(base):
    b = rgb(base)
    return {
        "k": rgba(mix(b, (0, 0, 0), 0.78)),          # outline
        "d": rgba(mix(b, (0, 0, 0), 0.42)),          # shadow of the element color
        "m": rgba(b),                                # base element color
        "l": rgba(mix(b, (255, 255, 255), 0.38)),    # light
        "w": rgba(mix(b, (255, 255, 255), 0.80)),    # highlight / glow core
        "G": rgba(GOLD), "g": rgba(GOLD_D),          # gold, gold shadow
        "P": rgba(PARCH), "p": rgba(PARCH_D),        # parchment
        "wo": rgba(WOOD), "wd": rgba(WOOD_D), "wl": rgba(WOOD_L),  # wood
    }


class Canvas:
    def __init__(self, size=SIZE):
        self.s = size
        self.px = [[CLEAR] * size for _ in range(size)]

    def set(self, x, y, c):
        xi = int(math.floor(x + 0.5))
        yi = int(math.floor(y + 0.5))
        if 0 <= xi < self.s and 0 <= yi < self.s and c is not None:
            self.px[yi][xi] = c

    def rect(self, x, y, w, h, c):
        for j in range(y, y + h):
            for i in range(x, x + w):
                self.set(i, j, c)

    def hline(self, x0, x1, y, c):
        for x in range(x0, x1 + 1):
            self.set(x, y, c)

    def vline(self, x, y0, y1, c):
        for y in range(y0, y1 + 1):
            self.set(x, y, c)

    def disc(self, cx, cy, r, c):
        for y in range(self.s):
            for x in range(self.s):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                if dx * dx + dy * dy <= r * r:
                    self.px[y][x] = c

    def ring(self, cx, cy, ri, ro, c):
        for y in range(self.s):
            for x in range(self.s):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                d2 = dx * dx + dy * dy
                if ri * ri <= d2 <= ro * ro:
                    self.px[y][x] = c

    def diamond(self, cx, cy, r, c):
        for y in range(self.s):
            for x in range(self.s):
                if abs(x + 0.5 - cx) + abs(y + 0.5 - cy) <= r:
                    self.px[y][x] = c


# ---------------------------------------------------------------- spellbook
def draw_spellbook(p):
    c = Canvas()
    c.rect(4, 5, 24, 23, p["k"])        # dark outline of the whole book
    c.rect(5, 6, 22, 21, p["G"])        # gold frame
    c.rect(6, 7, 18, 18, p["m"])        # cover
    c.rect(24, 8, 2, 16, p["P"])        # page block on the right
    c.vline(25, 8, 23, p["p"])
    c.rect(6, 7, 3, 18, p["d"])         # spine
    for y in (10, 16, 22):              # gold bands on the spine
        c.hline(6, 8, y, p["G"])
        c.hline(6, 8, y + 1, p["g"])
    c.hline(6, 23, 7, p["l"])           # top bevel
    c.hline(6, 23, 24, p["d"])          # bottom shade
    # glowing rune orb on the cover
    c.ring(15.5, 15.5, 4.6, 5.6, p["k"])
    c.disc(15.5, 15.5, 4.6, p["l"])
    c.disc(15.5, 15.5, 2.6, p["w"])
    # corner studs
    for sx, sy in ((7, 9), (22, 9), (7, 21), (22, 21)):
        c.rect(sx, sy, 2, 2, p["G"])
        c.set(sx + 1, sy + 1, p["g"])
    # clasp
    c.rect(25, 14, 2, 4, p["G"])
    c.set(26, 15, p["g"])
    c.set(26, 16, p["g"])
    return c


# -------------------------------------------------------------------- staff
def draw_staff(p):
    c = Canvas()
    pts = [(8 + i, 26 - i) for i in range(16)]   # diagonal shaft (8,26) -> (23,11)
    for x, y in pts:
        c.set(x, y, p["wl"])
        c.set(x + 1, y, p["wo"])
        c.set(x + 1, y + 1, p["wd"])
    for i in (3, 8, 13):                          # gold bindings
        x, y = pts[i]
        c.set(x, y, p["G"])
        c.set(x + 1, y, p["G"])
        c.set(x + 1, y + 1, p["g"])
    c.hline(7, 9, 27, p["G"])                     # ferrule on the bottom
    c.rect(22, 9, 4, 2, p["G"])                   # collar under the orb
    c.set(23, 10, p["g"])
    c.set(24, 10, p["g"])
    # crystal orb
    c.ring(24, 6.5, 3.6, 4.6, p["k"])
    c.disc(24, 6.5, 3.6, p["l"])
    c.disc(24, 6.5, 1.8, p["w"])
    return c


# ------------------------------------------------------------------- amulet
def draw_amulet(p):
    c = Canvas()
    for k in range(12):                           # chain, two dotted diagonals
        t = k / 11.0
        c.set(6 + t * 9, 4 + t * 10, p["g"] if k % 2 else p["G"])
        c.set(26 - t * 9, 4 + t * 10, p["g"] if k % 2 else p["G"])
    c.rect(15, 13, 2, 3, p["G"])                  # bail
    c.set(16, 14, p["g"])
    c.ring(15.5, 21, 3.4, 5.2, p["G"])            # gold setting
    for y in range(22, SIZE):                     # shade the lower half
        for x in range(9, 23):
            if c.px[y][x] == p["G"]:
                c.px[y][x] = p["g"]
    c.diamond(15.5, 20, 3.2, p["l"])              # gem
    c.diamond(15.5, 20, 1.4, p["w"])
    c.set(14, 18, p["w"])
    return c


# --------------------------------------------------------------------- ring
def draw_ring(p):
    c = Canvas()
    c.ring(15.5, 21, 4, 6, p["G"])                # band
    for y in range(22, SIZE):
        for x in range(9, 23):
            if c.px[y][x] == p["G"]:
                c.px[y][x] = p["g"]
    c.set(11, 17, p["w"])                         # rim highlights
    c.set(12, 16, p["w"])
    c.set(10, 19, p["w"])
    c.rect(13, 14, 6, 3, p["G"])                  # setting
    c.hline(13, 18, 16, p["g"])
    c.diamond(15.5, 11, 4.6, p["k"])              # gem
    c.diamond(15.5, 11, 3.6, p["l"])
    c.diamond(15.5, 11, 1.6, p["w"])
    return c


# ---------------------------------------------------------------- png write
def write_png(path, grid, w=SIZE, h=SIZE):
    raw = bytearray()
    for y in range(h):
        raw.append(0)                              # filter: none
        for x in range(w):
            raw += bytes(grid[y][x])

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def default_out():
    here = Path(__file__).resolve().parent
    rel = Path("src/main/resources/assets/elementalmagic/textures/item")
    for base in (here, here.parent, here.parent.parent):
        if (base / rel).is_dir():
            return base / rel
    return here / rel


def main():
    ap = argparse.ArgumentParser(description="32x32 item textures for elemental-magic")
    ap.add_argument("--out", help="output folder (default: auto-detect textures/item)")
    ap.add_argument("--overwrite", action="store_true", help="replace existing files")
    ap.add_argument("--only", nargs="+", help="only these element ids, e.g. --only fire water")
    args = ap.parse_args()

    out = Path(args.out) if args.out else default_out()
    out.mkdir(parents=True, exist_ok=True)

    kinds = [("spellbook", draw_spellbook), ("staff", draw_staff),
             ("amulet", draw_amulet), ("ring", draw_ring)]
    elements = [e for e in ELEMENTS if not args.only or e[0] in args.only]

    written = skipped = 0
    for eid, color in elements:
        p = palette(color)
        for suffix, fn in kinds:
            path = out / ("%s%s.png" % (eid, suffix))
            if path.exists() and not args.overwrite:
                skipped += 1
                print("skip   %s" % path.name)
                continue
            write_png(path, fn(p).px)
            written += 1
            print("write  %s" % path.name)

    print("")
    print("done: %d written, %d skipped (--overwrite to replace)" % (written, skipped))
    print("folder: %s" % out)


if __name__ == "__main__":
    main()
