# -*- coding: utf-8 -*-
"""批量生成人生系统高保真原型页面（复用 home-healing-male.html 框架）"""
import os, re

BASE = r"D:\javaweb\个人博客\人生系统\prototype\home-healing-male.html"
OUT = r"D:\javaweb\个人博客\人生系统\prototype"

with open(BASE, "r", encoding="utf-8") as f:
    tpl = f.read()

# 切出 main 块（<main ...> ... </main>）
m = re.search(r'<main class="content".*?</main>', tpl, re.S)
shell_head = tpl[:m.start()]
shell_tail = tpl[m.end():]

# 尾部 JS 空保护（其他页无 hero-anim / new-task）
shell_tail = shell_tail.replace(
    'if (window.lottie) {',
    'const heroAnimEl = document.getElementById("hero-anim");\n      if (window.lottie && heroAnimEl) {'
).replace(
    'container: document.getElementById("hero-anim"),',
    'container: heroAnimEl,'
).replace(
    'document.getElementById("new-task").addEventListener',
    '(document.getElementById("new-task") || {addEventListener(){}}).addEventListener'
)

EXTRA_CSS = """
      /* ===== 页面扩展样式（全部页面共用） ===== */
      .page-title { font-size: 22px; font-weight: 800; letter-spacing: -0.01em; color: #141d33; }
      .filter-pill { padding: 5px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; color: #5e6b85; background: #f7f9fc; border: 1px solid #dfe4ec; transition: all .15s; }
      .filter-pill.active { color: #ff9f45; background: #fff0de; border-color: #ff9f45; }
      .goal-card { padding: 16px; border: 1px solid #dfe4ec; border-radius: 14px; background: #fff; box-shadow: 0 2px 10px rgba(60,80,110,.05); transition: transform .15s, box-shadow .15s; }
      .goal-card:hover { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(60,80,110,.10); }
      .doc-row { display: grid; grid-template-columns: 26px minmax(0,1fr) auto; align-items: center; gap: 10px; padding: 11px 4px; border-top: 1px solid #e6eaf2; }
      .doc-row:first-of-type { border-top: 0; }
      .doc-icon { width: 26px; height: 26px; border-radius: 8px; display: grid; place-items: center; font-size: 12px; }
      .entry-row { display: flex; gap: 12px; padding: 12px 0; border-top: 1px solid #e6eaf2; }
      .entry-row:first-of-type { border-top: 0; }
      .mood-item { display: flex; align-items: center; gap: 12px; padding: 11px 4px; border-top: 1px solid #e6eaf2; }
      .mood-item:first-of-type { border-top: 0; }
      .intensity { display: flex; gap: 2px; }
      .intensity i { font-size: 9px; color: #d6dce6; }
      .intensity i.on { color: #ff9f45; }
      .kanban-col { background: #f2f4f8; border-radius: 12px; padding: 10px; min-width: 0; }
      .kanban-card { background: #fff; border: 1px solid #dfe4ec; border-radius: 10px; padding: 11px; margin-top: 9px; cursor: grab; transition: box-shadow .15s; }
      .kanban-card:hover { box-shadow: 0 6px 14px rgba(60,80,110,.10); }
      .stat-bar { height: 8px; background: #e6eaf2; border-radius: 6px; overflow: hidden; }
      .stat-bar span { display: block; height: 100%; background: linear-gradient(90deg,#1e2a4a,#ff9f45); }
      .up-btn { border: 1px solid #dfe4ec; border-radius: 9px; background: #fff; color: #5e6b85; font-size: 12px; font-weight: 700; padding: 6px 10px; transition: all .15s; }
      .up-btn:hover { color: #ff9f45; border-color: #ff9f45; }
      .up-btn-solid { border: 0; border-radius: 10px; background: linear-gradient(120deg,#ff9f45,#e0842a); color: #fff; font-weight: 700; padding: 8px 14px; box-shadow: 0 5px 12px rgba(255,159,69,.3); transition: transform .15s; }
      .up-btn-solid:hover { transform: translateY(-1px); }
      .setting-input { width: 100%; padding: 8px 12px; border: 1px solid #dfe4ec; border-radius: 10px; background: #fbfcfe; font-size: 12px; color: #141d33; outline: none; }
      .setting-input:focus { border-color: #ff9f45; box-shadow: 0 0 0 3px #fff0de; }
      .toggle { width: 40px; height: 22px; border-radius: 999px; border: 1px solid #dfe4ec; background: #e6eaf2; position: relative; transition: background .15s; }
      .toggle::after { content: ""; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 50%; background: #fff; box-shadow: 0 1px 3px rgba(0,0,0,.15); transition: left .15s; }
      .toggle.on { background: #ff9f45; border-color: #ff9f45; }
      .toggle.on::after { left: 20px; }
"""

CSS_INS = "</style>"
shell_head = shell_head.replace(CSS_INS, EXTRA_CSS + "\n" + CSS_INS, 1)

def build(fname, title, nav, main_html):
    head = shell_head
    head = head.replace('<title>人生系统 - 今天</title>', f'<title>人生系统 - {title}</title>')
    head = head.replace('class="nav-item active"', 'class="nav-item"')
    head = head.replace(f'class="nav-item" data-nav="{nav}"', f'class="nav-item active" data-nav="{nav}"')
    html = head + main_html + shell_tail
    with open(os.path.join(OUT, fname), "w", encoding="utf-8") as f:
        f.write(html)
    print(f"OK {fname}")

