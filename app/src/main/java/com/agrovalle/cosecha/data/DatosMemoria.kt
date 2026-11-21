package com.agrovalle.cosecha.data

import com.agrovalle.cosecha.model.Asistencia
import com.agrovalle.cosecha.model.CalibracionTara
import com.agrovalle.cosecha.model.CredencialCapataz
import com.agrovalle.cosecha.model.EstadoReclamoPeso
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.model.Pesaje
import com.agrovalle.cosecha.model.ReclamoPeso
import com.agrovalle.cosecha.model.RegistroIngresoManual
import com.agrovalle.cosecha.model.ResumenCuadrillaCierre
import com.agrovalle.cosecha.util.CalculadoraCierreTurno
import com.agrovalle.cosecha.util.CalculadoraPesaje
import com.agrovalle.cosecha.util.CalculadoraReclamoPeso
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FechaHoraUtil

/**
 * Datos en MEMORIA de la aplicacion (Singleton de Kotlin) - Sprint 1.
 *
 * Reemplaza por completo a la base de datos Room: el curso aun no ha visto
 * persistencia en disco, por lo que el manejo de datos se hace con
 * arreglos/listas en memoria, tal como indica el diagrama de secuencia.
 *
 * Es un `object`, asi que la informacion vive mientras el proceso de la
 * aplicacion siga vivo y es compartida por todas las Activities y ViewModels.
 */
object DatosMemoria {

    // ------------------------------------------------------------------------
    // Padron de la cuadrilla (AGV-001 / AGV-016)
    // ------------------------------------------------------------------------

    /** Codigo de la cuadrilla del sprint. */
    const val CODIGO_CUADRILLA = "CUA-01"

    /** Tamano del padron precargado (dentro del rango 25-35 de la HU). */
    const val TOTAL_PADRON = 30

    private val NOMBRES = listOf(
        "Juan", "Maria", "Pedro", "Luis", "Ana", "Carlos", "Jose", "Rosa",
        "Miguel", "Carmen", "Jorge", "Elena", "Victor", "Gloria", "Fernando",
        "Lucia", "Roberto", "Yolanda", "Julio", "Marisol", "Segundo", "Rita",
        "Manuel", "Elvira", "Segundo", "Teresa", "Angel", "Marleny", "Santos", "Julia"
    )

    private val APELLIDOS = listOf(
        "Perez", "Sanchez", "Lopez", "Mendoza", "Garcia", "Alva", "Ramirez",
        "Torres", "Huaman", "Diaz", "Rodriguez", "Quispe", "Flores", "Rojas",
        "Vasquez", "Chavez", "Castro", "Vargas", "Salirrosas", "Toledo",
        "Cabanillas", "Esquivel", "Namay", "Bocanegra", "Vereau", "Plasencia"
    )

    /**
     * Padron de la cuadrilla precargado con 30 jornaleros simulados.
     *
     * Codigos de fotocheck: `QR_AGV_001` ... `QR_AGV_030`.
     * DNIs de 8 digitos terminados en `1234`, `1371`, ... (sirven para probar
     * la busqueda por los ultimos 4 digitos de AGV-016).
     */
    val listaJornaleros: MutableList<Jornalero> = generarPadron()

    /** Credenciales autorizadas de capataz para autorizar reclamos de peso > 20 g (AGV-015). */
    val credencialesCapataz: List<CredencialCapataz> = listOf(
        CredencialCapataz(
            id = 1L,
            nombre = "Carlos Mendez (Capataz)",
            codigoQr = "QR_CAPATAZ_001",
            cuadrillaCodigo = CODIGO_CUADRILLA
        ),
        CredencialCapataz(
            id = 2L,
            nombre = "Elena Rojas (Supervisora)",
            codigoQr = "QR_CAPATAZ_002",
            cuadrillaCodigo = CODIGO_CUADRILLA
        )
    )

    // ------------------------------------------------------------------------
    // Asignacion jornalero -> cuartel (AGV-002)
    // ------------------------------------------------------------------------

    /**
     * Que jornalero va a que cuartel.
     * Clave = `Jornalero.id`, valor = cuartel (ej. "Cuartel 01").
     */
    val asignaciones: MutableMap<Long, String> = mutableMapOf()

    // ------------------------------------------------------------------------
    // Cuartel -> variedad de arandano (AGV-002)
    // ------------------------------------------------------------------------

