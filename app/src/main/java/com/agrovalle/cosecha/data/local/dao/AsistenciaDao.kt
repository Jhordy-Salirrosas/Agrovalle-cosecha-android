package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entities.AsistenciaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre registros de asistencia.
 */
@Dao
interface AsistenciaDao {

    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha_hora LIKE :fecha || '%'")
    fun contarPorFecha(fecha: String): Flow<Int>

    @Query("SELECT * FROM asistencias ORDER BY id DESC LIMIT 1")
    fun obtenerUltimoRegistro(): Flow<AsistenciaEntity?>

    @Query("SELECT * FROM asistencias WHERE jornalero_id = :jornaleroId AND fecha_hora LIKE :fecha || '%' LIMIT 1")
    suspend fun verificarDuplicado(jornaleroId: Long, fecha: String): AsistenciaEntity?

    @Insert
    suspend fun insertar(asistencia: AsistenciaEntity): Long
}
