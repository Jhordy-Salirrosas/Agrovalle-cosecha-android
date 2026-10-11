package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entities.PesajeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre pesajes.
 */
@Dao
interface PesajeDao {

    @Query("SELECT * FROM pesajes WHERE jornalero_id = :jornaleroId AND fecha_hora LIKE :fecha || '%'")
    fun obtenerPesajesDelDia(jornaleroId: Long, fecha: String): Flow<List<PesajeEntity>>

    @Query("SELECT COALESCE(SUM(peso_neto), 0.0) FROM pesajes WHERE jornalero_id = :jornaleroId AND fecha_hora LIKE :fecha || '%'")
    suspend fun obtenerAcumuladoDiario(jornaleroId: Long, fecha: String): Double

    @Insert
    suspend fun insertar(pesaje: PesajeEntity): Long
}
