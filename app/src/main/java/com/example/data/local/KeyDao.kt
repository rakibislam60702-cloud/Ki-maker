package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM license_keys ORDER BY createdAt DESC")
    fun getAllKeysFlow(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM license_keys ORDER BY createdAt DESC")
    suspend fun getAllKeys(): List<KeyEntity>

    @Query("SELECT * FROM license_keys WHERE keyCode = :keyCode LIMIT 1")
    suspend fun getKey(keyCode: String): KeyEntity?

    @Query("SELECT * FROM license_keys WHERE keyCode = :keyCode LIMIT 1")
    fun getKeyFlow(keyCode: String): Flow<KeyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(key: KeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<KeyEntity>)

    @Update
    suspend fun update(key: KeyEntity)

    @Delete
    suspend fun delete(key: KeyEntity)

    @Query("DELETE FROM license_keys WHERE keyCode = :keyCode")
    suspend fun deleteByKey(keyCode: String)

    @Query("DELETE FROM license_keys")
    suspend fun clearAll()
}
