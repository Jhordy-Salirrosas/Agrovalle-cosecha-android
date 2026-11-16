package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de bitacora.
 */
@Dao
interface BitacoraDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bitacora: BitacoraEntity): Long

    @Query("SELECT * FROM bitacora WHERE parihuela_id = :parihuelaId ORDER BY fecha_hora DESC")
    fun observeByParihuela(parihuelaId: Long): Flow<List<BitacoraEntity>>

    @Query("SELECT * FROM bitacora ORDER BY fecha_hora DESC")
    fun observeAll(): Flow<List<BitacoraEntity>>
}
