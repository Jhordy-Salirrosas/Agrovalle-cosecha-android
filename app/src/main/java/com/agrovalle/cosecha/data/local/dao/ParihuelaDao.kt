package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agrovalle.cosecha.data.local.entities.ParihuelaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre parihuelas.
 */
@Dao
interface ParihuelaDao {

    @Query("SELECT * FROM parihuelas WHERE estado = 'ABIERTA' LIMIT 1")
    fun obtenerParihuelaAbierta(): Flow<ParihuelaEntity?>

    @Insert
    suspend fun insertar(parihuela: ParihuelaEntity): Long

    @Update
    suspend fun actualizar(parihuela: ParihuelaEntity)

    @Query("UPDATE parihuelas SET estado = 'CERRADA', fecha_cierre = :fechaCierre WHERE estado = 'ABIERTA'")
    suspend fun cerrarParihuelaAbierta(fechaCierre: String)
}
