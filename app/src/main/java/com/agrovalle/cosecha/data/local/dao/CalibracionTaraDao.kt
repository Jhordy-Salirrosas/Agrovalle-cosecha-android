package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entities.CalibracionTaraEntity

/**
 * DAO para operaciones sobre calibraciones de tara.
 */
@Dao
interface CalibracionTaraDao {

    @Query("SELECT * FROM calibraciones_tara ORDER BY id DESC LIMIT 1")
    suspend fun obtenerUltimaCalibracion(): CalibracionTaraEntity?

    @Insert
    suspend fun insertar(calibracion: CalibracionTaraEntity): Long
}
