#!/usr/bin/env python3
"""Verify ic_stat_fash.png meets Android notification icon requirements."""
from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
STAT_PATHS = [
    REPO_ROOT / "app/src/main/res/drawable-nodpi/ic_stat_fash.png",
    REPO_ROOT / "app/src/main/res/drawable-mdpi/ic_stat_fash.png",
    REPO_ROOT / "app/src/main/res/drawable-hdpi/ic_stat_fash.png",
    REPO_ROOT / "app/src/main/res/drawable-xhdpi/ic_stat_fash.png",
    REPO_ROOT / "app/src/main/res/drawable-xxhdpi/ic_stat_fash.png",
    REPO_ROOT / "app/src/main/res/drawable-xxxhdpi/ic_stat_fash.png",
]


def read_png_rgba(path: Path) -> tuple[int, int, list[tuple[int, int, int, int]]]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: not a PNG")

    pos = 8
    width = height = 0
    raw = b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        pos += 4
        chunk_type = data[pos : pos + 4]
        pos += 4
        chunk = data[pos : pos + length]
        pos += length + 4  # skip CRC

        if chunk_type == b"IHDR":
            width, height = struct.unpack(">II", chunk[:8])
        elif chunk_type == b"IDAT":
            raw += chunk
        elif chunk_type == b"IEND":
            break

    if not raw:
        raise ValueError(f"{path}: missing IDAT")

    inflated = zlib.decompress(raw)
    stride = width * 4 + 1
    pixels: list[tuple[int, int, int, int]] = []
    prev = [0] * (width * 4)
    i = 0
    for _y in range(height):
        filter_type = inflated[i]
        i += 1
        row = list(inflated[i : i + width * 4])
        i += width * 4
        if filter_type == 1:  # Sub
            for x in range(4, len(row)):
                row[x] = (row[x] + row[x - 4]) & 0xFF
        elif filter_type == 2:  # Up
            for x in range(len(row)):
                row[x] = (row[x] + prev[x]) & 0xFF
        elif filter_type == 3:  # Average
            for x in range(len(row)):
                left = row[x - 4] if x >= 4 else 0
                up = prev[x]
                row[x] = (row[x] + ((left + up) // 2)) & 0xFF
        elif filter_type == 4:  # Paeth
            for x in range(len(row)):
                left = row[x - 4] if x >= 4 else 0
                up = prev[x]
                up_left = prev[x - 4] if x >= 4 else 0
                p = left + up - up_left
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - up_left)
                pred = left if pa <= pb and pa <= pc else up if pb <= pc else up_left
                row[x] = (row[x] + pred) & 0xFF
        prev = row
        for x in range(0, len(row), 4):
            pixels.append((row[x], row[x + 1], row[x + 2], row[x + 3]))
    return width, height, pixels


def verify_stat_icon(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(f"Missing {path}")

    width, height, pixels = read_png_rgba(path)
    corners = [
        (0, 0),
        (width - 1, 0),
        (0, height - 1),
        (width - 1, height - 1),
    ]
    for x, y in corners:
        _, _, _, a = pixels[y * width + x]
        if a > 32:
            raise ValueError(f"{path}: corner ({x},{y}) must be transparent (A={a})")

    transparent = white = other = 0
    for r, g, b, a in pixels:
        if a < 16:
            transparent += 1
        elif r >= 235 and g >= 235 and b >= 235:
            white += 1
        else:
            other += 1

    if other > 0:
        raise ValueError(f"{path}: found {other} non-white opaque pixels")
    if white < 20:
        raise ValueError(f"{path}: too few white pixels ({white})")

    print(f"OK {path.name} ({width}x{height}) transparent={transparent} white={white}")


def main() -> int:
    try:
        for path in STAT_PATHS:
            verify_stat_icon(path)
    except (OSError, ValueError, FileNotFoundError, zlib.error) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    print("All notification icon assets passed checks.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
