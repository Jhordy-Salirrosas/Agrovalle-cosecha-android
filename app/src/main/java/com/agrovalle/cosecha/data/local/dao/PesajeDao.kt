package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de pesajes.
 */
@Dao
interface PesajeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pesaje: PesajeEntity): Long

    @Update
    suspend fun update(pesaje: PesajeEntity)

    @Query("SELECT * FROM pesajes WHERE id = :id")
    suspend fun getById(id: Long): PesajeEntity?

    @Query("SELECT * FROM pesajes WHERE parihuela_id = :parihuelaId ORDER BY fecha_hora DESC")
    fun observeByParihuela(parihuelaId: Long): Flow<List<PesajeEntity>>

    @Query("SELECT * FROM pesajes ORDER BY fecha_hora DESC")
    fun observeAll(): Flow<List<PesajeEntity>>
}