    /** Variedad que se autocompleta segun el cuartel elegido. */
    val mapaCuarteles: Map<String, String> = mapOf(
        "Cuartel 01" to "Biloxi",
        "Cuartel 05" to "Ventura",
        "Cuartel 12" to "Biloxi",
        "Cuartel 18" to "Ventura"
    )

    /** Opciones que se muestran en el Spinner de cuartel (incluye el prompt). */
    val opcionesCuartel: List<String> = listOf("Seleccione Cuartel...") + mapaCuarteles.keys

    // ------------------------------------------------------------------------
    // Bitacora de operaciones del turno en memoria
    // ------------------------------------------------------------------------

    /** Asistencias registradas en el turno (AGV-001 / AGV-016). */
    val asistencias: MutableList<Asistencia> = mutableListOf()

    /** Bitácora de ingresos manuales por DNI con motivo, hora y capataz (AGV-016 Escenario 2.0). */
    val bitacoraIngresosManuales: MutableList<RegistroIngresoManual> = mutableListOf()

    /** Pesajes registrados (AGV-003 / AGV-004). */
    val pesajes: MutableList<Pesaje> = mutableListOf()

    /** Calibraciones de tara del turno (AGV-015). */
    val calibraciones: MutableList<CalibracionTara> = mutableListOf()

    /** Bitácora de reclamos de peso y compensatorios (AGV-015 escenarios 2.0 y 3.0). */
    val bitacoraReclamos: MutableList<ReclamoPeso> = mutableListOf()

    private var secuenciaAsistencia = 0L
    private var secuenciaIngresoManual = 0L
    private var secuenciaPesaje = 0L
    private var secuenciaCalibracion = 0L
    private var secuenciaReclamo = 0L

    // ------------------------------------------------------------------------
    // Consultas sobre el padron
    // ------------------------------------------------------------------------

    /** Busca un jornalero por su codigo de fotocheck (AGV-001). */
    fun buscarJornaleroPorQr(codigoQr: String): Jornalero? =
        listaJornaleros.firstOrNull { it.codigoQr.equals(codigoQr.trim(), ignoreCase = true) }

    /** Valida la credencial QR de un capataz autorizado (AGV-015 escenario 3.0). */
    fun buscarCapatazPorQr(codigoQr: String): CredencialCapataz? {
        val limpio = codigoQr.trim()
        if (limpio.isEmpty()) return null
        return credencialesCapataz.firstOrNull { it.codigoQr.equals(limpio, ignoreCase = true) }
    }

    /** Busca un jornalero por su id (usado al navegar con EXTRA_JORNALERO_ID). */
    fun buscarJornaleroPorId(id: Long): Jornalero? =
        listaJornaleros.firstOrNull { it.id == id }

    /** AGV-016: coincidencias por los ultimos digitos del DNI. */
    fun buscarJornalerosPorUltimosDigitos(digitos: String): List<Jornalero> {
        val limpio = digitos.trim()
        if (limpio.isEmpty()) return emptyList()
        return listaJornaleros.filter { it.dni.endsWith(limpio) }
    }

    // ------------------------------------------------------------------------
    // Asistencias (AGV-001 / AGV-016)
    // ------------------------------------------------------------------------

    /** Devuelve la asistencia del jornalero en esa fecha, si ya existe. */
    fun buscarAsistencia(jornaleroId: Long, fecha: String): Asistencia? =
        asistencias.firstOrNull { it.jornaleroId == jornaleroId && it.fecha == fecha }

    /**
     * Registra la asistencia del dia respetando la regla de NO duplicados.
     * @return la asistencia creada, o `null` si ya estaba registrado ese dia.
     */
    fun registrarAsistencia(
        jornaleroId: Long,
        fecha: String,
        horaIngreso: String,
        estado: String,
        metodo: String
    ): Asistencia? {
        if (buscarAsistencia(jornaleroId, fecha) != null) return null
        val nueva = Asistencia(
            id = ++secuenciaAsistencia,
            jornaleroId = jornaleroId,
            fecha = fecha,
            horaIngreso = horaIngreso,
            estado = estado,
            metodo = metodo
        )
        asistencias.add(nueva)
        return nueva
    }

    /** Cantidad de jornaleros que ya registraron asistencia en la fecha. */
    fun contarAsistencias(fecha: String): Int =
        asistencias.count { it.fecha == fecha }

