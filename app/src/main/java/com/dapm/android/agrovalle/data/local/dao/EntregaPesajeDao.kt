package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dapm.android.agrovalle.data.local.entities.EntregaPesajeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para el registro de entregas de pesaje (AGV-003, AGV-004).
 * Regla de inmutabilidad SENASA: los registros NUNCA se borran ni editan.
 * Las correcciones se realizan mediante registros compensatorios.
 */
@Dao
interface EntregaPesajeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertarEntrega(entrega: EntregaPesajeEntity): Long

    @Query("SELECT * FROM entregas_pesaje WHERE parihuela_id = :parihuelaId AND es_registro_compensatorio = 0 ORDER BY marca_tiempo DESC")
    fun obtenerEntregasPorParihuela(parihuelaId: Long): Flow<List<EntregaPesajeEntity>>

    @Query("SELECT * FROM entregas_pesaje WHERE jornalero_id = :jornaleroId ORDER BY marca_tiempo DESC")
    fun obtenerEntregasPorJornalero(jornaleroId: Long): Flow<List<EntregaPesajeEntity>>

    @Query("SELECT * FROM entregas_pesaje ORDER BY marca_tiempo DESC LIMIT 20")
    fun obtenerUltimasEntregas(): Flow<List<EntregaPesajeEntity>>

    @Query("SELECT SUM(cantidad_jabas) FROM entregas_pesaje WHERE parihuela_id = :parihuelaId AND es_registro_compensatorio = 0")
    suspend fun contarJabasPorParihuela(parihuelaId: Long): Int?

    @Query("SELECT SUM(peso_neto) FROM entregas_pesaje WHERE parihuela_id = :parihuelaId AND es_registro_compensatorio = 0")
    suspend fun sumarPesoNetoPorParihuela(parihuelaId: Long): Double?

    @Query("SELECT * FROM entregas_pesaje WHERE esta_sincronizado = 0")
    suspend fun obtenerPendientesSincronizar(): List<EntregaPesajeEntity>

    /**
     * Guarda un pesaje validando que la parihuela no haya sido sellada.
     * Retorna el ID del registro o -1 si la parihuela no está abierta.
     */
    @Transaction
    suspend fun guardarPesaje(
        entrega: EntregaPesajeEntity,
        parihuelaDao: ParihuelaDao
    ): Long {
        val parihuela = parihuelaDao.obtenerPorId(entrega.parihuelaId)
        return if (parihuela != null && parihuela.estadoParihuela == "ABIERTA") {
            val id = insertarEntrega(entrega)
            parihuelaDao.acumularEntrega(entrega.parihuelaId, entrega.cantidadJabas, entrega.pesoNeto)
            id
        } else {
            -1L
        }
    }
}
