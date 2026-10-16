package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity

@Dao
interface ParihuelaDao {

    @Insert
    suspend fun insertar(parihuela: ParihuelaEntity): Long

    @Update
    suspend fun actualizar(parihuela: ParihuelaEntity)

    @Query("SELECT * FROM parihuelas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ParihuelaEntity?

    /** Parihuela actualmente en curso (bloquea cambio de contexto, AGV-002). */
    @Query("SELECT * FROM parihuelas WHERE estado = 'ABIERTA' ORDER BY id DESC LIMIT 1")
    suspend fun getParihuelaAbierta(): ParihuelaEntity?

    @Query("UPDATE parihuelas SET total_jabas = total_jabas + :jabas WHERE id = :id")
    suspend fun sumarJabas(id: Long, jabas: Int)

    @Query("SELECT COUNT(*) FROM parihuelas")
    suspend fun contar(): Int
}
