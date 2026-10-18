package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno

/** Resultado de un intento de registro de asistencia (AGV-001 / AGV-016). */
data class ResultadoAsistencia(
    val exitoso: Boolean = false,
    val mensaje: String = "",
    val jornalero: Jornalero? = null,
    val horaIngreso: String = "",
    val estado: String = "",
    val metodo: String = ""
)

/**
 * Logica de la asistencia diaria - SIN Room.
 *
 *  - AGV-001: registro por lectura de fotocheck, marca horaria exacta, deteccion
 *    de tardanza, rechazo de credenciales invalidas y de duplicados.
 *  - AGV-016: plan B por ultimos digitos del DNI (ingreso manual).
 *
 * Los datos se leen y escriben directamente en [DatosMemoria] (listas/arreglos
 * en memoria) y el estado en curso se conserva en [SavedStateHandle] para
 * sobrevivir a la muerte del proceso.
 */
class AsistenciaViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _contador = MutableLiveData(0)
    val contador: LiveData<Int> = _contador

    private val _resultado = MutableLiveData<ResultadoAsistencia?>(null)
    val resultado: LiveData<ResultadoAsistencia?> = _resultado

    private val _coincidencias = MutableLiveData<List<Jornalero>>(emptyList())
    val coincidencias: LiveData<List<Jornalero>> = _coincidencias

    /** Ultimo fotocheck escaneado; se conserva entre rotaciones y muerte de proceso. */
    var ultimoCodigoEscaneado: String?
        get() = savedStateHandle[KEY_ULTIMO_CODIGO]
        set(value) {
            savedStateHandle[KEY_ULTIMO_CODIGO] = value
        }

    /** Ultimos digitos escritos en la pantalla de busqueda por DNI (AGV-016). */
    var ultimosDigitos: String
        get() = savedStateHandle[KEY_DIGITOS] ?: ""
        set(value) {
            savedStateHandle[KEY_DIGITOS] = value
        }

    /** Metodo usado en el ultimo registro: QR_SCAN o DNI_MANUAL. */
    var ultimoMetodo: String
        get() = savedStateHandle[KEY_METODO] ?: METODO_QR
        set(value) {
            savedStateHandle[KEY_METODO] = value
        }

    /**
     * ID del jornalero cuya asistencia se acaba de registrar. Es el valor que la
     * Activity envia a ContextoAgronomicoActivity por el Intent.
     */
    var jornaleroRegistradoId: Long
        get() = savedStateHandle[KEY_JORNALERO_ID] ?: 0L
        set(value) {
            savedStateHandle[KEY_JORNALERO_ID] = value
        }

    private fun fechaTurno(): String =
        SesionTurno.fechaJornada.value ?: FechaHoraUtil.hoy()

    fun actualizarContador() {
        _contador.value = DatosMemoria.contarAsistencias(fechaTurno())
    }

    /** AGV-001: el capataz escanea el QR / Code 128 del fotocheck. */
    fun registrarPorQr(codigoQr: String) {
        ultimoCodigoEscaneado = codigoQr.trim()
        val jornalero = DatosMemoria.buscarJornaleroPorQr(codigoQr)
        if (jornalero == null) {
            _resultado.value = ResultadoAsistencia(
                exitoso = false,
                mensaje = "Credencial no válida: el código no pertenece al padrón de la cuadrilla"
            )
            return
        }
        registrar(jornalero, METODO_QR)
    }

    /** AGV-016: ingreso manual confirmado desde la pantalla de búsqueda por DNI. */
    fun registrarManual(jornalero: Jornalero) {
        ultimosDigitos = jornalero.dni.takeLast(4)
        registrar(jornalero, METODO_DNI)
    }

    private fun registrar(jornalero: Jornalero, metodo: String) {
        val fecha = fechaTurno()

        val existente = DatosMemoria.buscarAsistencia(jornalero.id, fecha)
        if (existente != null) {
            _resultado.value = ResultadoAsistencia(
                exitoso = false,
                mensaje = "${jornalero.nombreCompleto} ya registró su asistencia hoy (${existente.horaIngreso})"
            )
            return
        }

        val hora = FechaHoraUtil.ahoraHora()
        val estado = if (FechaHoraUtil.esTardanza(hora, SesionTurno.esJornadaTarde)) {
            ESTADO_TARDANZA
        } else {
            ESTADO_PUNTUAL
        }

        val registrada = DatosMemoria.registrarAsistencia(
            jornaleroId = jornalero.id,
            fecha = fecha,
            horaIngreso = hora,
            estado = estado,
            metodo = metodo
        )

        if (registrada == null) {
            _resultado.value = ResultadoAsistencia(
                exitoso = false,
                mensaje = "Asistencia duplicada: ${jornalero.nombreCompleto} ya fue registrado hoy"
            )
            return
        }

        jornaleroRegistradoId = jornalero.id
        ultimoMetodo = metodo
        _contador.value = DatosMemoria.contarAsistencias(fecha)
        _resultado.value = ResultadoAsistencia(
            exitoso = true,
            mensaje = "Asistencia registrada",
            jornalero = jornalero,
            horaIngreso = hora,
            estado = estado,
            metodo = metodo
        )
    }

    /** AGV-016: coincidencias en el padron en memoria por los últimos dígitos del DNI. */
    fun buscarPorUltimosDigitos(digitos: String) {
        ultimosDigitos = digitos
        _coincidencias.value = if (digitos.isBlank()) {
            emptyList()
        } else {
            DatosMemoria.buscarJornalerosPorUltimosDigitos(digitos)
        }
    }

    fun consumirResultado() {
        _resultado.value = null
    }

    /** Limpia los datos en curso para el siguiente jornalero. */
    fun limpiar() {
        ultimoCodigoEscaneado = null
        jornaleroRegistradoId = 0L
        _coincidencias.value = emptyList()
    }

    companion object {
        const val ESTADO_PUNTUAL = "PUNTUAL"
        const val ESTADO_TARDANZA = "TARDANZA"
        const val METODO_QR = "QR_SCAN"
        const val METODO_DNI = "DNI_MANUAL"

        private const val KEY_ULTIMO_CODIGO = "key_ultimo_codigo_qr"
        private const val KEY_DIGITOS = "key_ultimos_digitos"
        private const val KEY_METODO = "key_ultimo_metodo"
        private const val KEY_JORNALERO_ID = "key_jornalero_registrado_id"
    }
}