    /** AGV-006: jornaleros que registraron asistencia en la fecha indicada. */
    fun jornalerosConAsistencia(fecha: String): List<Jornalero> =
        asistencias
            .asSequence()
            .filter { it.fecha == fecha }
            .mapNotNull { buscarJornaleroPorId(it.jornaleroId) }
            .distinctBy { it.id }
            .toList()

    /** Registra un evento en la bitácora de ingresos manuales (AGV-016 Escenario 2.0). */
    fun registrarBitacoraIngresoManual(
        jornalero: Jornalero,
        fecha: String,
        hora: String,
        motivo: String,
        capatazResponsable: String
    ): RegistroIngresoManual {
        val motivoLimpio = motivo.trim()
        val capatazLimpio = capatazResponsable.trim()
        require(motivoLimpio.isNotEmpty()) { "El motivo del ingreso manual es obligatorio" }
        require(capatazLimpio.isNotEmpty()) { "El capataz responsable es obligatorio" }
        val registro = RegistroIngresoManual(
            id = ++secuenciaIngresoManual,
            jornaleroId = jornalero.id,
            jornaleroNombre = jornalero.nombreCompleto,
            dni = jornalero.dni,
            cuadrillaCodigo = jornalero.cuadrillaCodigo,
            fecha = fecha,
            hora = hora,
            motivo = motivoLimpio,
            capatazResponsable = capatazLimpio
        )
        bitacoraIngresosManuales.add(registro)
        return registro
    }

    /** Bitácora de ingresos manuales de una fecha (los más recientes primero). */
    fun obtenerBitacoraIngresosManuales(fecha: String): List<RegistroIngresoManual> =
        bitacoraIngresosManuales
            .filter { it.fecha == fecha }
            .sortedByDescending { it.id }

    /** Resumen de asistencias e ingresos manuales por cuadrilla para el cierre de turno. */
    fun obtenerResumenCierrePorCuadrilla(fecha: String): List<ResumenCuadrillaCierre> {
        val asistenciasDia = asistencias.filter { it.fecha == fecha }
        val cuadrillasPadron = listaJornaleros.map { it.cuadrillaCodigo }.distinct()
        return CalculadoraCierreTurno.resumirPorCuadrilla(
            asistenciasDelDia = asistenciasDia,
            jornaleros = listaJornaleros,
            cuadrillasBase = cuadrillasPadron
        )
    }

    // ------------------------------------------------------------------------
    // Asignaciones y pesajes
    // ------------------------------------------------------------------------

    /** AGV-002: guarda que jornalero fue asignado a que cuartel. */
    fun asignarCuartel(jornaleroId: Long, cuartel: String) {
        asignaciones[jornaleroId] = cuartel
    }

    /** Cuartel asignado a un jornalero, si lo tiene. */
    fun obtenerCuartelAsignado(jornaleroId: Long): String? = asignaciones[jornaleroId]

    /** Variedad que corresponde a un cuartel (autocompletado de AGV-002). */
    fun obtenerVariedadPorCuartel(cuartel: String): String? = mapaCuarteles[cuartel]

    /** Agrega un pesaje a la bitacora en memoria y lo devuelve. */
    fun registrarPesaje(pesaje: Pesaje): Pesaje {
        val guardado = pesaje.copy(id = ++secuenciaPesaje)
        pesajes.add(guardado)
        return guardado
    }

    /** Pesajes originales registrados (excluye pesajes compensatorios, los más recientes primero). */
    fun pesajesOriginales(): List<Pesaje> =
        pesajes.filter { !it.esCompensatorio }.sortedByDescending { it.id }

    /** Busca un pesaje por su ID. */
    fun buscarPesajePorId(id: Long): Pesaje? = pesajes.firstOrNull { it.id == id }

    /** Pesajes de un jornalero. */
    fun pesajesDeJornalero(jornaleroId: Long): List<Pesaje> =
        pesajes.filter { it.jornaleroId == jornaleroId }

    /**
     * AGV-004 / H-11: acumulado diario del cosechador entre dos marcas de tiempo.
     * Cuando existe un pesaje compensatorio vinculado a un original, el compensatorio
     * (que almacena el nuevo peso neto positivo) sustituye al original en el acumulado
     * sin sumarlo encima y sin modificar el registro original.
     */
    fun acumuladoDiario(jornaleroId: Long, desde: Long, hasta: Long): Double {
        val delPeriodo = pesajes.filter {
            it.jornaleroId == jornaleroId && it.timestamp in desde..hasta
        }
        val originalesSustituidos = delPeriodo
            .filter { it.esCompensatorio }
            .mapNotNull { it.pesajeOriginalId }
            .toSet()
        val total = delPeriodo
            .filter { it.esCompensatorio || it.id !in originalesSustituidos }
            .sumOf { it.pesoNeto }
        return Math.round(total * 1000.0) / 1000.0
    }

