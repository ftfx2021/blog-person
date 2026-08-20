import { ipcRenderer } from 'electron'
import { z, type ZodType } from 'zod'
import { confirmationSchema, entityIdSchema } from '../../shared/contracts/common.js'
import { dashboardInputSchema, emptySchema, exportSchema, mysqlSettingsSchema, reminderSettingsSchema, restoreSchema, searchInputSchema } from '../../shared/contracts/system.js'
import { goalInputSchema,goalListSchema,goalRecordInputSchema,goalUpdateSchema,habitCheckinSchema,habitHistorySchema,habitInputSchema,habitUpdateSchema,milestoneInputSchema,milestoneToggleSchema,milestoneUpdateSchema,projectInputSchema,projectListSchema,projectStatusUpdateSchema,projectUpdateSchema,taskInputSchema,taskListSchema,taskTransitionSchema,taskUpdateSchema } from '../../shared/contracts/entities.js'

const idSchema=z.object({id:entityIdSchema})
const finishSchema=z.object({id:entityIdSchema,status:z.enum(['done','abandoned'])})

function invoke<T>(channel:string,schema:ZodType<T>,payload:T):Promise<any>{
  // Preload 在跨进程前再次校验参数；channel 只由闭包固定，渲染层不能构造任意通道。
  return ipcRenderer.invoke(channel,schema.parse(payload))
}

export const lifeSystemApi={
  goals:{list:(input:unknown={})=>invoke('goals:list',goalListSchema,input as any),get:(id:string)=>invoke('goals:get',idSchema,{id}),create:(input:unknown)=>invoke('goals:create',goalInputSchema,input),update:(input:unknown)=>invoke('goals:update',goalUpdateSchema,input),remove:(id:string)=>invoke('goals:delete',confirmationSchema,{id,confirmed:true}),finish:(input:unknown)=>invoke('goals:finish',finishSchema,input),record:(input:unknown)=>invoke('goals:record',goalRecordInputSchema,input),createMilestone:(input:unknown)=>invoke('goals:milestone:create',milestoneInputSchema,input),updateMilestone:(input:unknown)=>invoke('goals:milestone:update',milestoneUpdateSchema,input),toggleMilestone:(input:unknown)=>invoke('goals:milestone:toggle',milestoneToggleSchema,input),removeMilestone:(id:string)=>invoke('goals:milestone:delete',confirmationSchema,{id,confirmed:true})},
  projects:{list:(input:unknown={})=>invoke('projects:list',projectListSchema,input as any),get:(id:string)=>invoke('projects:get',idSchema,{id}),create:(input:unknown)=>invoke('projects:create',projectInputSchema,input),update:(input:unknown)=>invoke('projects:update',projectUpdateSchema,input),remove:(id:string)=>invoke('projects:delete',confirmationSchema,{id,confirmed:true}),updateStatus:(input:unknown)=>invoke('projects:status',projectStatusUpdateSchema,input)},
  tasks:{list:(input:unknown={})=>invoke('tasks:list',taskListSchema,input as any),get:(id:string)=>invoke('tasks:get',idSchema,{id}),create:(input:unknown)=>invoke('tasks:create',taskInputSchema,input),update:(input:unknown)=>invoke('tasks:update',taskUpdateSchema,input),remove:(id:string)=>invoke('tasks:delete',confirmationSchema,{id,confirmed:true}),transition:(input:unknown)=>invoke('tasks:transition',taskTransitionSchema,input)},
  habits:{list:()=>invoke('habits:list',emptySchema,{}),get:(id:string)=>invoke('habits:get',idSchema,{id}),create:(input:unknown)=>invoke('habits:create',habitInputSchema,input),update:(input:unknown)=>invoke('habits:update',habitUpdateSchema,input),remove:(id:string)=>invoke('habits:delete',confirmationSchema,{id,confirmed:true}),checkin:(input:unknown)=>invoke('habits:checkin',habitCheckinSchema,input),undo:(input:unknown)=>invoke('habits:undo',habitCheckinSchema,input),history:(input:unknown)=>invoke('habits:history',habitHistorySchema,input)},
  dashboard:{get:(input:unknown)=>invoke('dashboard:get',dashboardInputSchema,input)},
  search:{query:(input:unknown)=>invoke('search:query',searchInputSchema,input)},
  settings:{getMysql:()=>invoke('settings:mysql:get',emptySchema,{}),saveMysql:(input:unknown)=>invoke('settings:mysql:save',mysqlSettingsSchema,input),testMysql:(input:unknown)=>invoke('settings:mysql:test',mysqlSettingsSchema,input),health:()=>invoke('settings:mysql:health',emptySchema,{}),getReminders:()=>invoke('settings:reminders:get',emptySchema,{}),saveReminders:(input:unknown)=>invoke('settings:reminders:save',reminderSettingsSchema,input),milvusStatus:()=>invoke('settings:milvus:status',emptySchema,{})},
  backup:{create:()=>invoke('backup:create',emptySchema,{}),restore:(input:unknown)=>invoke('backup:restore',restoreSchema,input),export:(input:unknown)=>invoke('backup:export',exportSchema,input),tasks:()=>invoke('backup:tasks',emptySchema,{})}
}

export type LifeSystemApi=typeof lifeSystemApi