# ============ 1. 目标列表 ============
goals = '''
        <main class="content" aria-label="目标">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">目标 · 规划层</div>
              <h1 class="page-title mt-0.5 mb-1">目标列表</h1>
              <div class="text-[12px] subtle">进度只由真实数据与里程碑驱动，行动不折算进度。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>新建目标</button>
          </div>

          <div class="flex items-center gap-2 mb-4 flex-wrap">
            <button class="filter-pill active">全部</button>
            <button class="filter-pill">年度</button>
            <button class="filter-pill">季度</button>
            <button class="filter-pill">月度</button>
            <span class="w-px h-5 bg-[#dfe4ec] mx-1"></span>
            <button class="filter-pill">进行中</button>
            <button class="filter-pill">已完成</button>
            <button class="filter-pill">已放弃</button>
          </div>

          <div class="grid grid-cols-3 gap-3.5">
            <div class="goal-card">
              <div class="flex items-center gap-2"><span class="badge badge-amber">年度</span><span class="badge badge-primary">数值型</span><span class="text-[10px] text-[#6b7691] ml-auto">标签：健康</span></div>
              <div class="text-[15px] font-extrabold mt-2.5">体重回到 65 kg</div>
              <div class="text-[11px] subtle mt-1">68.6 → 65.0 kg · 下次记录 8/24</div>
              <div class="mini-progress mt-3"><span style="width:68%"></span></div>
              <div class="flex items-center justify-between mt-2.5">
                <span class="text-[12px] font-extrabold text-[#ff9f45]">68%</span>
                <div class="flex gap-2"><button class="up-btn">记录数据</button><button class="up-btn">详情</button></div>
              </div>
            </div>
            <div class="goal-card">
              <div class="flex items-center gap-2"><span class="badge badge-amber">季度</span><span class="badge badge-navy">里程碑型</span><span class="text-[10px] text-[#6b7691] ml-auto">标签：职业</span></div>
              <div class="text-[15px] font-extrabold mt-2.5">表达能力提升</div>
              <div class="text-[11px] subtle mt-1">5 个子项 · 2 已完成</div>
              <div class="flex gap-1.5 mt-3">
                <span class="w-5 h-5 rounded-full bg-[#1e2a4a] text-white grid place-items-center text-[10px]">✓</span>
                <span class="w-5 h-5 rounded-full bg-[#1e2a4a] text-white grid place-items-center text-[10px]">✓</span>
                <span class="w-5 h-5 rounded-full bg-[#e6eaf2] text-[#9aa5bd] grid place-items-center text-[10px]">3</span>
                <span class="w-5 h-5 rounded-full bg-[#e6eaf2] text-[#9aa5bd] grid place-items-center text-[10px]">4</span>
                <span class="w-5 h-5 rounded-full bg-[#e6eaf2] text-[#9aa5bd] grid place-items-center text-[10px]">5</span>
              </div>
              <div class="flex items-center justify-between mt-2.5">
                <span class="text-[12px] font-extrabold text-[#1e2a4a]">2/5</span>
                <div class="flex gap-2"><button class="up-btn">详情</button></div>
              </div>
            </div>
            <div class="goal-card">
              <div class="flex items-center gap-2"><span class="badge badge-amber">季度</span><span class="badge badge-forest">状态型</span><span class="text-[10px] text-[#6b7691] ml-auto">标签：健康</span></div>
              <div class="text-[15px] font-extrabold mt-2.5">改善睡眠</div>
              <div class="text-[11px] subtle mt-1">状态型目标 · 不显示伪进度</div>
              <div class="flex items-center gap-2 mt-3"><span class="badge badge-forest"><i class="fa-solid fa-circle text-[6px] mr-1 align-middle"></i>进行中</span><span class="text-[11px] subtle">上次更新 · 8/14</span></div>
              <div class="flex items-center justify-between mt-2.5">
                <span class="text-[11px] subtle">诚实记录，不硬凑数字</span>
                <div class="flex gap-2"><button class="up-btn">详情</button></div>
              </div>
            </div>
          </div>
        </main>
'''

# ============ 2. 行动页 ============
tasks = '''
        <main class="content" aria-label="行动">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">行动 · 执行层</div>
              <h1 class="page-title mt-0.5 mb-1">行动</h1>
              <div class="text-[12px] subtle">待办按截止时间排序，习惯独立打卡。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>新建待办</button>
          </div>

          <div class="flex items-center gap-2 mb-4 flex-wrap">
            <button class="filter-pill">全部</button>
            <button class="filter-pill active">今天</button>
            <button class="filter-pill">本周</button>
            <button class="filter-pill">无日期</button>
          </div>

          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(300px,.8fr)] gap-3.5">
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-2"><span class="badge badge-primary mr-2">今天</span>3 项待办</div>
                <div class="task-row" data-task="整理本周材料"><button class="check" aria-label="完成整理本周材料"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">整理本周材料</span><span class="text-[11px] text-[#3a4a6b] font-semibold">今天</span></div>
                <div class="task-row" data-task="回复客户邮件"><button class="check" aria-label="完成回复客户邮件"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">回复客户邮件</span><span class="text-[11px] subtle">18:00</span></div>
                <div class="task-row" data-task="预约体检"><button class="check" aria-label="完成预约体检"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">预约体检</span><span class="text-[11px] subtle">已计划</span></div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2"><span class="badge badge-navy mr-2">本周</span>4 项待办</div>
                <div class="task-row" data-task="写周报"><button class="check" aria-label="完成写周报"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">写周报</span><span class="text-[11px] subtle">周五</span></div>
                <div class="task-row" data-task="整理学习笔记"><button class="check" aria-label="完成整理学习笔记"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">整理学习笔记</span><span class="text-[11px] subtle">周六</span></div>
                <div class="task-row" data-task="更新简历"><button class="check" aria-label="完成更新简历"><i class="fa-solid fa-check text-[10px]"></i></button><span class="task-name">更新简历</span><span class="text-[11px] subtle">无日期</span></div>
              </section>
            </div>

            <section class="panel p-4">
              <div class="section-title mb-3">今日习惯</div>
              <div class="space-y-3">
                <div class="flex items-center justify-between p-3 rounded-xl border border-[#dfe4ec]">
                  <div><div class="font-semibold text-[13px]">写作</div><div class="text-[11px] subtle">连续 8 天 · 每日</div></div>
                  <button class="habit-action" data-habit="写作">打卡</button>
                </div>
                <div class="flex items-center justify-between p-3 rounded-xl border border-[#dfe4ec]">
                  <div><div class="font-semibold text-[13px]">拉伸</div><div class="text-[11px] subtle">连续 4 天 · 每日</div></div>
                  <button class="habit-action done" data-habit="拉伸"><i class="fa-solid fa-check mr-1"></i>已打卡</button>
                </div>
                <div class="flex items-center justify-between p-3 rounded-xl border border-[#dfe4ec]">
                  <div><div class="font-semibold text-[13px]">阅读</div><div class="text-[11px] subtle">本周 2/3 次 · 每周</div></div>
                  <button class="habit-action" data-habit="阅读">打卡</button>
                </div>
              </div>
              <button class="text-action text-[11px] mt-4">新建习惯 →</button>
            </section>
          </div>
        </main>
'''

