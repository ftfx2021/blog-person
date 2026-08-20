import type { PoolConnection } from 'mysql2/promise'
import { requirePool } from './pool.js'

export async function inTransaction<T>(operation: (connection: PoolConnection) => Promise<T>): Promise<T> {
  const connection = await requirePool().getConnection()
  try {
    await connection.beginTransaction()
    const result = await operation(connection)
    await connection.commit()
    return result
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    connection.release()
  }
}
