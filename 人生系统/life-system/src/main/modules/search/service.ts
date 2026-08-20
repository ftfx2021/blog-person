import type { RowDataPacket } from 'mysql2/promise'
import { inTransaction } from '../../infrastructure/db/transaction.js'

const allowedTypes=['goal','project','task','habit','document'] as const
export const searchService={
  search:(input:{keyword:string;types:string[];tags:string[];status?:string})=>inTransaction(async(connection)=>{
    const selected=input.types.length?allowedTypes.filter(type=>input.types.includes(type)):[...allowedTypes]
    const results:Record<string,unknown[]>={}
    for(const type of selected){
      const titleColumn=type==='habit'?'name':'title'
      const clauses=[`${titleColumn} LIKE ?`];const values:unknown[]=[`%${input.keyword}%`]
      if(input.status&&['goal','project','task'].includes(type)){clauses.push('status=?');values.push(input.status)}
      if(input.tags.length&&type!=='document'){clauses.push(`EXISTS (SELECT 1 FROM entity_tag et JOIN tag tg ON tg.id=et.tag_id WHERE et.entity_type=? AND et.entity_id=${type}.id AND tg.name IN (${input.tags.map(()=>'?').join(',')}))`);values.push(type,...input.tags)}
      // document 使用 ngram FULLTEXT；P0 其他实体按标题 LIKE，并明确不查询 mood_record。
      const sql=type==='document'
        ? 'SELECT id,title,doc_type AS subtype,updated_at AS updatedAt FROM document WHERE deleted_at IS NULL AND MATCH(title,raw_text) AGAINST (? IN NATURAL LANGUAGE MODE) ORDER BY updated_at DESC LIMIT 50'
        : `SELECT id,${titleColumn} AS title${['goal','project','task'].includes(type)?',status':''},updated_at AS updatedAt FROM ${type} WHERE ${clauses.join(' AND ')} ORDER BY updated_at DESC LIMIT 50`
      const[rows]=await connection.query<RowDataPacket[]>(sql,type==='document'?[input.keyword]:values)
      results[type]=rows
    }
    return results
  })
}