# ============ 3. 知识库 ============
knowledge = '''
        <main class="content" aria-label="知识">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">知识 · 长期语料 + RAG</div>
              <h1 class="page-title mt-0.5 mb-1">知识库</h1>
              <div class="text-[12px] subtle">网页、PDF、Word、Markdown、文章统一入库，参与 RAG 检索。</div>
            </div>
            <div class="flex gap-2">
              <button class="up-btn-solid"><i class="fa-solid fa-upload mr-1"></i>导入</button>
              <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>新建文章</button>
            </div>
          </div>

          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(280px,.7fr)] gap-3.5">
            <section class="panel p-4">
              <div class="flex items-center gap-2 mb-3 flex-wrap">
                <label class="search !w-[240px]"><i class="fa-solid fa-magnifying-glass text-[12px]"></i><input type="text" placeholder="搜索文档内容…" /></label>
                <button class="filter-pill active">全部</button>
                <button class="filter-pill">网页</button>
                <button class="filter-pill">PDF</button>
                <button class="filter-pill">Word</button>
                <button class="filter-pill">Markdown</button>
                <button class="filter-pill">文章</button>
                <button class="filter-pill">技能</button>
                <button class="filter-pill">Prompt</button>
              </div>
              <div>
                <div class="doc-row"><div class="doc-icon bg-[#fff0de] text-[#ff9f45]"><i class="fa-solid fa-globe"></i></div><div><div class="font-semibold text-[13px]">RAG 检索增强生成入门</div><div class="text-[11px] subtle">网页剪藏 · 标签：AI · 来源：zhihu</div></div><span class="text-[11px] text-[#6b7691]">8/18</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#e2e7f2] text-[#1e2a4a]"><i class="fa-solid fa-file-pdf"></i></div><div><div class="font-semibold text-[13px]">MySQL 全文索引实践</div><div class="text-[11px] subtle">PDF 导入 · 标签：数据库</div></div><span class="text-[11px] text-[#6b7691]">8/17</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#e6ebf5] text-[#3a4a6b]"><i class="fa-solid fa-file-word"></i></div><div><div class="font-semibold text-[13px]">项目管理流程规范 v2</div><div class="text-[11px] subtle">Word 导入 · 标签：管理</div></div><span class="text-[11px] text-[#6b7691]">8/16</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#e6eaf2] text-[#141d33]"><i class="fa-brands fa-markdown"></i></div><div><div class="font-semibold text-[13px]">人生系统架构笔记</div><div class="text-[11px] subtle">Markdown · 标签：项目</div></div><span class="text-[11px] text-[#6b7691]">8/15</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#fff0de] text-[#ff9f45]"><i class="fa-solid fa-pen"></i></div><div><div class="font-semibold text-[13px]">复盘方法论：三问模板</div><div class="text-[11px] subtle">自写文章 · 标签：复盘</div></div><span class="text-[11px] text-[#6b7691]">8/14</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#e2e7f2] text-[#1e2a4a]"><i class="fa-solid fa-toolbox"></i></div><div><div class="font-semibold text-[13px]">Vue3 组合式 API 速查</div><div class="text-[11px] subtle">技能资产 · 标签：前端</div></div><span class="text-[11px] text-[#6b7691]">8/13</span></div>
                <div class="doc-row"><div class="doc-icon bg-[#e6ebf5] text-[#3a4a6b]"><i class="fa-solid fa-keyboard"></i></div><div><div class="font-semibold text-[13px]">写周报的 Prompt</div><div class="text-[11px] subtle">Prompt 模板 · 标签：写作</div></div><span class="text-[11px] text-[#6b7691]">8/12</span></div>
              </div>
            </section>

            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-3">知识库统计</div>
                <div class="grid grid-cols-2 gap-3">
                  <div class="p-3 rounded-xl bg-[#f7f9fc]"><div class="text-[20px] font-extrabold text-[#ff9f45]">48</div><div class="text-[10px] subtle">文档总数</div></div>
                  <div class="p-3 rounded-xl bg-[#f7f9fc]"><div class="text-[20px] font-extrabold text-[#1e2a4a]">12</div><div class="text-[10px] subtle">待处理收藏</div></div>
                </div>
                <div class="mt-3"><div class="flex justify-between text-[11px] mb-1"><span class="subtle">已向量化</span><span class="font-bold text-[#1e2a4a]">46/48</span></div><div class="stat-bar"><span style="width:96%"></span></div></div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-3">最近索引</div>
                <div class="flex items-center gap-2.5"><div class="icon-chip icon-chip-3 bg-[#e2e7f2] text-[#1e2a4a]"><i class="fa-solid fa-circle-notch fa-spin text-[12px]"></i></div><div><div class="font-semibold text-[12px]">《沟通技巧整理》</div><div class="text-[10px] subtle">正在向量化 · 完成后进入问答</div></div></div>
                <div class="flex items-center gap-2.5 mt-2.5"><div class="icon-chip icon-chip-3 bg-[#e6ebf5] text-[#3a4a6b]"><i class="fa-solid fa-check text-[12px]"></i></div><div><div class="font-semibold text-[12px]">《RAG 入门》已就绪</div><div class="text-[10px] subtle">8/18 10:05 完成索引</div></div></div>
              </section>
              <button class="up-btn-solid w-full"><i class="fa-solid fa-comment-dots mr-1"></i>去 RAG 问答</button>
            </div>
          </div>
        </main>
'''

