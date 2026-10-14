package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecruitmentDao {
    @Query("SELECT * FROM recruitment_packs ORDER BY createdAt DESC")
    fun getAllPacks(): Flow<List<RecruitmentPackEntity>>

    @Query("SELECT * FROM recruitment_packs WHERE id = :id LIMIT 1")
    suspend fun getPackById(id: String): RecruitmentPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: RecruitmentPackEntity)

    @Update
    suspend fun updatePack(pack: RecruitmentPackEntity)

    @Delete
    suspend fun deletePack(pack: RecruitmentPackEntity)

    @Query("DELETE FROM recruitment_packs WHERE id = :id")
    suspend fun deletePackById(id: String)
}
