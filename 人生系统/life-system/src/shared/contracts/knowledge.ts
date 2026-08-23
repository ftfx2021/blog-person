import { z } from "zod";
import { entityIdSchema } from "./common.js";

export const knowledgeDocTypeSchema = z.enum(["webpage", "pdf", "docx", "markdown", "txt", "html", "note", "skill", "prompt", "promoted"]);
export const knowledgeListSchema = z.object({ docType: knowledgeDocTypeSchema.optional(), tag: z.string().trim().min(1).max(100).optional(), keyword: z.string().trim().max(100).optional(), kbId: entityIdSchema.optional(), folderId: entityIdSchema.optional(), includeDeleted: z.boolean().default(false) });
const knowledgeOwnershipSchema = z.object({
  kbId: entityIdSchema.optional(),
  folderId: entityIdSchema.nullable().optional(),
});
export const knowledgeNoteSchema = z.object({ title: z.string().trim().min(1).max(500), rawText: z.string().trim().min(1), docType: z.enum(["note", "skill", "prompt"]), tags: z.array(z.string().trim().min(1).max(100)).max(30).default([]) }).merge(knowledgeOwnershipSchema);
export const knowledgeUpdateSchema = z.object({ id: entityIdSchema, title: z.string().trim().min(1).max(500).optional(), rawText: z.string().trim().min(1).optional(), docType: knowledgeDocTypeSchema.optional(), tags: z.array(z.string().trim().min(1).max(100)).max(30).optional() });
export const knowledgeTagsSchema = z.object({ id: entityIdSchema, tags: z.array(z.string().trim().min(1).max(100)).max(30) });
export const knowledgeExportSchema = z.object({ id: entityIdSchema, format: z.enum(["pdf", "markdown", "txt"]) });
export const knowledgeImportFileSchema = z.object({ sourcePath: z.string().trim().min(1).optional() }).merge(knowledgeOwnershipSchema);
export const knowledgeImportUrlSchema = z.object({ url: z.string().url(), title: z.string().trim().min(1).max(500).optional() }).merge(knowledgeOwnershipSchema);
export const knowledgeBaseCreateSchema = z.object({ name: z.string().trim().min(1).max(50), description: z.string().trim().max(5000).optional(), color: z.string().trim().max(16).optional(), sort: z.number().int().min(0).default(0) });
export const knowledgeBaseUpdateSchema = knowledgeBaseCreateSchema.partial().extend({ id: entityIdSchema });
export const knowledgeFolderCreateSchema = z.object({ kbId: entityIdSchema, parentId: entityIdSchema.nullable().optional(), name: z.string().trim().min(1).max(50), sort: z.number().int().min(0).default(0) });
export const knowledgeFolderUpdateSchema = z.object({ id: entityIdSchema, name: z.string().trim().min(1).max(50).optional(), parentId: entityIdSchema.nullable().optional(), sort: z.number().int().min(0).optional() });
export const knowledgeFolderListSchema = z.object({ kbId: entityIdSchema });
export const knowledgeMoveSchema = z.object({ id: entityIdSchema, kbId: entityIdSchema, folderId: entityIdSchema.nullable().optional() });
export const tagRenameSchema = z.object({ id: entityIdSchema, name: z.string().trim().min(1).max(100) });