# ============ 4. 沉淀时间线 ============
timeline = '''
        <main class="content" aria-label="时间线">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">沉淀 · 叙事中枢</div>
              <h1 class="page-title mt-0.5 mb-1">沉淀时间线</h1>
              <div class="text-[12px] subtle">日记、灵感、决策、阅读、复盘，按时间回看，可升格进知识库。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>新建沉淀</button>
          </div>

          <div class="flex items-center gap-2 mb-4 flex-wrap">
            <button class="filter-pill active">全部</button>
            <button class="filter-pill">日记</button>
            <button class="filter-pill">灵感</button>
            <button class="filter-pill">决策</button>
            <button class="filter-pill">阅读</button>
            <button class="filter-pill">复盘</button>
          </div>

          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(280px,.7fr)] gap-3.5">
            <section class="panel p-5">
              <div class="rail-label mb-3">今天 · 8月18日</div>
              <div class="entry-row"><span class="badge badge-primary shrink-0 self-start">灵感</span><div class="min-w-0"><div class="font-semibold text-[13px]">把"想做"写成可执行的下一步动作</div><div class="text-[11px] subtle mt-1">关联：开发项目「人生系统」 · 10:22</div></div><button class="up-btn shrink-0 self-start ml-auto"><i class="fa-solid fa-up-right-from-square mr-1"></i>升格</button></div>
              <div class="entry-row"><span class="badge badge-amber shrink-0 self-start">日记</span><div class="min-w-0"><div class="font-semibold text-[13px]">今天把待办和知识库理了一遍</div><div class="text-[11px] subtle mt-1">关联：生活项目「人生系统整理」 · 09:40</div></div><button class="up-btn shrink-0 self-start ml-auto"><i class="fa-solid fa-up-right-from-square mr-1"></i>升格</button></div>
              <div class="rail-label mb-3 mt-5">昨天 · 8月17日</div>
              <div class="entry-row"><span class="badge badge-navy shrink-0 self-start">决策</span><div class="min-w-0"><div class="font-semibold text-[13px]">先做本地优先，再考虑同步</div><div class="text-[11px] subtle mt-1">保留了当时的理由与取舍</div></div><button class="up-btn shrink-0 self-start ml-auto"><i class="fa-solid fa-up-right-from-square mr-1"></i>升格</button></div>
              <div class="entry-row"><span class="badge badge-forest shrink-0 self-start">阅读</span><div class="min-w-0"><div class="font-semibold text-[13px]">《蔡康永的情商课》摘录：记录情绪后的需求</div><div class="text-[11px] subtle mt-1">来源：阅读 · 书摘</div></div><button class="up-btn shrink-0 self-start ml-auto"><i class="fa-solid fa-up-right-from-square mr-1"></i>升格</button></div>
              <div class="rail-label mb-3 mt-5">8月16日</div>
              <div class="entry-row"><span class="badge badge-rose shrink-0 self-start">复盘</span><div class="min-w-0"><div class="font-semibold text-[13px]">本周完成 3 件小事，都落地了</div><div class="text-[11px] subtle mt-1">三问模板 · 关联 2 个目标</div></div><button class="up-btn shrink-0 self-start ml-auto"><i class="fa-solid fa-up-right-from-square mr-1"></i>升格</button></div>
            </section>

            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-3">本月分布</div>
                <div class="space-y-2">
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">灵感</span><div class="stat-bar flex-1"><span style="width:45%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">9</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">日记</span><div class="stat-bar flex-1"><span style="width:30%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">6</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">决策</span><div class="stat-bar flex-1"><span style="width:20%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">4</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">复盘</span><div class="stat-bar flex-1"><span style="width:15%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">3</span></div>
                </div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2">升格说明</div>
                <div class="text-[11px] subtle leading-relaxed">时间线默认不参与 RAG（隐私 + 低检索价值）。证明有长期价值的条目，一键升格进知识库，从此可被检索。</div>
                <div class="flex items-center gap-2 mt-3 text-[11px]"><i class="fa-solid fa-arrow-up text-[#ff9f45]"></i><span>已升格 5 条 · 本月</span></div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 5. 情绪记录 ============
mood = '''
        <main class="content" aria-label="觉察">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">觉察 · 心理健康</div>
              <h1 class="page-title mt-0.5 mb-1">情绪记录</h1>
              <div class="text-[12px] subtle">事件驱动记录，细颗粒度词库，AI 周报辅助自我觉察。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>记录情绪</button>
          </div>

          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(300px,.8fr)] gap-3.5">
            <section class="panel p-4">
              <div class="section-title mb-3">最近记录</div>
              <div class="mood-item"><span class="badge badge-amber shrink-0">焦虑</span><div class="intensity w-10"><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle"></i><i class="fa-solid fa-circle"></i></div><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">周三 · 下午</div><div class="text-[10px] subtle truncate">项目上线前的紧张，需要确认下一步</div></div><span class="text-[10px] text-[#6b7691]">2天前</span></div>
              <div class="mood-item"><span class="badge badge-forest shrink-0">平静</span><div class="intensity w-10"><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle"></i><i class="fa-solid fa-circle"></i><i class="fa-solid fa-circle"></i></div><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">周二 · 晚间</div><div class="text-[10px] subtle truncate">写完周报后的放松</div></div><span class="text-[10px] text-[#6b7691]">3天前</span></div>
              <div class="mood-item"><span class="badge badge-primary shrink-0">被肯定</span><div class="intensity w-10"><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle"></i></div><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">周一 · 上午</div><div class="text-[10px] subtle truncate">方案被认可，需求是继续验证</div></div><span class="text-[10px] text-[#6b7691]">4天前</span></div>
              <div class="mood-item"><span class="badge badge-amber shrink-0">遗憾</span><div class="intensity w-10"><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle on"></i><i class="fa-solid fa-circle"></i><i class="fa-solid fa-circle"></i><i class="fa-solid fa-circle"></i></div><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">上周五</div><div class="text-[10px] subtle truncate">没赶上聚会，需要补一次见面</div></div><span class="text-[10px] text-[#6b7691]">5天前</span></div>
            </section>

            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-3">本周情绪分布</div>
                <div class="space-y-2">
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-9 subtle">平静</span><div class="stat-bar flex-1"><span style="width:35%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">3</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-9 subtle">焦虑</span><div class="stat-bar flex-1"><span style="width:25%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">2</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-9 subtle">被肯定</span><div class="stat-bar flex-1"><span style="width:15%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">1</span></div>
                  <div class="flex items-center gap-2 text-[11px]"><span class="w-9 subtle">遗憾</span><div class="stat-bar flex-1"><span style="width:15%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">1</span></div>
                </div>
                <div class="mt-3 pt-3 border-t border-[#e6eaf2] text-[11px] subtle">负面强度中位数 · 3/5 → 本周呈平稳趋势</div>
              </section>
              <button class="up-btn-solid w-full"><i class="fa-solid fa-file-lines mr-1"></i>生成 AI 周报</button>
              <section class="panel p-4" style="background:linear-gradient(135deg,#1e2a4a 0%,#141d33 100%);border-color:transparent">
                <div class="text-[12px] font-extrabold text-white mb-2">词库小贴士</div>
                <div class="text-[11px] text-white/75 leading-relaxed">记录时用细颗粒度词：<span class="text-[#ff9f45]">惆怅</span> 比「难过」更精确，<span class="text-[#ff9f45]">被背叛</span> 比「生气」更诚实。</div>
                <div class="text-[10px] text-white/50 mt-2">源自《蔡康永的情商课》情绪颗粒度方法</div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 6. 开发项目看板 ============
