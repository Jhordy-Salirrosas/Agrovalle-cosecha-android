package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Resultado de registrar jabas en una parihuela.
 */
data class ResultadoRegistroJabas(
    val parihuela: ParihuelaEntity,
    val nuevaParihuela: ParihuelaEntity? = null,
    val selladaPorLimite: Boolean = false
)

/**
 * Repositorio que encapsula el ciclo de vida de las parihuelas (Sprint 2).
 */
class ParihuelaRepository(
    private val parihuelaDao: ParihuelaDao,
    private val bitacoraDao: BitacoraDao
) {

    companion object {
        const val LIMITE_JABAS = 120
        const val EVENTO_SELLADO_LIMITE = "SELLADO_LIMITE"
        const val EVENTO_SELLADO_ANTICIPADO = "SELLADO_ANTICIPADO"
    }

    private val secuenciaCodigo = AtomicInteger(0)

    /**
     * Abre una nueva parihuela generando un código único.
     */
    suspend fun abrirNuevaParihuela(
        variedad: String,
        cuartel: String,
        ahora: Date = Date()
    ): ParihuelaEntity {
        val codigo = generarCodigoUnico(ahora)
        val nueva = ParihuelaEntity(
            codigo = codigo,
            estado = EstadoParihuela.ABIERTA,
            variedad = variedad,
            cuartel = cuartel,
            conteoJabas = 0,
            pisos = 0,
            horaInicio = ahora
        )
        val id = parihuelaDao.insert(nueva)
        return nueva.copy(id = id)
    }

    /**
     * Incrementa el conteo de jabas de una parihuela abierta.
     * Si alcanza [LIMITE_JABAS] (120), la sella automáticamente, registra en bitácora
     * y abre la siguiente parihuela con un código distinto.
     */
    suspend fun registrarJabas(
        parihuelaId: Long,
        cantidad: Int = 1,
        ahora: Date = Date()
    ): ResultadoRegistroJabas {
        require(cantidad > 0) { "La cantidad de jabas debe ser mayor a 0" }

        val actual = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")

        check(actual.estado == EstadoParihuela.ABIERTA) {
            "La parihuela ${actual.codigo} ya está sellada y no admite cambios de conteo"
        }

        val nuevoConteo = actual.conteoJabas + cantidad
        if (nuevoConteo >= LIMITE_JABAS) {
            val sellada = actual.copy(
                conteoJabas = nuevoConteo,
                estado = EstadoParihuela.SELLADA,
                horaSellado = ahora
            )
            parihuelaDao.update(sellada)

            bitacoraDao.insert(
                BitacoraEntity(
                    parihuelaId = sellada.id,
                    evento = EVENTO_SELLADO_LIMITE,
                    detalle = "Sellado automático al alcanzar $nuevoConteo jabas",
                    fechaHora = ahora
                )
            )

            val siguiente = abrirNuevaParihuela(
                variedad = actual.variedad,
                cuartel = actual.cuartel,
                ahora = ahora
            )

            return ResultadoRegistroJabas(
                parihuela = sellada,
                nuevaParihuela = siguiente,
                selladaPorLimite = true
            )
        }

        val actualizada = actual.copy(conteoJabas = nuevoConteo)
        parihuelaDao.update(actualizada)
        return ResultadoRegistroJabas(
            parihuela = actualizada,
            nuevaParihuela = null,
            selladaPorLimite = false
        )
    }

    /**
     * Sella anticipadamente una parihuela abierta exigiendo un motivo obligatorio.
     */
    suspend fun sellarAnticipadamente(
        parihuelaId: Long,
        motivo: String,
        ahora: Date = Date()
    ): ParihuelaEntity {
        val motivoLimpio = motivo.trim()
        require(motivoLimpio.isNotEmpty()) {
            "El motivo de sellado anticipado es obligatorio"
        }

        val actual = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")

        check(actual.estado == EstadoParihuela.ABIERTA) {
            "La parihuela ${actual.codigo} ya está sellada"
        }

        val sellada = actual.copy(
            estado = EstadoParihuela.SELLADA_ANTICIPADA,
            horaSellado = ahora,
            motivoSelladoAnticipado = motivoLimpio
        )
        parihuelaDao.update(sellada)

        bitacoraDao.insert(
            BitacoraEntity(
                parihuelaId = sellada.id,
                evento = EVENTO_SELLADO_ANTICIPADO,
                detalle = motivoLimpio,
                fechaHora = ahora
            )
        )

        return sellada
    }

    private suspend fun generarCodigoUnico(ahora: Date): String {
        var codigo: String
        do {
            val seq = secuenciaCodigo.incrementAndGet()
            codigo = "PAR-${ahora.time}-${seq.toString().padStart(4, '0')}"
        } while (parihuelaDao.getByCodigo(codigo) != null)
        return codigo
    }
}
