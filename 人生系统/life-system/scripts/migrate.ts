import { resolve } from 'node:path'
import { parseMysqlUrl } from '../src/main/infrastructure/db/configuration.js'
import { runMigrations } from '../src/main/infrastructure/migrations/runner.js'

const connectionUrl = process.env.LIFE_SYSTEM_MYSQL_URL
if (!connectionUrl) throw new Error('请设置 LIFE_SYSTEM_MYSQL_URL，例如 mysql://user:password@127.0.0.1:3306/life_system')

const result = await runMigrations(parseMysqlUrl(connectionUrl), resolve('migrations'))
console.log(`已应用：${result.applied.join(', ') || '无'}；已跳过：${result.skipped.join(', ') || '无'}`)
