import { readFile, readdir } from 'node:fs/promises'
import { join } from 'node:path'

const forbiddenRendererImports = /from\s+['"](?:electron|mysql2(?:\/promise)?|(?:node:)?fs(?:\/promises)?|@milvus-io\/[^'"]+|@zilliz\/[^'"]+)['"]/g
const forbiddenDomainImports = /from\s+['"](?:vue|electron|@renderer\/[^'"]+)['"]/g

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? walk(path) : path
  }))
  return files.flat().filter((path) => /\.(ts|vue)$/.test(path))
}

async function assertBoundary(directory, pattern, label) {
  const files = await walk(directory)
  const violations = []
  for (const file of files) {
    const source = await readFile(file, 'utf8')
    pattern.lastIndex = 0
    if (pattern.test(source)) violations.push(file)
  }
  if (violations.length > 0) throw new Error(`${label} 越界导入:\n${violations.join('\n')}`)
}

await assertBoundary('src/renderer', forbiddenRendererImports, '渲染层')
await assertBoundary('src/shared/domain', forbiddenDomainImports, '共享领域层')
console.log('进程边界检查通过')
