package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dapm.android.agrovalle.data.local.entities.CalibracionTaraEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la calibración de tara de jabas (AGV-015).
 * Se pesan 5 jabas vacías y se calcula el promedio.
 * Rango válido: 0.350 a 0.400 kg. Tolerancia: ±20 g.
 */
@Dao
interface CalibracionTaraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCalibracion(calibracion: CalibracionTaraEntity): Long

    @Query("SELECT * FROM calibraciones_tara WHERE esta_aprobado = 1 ORDER BY marca_tiempo DESC LIMIT 1")
    suspend fun obtenerUltimaCalibracionAprobada(): CalibracionTaraEntity?

    @Query("SELECT * FROM calibraciones_tara WHERE esta_aprobado = 1 ORDER BY marca_tiempo DESC LIMIT 1")
    fun observarUltimaCalibracion(): Flow<CalibracionTaraEntity?>

    @Query("SELECT tara_promedio_calculada FROM calibraciones_tara WHERE esta_aprobado = 1 ORDER BY marca_tiempo DESC LIMIT 1")
    suspend fun obtenerTaraActual(): Double?

    @Query("SELECT * FROM calibraciones_tara ORDER BY marca_tiempo DESC")
    fun obtenerHistorialCalibraciones(): Flow<List<CalibracionTaraEntity>>

    /**
     * Realiza la calibración de tara:
     * - Calcula promedio = pesoMuestra5Jabas / 5
     * - Valida rango [0.350, 0.400]
     * - Persiste el resultado con su estado de aprobación
     */
    suspend fun calibrarTara(pesoMuestra5Jabas: Double, nombreAsistente: String): CalibracionTaraEntity {
        val promedio = pesoMuestra5Jabas / 5.0
        val estaAprobado = promedio in 0.350..0.400
        val calibracion = CalibracionTaraEntity(
            pesoMuestra5Jabas = pesoMuestra5Jabas,
            taraPromedioCalculada = Math.round(promedio * 1000.0) / 1000.0,
            estaAprobado = estaAprobado,
            nombreAsistente = nombreAsistente
        )
        insertarCalibracion(calibracion)
        return calibracion
    }
}
