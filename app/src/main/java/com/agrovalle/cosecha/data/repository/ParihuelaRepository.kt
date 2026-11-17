package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Estados derivados del tiempo de espera de una parihuela abierta.
 */
enum class EstadoTiempoEspera {
    EN_ESPERA,
    POR_VENCER,
    EXCEDIDA
}

/**
 * Tiempo restante y estado de espera calculados desde la hora de inicio en BD.
 */
data class EstadoTiempoParihuela(
    val parihuelaId: Long,
    val restanteMs: Long,
    val estado: EstadoTiempoEspera
)

/**
 * Datos completos de origen de una parihuela consultada por su código QR.
 */
data class OrigenParihuela(
    val codigo: String,
    val variedad: String,
    val cuartel: String,
    val conteoJabas: Int,
    val pisos: Int,
    val horaInicio: Date,
    val horaSellado: Date?,
    val estado: EstadoParihuela,
    val motivoSelladoAnticipado: String?
)

/**
 * Resultado de consultar el origen de una parihuela por código.
 */
sealed interface ConsultaOrigenParihuela {
    data class Encontrado(val origen: OrigenParihuela) : ConsultaOrigenParihuela
    data object NoEncontrado : ConsultaOrigenParihuela
}

/**
 * Parihuela abierta con su estado de tiempo de espera calculado.
 */
data class ParihuelaAbiertaResumen(
    val parihuela: ParihuelaEntity,
    val tiempoEspera: EstadoTiempoParihuela
)

/**
 * Resumen observable para el panel del capataz.
 */
data class ResumenPanelCapataz(
    val totalAbiertas: Int = 0,
    val totalSelladas: Int = 0,
    val totalSelladasAnticipadas: Int = 0,
    val totalJabas: Int = 0,
    val jabasPorVariedad: Map<String, Int> = emptyMap(),
    val jabasPorCuartel: Map<String, Int> = emptyMap(),
    val abiertasPorUrgencia: List<ParihuelaAbiertaResumen> = emptyList()
)

/**
 * Avance observable de la parihuela abierta.
 */
data class AvanceParihuela(
    val parihuelaId: Long,
    val jabasActuales: Int,
    val faltanPara100: Int,
    val faltanPara120: Int,
    val porcentajeHacia120: Int
)

/**
 * Resultado de registrar un pesaje sobre la parihuela abierta.
 */
sealed interface ResultadoPesaje {
    data class Registrado(
        val pesaje: PesajeEntity,
        val resultadoJabas: ResultadoRegistroJabas
    ) : ResultadoPesaje

    data class RechazadoPorMezcla(val incidencia: IncidenciaEntity) : ResultadoPesaje
    data class RechazadoPorCapacidad(
        val parihuela: ParihuelaEntity,
        val jabasFaltantes: Int
    ) : ResultadoPesaje
    data object SinParihuelaAbierta : ResultadoPesaje
}

/**
 * Resultado de registrar jabas en una parihuela.
 */
data class ResultadoRegistroJabas(
    val parihuela: ParihuelaEntity,
    val nuevaParihuela: ParihuelaEntity? = null,
    val selladaPorLimite: Boolean = false,
    val rechazadoPorMezcla: Boolean = false,
    val rechazadoPorCapacidad: Boolean = false,
    val incidenciaGenerada: IncidenciaEntity? = null
)

/**
 * Repositorio que encapsula el ciclo de vida de las parihuelas (Sprint 2).
 */
