#!/usr/bin/env python3
"""Generates minimal valid blue PNG launcher icons for all Android densities."""
import struct
import zlib
import os

SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

# Navy blue color (R=26, G=38, B=136)
COLOR = (26, 38, 136)


def make_png(width: int, height: int, rgb: tuple) -> bytes:
    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    # Build raw image data (RGB, 8-bit, no alpha)
    raw = b""
    row = b"\x00" + bytes(rgb) * width   # filter byte 0 + pixels
    raw = row * height

    signature = b"\x89PNG\r\n\x1a\n"
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    ihdr = chunk(b"IHDR", ihdr_data)
    idat = chunk(b"IDAT", zlib.compress(raw))
    iend = chunk(b"IEND", b"")
    return signature + ihdr + idat + iend


def main():
    base = os.path.join("app", "src", "main", "res")
    for folder, size in SIZES.items():
        out_dir = os.path.join(base, folder)
        os.makedirs(out_dir, exist_ok=True)
        png_data = make_png(size, size, COLOR)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            path = os.path.join(out_dir, name)
            with open(path, "wb") as f:
                f.write(png_data)
            print(f"  Created {path} ({size}x{size})")
    print("Icons generated successfully.")


if __name__ == "__main__":
    main()
