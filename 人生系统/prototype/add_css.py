# -*- coding: utf-8 -*-
SRC = r"D:\javaweb\个人博客\人生系统\prototype\home-healing-male.html"
EXTRA = """
      .illu-3 { right: 8px; bottom: 10px; width: 80px; opacity: .85; }
      .task-row-3 { min-height: 36px; }
      .habit-action-3 { min-width: 46px; height: 24px; font-size: 11px; }
      .icon-chip-3 { width: 26px; height: 26px; }
      .quick-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: linear-gradient(135deg, #fff0de, #e2e7f2); border: 1px solid transparent; border-radius: 10px; font-size: 12px; font-weight: 700; color: #141d33; transition: transform .15s, border-color .15s; text-align: left; }
      .quick-btn:hover { transform: translateY(-1px); border-color: #ff9f45; }
      .quick-btn i { color: #ff9f45; }
      .ai-link { display: flex; align-items: center; gap: 8px; width: 100%; padding: 7px 10px; background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.1); border-radius: 8px; color: rgba(255,255,255,.85); font-size: 12px; font-weight: 600; transition: background .15s; text-align: left; }
      .ai-link:hover { background: rgba(255,255,255,.12); }
      .ai-link i { color: #ff9f45; width: 14px; }
      .asset-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: #f7f9fc; border: 1px solid #dfe4ec; border-radius: 10px; font-size: 12px; font-weight: 700; color: #141d33; transition: transform .15s, border-color .15s; text-align: left; }
      .asset-btn:hover { transform: translateY(-1px); border-color: #1e2a4a; }
      .asset-btn i { color: #1e2a4a; }
      .asset-btn span:first-of-type { flex: 1; }
"""
with open(SRC, "r", encoding="utf-8") as f:
    lines = f.read().splitlines()
for i, l in enumerate(lines):
    if ".illu-2 {" in l:
        # 在此行后插入 EXTRA
        extra_lines = EXTRA.lstrip("\n").splitlines()
        result = lines[:i+1] + extra_lines + lines[i+1:]
        with open(SRC, "w", encoding="utf-8") as f:
            f.write("\n".join(result))
        print(f"OK 在第 {i+1} 行后插入 {len(extra_lines)} 行 CSS")
        break
else:
    print("未找到 .illu-2 {")
