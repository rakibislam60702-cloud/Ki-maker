package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogsFlow(): Flow<List<SessionLogEntity>>

    @Insert
    suspend fun insertLog(log: SessionLogEntity)

    @Query("DELETE FROM session_logs")
    suspend fun clearLogs()
}
