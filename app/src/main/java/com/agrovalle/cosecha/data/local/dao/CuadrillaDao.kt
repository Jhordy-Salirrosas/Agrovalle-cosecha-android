package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.CuadrillaEntity

@Dao
interface CuadrillaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(cuadrilla: CuadrillaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(cuadrillas: List<CuadrillaEntity>)

    @Query("SELECT * FROM cuadrillas ORDER BY codigo")
    suspend fun listarTodas(): List<CuadrillaEntity>

    @Query("SELECT * FROM cuadrillas WHERE codigo = :codigo LIMIT 1")
    suspend fun buscarPorCodigo(codigo: String): CuadrillaEntity?

    @Query("SELECT COUNT(*) FROM cuadrillas")
    suspend fun contar(): Int
}
