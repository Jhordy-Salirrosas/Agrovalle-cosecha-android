package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de incidencias.
 */
@Dao
interface IncidenciaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(incidencia: IncidenciaEntity): Long

    @Query("SELECT * FROM incidencias WHERE parihuela_id = :parihuelaId ORDER BY fecha_hora DESC")
    fun observeByParihuela(parihuelaId: Long): Flow<List<IncidenciaEntity>>

    @Query("SELECT * FROM incidencias ORDER BY fecha_hora DESC")
    fun observeAll(): Flow<List<IncidenciaEntity>>
}
