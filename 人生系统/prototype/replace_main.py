# -*- coding: utf-8 -*-
import os
SRC = r"D:\javaweb\个人博客\人生系统\prototype\home-healing-male.html"
NEW = r"D:\javaweb\个人博客\人生系统\prototype\main_new.html"
with open(SRC, "r", encoding="utf-8") as f:
    lines = f.read().splitlines()
with open(NEW, "r", encoding="utf-8") as f:
    new_lines = f.read().splitlines()
# 1-indexed 241..331 inclusive (the grid block + closing </main>)
# → 0-indexed slice lines[240:331]
result = lines[:240] + new_lines + lines[331:]
with open(SRC, "w", encoding="utf-8") as f:
    f.write("\n".join(result))
print(f"OK 替换完成,原 {len(lines)} 行 → 新 {len(result)} 行")
