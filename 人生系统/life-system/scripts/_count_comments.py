import os

def count_file(path):
    ext = os.path.splitext(path)[1].lower()
    if ext in ('.ts', '.tsx', '.js', '.jsx', '.vue'):
        with open(path, encoding='utf-8') as f:
            lines = f.readlines()
        total = len(lines)
        comments = 0
        in_block = False
        for raw in lines:
            line = raw.strip()
            if in_block:
                comments += 1
                if '*/' in line:
                    in_block = False
                continue
            if line.startswith('//') or line.startswith('*') or line.startswith('/*') or line.startswith('<!--'):
                comments += 1
                if '/*' in line and '*/' not in line:
                    in_block = True
                continue
            if '/*' in line and '*/' not in line:
                comments += 1
                in_block = True
                continue
        return total, comments
    elif ext == '.sql':
        with open(path, encoding='utf-8') as f:
            lines = f.readlines()
        total = len(lines)
        comments = sum(1 for l in lines if l.strip().startswith('--'))
        return total, comments
    return 0, 0

results = []
for root, dirs, files in os.walk('src'):
    if 'node_modules' in root:
        continue
    for f in files:
        p = os.path.join(root, f)
        t, c = count_file(p)
        if t:
            results.append((p, t, c, round(c/max(t, 1)*100, 1)))

results.sort(key=lambda x: -x[3])
grand_t = sum(x[1] for x in results)
grand_c = sum(x[2] for x in results)
print(f"{'文件':<52} {'总行':>6} {'注释行':>7} {'占比':>6}")
for p, t, c, r in results:
    flag = "  ⚠零注释" if c == 0 else ("  ⚠<10%" if r < 10 else "")
    print(f"{p:<52} {t:>6} {c:>7} {r:>5.1f}%{flag}")
print(f"\n合计: {grand_t} 行, 注释 {grand_c} 行, 占比 {round(grand_c/max(grand_t,1)*100,1)}%")
for root, dirs, files in os.walk('migrations'):
    for f in files:
        p = os.path.join(root, f)
        t, c = count_file(p)
        if t:
            print(f"SQL {p}: {t} 行, 注释 {c} 行, 占比 {round(c/max(t,1)*100,1)}%")