devboard = '''
        <main class="content" aria-label="交付">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">交付 · 软件开发项目</div>
              <h1 class="page-title mt-0.5 mb-1">开发项目</h1>
              <div class="text-[12px] subtle">想法收集 → 评估 → 立项 → 状态流，阶段可自定义。</div>
            </div>
            <div class="flex gap-2">
              <button class="up-btn"><i class="fa-solid fa-lightbulb mr-1"></i>新建想法</button>
              <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>新建项目</button>
            </div>
          </div>

          <section class="panel p-3.5 mb-4 flex items-center justify-between" style="background:linear-gradient(135deg,#fff0de 0%,#e2e7f2 100%);border-color:transparent">
            <div class="flex items-center gap-3">
              <div class="icon-chip bg-white text-[#ff9f45]"><i class="fa-solid fa-lightbulb"></i></div>
              <div>
                <div class="font-bold text-[13px]">2 个想法待评估</div>
                <div class="text-[11px] subtle">「语音记笔记」「博客主题重设计」等待你的价值 / 可行性打分</div>
              </div>
            </div>
            <button class="up-btn-solid">去评估 →</button>
          </section>

          <div class="grid grid-cols-5 gap-3">
            <div class="kanban-col">
              <div class="flex items-center justify-between px-1"><span class="text-[12px] font-extrabold text-[#5e6b85]">需求</span><span class="text-[11px] text-[#9aa5bd]">1</span></div>
              <div class="kanban-card">
                <div class="font-semibold text-[12px]">人生系统</div>
                <div class="text-[10px] subtle mt-0.5">目标与行动 + 知识沉淀 + AI</div>
                <div class="flex items-center gap-1.5 mt-2"><span class="badge badge-primary">规划中</span><i class="fa-solid fa-grip-vertical text-[#d6dce6] ml-auto"></i></div>
              </div>
            </div>
            <div class="kanban-col">
              <div class="flex items-center justify-between px-1"><span class="text-[12px] font-extrabold text-[#5e6b85]">开发</span><span class="text-[11px] text-[#9aa5bd]">1</span></div>
              <div class="kanban-card">
                <div class="font-semibold text-[12px]">仓库管理系统 v2</div>
                <div class="text-[10px] subtle mt-0.5">Python + Vue 重构</div>
                <div class="flex items-center gap-1.5 mt-2"><span class="badge badge-navy">开发中</span><span class="text-[10px] text-[#6b7691]">60%</span><i class="fa-solid fa-grip-vertical text-[#d6dce6] ml-auto"></i></div>
              </div>
            </div>
            <div class="kanban-col">
              <div class="flex items-center justify-between px-1"><span class="text-[12px] font-extrabold text-[#5e6b85]">测试</span><span class="text-[11px] text-[#9aa5bd]">0</span></div>
              <div class="text-[11px] text-[#9aa5bd] text-center py-6">暂无</div>
            </div>
            <div class="kanban-col">
              <div class="flex items-center justify-between px-1"><span class="text-[12px] font-extrabold text-[#5e6b85]">交付</span><span class="text-[11px] text-[#9aa5bd]">1</span></div>
              <div class="kanban-card">
                <div class="font-semibold text-[12px]">个人博客</div>
                <div class="text-[10px] subtle mt-0.5">已上线 v1.0</div>
                <div class="flex items-center gap-1.5 mt-2"><span class="badge badge-forest">已交付</span><i class="fa-solid fa-grip-vertical text-[#d6dce6] ml-auto"></i></div>
              </div>
            </div>
            <div class="kanban-col">
              <div class="flex items-center justify-between px-1"><span class="text-[12px] font-extrabold text-[#5e6b85]">维护</span><span class="text-[11px] text-[#9aa5bd]">1</span></div>
              <div class="kanban-card">
                <div class="font-semibold text-[12px]">库存预警系统</div>
                <div class="text-[10px] subtle mt-0.5">日常维护中</div>
                <div class="flex items-center gap-1.5 mt-2"><span class="badge badge-amber">维护中</span><i class="fa-solid fa-grip-vertical text-[#d6dce6] ml-auto"></i></div>
              </div>
            </div>
          </div>
          <div class="text-[11px] subtle mt-3"><i class="fa-solid fa-circle-info mr-1 text-[#9aa5bd]"></i>拖动卡片切换阶段 · 阶段可在项目设置中自定义</div>
        </main>
'''

build("goals.html", "目标列表", "今天", goals)
build("tasks.html", "行动", "行动", tasks)
build("knowledge.html", "知识库", "知识", knowledge)
build("timeline.html", "沉淀时间线", "知识", timeline)
build("mood.html", "情绪记录", "觉察", mood)
build("devboard.html", "开发项目", "交付", devboard)

# ============ 7. 收藏箱 ============
inbox = '''
        <main class="content" aria-label="收藏箱">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">知识 · 暂存区</div>
              <h1 class="page-title mt-0.5 mb-1">收藏箱</h1>
              <div class="text-[12px] subtle">链接、片段、稍后读先轻量暂存；值得的剪藏入库，其余丢弃或仅留链接。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>收藏新内容</button>
          </div>
          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(280px,.7fr)] gap-3.5">
            <section class="panel p-4">
              <div class="flex items-center gap-2 mb-3 flex-wrap">
                <button class="filter-pill active">全部 · 12</button>
                <button class="filter-pill">链接</button>
                <button class="filter-pill">片段</button>
                <button class="filter-pill">稍后读</button>
              </div>
              <div>
                <div class="doc-row"><div class="doc-icon bg-[#fff0de] text-[#ff9f45]"><i class="fa-solid fa-link"></i></div><div><div class="font-semibold text-[13px]">RAG 检索增强生成入门</div><div class="text-[11px] subtle">链接 · zhihu.com · 标签：AI</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">剪藏入库</button><button class="up-btn">丢弃</button></div></div>
                <div class="doc-row"><div class="doc-icon bg-[#e2e7f2] text-[#1e2a4a]"><i class="fa-solid fa-scissors"></i></div><div><div class="font-semibold text-[13px]">「进度不由行动换算」——产品定义时的关键决策</div><div class="text-[11px] subtle">片段 · 自存 · 标签：人生系统</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">剪藏入库</button><button class="up-btn">丢弃</button></div></div>
                <div class="doc-row"><div class="doc-icon bg-[#e6ebf5] text-[#3a4a6b]"><i class="fa-regular fa-clock"></i></div><div><div class="font-semibold text-[13px]">《蔡康永的情商课》情绪日记三要素</div><div class="text-[11px] subtle">稍后读 · 书摘 · 标签：心理</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">剪藏入库</button><button class="up-btn">丢弃</button></div></div>
                <div class="doc-row"><div class="doc-icon bg-[#fff0de] text-[#ff9f45]"><i class="fa-solid fa-link"></i></div><div><div class="font-semibold text-[13px]">MySQL ngram 全文索引中文实践</div><div class="text-[11px] subtle">链接 · dev.mysql.com · 标签：数据库</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">剪藏入库</button><button class="up-btn">丢弃</button></div></div>
              </div>
            </section>
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-3">处理规则</div>
                <div class="space-y-2 text-[12px]">
                  <div class="flex items-center gap-2.5"><div class="icon-chip icon-chip-3 bg-[#fff0de] text-[#ff9f45]"><i class="fa-solid fa-scissors"></i></div><div><div class="font-semibold text-[12px]">剪藏入库</div><div class="text-[10px] subtle">抓取正文 → 知识库 → 参与 RAG</div></div></div>
                  <div class="flex items-center gap-2.5"><div class="icon-chip icon-chip-3 bg-[#e2e7f2] text-[#1e2a4a]"><i class="fa-solid fa-bookmark"></i></div><div><div class="font-semibold text-[12px]">仅留链接</div><div class="text-[10px] subtle">只存 URL，不抓取正文</div></div></div>
                  <div class="flex items-center gap-2.5"><div class="icon-chip icon-chip-3 bg-[#e6ebf5] text-[#3a4a6b]"><i class="fa-solid fa-trash-can"></i></div><div><div class="font-semibold text-[12px]">丢弃</div><div class="text-[10px] subtle">不再需要，删除</div></div></div>
                </div>
                <div class="mt-3 pt-3 border-t border-[#e6eaf2] text-[11px] subtle">收藏是过程，知识库是结果。暂存区不参与检索。</div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2">本周处理情况</div>
                <div class="flex justify-between text-[11px] mb-1"><span class="subtle">已处理</span><span class="font-bold text-[#1e2a4a]">8 / 12</span></div>
                <div class="stat-bar"><span style="width:67%"></span></div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 8. RAG 问答 ============
qa = '''
        <main class="content" aria-label="问答">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">知识 · RAG 检索问答</div>
              <h1 class="page-title mt-0.5 mb-1">知识问答</h1>
              <div class="text-[12px] subtle">只检索知识库，回答带引用来源；无网络时显示相似资料。</div>
            </div>
            <span class="badge badge-forest"><i class="fa-solid fa-circle text-[6px] mr-1 align-middle"></i>知识库在线</span>
          </div>
          <div class="grid grid-cols-[minmax(0,1.4fr)_minmax(280px,.7fr)] gap-3.5">
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="text-[11px] subtle mb-2">问知识库</div>
                <div class="flex gap-2">
                  <label class="search flex-1 !w-auto"><i class="fa-solid fa-magnifying-glass text-[12px]"></i><input type="text" placeholder="例如：我写过关于 RAG 分块策略的笔记吗？" /></label>
                  <button class="up-btn-solid">提问</button>
                </div>
                <div class="flex gap-2 mt-2 flex-wrap">
                  <button class="filter-pill">我有没有写过 MySQL 相关的笔记？</button>
                  <button class="filter-pill">复盘方法论是什么？</button>
                  <button class="filter-pill">Prompt 模板有哪些？</button>
                </div>
              </section>
              <section class="panel p-4">
                <div class="flex items-center gap-2 mb-3"><span class="badge badge-primary">回答</span><span class="text-[11px] subtle">来自 3 篇文档 · 约 42 秒前</span></div>
                <p class="text-[13px] leading-relaxed text-[#2f3b4a]">根据你的知识库，关于 RAG 分块你记录过：<span class="font-semibold">「按标题和段落分组，在约 400–700 tokens 处切分，保留 80–120 tokens 重叠；代码、表格和引用不跨段截断」</span>[1]。另外在《人生系统架构笔记》里提到了 chunk 元数据需包含 documentTitle、headingPath 与页码[2]。</p>
                <div class="mt-3 space-y-2">
                  <div class="p-3 rounded-xl bg-[#f7f9fc] border border-[#e6eaf2] text-[12px]"><span class="badge badge-navy mr-2">[1]</span><span class="font-semibold">RAG 检索增强生成入门</span><span class="subtle text-[11px]"> · 第 3 节「分块策略」</span></div>
                  <div class="p-3 rounded-xl bg-[#f7f9fc] border border-[#e6eaf2] text-[12px]"><span class="badge badge-navy mr-2">[2]</span><span class="font-semibold">人生系统架构笔记</span><span class="subtle text-[11px]"> · 第 5.2 节「RAG 索引与问答管线」</span></div>
                </div>
                <div class="text-[10px] subtle mt-3"><i class="fa-regular fa-circle-check mr-1 text-[#1fa97f]"></i>无证据时 AI 会明确说不知道 · 引用可点击回到原文</div>
              </section>
            </div>
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-3">相似资料</div>
                <div class="space-y-2 text-[12px]">
                  <div class="p-2.5 rounded-lg bg-[#f7f9fc]"><div class="font-semibold">RAG 检索增强生成入门</div><div class="text-[10px] subtle">相似度 0.92 · 网页剪藏</div></div>
                  <div class="p-2.5 rounded-lg bg-[#f7f9fc]"><div class="font-semibold">人生系统架构笔记</div><div class="text-[10px] subtle">相似度 0.87 · Markdown</div></div>
                  <div class="p-2.5 rounded-lg bg-[#f7f9fc]"><div class="font-semibold">MySQL 全文索引实践</div><div class="text-[10px] subtle">相似度 0.74 · PDF</div></div>
                </div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2">检索范围</div>
                <div class="text-[11px] subtle leading-relaxed">默认只检索<b class="text-[#1e2a4a]">知识库文档</b>。情绪记录、时间线沉淀不参与检索；仅当你明确要求时才读取其它模块。</div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 9. 想法收集 ============
