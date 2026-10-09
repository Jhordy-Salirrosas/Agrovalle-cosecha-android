package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dapm.android.agrovalle.data.local.entities.JornaleroEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre la tabla de jornaleros.
 * Provee búsquedas reactivas por QR (fotocheck) y DNI.
 */
@Dao
interface JornaleroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarJornalero(jornalero: JornaleroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(jornaleros: List<JornaleroEntity>)

    @Query("SELECT * FROM jornaleros WHERE codigo_qr = :codigoQr AND esta_activo = 1 LIMIT 1")
    suspend fun obtenerJornaleroPorQr(codigoQr: String): JornaleroEntity?

    @Query("SELECT * FROM jornaleros WHERE dni = :dni AND esta_activo = 1 LIMIT 1")
    suspend fun buscarJornaleroPorDni(dni: String): JornaleroEntity?

    @Query("SELECT * FROM jornaleros WHERE dni LIKE :dniBusqueda || '%' AND esta_activo = 1 ORDER BY apellidos ASC")
    fun buscarJornalerosPorDniParcial(dniBusqueda: String): Flow<List<JornaleroEntity>>

    @Query("SELECT * FROM jornaleros WHERE esta_activo = 1 ORDER BY apellidos ASC, nombres ASC")
    fun obtenerTodosActivos(): Flow<List<JornaleroEntity>>

    @Query("SELECT * FROM jornaleros WHERE codigo_cuadrilla = :codigoCuadrilla AND esta_activo = 1 ORDER BY apellidos ASC")
    fun obtenerPorCuadrilla(codigoCuadrilla: String): Flow<List<JornaleroEntity>>

    @Query("SELECT COUNT(*) FROM jornaleros WHERE esta_activo = 1")
    suspend fun contarActivos(): Int
}
