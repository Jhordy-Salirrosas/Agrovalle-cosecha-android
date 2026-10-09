package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dapm.android.agrovalle.data.local.entities.AsistenciaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para el registro y consulta de asistencias diarias.
 * Incluye validación de duplicados por jornalero+fecha.
 */
@Dao
interface AsistenciaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registrarAsistencia(asistencia: AsistenciaEntity): Long

    @Query("SELECT * FROM asistencias WHERE fecha = :fecha ORDER BY marca_tiempo DESC")
    fun obtenerAsistenciasPorFecha(fecha: String): Flow<List<AsistenciaEntity>>

    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha = :fecha")
    fun contarAsistenciasDelDia(fecha: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM asistencias WHERE jornalero_id = :jornaleroId AND fecha = :fecha)")
    suspend fun existeAsistencia(jornaleroId: Long, fecha: String): Boolean

    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha = :fecha AND estado_asistencia = 'PUNTUAL'")
    fun contarPuntuales(fecha: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha = :fecha AND estado_asistencia = 'TARDANZA'")
    fun contarTardanzas(fecha: String): Flow<Int>

    @Query("SELECT * FROM asistencias WHERE esta_sincronizado = 0")
    suspend fun obtenerPendientesSincronizar(): List<AsistenciaEntity>

    /**
     * Registra asistencia solo si el jornalero no tiene registro previo en la fecha.
     * Retorna el ID insertado o -1 si ya existía.
     */
    @Transaction
    suspend fun registrarAsistenciaSiNoExiste(asistencia: AsistenciaEntity): Long {
        val existe = existeAsistencia(asistencia.jornaleroId, asistencia.fecha)
        return if (!existe) {
            registrarAsistencia(asistencia)
        } else {
            -1L
        }
    }
}
