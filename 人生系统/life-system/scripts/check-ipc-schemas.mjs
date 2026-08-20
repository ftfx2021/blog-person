import { readFile, readdir } from 'node:fs/promises'
import { join } from 'node:path'

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true })
  const files = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name)
    return entry.isDirectory() ? walk(path) : path
  }))
  return files.flat().filter((path) => path.endsWith('.ts'))
}

for (const file of await walk('src/main/ipc')) {
  const source = await readFile(file, 'utf8')
  if (source.includes('ipcMain.handle(') && !source.includes('.parse(')) {
    throw new Error(`IPC handler 缺少 Zod parse: ${file}`)
  }
}
console.log('IPC schema 检查通过')