ideas = '''
        <main class="content" aria-label="想法">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">交付 · 创意漏斗</div>
              <h1 class="page-title mt-0.5 mb-1">想法收集</h1>
              <div class="text-[12px] subtle">轻量收集 → 评估打分 → 一键立项 → 进入开发状态流。</div>
            </div>
            <button class="primary" id="new-task"><i class="fa-solid fa-plus mr-2"></i>记一个想法</button>
          </div>
          <div class="grid grid-cols-2 gap-3.5">
            <section class="panel p-4">
              <div class="section-title mb-3"><span class="badge badge-primary mr-2">收集箱</span>3 个想法</div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">语音记笔记</div><div class="text-[10px] subtle mt-0.5">说一句话自动转文字入收藏箱</div></div><span class="text-[10px] text-[#6b7691]">8/16</span></div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">博客主题重设计</div><div class="text-[10px] subtle mt-0.5">换一个更适合阅读的深色主题</div></div><span class="text-[10px] text-[#6b7691]">8/15</span></div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">待办语音提醒</div><div class="text-[10px] subtle mt-0.5">到点用语音提醒当天最重要的事</div></div><span class="text-[10px] text-[#6b7691]">8/14</span></div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3"><span class="badge badge-amber mr-2">评估中</span>2 个想法</div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">本地 RAG 助手</div><div class="text-[10px] subtle mt-0.5">价值 4 · 可行性 3 · 备注：技术已验证</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">评估</button><button class="up-btn-solid !py-1">立项</button></div></div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">习惯打卡小部件</div><div class="text-[10px] subtle mt-0.5">价值 3 · 可行性 5 · 备注：工作量小</div></div><div class="flex gap-1.5 shrink-0"><button class="up-btn">评估</button><button class="up-btn-solid !py-1">立项</button></div></div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3"><span class="badge badge-forest mr-2">已立项</span>2 个</div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">人生系统</div><div class="text-[10px] subtle mt-0.5">→ 开发项目 · 需求阶段</div></div><i class="fa-solid fa-arrow-right text-[#ff9f45]"></i></div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">仓库管理系统 v2</div><div class="text-[10px] subtle mt-0.5">→ 开发项目 · 开发阶段</div></div><i class="fa-solid fa-arrow-right text-[#ff9f45]"></i></div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3"><span class="badge badge-rose mr-2">已放弃</span>1 个</div>
              <div class="mood-item"><div class="min-w-0 flex-1"><div class="font-semibold text-[12px]">社区分享功能</div><div class="text-[10px] subtle mt-0.5">单人系统不需要协作 · 8/10 放弃</div></div><button class="up-btn shrink-0">复活</button></div>
            </section>
          </div>
        </main>
'''

