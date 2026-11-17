package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.dao.ReasignacionDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoIncidencia
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import com.agrovalle.cosecha.data.local.entity.ReasignacionEntity
import com.agrovalle.cosecha.util.QrUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * Almacén del cuartel actual asignado a cada jornalero.
 */
interface CuartelJornaleroStore {
    fun obtenerAsignacionesPorNombre(): Map<String, String>
    fun obtenerCuartel(jornalero: String): String?
    fun actualizarCuartel(jornalero: String, cuartelDestino: String): String?
    fun restaurarCuartel(jornalero: String, cuartelAnterior: String?)
}

/**
 * Implementación por defecto respaldada por [DatosMemoria].
 */
object DatosMemoriaCuartelStore : CuartelJornaleroStore {
    private fun buscarJornalero(nombre: String) =
        DatosMemoria.listaJornaleros.firstOrNull {
            it.nombreCompleto.equals(nombre.trim(), ignoreCase = true)
        }

    override fun obtenerAsignacionesPorNombre(): Map<String, String> {
        return DatosMemoria.asignaciones.mapNotNull { (id, cuartel) ->
            DatosMemoria.buscarJornaleroPorId(id)?.let { it.nombreCompleto to cuartel }
        }.toMap()
    }

    override fun obtenerCuartel(jornalero: String): String? {
        val j = buscarJornalero(jornalero) ?: return null
        return DatosMemoria.obtenerCuartelAsignado(j.id)
    }

    override fun actualizarCuartel(jornalero: String, cuartelDestino: String): String? {
        val j = buscarJornalero(jornalero) ?: return null
        val anterior = DatosMemoria.obtenerCuartelAsignado(j.id)
        DatosMemoria.asignarCuartel(j.id, cuartelDestino)
        return anterior
    }

    override fun restaurarCuartel(jornalero: String, cuartelAnterior: String?) {
        val j = buscarJornalero(jornalero) ?: return
        if (cuartelAnterior == null) {
            DatosMemoria.asignaciones.remove(j.id)
        } else {
            DatosMemoria.asignarCuartel(j.id, cuartelAnterior)
        }
    }
}

private object NoOpReasignacionDao : ReasignacionDao {
    override suspend fun insert(reasignacion: ReasignacionEntity): Long = 0L
    override fun observeAll(): Flow<List<ReasignacionEntity>> = flowOf(emptyList())
    override suspend fun getUltimaPorJornalero(jornalero: String): ReasignacionEntity? = null
}

/**
 * Representa un jornalero activo con su cuartel actual, kilos, jabas y estado de rendimiento.
 */
data class JornaleroCuartelResumen(
    val nombre: String,
    val cuartelActual: String,
    val jabasAcumuladas: Int = 0,
    val kilosNetos: Double = 0.0,
    val horaPrimerPesaje: Date? = null,
    val tasaKgPorHora: Double? = null,
    val bajoRendimiento: Boolean = false
)

/**
 * Métricas de rendimiento calculadas desde los pesajes reales de un jornalero (AGV-005).
 */
data class RendimientoJornalero(
    val jornalero: String,
    val totalKilos: Double,
    val totalJabas: Int,
    val horaPrimerPesaje: Date,
    val tasaKgPorHora: Double?,
    val bajoRendimiento: Boolean
)

/**
 * Ejecutor de bloques transaccionales (Room withTransaction en producción).
 */
interface TransaccionRunner {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}

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
    val estado: EstadoTiempoEspera,
    val cronometroIniciado: Boolean = true
)

/**
 * Datos completos de origen de una parihuela consultada por su código QR.
 */
data class OrigenParihuela(
    val codigo: String,
    val variedad: String,
    val cuartel: String,
    val clp: String,
    val cuadrilla: String,
    val conteoJabas: Int,
    val pisos: Int,
    val horaInicio: Date?,
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
    val totalRetenidasCalidad: Int = 0,
    val totalSelladas: Int = 0,
    val totalSelladasAnticipadas: Int = 0,
    val totalJabas: Int = 0,
    val totalKilosNetos: Double = 0.0,
    val totalJornalerosActivos: Int = 0,
    val jabasPorVariedad: Map<String, Int> = emptyMap(),
    val jabasPorCuartel: Map<String, Int> = emptyMap(),
    val kilosPorCuartel: Map<String, Double> = emptyMap(),
    val abiertasPorUrgencia: List<ParihuelaAbiertaResumen> = emptyList(),
    val retenidasCalidad: List<ParihuelaEntity> = emptyList(),
    val rendimientoDetallado: List<RendimientoJornalero> = emptyList(),
    val jornalerosActivos: List<JornaleroCuartelResumen> = emptyList(),
    val jornalerosPorCuartel: Map<String, List<JornaleroCuartelResumen>> = emptyMap()
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
    val incidenciaGenerada: IncidenciaEntity? = null,
    val cronometroIniciadoEnEstePesaje: Boolean = false
)

