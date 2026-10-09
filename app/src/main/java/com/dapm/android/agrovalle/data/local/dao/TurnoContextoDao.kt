package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dapm.android.agrovalle.data.local.entities.TurnoContextoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión del turno de contexto activo.
 * Solo debe existir un turno con estado "ABIERTO" a la vez.
 */
@Dao
interface TurnoContextoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTurno(turno: TurnoContextoEntity): Long

    @Update
    suspend fun actualizarTurno(turno: TurnoContextoEntity)

    @Query("SELECT * FROM turnos_contexto WHERE estado_turno = 'ABIERTO' LIMIT 1")
    suspend fun obtenerTurnoAbierto(): TurnoContextoEntity?

    @Query("SELECT * FROM turnos_contexto WHERE estado_turno = 'ABIERTO' LIMIT 1")
    fun observarTurnoAbierto(): Flow<TurnoContextoEntity?>

    @Query("UPDATE turnos_contexto SET estado_turno = 'CERRADO' WHERE id = :turnoId")
    suspend fun cerrarTurno(turnoId: Long)

    @Query("UPDATE turnos_contexto SET parihuela_activa_id = :parihuelaId WHERE id = :turnoId")
    suspend fun asignarParihuelaActiva(turnoId: Long, parihuelaId: Long)
}