    // ------------------------------------------------------------------------
    // Reclamos de peso y compensatorios (AGV-015 escenarios 2.0 y 3.0)
    // ------------------------------------------------------------------------

    sealed interface ResultadoRegistroReclamo {
        data class DentroDeTolerancia(
            val reclamo: ReclamoPeso,
            val pesajeOriginal: Pesaje
        ) : ResultadoRegistroReclamo

        data class CompensacionRegistrada(
            val reclamo: ReclamoPeso,
            val pesajeOriginal: Pesaje,
            val pesajeCompensatorio: Pesaje
        ) : ResultadoRegistroReclamo
    }

    /**
     * Evalúa y registra un reclamo de peso sobre un pesaje original sin modificarlo ni borrarlo:
     * - Diferencia <= 20 g: mantiene el original y registra el reclamo en bitácora.
     * - Diferencia > 20 g: exige credencial QR de capataz válida, crea pesaje compensatorio
     *   vinculado al original y registra tanto reclamo como compensatorio en bitácora.
     */
    fun registrarReclamoPeso(
        pesajeOriginalId: Long,
        nuevoPesoBrutoKg: Double,
        taraPorJabaKg: Double,
        codigoQrCapataz: String? = null,
        fecha: String = FechaHoraUtil.hoy(),
        hora: String = FechaHoraUtil.ahoraHora(),
        timestamp: Long = FechaHoraUtil.timestampActual()
    ): ResultadoRegistroReclamo {
        val original = buscarPesajePorId(pesajeOriginalId)
            ?: throw IllegalArgumentException("Pesaje original no encontrado: $pesajeOriginalId")
        require(!original.esCompensatorio) {
            "No se puede iniciar un reclamo sobre un pesaje compensatorio"
        }

        val yaTieneCompensatorio = bitacoraReclamos.any {
            it.pesajeOriginalId == original.id && it.estado == EstadoReclamoPeso.COMPENSADO
        } || pesajes.any { it.esCompensatorio && it.pesajeOriginalId == original.id }
        check(!yaTieneCompensatorio) {
            "El pesaje #${original.id} ya tiene un reclamo compensatorio registrado y no admite otro"
        }

        val jornaleroNombre = buscarJornaleroPorId(original.jornaleroId)?.nombreCompleto
            ?: "Jornalero #${original.jornaleroId}"
        val taraAplicada = CalculadoraReclamoPeso.resolverTaraTotal(
            taraTotalOriginalKg = original.taraTotal,
            cantidadJabas = original.cantidadJabas,
            taraPorJabaRespaldoKg = taraPorJabaKg
        )
        val nuevoNetoKg = CalculadoraReclamoPeso.calcularPesoNetoConTaraTotal(
            pesoBrutoKg = nuevoPesoBrutoKg,
            taraTotalKg = taraAplicada
        )
        val evaluacion = CalculadoraReclamoPeso.evaluarReclamo(
            pesoOriginalNetoKg = original.pesoNeto,
            pesoNuevoNetoKg = nuevoNetoKg
        )

        if (!evaluacion.excedeTolerancia) {
            val reclamo = ReclamoPeso(
                id = ++secuenciaReclamo,
                fecha = fecha,
                hora = hora,
                timestamp = timestamp,
                jornaleroId = original.jornaleroId,
                jornaleroNombre = jornaleroNombre,
                pesajeOriginalId = original.id,
                pesoOriginalNetoKg = original.pesoNeto,
                pesoBrutoReclamoKg = nuevoPesoBrutoKg,
                taraAplicadaKg = taraAplicada,
                pesoNuevoNetoKg = evaluacion.pesoNuevoNetoKg,
                diferenciaKg = evaluacion.diferenciaKg,
                diferenciaGramos = evaluacion.diferenciaGramos,
                diferenciaAbsolutaGramos = evaluacion.diferenciaAbsolutaGramos,
                excedeTolerancia = false,
                estado = EstadoReclamoPeso.DENTRO_TOLERANCIA
            )
            bitacoraReclamos.add(reclamo)
            return ResultadoRegistroReclamo.DentroDeTolerancia(reclamo, original)
        }

        val qrLimpio = codigoQrCapataz?.trim().orEmpty()
        check(qrLimpio.isNotEmpty()) {
            "La diferencia supera ${Constantes.TOLERANCIA_RECLAMO_GRAMOS.toInt()} g y exige autorización escaneando el QR del capataz"
        }
        val capataz = buscarCapatazPorQr(qrLimpio)
            ?: throw IllegalArgumentException("Credencial QR no válida para autorización de capataz")

        val compensatorio = registrarPesaje(
            Pesaje(
                jornaleroId = original.jornaleroId,
                cantidadJabas = original.cantidadJabas,
                pesoBruto = Math.round(nuevoPesoBrutoKg * 100.0) / 100.0,
                taraTotal = taraAplicada,
                pesoNeto = evaluacion.pesoNuevoNetoKg,
                timestamp = timestamp,
                esTaraPorDefecto = original.esTaraPorDefecto,
                esCompensatorio = true,
                pesajeOriginalId = original.id
            )
        )

        val reclamo = ReclamoPeso(
            id = ++secuenciaReclamo,
            fecha = fecha,
            hora = hora,
            timestamp = timestamp,
            jornaleroId = original.jornaleroId,
            jornaleroNombre = jornaleroNombre,
            pesajeOriginalId = original.id,
            pesoOriginalNetoKg = original.pesoNeto,
            pesoBrutoReclamoKg = nuevoPesoBrutoKg,
            taraAplicadaKg = taraAplicada,
            pesoNuevoNetoKg = evaluacion.pesoNuevoNetoKg,
            diferenciaKg = evaluacion.diferenciaKg,
            diferenciaGramos = evaluacion.diferenciaGramos,
            diferenciaAbsolutaGramos = evaluacion.diferenciaAbsolutaGramos,
            excedeTolerancia = true,
            estado = EstadoReclamoPeso.COMPENSADO,
            pesajeCompensatorioId = compensatorio.id,
            pesoCompensatorioKg = compensatorio.pesoNeto,
            capatazAutorizador = capataz.nombre,
            codigoQrCapataz = capataz.codigoQr
        )
        bitacoraReclamos.add(reclamo)
        return ResultadoRegistroReclamo.CompensacionRegistrada(reclamo, original, compensatorio)
    }

