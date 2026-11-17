package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.ReasignacionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de reasignaciones de jornaleros.
 */
@Dao
interface ReasignacionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reasignacion: ReasignacionEntity): Long

    @Query("SELECT * FROM reasignaciones ORDER BY hora DESC, id DESC")
    fun observeAll(): Flow<List<ReasignacionEntity>>

    @Query("SELECT * FROM reasignaciones WHERE jornalero = :jornalero ORDER BY hora DESC, id DESC LIMIT 1")
    suspend fun getUltimaPorJornalero(jornalero: String): ReasignacionEntity?
}