/**
 * Repositorio que encapsula el ciclo de vida de las parihuelas (Sprint 2).
 */
class ParihuelaRepository(
    private val parihuelaDao: ParihuelaDao,
    private val bitacoraDao: BitacoraDao,
    private val incidenciaDao: IncidenciaDao,
    private val pesajeDao: PesajeDao,
    private val transaccionRunner: TransaccionRunner = object : TransaccionRunner {
        override suspend fun <T> ejecutar(bloque: suspend () -> T): T = bloque()
    },
    private val reasignacionDao: ReasignacionDao = NoOpReasignacionDao,
    private val cuartelJornaleroStore: CuartelJornaleroStore = DatosMemoriaCuartelStore
) {

    companion object {
        private val mutexTransaccion = Mutex()
        private val secuenciaCodigo = AtomicInteger(0)

        const val LIMITE_JABAS = 120
        const val LIMITE_ESPERA_MS = 45L * 60L * 1000L
        const val UMBRAL_POR_VENCER_MS = 5L * 60L * 1000L
        const val EVENTO_SELLADO_LIMITE = "SELLADO_LIMITE"
        const val EVENTO_SELLADO_ANTICIPADO = "SELLADO_ANTICIPADO"
        const val EVENTO_INTENTO_EDICION = "INTENTO_EDICION"
        const val EVENTO_ALERTA_POR_VENCER = "ALERTA_POR_VENCER"
        const val EVENTO_RETENCION_EXCEDIDA = "RETENCION_EXCEDIDA"
        const val EVENTO_TIEMPO_RETENCION = "TIEMPO_RETENCION_TOTAL"
        const val EVENTO_RETENIDA_CALIDAD = "RETENIDA_CALIDAD"
        const val EVENTO_LIBERACION_CALIDAD = "LIBERACION_CALIDAD"
        const val TIPO_MEZCLA_VARIEDAD = "MEZCLA_VARIEDAD"
        const val TIPO_MEZCLA_CUARTEL = "MEZCLA_CUARTEL"
        const val ACCION_REVISAR_LOTE_PARIHUELA = "Revisar el lote o la parihuela"
        const val ACCION_SEPARAR_JABA = "Separar jaba y reubicar en parihuela correcta"
        const val ACCION_DESCARTAR_ERROR = "Descartar por error de lectura"
        const val ACCION_LIBERAR = "Liberar"
        val ACCIONES_INCIDENCIA = listOf(
            ACCION_REVISAR_LOTE_PARIHUELA,
            ACCION_SEPARAR_JABA,
            ACCION_DESCARTAR_ERROR
        )

        const val MOTIVO_REASIG_REFUERZO_CARGA = "Refuerzo en cuartel con alta carga"
        const val MOTIVO_REASIG_COBERTURA_AUSENCIA = "Cobertura por ausencia de jornalero"
        const val MOTIVO_REASIG_AVANCE_CUARTEL = "Avance completado en cuartel de origen"
        const val MOTIVO_REASIG_PRIORIDAD_VARIEDAD = "Priorización de variedad para empaque"
        val MOTIVOS_REASIGNACION = listOf(
            MOTIVO_REASIG_REFUERZO_CARGA,
            MOTIVO_REASIG_COBERTURA_AUSENCIA,
            MOTIVO_REASIG_AVANCE_CUARTEL,
            MOTIVO_REASIG_PRIORIDAD_VARIEDAD
        )
        const val UMBRAL_BAJO_RENDIMIENTO_KG_HORA = 8.75 // 35 kg / 4 h
        const val TIEMPO_MINIMO_CALCULO_TASA_MS = 60_000L

        /**
         * Calcula la tasa de rendimiento (kg/h) desde el primer pesaje y determina
         * si está por debajo del umbral de 8,75 kg/h (35 kg en 4 h).
         * Si el tiempo transcurrido es casi cero (< 1 min), no marca alerta.
         */
        fun calcularTasaRendimiento(
            totalKilos: Double,
            horaPrimerPesaje: Date,
            ahora: Date = Date()
        ): Pair<Double?, Boolean> {
            val transcurridoMs = (ahora.time - horaPrimerPesaje.time).coerceAtLeast(0L)
            if (transcurridoMs < TIEMPO_MINIMO_CALCULO_TASA_MS) {
                return null to false
            }
            val horas = transcurridoMs.toDouble() / (60.0 * 60.0 * 1000.0)
            val tasa = totalKilos / horas
            return tasa to (tasa < UMBRAL_BAJO_RENDIMIENTO_KG_HORA)
        }

        /**
         * Calcula la demora en milisegundos hasta el momento exacto en que la parihuela
         * entrará en POR_VENCER (horaInicio + LIMITE_ESPERA_MS - UMBRAL_POR_VENCER_MS).
         * Si el momento ya pasó, devuelve 0L para ejecución inmediata.
         */
        fun calcularDemoraAlertaExactaMs(horaInicio: Date, ahora: Date = Date()): Long {
            val momentoAlerta = horaInicio.time + LIMITE_ESPERA_MS - UMBRAL_POR_VENCER_MS
            return (momentoAlerta - ahora.time).coerceAtLeast(0L)
        }

        /**
         * Calcula el tiempo total de retención transcurrido desde el primer pesaje
         * ([horaInicio]) hasta el sellado ([horaSellado]). Devuelve 0 ms si no hubo pesajes.
         */
        fun calcularTiempoRetencionTotalMs(horaInicio: Date?, horaSellado: Date): Long {
            if (horaInicio == null) return 0L
            return (horaSellado.time - horaInicio.time).coerceAtLeast(0L)
        }

        /** Formatea la duración total de retención en minutos y segundos legibles. */
        fun formatearDuracionRetencion(duracionMs: Long): String {
            val totalSeg = duracionMs.coerceAtLeast(0L) / 1000L
            val minutos = totalSeg / 60L
            val segundos = totalSeg % 60L
            return "$minutos min $segundos s"
        }
    }

    private suspend fun <T> enTransaccionAtomica(bloque: suspend () -> T): T {
        return mutexTransaccion.withLock {
            transaccionRunner.ejecutar(bloque)
        }
    }

    /**
     * Abre una nueva parihuela generando un código único.
     * Exige variedad, cuartel, CLP y cuadrilla obligatorios (sin valores por defecto).
     * Garantiza que solo exista una parihuela ABIERTA a la vez.
     */
    suspend fun abrirNuevaParihuela(
        variedad: String,
        cuartel: String,
        clp: String,
        cuadrilla: String,
        ahora: Date = Date()
    ): ParihuelaEntity = enTransaccionAtomica {
        abrirNuevaParihuelaInterno(variedad, cuartel, clp, cuadrilla, ahora)
    }

    private suspend fun abrirNuevaParihuelaInterno(
        variedad: String,
        cuartel: String,
        clp: String,
        cuadrilla: String,
        ahora: Date
    ): ParihuelaEntity {
        val variedadLimpia = variedad.trim()
        val cuartelLimpio = cuartel.trim()
        val clpLimpio = clp.trim()
        val cuadrillaLimpia = cuadrilla.trim()
        require(
            variedadLimpia.isNotEmpty() &&
                cuartelLimpio.isNotEmpty() &&
                clpLimpio.isNotEmpty() &&
                cuadrillaLimpia.isNotEmpty()
        ) {
            "La variedad, el cuartel, el CLP y la cuadrilla son obligatorios para abrir una parihuela"
        }

        val abiertaExistente = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first().firstOrNull()
        val retenidaExistente = parihuelaDao.observeByEstado(EstadoParihuela.RETENIDA_CALIDAD).first().firstOrNull()
        check(abiertaExistente == null && retenidaExistente == null) {
            "Ya existe una parihuela activa (${(abiertaExistente ?: retenidaExistente)?.codigo})"
        }

        val codigo = generarCodigoUnico(ahora)
        val nueva = ParihuelaEntity(
            codigo = codigo,
            estado = EstadoParihuela.ABIERTA,
            variedad = variedadLimpia,
            cuartel = cuartelLimpio,
            clp = clpLimpio,
            cuadrilla = cuadrillaLimpia,
            conteoJabas = 0,
            pisos = 0,
            horaInicio = null
        )
        val id = parihuelaDao.insert(nueva)
        return nueva.copy(id = id)
    }

    /**
     * Expone la parihuela activa actual (ABIERTA o RETENIDA_CALIDAD) desde el repositorio.
     */
    fun observarParihuelaAbierta(): Flow<ParihuelaEntity?> =
        parihuelaDao.observeAll().map { lista ->
            lista.firstOrNull {
                it.estado == EstadoParihuela.ABIERTA || it.estado == EstadoParihuela.RETENIDA_CALIDAD
            }
        }

    /**
     * Expone las parihuelas actualmente retenidas por calidad.
     */
    fun observarParihuelasRetenidas(): Flow<List<ParihuelaEntity>> =
        parihuelaDao.observeByEstado(EstadoParihuela.RETENIDA_CALIDAD)

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
    ): ResultadoRegistroJabas = enTransaccionAtomica {
        registrarJabasInterno(parihuelaId, cantidad, variedad, cuartel, ahora)
    }

    private suspend fun registrarJabasInterno(
        parihuelaId: Long,
        cantidad: Int,
        variedad: String,
        cuartel: String,
        ahora: Date
    ): ResultadoRegistroJabas {
        require(cantidad > 0) { "La cantidad de jabas debe ser mayor a 0" }

        val actual = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")

        if (actual.estado == EstadoParihuela.RETENIDA_CALIDAD) {
            throw IllegalStateException(
                "La parihuela ${actual.codigo} está retenida por calidad y no admite pesajes hasta liberarla"
            )
        }

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

        val esPrimerPesaje = actual.horaInicio == null
        val horaInicioEfectiva = actual.horaInicio ?: ahora
        val actualConInicio = if (esPrimerPesaje) {
            actual.copy(horaInicio = horaInicioEfectiva)
        } else {
            actual
        }

        if (nuevoConteo == LIMITE_JABAS) {
            val sellada = actualConInicio.copy(
                conteoJabas = nuevoConteo,
                estado = EstadoParihuela.SELLADA,
                horaSellado = ahora
            )
            parihuelaDao.update(sellada)

            registrarRetencionExcedidaSiCorresponde(actualConInicio, ahora)

            bitacoraDao.insert(
                BitacoraEntity(
                    parihuelaId = sellada.id,
                    evento = EVENTO_SELLADO_LIMITE,
                    detalle = "Sellado automático al alcanzar $nuevoConteo jabas",
                    fechaHora = ahora
                )
            )
            registrarTiempoRetencionTotalAlSellar(sellada, ahora)

            val siguiente = abrirNuevaParihuelaInterno(
                variedad = actual.variedad,
                cuartel = actual.cuartel,
                clp = actual.clp,
                cuadrilla = actual.cuadrilla,
                ahora = ahora
            )

            return ResultadoRegistroJabas(
                parihuela = sellada,
                nuevaParihuela = siguiente,
                selladaPorLimite = true,
                cronometroIniciadoEnEstePesaje = esPrimerPesaje
            )
        }

        val actualizada = actualConInicio.copy(conteoJabas = nuevoConteo)
        parihuelaDao.update(actualizada)
        return ResultadoRegistroJabas(
            parihuela = actualizada,
            nuevaParihuela = null,
            selladaPorLimite = false,
            cronometroIniciadoEnEstePesaje = esPrimerPesaje
        )
    }

    /**
     * Sella anticipadamente una parihuela abierta exigiendo un motivo obligatorio.
     */
    suspend fun sellarAnticipadamente(
        parihuelaId: Long,
        motivo: String,
        ahora: Date = Date()
    ): ParihuelaEntity = enTransaccionAtomica {
        val motivoLimpio = motivo.trim()
        require(motivoLimpio.isNotEmpty()) {
            "El motivo de sellado anticipado es obligatorio"
        }

        val actual = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")

        if (actual.estado == EstadoParihuela.RETENIDA_CALIDAD) {
            throw IllegalStateException(
                "La parihuela ${actual.codigo} está retenida por calidad y no se puede sellar hasta liberarla"
            )
        }

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

        registrarRetencionExcedidaSiCorresponde(actual, ahora)

        bitacoraDao.insert(
            BitacoraEntity(
                parihuelaId = sellada.id,
                evento = EVENTO_SELLADO_ANTICIPADO,
                detalle = motivoLimpio,
                fechaHora = ahora
            )
        )
        registrarTiempoRetencionTotalAlSellar(sellada, ahora)

        return@enTransaccionAtomica sellada
    }

    private suspend fun registrarTiempoRetencionTotalAlSellar(
        parihuela: ParihuelaEntity,
        horaSellado: Date
    ) {
        val inicio = parihuela.horaInicio
        val totalMs = calcularTiempoRetencionTotalMs(inicio, horaSellado)
        val detalle = if (inicio == null) {
            "Tiempo total de retención: ${formatearDuracionRetencion(totalMs)} (sin pesajes previos al sellado)"
        } else {
            "Tiempo total de retención: ${formatearDuracionRetencion(totalMs)} (${totalMs / 60_000L} min, $totalMs ms desde primer pesaje hasta sellado)"
        }
        bitacoraDao.insert(
            BitacoraEntity(
                parihuelaId = parihuela.id,
                evento = EVENTO_TIEMPO_RETENCION,
                detalle = detalle,
                fechaHora = horaSellado
            )
        )
    }

    private suspend fun registrarRetencionExcedidaSiCorresponde(
        parihuela: ParihuelaEntity,
        ahora: Date
    ) {
        val tiempo = calcularTiempoEspera(parihuela, ahora)
        if (tiempo.estado == EstadoTiempoEspera.EXCEDIDA &&
            !bitacoraDao.existeEvento(parihuela.id, EVENTO_RETENCION_EXCEDIDA)
        ) {
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
     * Resuelve una incidencia PENDIENTE con una acción de un toque, hora y responsable.
     * Si la acción es [ACCION_REVISAR_LOTE_PARIHUELA], pasa la parihuela a [EstadoParihuela.RETENIDA_CALIDAD].
     */
    suspend fun resolverIncidencia(
        incidenciaId: Long,
        accion: String,
        responsable: String,
        ahora: Date = Date()
    ): IncidenciaEntity = enTransaccionAtomica {
        val accionLimpia = accion.trim()
        val responsableLimpio = responsable.trim()
        require(accionLimpia.isNotEmpty()) { "La acción es obligatoria" }
        require(responsableLimpio.isNotEmpty()) { "La responsable es obligatoria" }

        val actual = incidenciaDao.getById(incidenciaId)
            ?: throw IllegalArgumentException("Incidencia no encontrada: $incidenciaId")
        check(actual.estado == EstadoIncidencia.PENDIENTE) {
            "La incidencia #$incidenciaId ya fue revisada"
        }

        val revisada = actual.copy(
            estado = EstadoIncidencia.REVISADA,
            accionTomada = accionLimpia,
            horaRevision = ahora,
            responsable = responsableLimpio
        )
        incidenciaDao.update(revisada)

        if (accionLimpia.equals(ACCION_REVISAR_LOTE_PARIHUELA, ignoreCase = true)) {
            val parihuela = parihuelaDao.getById(actual.parihuelaId)
            if (parihuela != null && parihuela.estado == EstadoParihuela.ABIERTA) {
                parihuelaDao.update(parihuela.copy(estado = EstadoParihuela.RETENIDA_CALIDAD))
                bitacoraDao.insert(
                    BitacoraEntity(
                        parihuelaId = parihuela.id,
                        evento = EVENTO_RETENIDA_CALIDAD,
                        detalle = "Retenida por calidad ($accionLimpia) por $responsableLimpio",
                        fechaHora = ahora
                    )
                )
            }
        }

        return@enTransaccionAtomica revisada
    }

    /**
     * Libera una parihuela que se encuentra en [EstadoParihuela.RETENIDA_CALIDAD],
     * devolviéndola a [EstadoParihuela.ABIERTA] y registrando la acción [ACCION_LIBERAR] en bitácora.
     */
    suspend fun liberarParihuelaRetenida(
        parihuelaId: Long,
        responsable: String,
        ahora: Date = Date()
    ): ParihuelaEntity = enTransaccionAtomica {
        val responsableLimpio = responsable.trim()
        require(responsableLimpio.isNotEmpty()) { "La responsable es obligatoria" }

        val parihuela = parihuelaDao.getById(parihuelaId)
            ?: throw IllegalArgumentException("Parihuela no encontrada: $parihuelaId")
        check(parihuela.estado == EstadoParihuela.RETENIDA_CALIDAD) {
            "La parihuela ${parihuela.codigo} no está retenida por calidad"
        }

        val liberada = parihuela.copy(estado = EstadoParihuela.ABIERTA)
        parihuelaDao.update(liberada)

        bitacoraDao.insert(
            BitacoraEntity(
                parihuelaId = parihuela.id,
                evento = EVENTO_LIBERACION_CALIDAD,
                detalle = "Acción: $ACCION_LIBERAR por $responsableLimpio",
                fechaHora = ahora
            )
        )

        return@enTransaccionAtomica liberada
    }

    /**
     * Calcula el tiempo restante y el estado de espera a partir de [ParihuelaEntity.horaInicio].
     * Si aún no tiene pesajes ([ParihuelaEntity.horaInicio] == null), el cronómetro no corre.
     */
    fun calcularTiempoEspera(
        parihuela: ParihuelaEntity,
        ahora: Date = Date()
    ): EstadoTiempoParihuela {
        val inicio = parihuela.horaInicio
            ?: return EstadoTiempoParihuela(
                parihuelaId = parihuela.id,
                restanteMs = LIMITE_ESPERA_MS,
                estado = EstadoTiempoEspera.EN_ESPERA,
                cronometroIniciado = false
            )
        val transcurridoMs = (ahora.time - inicio.time).coerceAtLeast(0L)
        val restanteSinAcotar = LIMITE_ESPERA_MS - transcurridoMs
        val estado = when {
            restanteSinAcotar <= 0L -> EstadoTiempoEspera.EXCEDIDA
            restanteSinAcotar <= UMBRAL_POR_VENCER_MS -> EstadoTiempoEspera.POR_VENCER
            else -> EstadoTiempoEspera.EN_ESPERA
        }
        return EstadoTiempoParihuela(
            parihuelaId = parihuela.id,
            restanteMs = restanteSinAcotar.coerceAtLeast(0L),
            estado = estado,
            cronometroIniciado = true
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
     * Consulta el origen completo de una parihuela por su código o trama QR.
     * Devuelve [ConsultaOrigenParihuela.NoEncontrado] si el código no existe.
     */
    suspend fun consultarOrigenPorCodigo(codigoOTrama: String): ConsultaOrigenParihuela {
        val codigo = QrUtil.extraerCodigo(codigoOTrama)
        if (codigo.isEmpty()) return ConsultaOrigenParihuela.NoEncontrado
        val parihuela = parihuelaDao.getByCodigo(codigo)
            ?: return ConsultaOrigenParihuela.NoEncontrado
        return ConsultaOrigenParihuela.Encontrado(
            OrigenParihuela(
                codigo = parihuela.codigo,
                variedad = parihuela.variedad,
                cuartel = parihuela.cuartel,
                clp = parihuela.clp,
                cuadrilla = parihuela.cuadrilla,
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
        tickFlow: Flow<Long> = flowOf(0L),
        ahoraProvider: () -> Date = { Date() }
    ): Flow<ResumenPanelCapataz> {
        return combine(
            parihuelaDao.observeAll(),
            pesajeDao.observeAll(),
            reasignacionDao.observeAll(),
            tickFlow
        ) { parihuelas, pesajes, reasignaciones, _ ->
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

            val retenidas = parihuelas.filter { it.estado == EstadoParihuela.RETENIDA_CALIDAD }
            val rendimientoDetallado = construirRendimientoDetallado(pesajes, ahora)
            val jornalerosActivos = construirJornalerosConCuartel(
                parihuelas = parihuelas,
                pesajes = pesajes,
                reasignaciones = reasignaciones,
                ahora = ahora
            )
            val porCuartel = jornalerosActivos
                .groupBy { it.cuartelActual }
                .toSortedMap()

            ResumenPanelCapataz(
                totalAbiertas = abiertas.size,
                totalRetenidasCalidad = retenidas.size,
                totalSelladas = parihuelas.count { it.estado == EstadoParihuela.SELLADA },
                totalSelladasAnticipadas = parihuelas.count {
                    it.estado == EstadoParihuela.SELLADA_ANTICIPADA
                },
                totalJabas = parihuelas.sumOf { it.conteoJabas },
                totalKilosNetos = pesajes.sumOf { it.pesoKg },
                totalJornalerosActivos = jornalerosActivos.size,
                jabasPorVariedad = parihuelas
                    .groupBy { it.variedad }
                    .mapValues { (_, lista) -> lista.sumOf { it.conteoJabas } },
                jabasPorCuartel = parihuelas
                    .groupBy { it.cuartel }
                    .mapValues { (_, lista) -> lista.sumOf { it.conteoJabas } },
                kilosPorCuartel = porCuartel.mapValues { (_, lista) -> lista.sumOf { it.kilosNetos } },
                abiertasPorUrgencia = abiertas,
                retenidasCalidad = retenidas,
                rendimientoDetallado = rendimientoDetallado,
                jornalerosActivos = jornalerosActivos,
                jornalerosPorCuartel = porCuartel
            )
        }
    }

    /**
     * Expone el listado observable de jornaleros activos con su cuartel actual y métricas.
     */
    fun observarJornalerosConCuartel(
        ahoraProvider: () -> Date = { Date() }
    ): Flow<List<JornaleroCuartelResumen>> {
        return combine(
            parihuelaDao.observeAll(),
            pesajeDao.observeAll(),
            reasignacionDao.observeAll()
        ) { parihuelas, pesajes, reasignaciones ->
            construirJornalerosConCuartel(parihuelas, pesajes, reasignaciones, ahoraProvider())
        }
    }

    /**
     * Expone el rendimiento detallado (kilos, jabas, primer pesaje, tasa kg/h y alerta) por jornalero.
     */
    fun observarRendimientoDetalladoPorJornalero(
        ahoraProvider: () -> Date = { Date() }
    ): Flow<List<RendimientoJornalero>> {
        return pesajeDao.observeAll().map { pesajes ->
            construirRendimientoDetallado(pesajes, ahoraProvider())
        }
    }

    /**
     * Expone el historial observable de reasignaciones de cuartel.
     */
    fun observarReasignaciones(): Flow<List<ReasignacionEntity>> =
        reasignacionDao.observeAll()

    /**
     * Reasigna un jornalero a un nuevo cuartel eligiendo uno de los motivos predefinidos (AGV-005).
     * Todo el guardado se ejecuta en una sola transacción: inserta la [ReasignacionEntity]
     * con `sincronizado = false` y actualiza el cuartel actual del jornalero.
     */
    suspend fun reasignarJornaleroACuartel(
        jornalero: String,
        cuartelDestino: String,
        motivo: String,
        cuartelOrigen: String? = null,
        hora: Date = Date()
    ): ReasignacionEntity = enTransaccionAtomica {
        val jornaleroLimpio = jornalero.trim()
        val destinoLimpio = cuartelDestino.trim()
        val motivoLimpio = motivo.trim()

        require(jornaleroLimpio.isNotEmpty()) { "El jornalero es obligatorio" }
        require(destinoLimpio.isNotEmpty()) { "El cuartel de destino es obligatorio" }
        val motivoCanonico = MOTIVOS_REASIGNACION.firstOrNull {
            it.equals(motivoLimpio, ignoreCase = true)
        } ?: throw IllegalArgumentException(
            "Debe elegir un motivo predefinido de reasignación válido"
        )

        val origenResuelto = (cuartelOrigen?.trim()?.takeIf { it.isNotEmpty() }
            ?: obtenerCuartelActualJornaleroInterno(jornaleroLimpio))
            ?: throw IllegalStateException(
                "El jornalero $jornaleroLimpio no tiene cuartel de origen asignado"
            )

        require(!destinoLimpio.equals(origenResuelto, ignoreCase = true)) {
            "El cuartel de destino ($destinoLimpio) debe ser distinto al cuartel de origen ($origenResuelto)"
        }

        val nueva = ReasignacionEntity(
            jornalero = jornaleroLimpio,
            cuartelOrigen = origenResuelto,
            cuartelDestino = destinoLimpio,
            motivo = motivoCanonico,
            hora = hora,
            sincronizado = false
        )

        val anterior = cuartelJornaleroStore.obtenerCuartel(jornaleroLimpio)
        try {
            val id = reasignacionDao.insert(nueva)
            cuartelJornaleroStore.actualizarCuartel(jornaleroLimpio, destinoLimpio)
            return@enTransaccionAtomica nueva.copy(id = id)
        } catch (t: Throwable) {
            cuartelJornaleroStore.restaurarCuartel(jornaleroLimpio, anterior)
            throw t
        }
    }

    private suspend fun obtenerCuartelActualJornaleroInterno(jornalero: String): String? {
        cuartelJornaleroStore.obtenerCuartel(jornalero)?.let { return it }
        reasignacionDao.getUltimaPorJornalero(jornalero)?.cuartelDestino?.let { return it }
        val parihuelasById = parihuelaDao.observeAll().first().associateBy { it.id }
        val ultimoPesaje = pesajeDao.observeAll().first()
            .filter { it.jornalero.equals(jornalero, ignoreCase = true) }
            .maxWithOrNull(compareBy<PesajeEntity> { it.fechaHora.time }.thenBy { it.id })
        return ultimoPesaje?.let { parihuelasById[it.parihuelaId]?.cuartel }
    }

    private fun construirRendimientoDetallado(
        pesajes: List<PesajeEntity>,
        ahora: Date
    ): List<RendimientoJornalero> {
        if (pesajes.isEmpty()) return emptyList()
        return pesajes
            .groupBy { it.jornalero.trim().lowercase() }
            .values
            .map { lista ->
                val ordenado = lista.sortedWith(compareBy<PesajeEntity> { it.fechaHora.time }.thenBy { it.id })
                val nombre = ordenado.first().jornalero.trim()
                val totalKilos = ordenado.sumOf { it.pesoKg }
                val totalJabas = ordenado.sumOf { it.cantidadJabas }
                val primerPesaje = ordenado.first().fechaHora
                val (tasa, bajoUmbral) = calcularTasaRendimiento(totalKilos, primerPesaje, ahora)
                RendimientoJornalero(
                    jornalero = nombre,
                    totalKilos = totalKilos,
                    totalJabas = totalJabas,
                    horaPrimerPesaje = primerPesaje,
                    tasaKgPorHora = tasa,
                    bajoRendimiento = bajoUmbral
                )
            }
            .sortedBy { it.jornalero }
    }

    private fun construirJornalerosConCuartel(
        parihuelas: List<ParihuelaEntity>,
        pesajes: List<PesajeEntity>,
        reasignaciones: List<ReasignacionEntity>,
        ahora: Date
    ): List<JornaleroCuartelResumen> {
        val parihuelasById = parihuelas.associateBy { it.id }
        val cuartelInicialPorPesaje = linkedMapOf<String, String>()
        val nombresCanonicos = linkedMapOf<String, String>()

        pesajes
            .sortedWith(compareBy<PesajeEntity> { it.fechaHora.time }.thenBy { it.id })
            .forEach { p ->
                val clave = p.jornalero.trim().lowercase()
                nombresCanonicos.putIfAbsent(clave, p.jornalero.trim())
                parihuelasById[p.parihuelaId]?.cuartel?.let { cuartel ->
                    cuartelInicialPorPesaje[clave] = cuartel
                }
            }

        val rendimientosPorClave = construirRendimientoDetallado(pesajes, ahora)
            .associateBy { it.jornalero.trim().lowercase() }

        val cuartelPorReasignacion = linkedMapOf<String, String>()
        reasignaciones
            .sortedWith(compareBy<ReasignacionEntity> { it.hora.time }.thenBy { it.id })
            .forEach { r ->
                val clave = r.jornalero.trim().lowercase()
                nombresCanonicos.putIfAbsent(clave, r.jornalero.trim())
                cuartelPorReasignacion[clave] = r.cuartelDestino
            }

        val asignacionesEnStore = linkedMapOf<String, String>()
        cuartelJornaleroStore.obtenerAsignacionesPorNombre().forEach { (nombre, cuartel) ->
            val clave = nombre.trim().lowercase()
            nombresCanonicos.putIfAbsent(clave, nombre.trim())
            asignacionesEnStore[clave] = cuartel
        }

        return nombresCanonicos.entries.mapNotNull { (clave, nombre) ->
            val cuartelActual = asignacionesEnStore[clave]
                ?: cuartelPorReasignacion[clave]
                ?: cuartelInicialPorPesaje[clave]
                ?: return@mapNotNull null
            val rend = rendimientosPorClave[clave]
            JornaleroCuartelResumen(
                nombre = nombre,
                cuartelActual = cuartelActual,
                jabasAcumuladas = rend?.totalJabas ?: 0,
                kilosNetos = rend?.totalKilos ?: 0.0,
                horaPrimerPesaje = rend?.horaPrimerPesaje,
                tasaKgPorHora = rend?.tasaKgPorHora,
                bajoRendimiento = rend?.bajoRendimiento ?: false
            )
        }.sortedWith(compareBy<JornaleroCuartelResumen> { it.cuartelActual }.thenBy { it.nombre })
    }

    /**
     * Registra un pesaje asignándolo a la parihuela ABIERTA actual, aplicando
     * validación anti-mezcla y sellado automático al llegar a 120 jabas.
     */
    suspend fun registrarPesaje(
        jornalero: String,
        cantidadJabas: Int,
        pesoKg: Double,
        variedad: String,
        cuartel: String,
        ahora: Date = Date()
    ): ResultadoPesaje = enTransaccionAtomica {
        require(jornalero.isNotBlank()) { "El jornalero es obligatorio" }
        require(pesoKg.isFinite() && pesoKg > 0.0) {
            "El peso neto en kilos es obligatorio y debe ser mayor a 0"
        }
        val abierta = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first().firstOrNull()
            ?: run {
                val retenida = parihuelaDao.observeByEstado(EstadoParihuela.RETENIDA_CALIDAD)
                    .first()
                    .firstOrNull()
                if (retenida != null) {
                    throw IllegalStateException(
                        "La parihuela ${retenida.codigo} está retenida por calidad y no admite pesajes hasta liberarla"
                    )
                }
                return@enTransaccionAtomica ResultadoPesaje.SinParihuelaAbierta
            }

        val resultadoJabas = registrarJabasInterno(
            parihuelaId = abierta.id,
            cantidad = cantidadJabas,
            variedad = variedad,
            cuartel = cuartel,
            ahora = ahora
        )

        if (resultadoJabas.rechazadoPorMezcla) {
            return@enTransaccionAtomica ResultadoPesaje.RechazadoPorMezcla(resultadoJabas.incidenciaGenerada!!)
        }

        if (resultadoJabas.rechazadoPorCapacidad) {
            val faltantes = (LIMITE_JABAS - abierta.conteoJabas).coerceAtLeast(0)
            return@enTransaccionAtomica ResultadoPesaje.RechazadoPorCapacidad(
                parihuela = abierta,
                jabasFaltantes = faltantes
            )
        }

        val pesaje = PesajeEntity(
            parihuelaId = abierta.id,
            jornalero = jornalero.trim(),
            cantidadJabas = cantidadJabas,
            pesoKg = pesoKg,
            fechaHora = ahora
        )
        val pesajeId = pesajeDao.insert(pesaje)
        return@enTransaccionAtomica ResultadoPesaje.Registrado(
            pesaje = pesaje.copy(id = pesajeId),
            resultadoJabas = resultadoJabas
        )
    }

    /**
     * Expone el avance observable de la parihuela abierta actual.
     */
    fun observarAvanceParihuelaAbierta(): Flow<AvanceParihuela?> {
        return observarParihuelaAbierta().map { actual ->
            actual ?: return@map null
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
    ): PesajeEntity = enTransaccionAtomica {
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
        return@enTransaccionAtomica actualizado
    }

    /**
     * Evalúa las parihuelas abiertas de forma atómica y decide cuáles deben disparar
     * notificación POR_VENCER (una sola vez) y cuáles deben marcar RETENCION_EXCEDIDA (una sola vez).
     */
    suspend fun evaluarAlertasTiempo(ahora: Date = Date()): List<ParihuelaEntity> = enTransaccionAtomica {
        val abiertas = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first()
        val porNotificar = mutableListOf<ParihuelaEntity>()

        for (parihuela in abiertas) {
            if (parihuela.horaInicio == null) continue
            val tiempo = calcularTiempoEspera(parihuela, ahora)
            when (tiempo.estado) {
                EstadoTiempoEspera.EN_ESPERA -> Unit
                EstadoTiempoEspera.POR_VENCER -> {
                    val yaAvisada = bitacoraDao.existeEvento(parihuela.id, EVENTO_ALERTA_POR_VENCER)
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
                    val yaMarcada = bitacoraDao.existeEvento(parihuela.id, EVENTO_RETENCION_EXCEDIDA)
                    if (!yaMarcada) {
                        bitacoraDao.insert(
                            BitacoraEntity(
                                parihuelaId = parihuela.id,
                                evento = EVENTO_RETENCION_EXCEDIDA,
                                detalle = "Retención de tiempo excedida para parihuela ${parihuela.codigo}",
                                fechaHora = ahora
                            )
                        )
                        porNotificar.add(parihuela)
                    }
                }
            }
        }
        return@enTransaccionAtomica porNotificar
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
