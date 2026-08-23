import { randomUUID } from "node:crypto";
import { requirePool } from "../../infrastructure/db/pool.js";
import { utcNow } from "../common/database.js";

export const DEFAULT_KB_ID = "00000000-0000-0000-0000-000000000001";

function invalid(message: string): never {
  throw Object.assign(new Error(message), { code: "INVALID_STATE" });
}

async function ensureBase(id: string): Promise<any> {
  const [rows] = await requirePool().query<any[]>(
    "SELECT id,name FROM knowledge_base WHERE id=?",
    [id],
  );
  if (!rows[0]) invalid("知识库不存在");
  return rows[0];
}

async function ensureFolder(id: string): Promise<any> {
  const [rows] = await requirePool().query<any[]>(
    "SELECT id,kb_id AS kbId,parent_id AS parentId,name,sort FROM knowledge_folder WHERE id=?",
    [id],
  );
  if (!rows[0]) invalid("文件夹不存在");
  return rows[0];
}

// 在写入文档和移动前验证库/文件夹归属，防止客户端伪造跨库路径。
export async function validateOwnership(
  kbId: string | undefined,
  folderId: string | null | undefined,
): Promise<{ kbId: string; folderId: string | null }> {
  const targetKbId = kbId ?? DEFAULT_KB_ID;
  await ensureBase(targetKbId);
  if (!folderId) return { kbId: targetKbId, folderId: null };
  const folder = await ensureFolder(folderId);
  if (folder.kbId !== targetKbId) invalid("文件夹不属于目标知识库");
  return { kbId: targetKbId, folderId };
}

async function folderTree(kbId: string): Promise<any[]> {
  const [rows] = await requirePool().query<any[]>(
    "SELECT id,kb_id AS kbId,parent_id AS parentId,name,sort FROM knowledge_folder WHERE kb_id=? ORDER BY sort,created_at",
    [kbId],
  );
  const byId = new Map(rows.map((row) => [row.id, { ...row, children: [] as any[] }]));
  const roots: any[] = [];
  for (const row of byId.values()) {
    const parent = row.parentId ? byId.get(row.parentId) : undefined;
    if (parent) parent.children.push(row); else roots.push(row);
  }
  return roots;
}

async function assertNoCycle(id: string, parentId: string | null | undefined): Promise<void> {
  if (!parentId) return;
  if (id === parentId) invalid("文件夹不能设置为自己的父级");
  // 从候选父级向上遍历，命中自身表示把父级指到了自己的后代。
  let cursor: string | null = parentId;
  const seen = new Set<string>();
  while (cursor) {
    if (cursor === id) invalid("文件夹不能移动到自己的子文件夹中");
    if (seen.has(cursor)) invalid("文件夹层级存在循环");
    seen.add(cursor);
    const node = await ensureFolder(cursor);
    cursor = node.parentId;
  }
}

