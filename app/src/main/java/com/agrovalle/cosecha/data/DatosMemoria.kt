package com.agrovalle.cosecha.data

import com.agrovalle.cosecha.model.Asistencia
import com.agrovalle.cosecha.model.CalibracionTara
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.model.Pesaje

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

    /** Pesajes registrados (AGV-003 / AGV-004). */
    val pesajes: MutableList<Pesaje> = mutableListOf()

    /** Calibraciones de tara del turno (AGV-015). */
    val calibraciones: MutableList<CalibracionTara> = mutableListOf()

    private var secuenciaAsistencia = 0L
    private var secuenciaPesaje = 0L
    private var secuenciaCalibracion = 0L

    // ------------------------------------------------------------------------
    // Consultas sobre el padron
    // ------------------------------------------------------------------------

    /** Busca un jornalero por su codigo de fotocheck (AGV-001). */
    fun buscarJornaleroPorQr(codigoQr: String): Jornalero? =
        listaJornaleros.firstOrNull { it.codigoQr.equals(codigoQr.trim(), ignoreCase = true) }

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

    /** Pesajes de un jornalero. */
    fun pesajesDeJornalero(jornaleroId: Long): List<Pesaje> =
        pesajes.filter { it.jornaleroId == jornaleroId }

    /** AGV-004: acumulado diario del cosechador entre dos marcas de tiempo. */
    fun acumuladoDiario(jornaleroId: Long, desde: Long, hasta: Long): Double =
        pesajes.filter { it.jornaleroId == jornaleroId && it.timestamp in desde..hasta }
            .sumOf { it.pesoNeto }

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
        pesajes.clear()
        calibraciones.clear()
        secuenciaAsistencia = 0L
        secuenciaPesaje = 0L
        secuenciaCalibracion = 0L
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
