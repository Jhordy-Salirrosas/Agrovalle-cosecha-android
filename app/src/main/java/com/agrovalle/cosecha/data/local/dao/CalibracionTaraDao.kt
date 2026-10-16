package com.agrovalle.cosecha.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agrovalle.cosecha.data.local.entity.CalibracionTaraEntity

@Dao
interface CalibracionTaraDao {

    @Insert
    suspend fun insertarCalibracion(calibracion: CalibracionTaraEntity): Long

    /** Tara vigente del turno (AGV-015). */
    @Query("SELECT * FROM calibraciones ORDER BY id DESC LIMIT 1")
    suspend fun getUltimaCalibracion(): CalibracionTaraEntity?

    @Query("SELECT COUNT(*) FROM calibraciones")
    suspend fun contar(): Int
}
