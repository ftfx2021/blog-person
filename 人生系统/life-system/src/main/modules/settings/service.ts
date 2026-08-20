import { readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { app, safeStorage } from 'electron'
import mysql from 'mysql2/promise'
import type { MysqlSettings, ReminderSettings } from '../../../shared/contracts/system.js'
import { configurePool, requirePool } from '../../infrastructure/db/pool.js'
import { runMigrations } from '../../infrastructure/migrations/runner.js'
import { applicationPaths } from '../../infrastructure/filesystem/paths.js'
import { utcNow } from '../common/database.js'

interface PersistedConnection extends Omit<MysqlSettings,'password'>{encryptedPassword:string}
const configurationPath=()=>join(app.getPath('userData'),'data','mysql-connection.json')

async function persistConnection(input:MysqlSettings):Promise<void>{if(!safeStorage.isEncryptionAvailable())throw Object.assign(new Error('系统安全存储当前不可用，不能保存密码'),{code:'FILESYSTEM_ERROR'});await applicationPaths();const persisted:PersistedConnection={...input,encryptedPassword:safeStorage.encryptString(input.password).toString('base64')};delete (persisted as any).password;await writeFile(configurationPath(),JSON.stringify(persisted,null,2),'utf8')}
export async function loadConnection():Promise<MysqlSettings|null>{try{const persisted=JSON.parse(await readFile(configurationPath(),'utf8')) as PersistedConnection;return{...persisted,password:safeStorage.decryptString(Buffer.from(persisted.encryptedPassword,'base64'))}}catch{return null}}
export const settingsService={
  saveMysql:async(input:MysqlSettings)=>{await persistConnection(input);const pool=await configurePool(input);const migrations=await runMigrations(input,app.isPackaged?join(process.resourcesPath,'migrations'):join(app.getAppPath(),'migrations'));await pool.query('INSERT INTO app_setting (`key`,value_json,updated_at) VALUES (\'mysql.connection\',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)',[JSON.stringify({host:input.host,port:input.port,user:input.user,database:input.database,connectTimeout:input.connectTimeout,passwordEncrypted:true}),utcNow()]);return{saved:true,migrations}},
  getMysql:async()=>{const value=await loadConnection();return value?{...value,password:value.password?'********':''}:null},
  testMysql:async(input:MysqlSettings)=>{const started=Date.now();const connection=await mysql.createConnection({...input,timezone:'Z'});try{const[rows]=await connection.query<any[]>('SELECT VERSION() AS version,1 AS healthy');return{healthy:true,latencyMs:Date.now()-started,version:rows[0]!.version}}finally{await connection.end()}},
  health:async()=>{const started=Date.now();const[rows]=await requirePool().query<any[]>('SELECT VERSION() AS version');return{healthy:true,latencyMs:Date.now()-started,version:rows[0]!.version}},
  saveReminders:async(input:ReminderSettings)=>{await requirePool().query('INSERT INTO app_setting (`key`,value_json,updated_at) VALUES (\'notify.preferences\',?,?) ON DUPLICATE KEY UPDATE value_json=VALUES(value_json),updated_at=VALUES(updated_at)',[JSON.stringify(input),utcNow()]);return input},
  getReminders:async()=>{const[rows]=await requirePool().query<any[]>('SELECT value_json AS valueJson FROM app_setting WHERE `key`=\'notify.preferences\'');return rows[0]?.valueJson??{criticalEnabled:true,periodicEnabled:true,recommendationEnabled:false,frequency:'realtime',aggregationMinutes:30,readRetentionDays:30,recommendationRequiresConfirmation:true}},
  milvusStatus:async()=>({enabled:false,status:'P1_DISABLED',message:'未启用（P1）'})
}
