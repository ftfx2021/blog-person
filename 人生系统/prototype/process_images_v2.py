# -*- coding: utf-8 -*-
"""插画处理 v2：两轮 flood-fill（适应渐变背景）+ 水印带清理"""
import os
from collections import deque
from PIL import Image

SRC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "images")
DST_DIR = os.path.join(SRC_DIR, "processed")
FILES = ["desk.png", "notebook.png", "bookshelf.png", "mountain.png"]
TOL1 = 58          # 第一轮容差
TOL2 = 96          # 第二轮容差（渐变边缘过渡带）
BAND_W = 0.55
BAND_H = 0.15
LIGHT = 135

def process(src_path, dst_path):
    img = Image.open(src_path).convert("RGBA")
    w, h = img.size
    px = img.load()

    corners = [px[3, 3], px[w - 4, 3], px[3, h - 4], px[w - 4, h - 4]]
    bg = tuple(sum(c[i] for c in corners) // 4 for i in range(3))

    def run_fill(tol):
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
            if abs(r - bg[0]) <= tol and abs(g - bg[1]) <= tol and abs(b - bg[2]) <= tol:
                px[x, y] = (r, g, b, 0)
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and not visited[ny][nx]:
                        visited[ny][nx] = True
                        q.append((nx, ny))

    run_fill(TOL1)
    run_fill(TOL2)

    band_w = int(w * BAND_W)
    band_h = int(h * BAND_H)
    for y in range(h - band_h, h):
        for x in range(w - band_w, w):
            r, g, b, a = px[x, y]
            if a > 0 and (r + g + b) / 3 > LIGHT:
                px[x, y] = (r, g, b, 0)

    os.makedirs(DST_DIR, exist_ok=True)
    img.save(dst_path)
    # 验证统计
    total = w * h
    tr = sum(1 for y in range(0, h, 4) for x in range(0, w, 4) if px[x, y][3] == 0)
    band = sum(1 for y in range(int(h * 0.85), h, 2) for x in range(int(w * 0.45), w, 2)
               if px[x, y][3] > 0 and sum(px[x, y][:3]) / 3 > 135)
    print(f"{os.path.basename(src_path)}: 透明~{tr / (total / 16) * 100:.0f}% 右下浅色残留~{band}px bg={bg}")

if __name__ == "__main__":
    for f in FILES:
        src = os.path.join(SRC_DIR, f)
        if os.path.exists(src):
            process(src, os.path.join(DST_DIR, f))
        else:
            print(f"SKIP {f}")
