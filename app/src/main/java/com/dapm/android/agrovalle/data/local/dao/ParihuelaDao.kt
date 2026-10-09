package com.dapm.android.agrovalle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dapm.android.agrovalle.data.local.entities.ParihuelaEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestión de parihuelas (pallets) de acopio.
 * Regla de segregación: no mezclar variedades ni cuarteles.
 * Estándar de estiba: 100-120 jabas por parihuela.
 */
@Dao
interface ParihuelaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarParihuela(parihuela: ParihuelaEntity): Long

    @Update
    suspend fun actualizarParihuela(parihuela: ParihuelaEntity)

    @Query("SELECT * FROM parihuelas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ParihuelaEntity?

    @Query("SELECT * FROM parihuelas WHERE estado_parihuela = 'ABIERTA' LIMIT 1")
    suspend fun obtenerParihuelaAbierta(): ParihuelaEntity?

    @Query("SELECT * FROM parihuelas WHERE estado_parihuela = 'ABIERTA' LIMIT 1")
    fun observarParihuelaAbierta(): Flow<ParihuelaEntity?>

    @Query("SELECT * FROM parihuelas ORDER BY hora_apertura DESC")
    fun obtenerTodas(): Flow<List<ParihuelaEntity>>

    /**
     * Sella la parihuela (cambia estado a SELLADA y registra hora).
     */
    @Query("UPDATE parihuelas SET estado_parihuela = 'SELLADA', hora_sellado = :horaSellado WHERE id = :parihuelaId")
    suspend fun sellarParihuela(parihuelaId: Long, horaSellado: Long = System.currentTimeMillis())

    /**
     * Abre una nueva parihuela verificando que no haya otra abierta.
     * Retorna el ID de la nueva parihuela o -1 si ya hay una abierta.
     */
    @Transaction
    suspend fun abrirParihuela(parihuela: ParihuelaEntity): Long {
        val abierta = obtenerParihuelaAbierta()
        return if (abierta == null) {
            insertarParihuela(parihuela)
        } else {
            -1L
        }
    }

    /**
     * Acumula jabas y peso neto en la parihuela activa.
     */
    @Query("UPDATE parihuelas SET total_jabas = total_jabas + :jabas, peso_neto_total = peso_neto_total + :pesoNeto WHERE id = :parihuelaId")
    suspend fun acumularEntrega(parihuelaId: Long, jabas: Int, pesoNeto: Double)
}
