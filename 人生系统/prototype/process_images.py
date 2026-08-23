# -*- coding: utf-8 -*-
"""人生系统原型插画处理：去背景(flood-fill) + 去水印带"""
import os
from collections import deque
from PIL import Image

SRC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images")
DST_DIR = os.path.join(SRC_DIR, "processed")
FILES = ["desk.png", "notebook.png", "bookshelf.png", "mountain.png"]
TOL = 46          # 背景色差容差
BAND_W = 0.55     # 水印带：右下角宽占比
BAND_H = 0.15     # 水印带：底部高占比
LIGHT = 135       # 水印浅色阈值（RGB 均值）

def process(src_path, dst_path):
    img = Image.open(src_path).convert("RGBA")
    w, h = img.size
    px = img.load()

    # 背景参考色 = 四角平均
    corners = [px[3, 3], px[w - 4, 3], px[3, h - 4], px[w - 4, h - 4]]
    bg = tuple(sum(c[i] for c in corners) // 4 for i in range(3))

    # 1) flood-fill 从边缘去背景 -> alpha 0
    visited = [[False] * w for _ in range(h)]
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if not visited[y][x]:
                visited[y][x] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if not visited[y][x]:
                visited[y][x] = True
                q.append((x, y))

    while q:
        x, y = q.popleft()
        r, g, b, a = px[x, y]
        if a == 0:
            continue
        if abs(r - bg[0]) <= TOL and abs(g - bg[1]) <= TOL and abs(b - bg[2]) <= TOL:
            px[x, y] = (r, g, b, 0)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                    visited[ny][nx] = True
                    q.append((nx, ny))

    # 2) 水印带：右下角浅色像素置透明
    band_w = int(w * BAND_W)
    band_h = int(h * BAND_H)
    for y in range(h - band_h, h):
        for x in range(w - band_w, w):
            r, g, b, a = px[x, y]
            if a > 0 and (r + g + b) / 3 > LIGHT:
                px[x, y] = (r, g, b, 0)

    os.makedirs(DST_DIR, exist_ok=True)
    img.save(dst_path)
    print(f"OK {os.path.basename(src_path)} -> {dst_path}  bg={bg}")

if __name__ == "__main__":
    for f in FILES:
        src = os.path.join(SRC_DIR, f)
        if os.path.exists(src):
            process(src, os.path.join(DST_DIR, f))
        else:
            print(f"SKIP {f} not found")
