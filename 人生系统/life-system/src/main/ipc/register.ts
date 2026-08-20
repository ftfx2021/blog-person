import { ipcMain } from 'electron'
import type { ZodType } from 'zod'
import { toResult } from './result.js'

export function registerHandler<T>(channel:string,schema:ZodType<T>,handler:(input:T)=>Promise<unknown>):void{
  ipcMain.handle(channel,async(_event,payload)=>{
    // 每个白名单 handler 在进入业务服务前统一执行 Zod parse，拒绝任意形状参数。
    return toResult(()=>handler(schema.parse(payload)))
  })
}
