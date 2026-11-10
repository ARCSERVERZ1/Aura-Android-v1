package com.familysafety.guard.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationEntity): Long

    @Query("SELECT * FROM offline_location_queue ORDER BY timestamp ASC LIMIT 50")
    suspend fun getQueuedRequests(): List<LocationEntity>

    @Query("SELECT * FROM offline_location_queue ORDER BY timestamp DESC")
    fun observeQueuedRequests(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM offline_location_queue")
    suspend fun getQueueCount(): Int

    @Query("SELECT COUNT(*) FROM offline_location_queue")
    fun observeQueueCount(): Flow<Int>

    @Delete
    suspend fun delete(entity: LocationEntity)

    @Query("DELETE FROM offline_location_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM offline_location_queue")
    suspend fun clearAll()

    /**
     * Requirement: Store up to 50 requests in local storage.
     * If queue exceeds 50, remove oldest entries (FIFO queue pruning).
     */
    @Transaction
    suspend fun insertWithQueueCap(entity: LocationEntity, maxLimit: Int = 50) {
        insert(entity)
        val currentCount = getQueueCount()
        if (currentCount > maxLimit) {
            val excessCount = currentCount - maxLimit
            pruneOldestEntries(excessCount)
        }
    }

    @Query("""
        DELETE FROM offline_location_queue 
        WHERE id IN (
            SELECT id FROM offline_location_queue 
            ORDER BY timestamp ASC 
            LIMIT :count
        )
    """)
    suspend fun pruneOldestEntries(count: Int)
}