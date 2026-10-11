package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entities.JornaleroEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre jornaleros.
 * TODO [Coder 1]: Implementar sincronización con backend remoto.
 */
@Dao
interface JornaleroDao {

    @Query("SELECT * FROM jornaleros WHERE esta_activo = 1")
    fun obtenerTodos(): Flow<List<JornaleroEntity>>

    @Query("SELECT * FROM jornaleros WHERE dni LIKE '%' || :digitos || '%' AND esta_activo = 1")
    fun buscarPorDni(digitos: String): Flow<List<JornaleroEntity>>

    @Query("SELECT * FROM jornaleros WHERE codigo_qr = :codigoQr AND esta_activo = 1 LIMIT 1")
    suspend fun buscarPorQr(codigoQr: String): JornaleroEntity?

    @Query("SELECT * FROM jornaleros WHERE id = :id")
    suspend fun obtenerPorId(id: Long): JornaleroEntity?

    @Insert
    suspend fun insertar(jornalero: JornaleroEntity): Long

    @Insert
    suspend fun insertarTodos(jornaleros: List<JornaleroEntity>)
}
