import { requirePool } from "../../infrastructure/db/pool.js";

function conflict(message: string): never {
  throw Object.assign(new Error(message), { code: "CONFLICT" });
}

export const tagService = {
  // 标签是跨知识库的横向维度，数量统计只计算仍可见的文档。
  async list() {
    const [rows] = await requirePool().query<any[]>(
      "SELECT t.id,t.name,COUNT(d.id) AS documentCount FROM tag t LEFT JOIN entity_tag et ON et.tag_id=t.id AND et.entity_type='document' LEFT JOIN document d ON d.id=et.entity_id AND d.deleted_at IS NULL GROUP BY t.id,t.name ORDER BY t.name",
    );
    return rows;
  },
  async rename(id: string, name: string) {
    const [currentRows] = await requirePool().query<any[]>("SELECT id FROM tag WHERE id=?", [id]);
    if (!currentRows[0]) conflict("标签不存在");
    const [duplicateRows] = await requirePool().query<any[]>("SELECT id FROM tag WHERE name=?", [name]);
    if (duplicateRows[0] && duplicateRows[0].id !== id) {
      // 新名字存在时合并关联，再删除旧标签，避免同一实体出现重复标签。
      await requirePool().query("INSERT IGNORE INTO entity_tag (entity_type,entity_id,tag_id) SELECT entity_type,entity_id,? FROM entity_tag WHERE tag_id=?", [duplicateRows[0].id, id]);
      await requirePool().query("DELETE FROM entity_tag WHERE tag_id=?", [id]);
      await requirePool().query("DELETE FROM tag WHERE id=?", [id]);
      return { id: duplicateRows[0].id, name, merged: true };
    }
    await requirePool().query("UPDATE tag SET name=? WHERE id=?", [name, id]);
    return { id, name, merged: false };
  },
  async remove(id: string) {
    // 删除标签只解除关联，不删除任何文档或其它实体。
    await requirePool().query("DELETE FROM entity_tag WHERE tag_id=?", [id]);
    await requirePool().query("DELETE FROM tag WHERE id=?", [id]);
    return { removed: true };
  },
};
