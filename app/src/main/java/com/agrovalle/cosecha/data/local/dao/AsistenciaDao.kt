package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.AsistenciaEntity

@Dao
interface AsistenciaDao {

    /**
     * Devuelve el id insertado o -1 si ya existía una asistencia del mismo
     * jornalero en la misma fecha (índice único jornalero_id + fecha).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(asistencia: AsistenciaEntity): Long

    @Query("SELECT * FROM asistencias WHERE jornalero_id = :jornaleroId AND fecha = :fecha LIMIT 1")
    suspend fun buscarPorJornaleroYFecha(jornaleroId: Long, fecha: String): AsistenciaEntity?

    /** Contador de la cuadrilla para el turno (0/35 mostrado en pantalla). */
    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha = :fecha")
    suspend fun contarPorFecha(fecha: String): Int

    /** Control de ingresos manuales (AGV-016, escenario 2). */
    @Query("SELECT COUNT(*) FROM asistencias WHERE fecha = :fecha AND metodo = 'DNI_MANUAL'")
    suspend fun contarIngresosManualesPorFecha(fecha: String): Int

    @Query("SELECT * FROM asistencias WHERE fecha = :fecha ORDER BY hora_ingreso DESC")
    suspend fun listarPorFecha(fecha: String): List<AsistenciaEntity>
}