# ============ 10. 开发项目详情 ============
devdetail = '''
        <main class="content" aria-label="项目详情">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">交付 · 开发项目</div>
              <h1 class="page-title mt-0.5 mb-1">人生系统</h1>
              <div class="text-[12px] subtle">目标与行动管理 + 知识沉淀 + AI 辅助 · 阶段可自定义</div>
            </div>
            <div class="flex gap-2"><button class="up-btn"><i class="fa-solid fa-pen mr-1"></i>编辑</button><button class="up-btn-solid"><i class="fa-solid fa-arrow-right mr-1"></i>推进到下一阶段</button></div>
          </div>
          <div class="grid grid-cols-3 gap-3.5">
            <section class="panel p-4 col-span-2">
              <div class="section-title mb-3">阶段进度</div>
              <div class="flex items-center gap-1 mb-4">
                <div class="flex-1"><div class="h-1.5 rounded-full bg-[#ff9f45]"></div></div>
                <div class="flex-1"><div class="h-1.5 rounded-full bg-[#1e2a4a]"></div></div>
                <div class="flex-1"><div class="h-1.5 rounded-full bg-[#e6eaf2]"></div></div>
                <div class="flex-1"><div class="h-1.5 rounded-full bg-[#e6eaf2]"></div></div>
                <div class="flex-1"><div class="h-1.5 rounded-full bg-[#e6eaf2]"></div></div>
              </div>
              <div class="grid grid-cols-5 gap-2 text-center">
                <div><span class="badge badge-primary">需求</span><div class="text-[10px] subtle mt-1">已开始</div></div>
                <div><span class="badge badge-navy">开发</span><div class="text-[10px] subtle mt-1">进行中</div></div>
                <div><span class="badge badge-forest">测试</span><div class="text-[10px] subtle mt-1">待开始</div></div>
                <div><span class="badge badge-amber">交付</span><div class="text-[10px] subtle mt-1">待开始</div></div>
                <div><span class="badge badge-rose">维护</span><div class="text-[10px] subtle mt-1">待开始</div></div>
              </div>
              <div class="mt-4 pt-4 border-t border-[#e6eaf2]">
                <div class="rail-label mb-2">项目信息</div>
                <div class="grid grid-cols-2 gap-3 text-[12px]">
                  <div><span class="subtle">状态：</span><span class="font-semibold text-[#1e2a4a]">开发中</span></div>
                  <div><span class="subtle">仓库：</span><a class="font-semibold text-[#ff9f45]">github.com/you/life-system →</a></div>
                  <div><span class="subtle">开始：</span><span class="font-semibold text-[#1e2a4a]">2026-08-10</span></div>
                  <div><span class="subtle">标签：</span><span class="badge badge-amber">AI</span> <span class="badge badge-amber">桌面端</span></div>
                </div>
              </div>
            </section>
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-2">项目描述</div>
                <div class="text-[12px] subtle leading-relaxed">个人人生管理系统：四大模块（目标行动 / 知识沉淀 / 心理健康 / 项目交付），Electron + Vue3 桌面端，本地 MySQL + Milvus，RAG 驱动知识问答。</div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2">阶段记录</div>
                <div class="space-y-2 text-[12px]">
                  <div class="flex items-center gap-2"><i class="fa-solid fa-circle text-[8px] text-[#ff9f45]"></i><span>8/15 进入开发阶段</span></div>
                  <div class="flex items-center gap-2"><i class="fa-solid fa-circle text-[8px] text-[#1e2a4a]"></i><span>8/10 立项 · 需求阶段</span></div>
                </div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 11. 情绪周报 ============
moodreport = '''
        <main class="content" aria-label="情绪周报">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">觉察 · AI 周报</div>
              <h1 class="page-title mt-0.5 mb-1">情绪周报</h1>
              <div class="text-[12px] subtle">8/12 – 8/18 · 由 8 条情绪记录生成 · 自我觉察辅助，非心理诊断</div>
            </div>
            <div class="flex gap-2"><button class="up-btn"><i class="fa-solid fa-arrows-rotate mr-1"></i>重新生成</button><button class="up-btn-solid"><i class="fa-solid fa-download mr-1"></i>导出</button></div>
          </div>
          <div class="grid grid-cols-3 gap-3.5">
            <section class="panel p-4">
              <div class="section-title mb-3">情绪分布</div>
              <div class="space-y-2">
                <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">平静</span><div class="stat-bar flex-1"><span style="width:38%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">3</span></div>
                <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">焦虑</span><div class="stat-bar flex-1"><span style="width:25%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">2</span></div>
                <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">被肯定</span><div class="stat-bar flex-1"><span style="width:13%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">1</span></div>
                <div class="flex items-center gap-2 text-[11px]"><span class="w-10 subtle">遗憾</span><div class="stat-bar flex-1"><span style="width:13%"></span></div><span class="font-bold text-[#1e2a4a] w-4 text-right">1</span></div>
              </div>
              <div class="mt-3 pt-3 border-t border-[#e6eaf2] text-[11px] subtle">负面情绪强度中位数：3/5 → 平稳</div>
            </section>
            <section class="panel p-4 col-span-2">
              <div class="section-title mb-3"><i class="fa-regular fa-file-lines mr-1 text-[#ff9f45]"></i>AI 总结</div>
              <p class="text-[13px] leading-relaxed text-[#2f3b4a]">这一周你记录的情绪里，<b>焦虑</b>出现 2 次且都与「项目上线」相关，强度均为 3。有意思的是，两次焦虑记录后你都写了「需要确认下一步」——说明焦虑背后是<b>对不确定性的不适</b>，而不是对工作本身。</p>
              <div class="p-3.5 rounded-xl mt-3" style="background:linear-gradient(135deg,#fff0de 0%,#e2e7f2 100%)">
                <div class="text-[12px] font-extrabold text-[#141d33] mb-1.5">给你的建议（怎么办导向）</div>
                <div class="text-[12px] text-[#4a5570] leading-relaxed">下周遇到上线节点，可以试试<b>「先陈述事实，再表达感受」</b>：把「我很焦虑」换成「这周有 3 个待确认项，我需要逐个核对」。把模糊的压力变成具体清单，通常焦虑会降下来。</div>
              </div>
              <div class="text-[10px] subtle mt-3">基于你的记录生成 · 非诊断 · 建议仅作参考</div>
            </section>
          </div>
        </main>