    /** Bitácora de reclamos de peso ordenada del más reciente al más antiguo. */
    fun obtenerBitacoraReclamos(): List<ReclamoPeso> =
        bitacoraReclamos.sortedByDescending { it.id }

    // ------------------------------------------------------------------------
    // Calibracion de tara (AGV-015)
    // ------------------------------------------------------------------------

    /** Guarda una calibracion y la marca como la vigente del turno. */
    fun guardarCalibracion(calibracion: CalibracionTara): CalibracionTara {
        val guardada = calibracion.copy(id = ++secuenciaCalibracion)
        calibraciones.add(guardada)
        return guardada
    }

    /** Ultima calibracion registrada (o `null` si todavia no se calibro). */
    fun ultimaCalibracion(): CalibracionTara? = calibraciones.lastOrNull()

    // ------------------------------------------------------------------------
    // Arranque / reinicio del turno
    // ------------------------------------------------------------------------

    /** Deja el padron intacto y limpia la bitacora del turno. */
    fun reiniciarTurno() {
        asignaciones.clear()
        asistencias.clear()
        bitacoraIngresosManuales.clear()
        pesajes.clear()
        calibraciones.clear()
        bitacoraReclamos.clear()
        secuenciaAsistencia = 0L
        secuenciaIngresoManual = 0L
        secuenciaPesaje = 0L
        secuenciaCalibracion = 0L
        secuenciaReclamo = 0L
    }

    private fun generarPadron(): MutableList<Jornalero> = MutableList(TOTAL_PADRON) { i ->
        // El id es 1..30 para que el primero coincida con QR_AGV_001.
        Jornalero(
            id = (i + 1).toLong(),
            dni = "%08d".format(40_000_000L + (i * 137L) + 1234L),
            nombres = NOMBRES[i % NOMBRES.size],
            apellidos = "${APELLIDOS[i % APELLIDOS.size]} ${APELLIDOS[(i + 7) % APELLIDOS.size]}",
            codigoQr = "QR_AGV_%03d".format(i + 1),
            cuadrillaCodigo = CODIGO_CUADRILLA
        )
    }
}
