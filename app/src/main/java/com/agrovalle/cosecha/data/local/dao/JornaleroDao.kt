package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.JornaleroEntity

@Dao
interface JornaleroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(jornalero: JornaleroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(jornaleros: List<JornaleroEntity>)

    /** Lectura del fotocheck por QR / Code 128 (AGV-001). */
    @Query("SELECT * FROM jornaleros WHERE codigo_qr = :codigoQr LIMIT 1")
    suspend fun buscarPorQr(codigoQr: String): JornaleroEntity?

    @Query("SELECT * FROM jornaleros WHERE dni = :dni LIMIT 1")
    suspend fun buscarPorDni(dni: String): JornaleroEntity?

    /**
     * Plan B de AGV-016: busca en el padrón local los jornaleros cuyo DNI
     * termina en los últimos dígitos ingresados por el capataz.
     */
    @Query("SELECT * FROM jornaleros WHERE dni LIKE '%' || :ultimosDigitos ORDER BY dni LIMIT 15")
    suspend fun buscarPorUltimosDigitosDni(ultimosDigitos: String): List<JornaleroEntity>

    @Query("SELECT * FROM jornaleros WHERE cuadrilla_codigo = :codigoCuadrilla ORDER BY apellidos")
    suspend fun listarPorCuadrilla(codigoCuadrilla: String): List<JornaleroEntity>

    @Query("SELECT COUNT(*) FROM jornaleros")
    suspend fun contar(): Int
}