class ParihuelaRepository(
    private val parihuelaDao: ParihuelaDao,
    private val bitacoraDao: BitacoraDao,
    private val incidenciaDao: IncidenciaDao,
    private val pesajeDao: PesajeDao
) {

    companion object {
        const val LIMITE_JABAS = 120
        const val LIMITE_ESPERA_MS = 30L * 60L * 1000L
        const val UMBRAL_POR_VENCER_MS = 5L * 60L * 1000L
        const val EVENTO_SELLADO_LIMITE = "SELLADO_LIMITE"
        const val EVENTO_SELLADO_ANTICIPADO = "SELLADO_ANTICIPADO"
        const val EVENTO_INTENTO_EDICION = "INTENTO_EDICION"
        const val EVENTO_ALERTA_POR_VENCER = "ALERTA_POR_VENCER"
        const val EVENTO_RETENCION_EXCEDIDA = "RETENCION_EXCEDIDA"
        const val TIPO_MEZCLA_VARIEDAD = "MEZCLA_VARIEDAD"
        const val TIPO_MEZCLA_CUARTEL = "MEZCLA_CUARTEL"

        /**
         * Calcula la demora en milisegundos hasta el momento exacto en que la parihuela
         * entrará en POR_VENCER (horaInicio + LIMITE_ESPERA_MS - UMBRAL_POR_VENCER_MS).
         * Si el momento ya pasó, devuelve 0L para ejecución inmediata.
         */
        fun calcularDemoraAlertaExactaMs(horaInicio: Date, ahora: Date = Date()): Long {
            val momentoAlerta = horaInicio.time + LIMITE_ESPERA_MS - UMBRAL_POR_VENCER_MS
            return (momentoAlerta - ahora.time).coerceAtLeast(0L)
        }
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
     * Incrementa el conteo de jabas de una parihuela abierta validando que no exista
     * mezcla de variedad ni de cuartel.
     * Si hay diferencia, rechaza el registro sin alterar el conteo y guarda una incidencia.
     * Si coincide y alcanza [LIMITE_JABAS] (120), la sella automáticamente, registra en bitácora
     * y abre la siguiente parihuela con un código distinto.
     */
    suspend fun registrarJabas(
        parihuelaId: Long,
        cantidad: Int = 1,
        variedad: String,
        cuartel: String,
        ahora: Date = Date()
    ): ResultadoRegistroJabas {
        require(cantidad > 0) { "La cantidad de jabas debe ser mayor a 0" }

        val actual = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")

        if (actual.estado != EstadoParihuela.ABIERTA) {
            bitacoraDao.insert(
                BitacoraEntity(
                    parihuelaId = actual.id,
                    evento = EVENTO_INTENTO_EDICION,
                    detalle = "Intento de registrar $cantidad jabas sobre parihuela en estado ${actual.estado}",
                    fechaHora = ahora
                )
            )
            throw IllegalStateException(
                "La parihuela ${actual.codigo} ya está sellada y no admite cambios de conteo"
            )
        }

        val variedadLimpia = variedad.trim()
        val cuartelLimpio = cuartel.trim()
        require(variedadLimpia.isNotEmpty()) { "La variedad del pesaje es obligatoria" }
        require(cuartelLimpio.isNotEmpty()) { "El cuartel del pesaje es obligatorio" }

        val tipoMezcla = when {
            !variedadLimpia.equals(actual.variedad.trim(), ignoreCase = true) -> TIPO_MEZCLA_VARIEDAD
            !cuartelLimpio.equals(actual.cuartel.trim(), ignoreCase = true) -> TIPO_MEZCLA_CUARTEL
            else -> null
        }

        if (tipoMezcla != null) {
            val detalle = if (tipoMezcla == TIPO_MEZCLA_VARIEDAD) {
                "Variedad entrante '$variedadLimpia' difiere de '${actual.variedad}'"
            } else {
                "Cuartel entrante '$cuartelLimpio' difiere de '${actual.cuartel}'"
            }
            val incidencia = IncidenciaEntity(
                parihuelaId = actual.id,
                tipo = tipoMezcla,
                detalle = detalle,
                fechaHora = ahora
            )
            val idIncidencia = incidenciaDao.insert(incidencia)
            return ResultadoRegistroJabas(
                parihuela = actual,
                rechazadoPorMezcla = true,
                incidenciaGenerada = incidencia.copy(id = idIncidencia)
            )
        }

        val nuevoConteo = actual.conteoJabas + cantidad
        if (nuevoConteo > LIMITE_JABAS) {
            return ResultadoRegistroJabas(
                parihuela = actual,
                rechazadoPorCapacidad = true
            )
        }

        if (nuevoConteo == LIMITE_JABAS) {
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

        if (actual.estado != EstadoParihuela.ABIERTA) {
            bitacoraDao.insert(
                BitacoraEntity(
                    parihuelaId = actual.id,
                    evento = EVENTO_INTENTO_EDICION,
                    detalle = "Intento de sellado anticipado sobre parihuela en estado ${actual.estado}",
                    fechaHora = ahora
                )
            )
            throw IllegalStateException("La parihuela ${actual.codigo} ya está sellada")
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

    /**
     * Expone la lista observable de incidencias vinculadas a una parihuela.
     */
    fun observarIncidenciasPorParihuela(parihuelaId: Long): Flow<List<IncidenciaEntity>> =
        incidenciaDao.observeByParihuela(parihuelaId)

    /**
     * Expone todas las incidencias registradas ordenadas por fecha descendente.
     */
    fun observarTodasLasIncidencias(): Flow<List<IncidenciaEntity>> =
        incidenciaDao.observeAll()

    /**
     * Calcula el tiempo restante y el estado de espera a partir de [ParihuelaEntity.horaInicio].
     */
    fun calcularTiempoEspera(
        parihuela: ParihuelaEntity,
        ahora: Date = Date()
    ): EstadoTiempoParihuela {
        val transcurridoMs = (ahora.time - parihuela.horaInicio.time).coerceAtLeast(0L)
        val restanteSinAcotar = LIMITE_ESPERA_MS - transcurridoMs
        val estado = when {
            restanteSinAcotar <= 0L -> EstadoTiempoEspera.EXCEDIDA
            restanteSinAcotar <= UMBRAL_POR_VENCER_MS -> EstadoTiempoEspera.POR_VENCER
            else -> EstadoTiempoEspera.EN_ESPERA
        }
        return EstadoTiempoParihuela(
            parihuelaId = parihuela.id,
            restanteMs = restanteSinAcotar.coerceAtLeast(0L),
            estado = estado
        )
    }

    /**
     * Obtiene el estado de tiempo de espera leyendo la parihuela directamente desde la BD.
     */
    suspend fun obtenerEstadoTiempoParihuela(
        parihuelaId: Long,
        ahora: Date = Date()
    ): EstadoTiempoParihuela? {
        val parihuela = parihuelaDao.getById(parihuelaId) ?: return null
        if (parihuela.estado != EstadoParihuela.ABIERTA) return null
        return calcularTiempoEspera(parihuela, ahora)
    }

    /**
     * Consulta el origen completo de una parihuela por su código (QR).
     * Devuelve [ConsultaOrigenParihuela.NoEncontrado] si el código no existe.
     */
    suspend fun consultarOrigenPorCodigo(codigo: String): ConsultaOrigenParihuela {
        val parihuela = parihuelaDao.getByCodigo(codigo.trim())
            ?: return ConsultaOrigenParihuela.NoEncontrado
        return ConsultaOrigenParihuela.Encontrado(
            OrigenParihuela(
                codigo = parihuela.codigo,
                variedad = parihuela.variedad,
                cuartel = parihuela.cuartel,
                conteoJabas = parihuela.conteoJabas,
                pisos = parihuela.pisos,
                horaInicio = parihuela.horaInicio,
                horaSellado = parihuela.horaSellado,
                estado = parihuela.estado,
                motivoSelladoAnticipado = parihuela.motivoSelladoAnticipado
            )
        )
    }

    /**
     * Expone el resumen observable para el panel del capataz, calculado desde la BD.
     */
    fun observarResumenPanel(
        ahoraProvider: () -> Date = { Date() }
    ): Flow<ResumenPanelCapataz> {
        return parihuelaDao.observeAll().map { parihuelas ->
            val ahora = ahoraProvider()
            val abiertas = parihuelas
                .filter { it.estado == EstadoParihuela.ABIERTA }
                .map { parihuela ->
                    ParihuelaAbiertaResumen(
                        parihuela = parihuela,
                        tiempoEspera = calcularTiempoEspera(parihuela, ahora)
                    )
                }
                .sortedBy { it.tiempoEspera.restanteMs }

            ResumenPanelCapataz(
                totalAbiertas = abiertas.size,
                totalSelladas = parihuelas.count { it.estado == EstadoParihuela.SELLADA },
                totalSelladasAnticipadas = parihuelas.count {
                    it.estado == EstadoParihuela.SELLADA_ANTICIPADA
                },
                totalJabas = parihuelas.sumOf { it.conteoJabas },
                jabasPorVariedad = parihuelas
                    .groupBy { it.variedad }
                    .mapValues { (_, lista) -> lista.sumOf { it.conteoJabas } },
                jabasPorCuartel = parihuelas
                    .groupBy { it.cuartel }
                    .mapValues { (_, lista) -> lista.sumOf { it.conteoJabas } },
                abiertasPorUrgencia = abiertas
            )
        }
    }

    /**
     * Registra un pesaje asignándolo a la parihuela ABIERTA actual, aplicando
     * validación anti-mezcla y sellado automático al llegar a 120 jabas.
     */
    suspend fun registrarPesaje(
        jornalero: String,
        cantidadJabas: Int,
        variedad: String,
        cuartel: String,
        ahora: Date = Date()
    ): ResultadoPesaje {
        require(jornalero.isNotBlank()) { "El jornalero es obligatorio" }
        val abierta = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first().firstOrNull()
            ?: return ResultadoPesaje.SinParihuelaAbierta

        val resultadoJabas = registrarJabas(
            parihuelaId = abierta.id,
            cantidad = cantidadJabas,
            variedad = variedad,
            cuartel = cuartel,
            ahora = ahora
        )

        if (resultadoJabas.rechazadoPorMezcla) {
            return ResultadoPesaje.RechazadoPorMezcla(resultadoJabas.incidenciaGenerada!!)
        }

        if (resultadoJabas.rechazadoPorCapacidad) {
            val faltantes = (LIMITE_JABAS - abierta.conteoJabas).coerceAtLeast(0)
            return ResultadoPesaje.RechazadoPorCapacidad(
                parihuela = abierta,
                jabasFaltantes = faltantes
            )
        }

        val pesaje = PesajeEntity(
            parihuelaId = abierta.id,
            jornalero = jornalero.trim(),
            cantidadJabas = cantidadJabas,
            fechaHora = ahora
        )
        val pesajeId = pesajeDao.insert(pesaje)
        return ResultadoPesaje.Registrado(
            pesaje = pesaje.copy(id = pesajeId),
            resultadoJabas = resultadoJabas
        )
    }

    /**
     * Expone el avance observable de la parihuela abierta actual.
     */
    fun observarAvanceParihuelaAbierta(): Flow<AvanceParihuela?> {
        return parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).map { abiertas ->
            val actual = abiertas.firstOrNull() ?: return@map null
            val jabas = actual.conteoJabas
            AvanceParihuela(
                parihuelaId = actual.id,
                jabasActuales = jabas,
                faltanPara100 = (100 - jabas).coerceAtLeast(0),
                faltanPara120 = (LIMITE_JABAS - jabas).coerceAtLeast(0),
                porcentajeHacia120 = ((jabas * 100) / LIMITE_JABAS).coerceIn(0, 100)
            )
        }
    }

    /**
     * Expone el rendimiento (total de jabas) agrupado por jornalero.
     */
    fun observarRendimientoPorJornalero(): Flow<Map<String, Int>> {
        return pesajeDao.observeAll().map { pesajes ->
            pesajes
                .groupBy { it.jornalero }
                .mapValues { (_, lista) -> lista.sumOf { it.cantidadJabas } }
        }
    }

    /**
     * Reasigna el jornalero de un pesaje si su parihuela sigue ABIERTA.
     * Si la parihuela ya está sellada, registra INTENTO_EDICION en bitácora y rechaza.
     */
    suspend fun reasignarJornalero(
        pesajeId: Long,
        nuevoJornalero: String,
        ahora: Date = Date()
    ): PesajeEntity {
        val jornaleroLimpio = nuevoJornalero.trim()
        require(jornaleroLimpio.isNotEmpty()) { "El nuevo jornalero es obligatorio" }

        val pesaje = pesajeDao.getById(pesajeId)
            ?: throw IllegalArgumentException("Pesaje no encontrado: $pesajeId")
        val parihuela = parihuelaDao.getById(pesaje.parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: ${pesaje.parihuelaId}")

        if (parihuela.estado != EstadoParihuela.ABIERTA) {
            bitacoraDao.insert(
                BitacoraEntity(
                    parihuelaId = parihuela.id,
                    evento = EVENTO_INTENTO_EDICION,
                    detalle = "Intento de reasignar pesaje #$pesajeId sobre parihuela en estado ${parihuela.estado}",
                    fechaHora = ahora
                )
            )
            throw IllegalStateException(
                "No se puede reasignar un pesaje de una parihuela sellada (${parihuela.codigo})"
            )
        }

        val actualizado = pesaje.copy(jornalero = jornaleroLimpio)
        pesajeDao.update(actualizado)
        return actualizado
    }

    /**
     * Evalúa las parihuelas abiertas y decide cuáles deben disparar notificación POR_VENCER
     * (una sola vez por parihuela) y cuáles deben marcar RETENCION_EXCEDIDA (una sola vez).
     */
    suspend fun evaluarAlertasTiempo(ahora: Date = Date()): List<ParihuelaEntity> {
        val abiertas = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first()
        val eventosExistentes = bitacoraDao.observeAll().first()
        val porNotificar = mutableListOf<ParihuelaEntity>()

        for (parihuela in abiertas) {
            val tiempo = calcularTiempoEspera(parihuela, ahora)
            when (tiempo.estado) {
                EstadoTiempoEspera.EN_ESPERA -> Unit
                EstadoTiempoEspera.POR_VENCER -> {
                    val yaAvisada = eventosExistentes.any {
                        it.parihuelaId == parihuela.id && it.evento == EVENTO_ALERTA_POR_VENCER
                    }
                    if (!yaAvisada) {
                        bitacoraDao.insert(
                            BitacoraEntity(
                                parihuelaId = parihuela.id,
                                evento = EVENTO_ALERTA_POR_VENCER,
                                detalle = "Alerta de tiempo por vencer (${tiempo.restanteMs / 60_000L} min restantes)",
                                fechaHora = ahora
                            )
                        )
                        porNotificar.add(parihuela)
                    }
                }

                EstadoTiempoEspera.EXCEDIDA -> {
                    val yaMarcada = eventosExistentes.any {
                        it.parihuelaId == parihuela.id && it.evento == EVENTO_RETENCION_EXCEDIDA
                    }
                    if (!yaMarcada) {
                        bitacoraDao.insert(
                            BitacoraEntity(
                                parihuelaId = parihuela.id,
                                evento = EVENTO_RETENCION_EXCEDIDA,
                                detalle = "Retención de tiempo excedida para parihuela ${parihuela.codigo}",
                                fechaHora = ahora
                            )
                        )
                    }
                }
            }
        }
        return porNotificar
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