'''

# ============ 12. 目标详情 ============
goaldetail = '''
        <main class="content" aria-label="目标详情">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">目标 · 详情</div>
              <h1 class="page-title mt-0.5 mb-1">体重回到 65 kg</h1>
              <div class="text-[12px] subtle">年度 · 数值型 · 标签：健康 · 68.6 → 65.0 kg</div>
            </div>
            <div class="flex gap-2"><button class="up-btn"><i class="fa-solid fa-pen mr-1"></i>编辑</button><button class="up-btn-solid"><i class="fa-solid fa-plus mr-1"></i>记录数据</button></div>
          </div>
          <div class="grid grid-cols-3 gap-3.5">
            <section class="panel p-4 col-span-2">
              <div class="section-title mb-3">进展趋势</div>
              <div class="flex items-end gap-2 h-[120px] px-2">
                <div class="flex-1 flex flex-col justify-end items-center gap-1"><div class="text-[10px] text-[#6b7691]">69.2</div><div class="w-full h-[58%] rounded-t-md bg-[#1e2a4a]/70"></div><div class="text-[10px] subtle">7/20</div></div>
                <div class="flex-1 flex flex-col justify-end items-center gap-1"><div class="text-[10px] text-[#6b7691]">68.8</div><div class="w-full h-[48%] rounded-t-md bg-[#1e2a4a]/70"></div><div class="text-[10px] subtle">7/27</div></div>
                <div class="flex-1 flex flex-col justify-end items-center gap-1"><div class="text-[10px] text-[#6b7691]">68.6</div><div class="w-full h-[42%] rounded-t-md bg-[#1e2a4a]"></div><div class="text-[10px] subtle">8/3</div></div>
                <div class="flex-1 flex flex-col justify-end items-center gap-1"><div class="text-[10px] text-[#6b7691]">68.2</div><div class="w-full h-[36%] rounded-t-md bg-[#1e2a4a]"></div><div class="text-[10px] subtle">8/10</div></div>
                <div class="flex-1 flex flex-col justify-end items-center gap-1"><div class="text-[10px] text-[#ff9f45] font-bold">67.9</div><div class="w-full h-[30%] rounded-t-md bg-[#ff9f45]"></div><div class="text-[10px] subtle">8/17</div></div>
              </div>
              <div class="flex items-center justify-between mt-3">
                <span class="text-[11px] subtle">起点 69.2 · 当前 67.9 · 目标 65.0</span>
                <span class="text-[14px] font-extrabold text-[#ff9f45]">68%</span>
              </div>
              <div class="mini-progress mt-2"><span style="width:68%"></span></div>
            </section>
            <div class="space-y-3.5">
              <section class="panel p-4">
                <div class="section-title mb-2">支持行动</div>
                <div class="space-y-2 text-[12px]">
                  <div class="flex items-center gap-2"><i class="fa-regular fa-square text-[#cdd5e0]"></i><span>晨跑 30 分钟</span><span class="ml-auto text-[10px] text-[#6b7691]">习惯</span></div>
                  <div class="flex items-center gap-2"><i class="fa-regular fa-square text-[#cdd5e0]"></i><span>记录一次体重</span><span class="ml-auto text-[10px] text-[#6b7691]">待办</span></div>
                  <div class="text-[11px] subtle pt-2 border-t border-[#e6eaf2]">行动只做支持展示，不折算进度</div>
                </div>
              </section>
              <section class="panel p-4">
                <div class="section-title mb-2">数据点记录</div>
                <div class="space-y-2 text-[12px]">
                  <div class="flex justify-between"><span class="subtle">8/17</span><span class="font-semibold">67.9 kg</span></div>
                  <div class="flex justify-between"><span class="subtle">8/10</span><span class="font-semibold">68.2 kg</span></div>
                  <div class="flex justify-between"><span class="subtle">8/3</span><span class="font-semibold">68.6 kg</span></div>
                </div>
              </section>
            </div>
          </div>
        </main>
'''

# ============ 13. 设置 ============
settings = '''
        <main class="content" aria-label="设置">
          <div class="flex items-center justify-between mb-4">
            <div>
              <div class="text-[12px] subtle font-semibold tracking-wider">设置</div>
              <h1 class="page-title mt-0.5 mb-1">设置</h1>
              <div class="text-[12px] subtle">数据源连接、AI 密钥、备份与降噪。</div>
            </div>
            <button class="up-btn-solid"><i class="fa-solid fa-floppy-disk mr-1"></i>保存</button>
          </div>
          <div class="grid grid-cols-2 gap-3.5">
            <section class="panel p-4">
              <div class="section-title mb-3">数据源连接</div>
              <div class="space-y-3">
                <div>
                  <div class="text-[11px] font-bold text-[#4a5570] mb-1">MySQL 连接</div>
                  <input class="setting-input" value="mysql://127.0.0.1:3306/life_system" />
                  <div class="flex items-center gap-2 mt-1.5"><span class="badge badge-forest"><i class="fa-solid fa-circle text-[6px] mr-1 align-middle"></i>已连接</span><button class="up-btn">测试连接</button></div>
                </div>
                <div>
                  <div class="text-[11px] font-bold text-[#4a5570] mb-1">Milvus 连接</div>
                  <input class="setting-input" value="http://127.0.0.1:19530" />
                  <div class="flex items-center gap-2 mt-1.5"><span class="badge badge-forest"><i class="fa-solid fa-circle text-[6px] mr-1 align-middle"></i>已连接</span><button class="up-btn">测试连接</button></div>
                </div>
                <div class="text-[11px] subtle">部署拓扑不属于软件范围，连接可达性由你负责。可点「测试连接」校验。</div>
              </div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3">AI 与密钥</div>
              <div class="space-y-3">
                <div>
                  <div class="text-[11px] font-bold text-[#4a5570] mb-1">LLM API 密钥（DeepSeek 兼容）</div>
                  <input class="setting-input" type="password" placeholder="sk-****" />
                  <div class="text-[11px] subtle mt-1">密钥仅存本机安全存储，不进日志与备份。</div>
                </div>
                <div>
                  <div class="text-[11px] font-bold text-[#4a5570] mb-1">Embedding 模型</div>
                  <input class="setting-input" value="bge-small-zh-v1.5（本地）" />
                </div>
                <div class="text-[11px] subtle">无密钥 / 断网时：核心功能不受影响，AI 问答与周报显示「不可用」并引导降级。</div>
              </div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3">备份与导出</div>
              <div class="flex gap-2 mb-3">
                <button class="up-btn-solid"><i class="fa-solid fa-download mr-1"></i>一键备份</button>
                <button class="up-btn"><i class="fa-solid fa-upload mr-1"></i>恢复</button>
                <button class="up-btn"><i class="fa-solid fa-file-export mr-1"></i>导出 JSON</button>
              </div>
              <div class="text-[11px] subtle leading-relaxed">备份含 MySQL 逻辑导出 + Milvus 快照 + 原始文件 + manifest（SHA-256 校验）。恢复为整体替换，不做冲突合并。上次备份：昨天 18:00。</div>
            </section>
            <section class="panel p-4">
              <div class="section-title mb-3">提醒与降噪</div>
              <div class="space-y-2.5">
                <div class="flex items-center justify-between"><span class="text-[12px] font-semibold">关键提醒（目标到期 / 习惯打卡 / 数据录入）</span><button class="toggle on"></button></div>
                <div class="flex items-center justify-between"><span class="text-[12px] font-semibold">周期报告（周报 / 复盘）</span><button class="toggle"></button></div>
                <div class="flex items-center justify-between"><span class="text-[12px] font-semibold">主动推荐</span><button class="toggle"></button></div>
                <div class="text-[11px] subtle pt-2 border-t border-[#e6eaf2]">所有提醒仅站内聚合，无外部推送。主动推荐默认关闭。</div>
              </div>
            </section>
          </div>
        </main>
'''

build("inbox.html", "收藏箱", "知识", inbox)
build("qa.html", "知识问答", "知识", qa)
build("ideas.html", "想法收集", "交付", ideas)
build("dev-project-detail.html", "开发项目详情", "交付", devdetail)
build("mood-report.html", "情绪周报", "觉察", moodreport)
build("goal-detail.html", "目标详情", "今天", goaldetail)
build("settings.html", "设置", "今天", settings)
print("全部完成")