export const knowledgeBaseService = {
  async list() {
    const [rows] = await requirePool().query<any[]>(
      "SELECT kb.id,kb.name,kb.description,kb.color,kb.sort,kb.created_at AS createdAt,kb.updated_at AS updatedAt,(SELECT COUNT(*) FROM document d WHERE d.kb_id=kb.id AND d.deleted_at IS NULL) AS documentCount,(SELECT COUNT(*) FROM knowledge_folder f WHERE f.kb_id=kb.id) AS folderCount FROM knowledge_base kb ORDER BY kb.sort,kb.created_at",
    );
    return rows;
  },
  async create(input: { name: string; description?: string; color?: string; sort?: number }) {
    const [duplicates] = await requirePool().query<any[]>("SELECT id FROM knowledge_base WHERE name=?", [input.name.trim()]);
    if (duplicates[0]) invalid("知识库名称已存在");
    const id = randomUUID();
    await requirePool().query(
      "INSERT INTO knowledge_base (id,name,description,color,sort,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
      [id, input.name.trim(), input.description?.trim() || null, input.color?.trim() || null, input.sort ?? 0, utcNow(), utcNow()],
    );
    return ensureBase(id);
  },
  async update(input: { id: string; name?: string; description?: string; color?: string; sort?: number }) {
    const [currentRows] = await requirePool().query<any[]>(
      "SELECT id,name,description,color,sort FROM knowledge_base WHERE id=?",
      [input.id],
    );
    const current = currentRows[0];
    if (!current) invalid("知识库不存在");
    if (input.name && input.name.trim() !== current.name) {
      const [duplicates] = await requirePool().query<any[]>("SELECT id FROM knowledge_base WHERE name=? AND id<>?", [input.name.trim(), input.id]);
      if (duplicates[0]) invalid("知识库名称已存在");
    }
    await requirePool().query(
      "UPDATE knowledge_base SET name=?,description=?,color=?,sort=?,updated_at=? WHERE id=?",
      [input.name?.trim() ?? current.name, input.description?.trim() ?? current.description, input.color?.trim() ?? current.color, input.sort ?? current.sort, utcNow(), input.id],
    );
    return ensureBase(input.id);
  },
  async remove(id: string) {
    // 默认知识库是存量文档和未指定归属的新文档的稳定落点，不能被删除。
    if (id === DEFAULT_KB_ID) invalid("默认知识库不能删除");
    await ensureBase(id);
    const [rows] = await requirePool().query<any[]>("SELECT COUNT(*) AS count FROM document WHERE kb_id=?", [id]);
    if (Number(rows[0]?.count) > 0) invalid("库内有文档，请先移动或删除");
    await requirePool().query("DELETE FROM knowledge_base WHERE id=?", [id]);
    return { removed: true };
  },
  async folders(kbId: string) { await ensureBase(kbId); return folderTree(kbId); },
  async createFolder(input: { kbId: string; parentId?: string | null; name: string; sort?: number }) {
    await ensureBase(input.kbId);
    if (input.parentId) { const parent = await ensureFolder(input.parentId); if (parent.kbId !== input.kbId) invalid("父文件夹不属于当前知识库"); }
    // MySQL 的 NULL 唯一键无法约束根目录重名，这里显式补齐一致的目录语义。
    const [duplicates] = await requirePool().query<any[]>("SELECT id FROM knowledge_folder WHERE kb_id=? AND parent_id <=> ? AND name=?", [input.kbId, input.parentId ?? null, input.name.trim()]);
    if (duplicates[0]) invalid("同级文件夹名称已存在");
    const id = randomUUID();
    await requirePool().query("INSERT INTO knowledge_folder (id,kb_id,parent_id,name,sort,created_at,updated_at) VALUES (?,?,?,?,?,?,?)", [id, input.kbId, input.parentId ?? null, input.name.trim(), input.sort ?? 0, utcNow(), utcNow()]);
    return ensureFolder(id);
  },
  async updateFolder(input: { id: string; name?: string; parentId?: string | null; sort?: number }) {
    const current = await ensureFolder(input.id);
    const parentId = input.parentId === undefined ? current.parentId : input.parentId;
    await assertNoCycle(input.id, parentId);
    if (parentId) { const parent = await ensureFolder(parentId); if (parent.kbId !== current.kbId) invalid("文件夹不能跨知识库移动"); }
    const targetName = input.name?.trim() ?? current.name;
    const [duplicates] = await requirePool().query<any[]>("SELECT id FROM knowledge_folder WHERE kb_id=? AND parent_id <=> ? AND name=? AND id<>?", [current.kbId, parentId, targetName, input.id]);
    if (duplicates[0]) invalid("同级文件夹名称已存在");
    await requirePool().query("UPDATE knowledge_folder SET name=?,parent_id=?,sort=?,updated_at=? WHERE id=?", [targetName, parentId, input.sort ?? current.sort ?? 0, utcNow(), input.id]);
    return ensureFolder(input.id);
  },
  async removeFolder(id: string) {
    await ensureFolder(id);
    const [children] = await requirePool().query<any[]>("SELECT COUNT(*) AS count FROM knowledge_folder WHERE parent_id=?", [id]);
    const [documents] = await requirePool().query<any[]>("SELECT COUNT(*) AS count FROM document WHERE folder_id=?", [id]);
    if (Number(children[0]?.count) || Number(documents[0]?.count)) invalid("文件夹非空，不能删除");
    await requirePool().query("DELETE FROM knowledge_folder WHERE id=?", [id]);
    return { removed: true };
  },
};
