package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de parihuelas.
 */
@Dao
interface ParihuelaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(parihuela: ParihuelaEntity): Long

    @Update
    suspend fun update(parihuela: ParihuelaEntity)

    @Query("SELECT * FROM parihuelas WHERE id = :id")
    suspend fun getById(id: Long): ParihuelaEntity?

    @Query("SELECT * FROM parihuelas WHERE codigo = :codigo LIMIT 1")
    suspend fun getByCodigo(codigo: String): ParihuelaEntity?

    @Query("SELECT * FROM parihuelas WHERE estado = :estado ORDER BY hora_inicio DESC")
    fun observeByEstado(estado: EstadoParihuela): Flow<List<ParihuelaEntity>>

    @Query("SELECT * FROM parihuelas ORDER BY hora_inicio DESC")
    fun observeAll(): Flow<List<ParihuelaEntity>>
}
