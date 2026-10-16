package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.PesajeEntity

@Dao
interface PesajeDao {

    @Insert
    suspend fun insertarPesaje(pesaje: PesajeEntity): Long

    /** Acumulado diario del cosechador (AGV-004). */
    @Query(
        "SELECT SUM(peso_neto) FROM entregas_pesaje " +
            "WHERE jornalero_id = :jornaleroId AND timestamp BETWEEN :desde AND :hasta"
    )
    suspend fun getAcumuladoDiarioId(jornaleroId: Long, desde: Long, hasta: Long): Double?

    @Query("SELECT * FROM entregas_pesaje WHERE jornalero_id = :jornaleroId ORDER BY timestamp DESC")
    suspend fun getPesajesPorJornaleroId(jornaleroId: Long): List<PesajeEntity>

    @Query("SELECT * FROM entregas_pesaje WHERE parihuela_id = :parihuelaId ORDER BY timestamp")
    suspend fun getPesajesPorParihuelaId(parihuelaId: Long): List<PesajeEntity>

    @Query("SELECT COUNT(*) FROM entregas_pesaje")
    suspend fun contar(): Int
}
