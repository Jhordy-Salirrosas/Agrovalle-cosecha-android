package com.dapm.android.agrovalle.ui.asistencia

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dapm.android.agrovalle.data.local.dao.AsistenciaDao
import com.dapm.android.agrovalle.data.local.dao.JornaleroDao
import com.dapm.android.agrovalle.data.local.entities.AsistenciaEntity
import com.dapm.android.agrovalle.data.local.entities.JornaleroEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel para el módulo de Asistencia (AGV-001, AGV-016).
 * Gestiona la búsqueda de jornaleros por DNI o QR, el registro
 * de asistencia con clasificación automática PUNTUAL/TARDANZA,
 * y los contadores reactivos del día.
 */
class AsistenciaViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val jornaleroDao: JornaleroDao,
    private val asistenciaDao: AsistenciaDao
) : ViewModel() {

    // ─── Estado de UI ───────────────────────────────────────
    private val _jornaleroEncontrado = MutableStateFlow<JornaleroEntity?>(null)
    val jornaleroEncontrado: StateFlow<JornaleroEntity?> = _jornaleroEncontrado.asStateFlow()

    private val _mensajeUi = MutableStateFlow<MensajeUi?>(null)
    val mensajeUi: StateFlow<MensajeUi?> = _mensajeUi.asStateFlow()

    private val _registroExitoso = MutableSharedFlow<ResultadoAsistencia>()
    val registroExitoso: SharedFlow<ResultadoAsistencia> = _registroExitoso.asSharedFlow()

    private val _metodoRegistro = MutableStateFlow("DNI_MANUAL")

    // ─── Contadores reactivos del día ───────────────────────
    private val fechaHoy: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val contadorAsistencias: StateFlow<Int> = asistenciaDao
        .contarAsistenciasDelDia(fechaHoy)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val contadorPuntuales: StateFlow<Int> = asistenciaDao
        .contarPuntuales(fechaHoy)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val contadorTardanzas: StateFlow<Int> = asistenciaDao
        .contarTardanzas(fechaHoy)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Operaciones ────────────────────────────────────────

    /** Busca un jornalero por su DNI (8 dígitos exactos). */
    fun buscarJornaleroPorDni(dni: String) {
        if (dni.length != 8) {
            _mensajeUi.value = MensajeUi("Ingrese un DNI válido de 8 dígitos", TipoMensaje.ERROR)
            return
        }
        viewModelScope.launch {
            val jornalero = jornaleroDao.buscarJornaleroPorDni(dni)
            if (jornalero != null) {
                _jornaleroEncontrado.value = jornalero
                _metodoRegistro.value = "DNI_MANUAL"
                _mensajeUi.value = null
            } else {
                _jornaleroEncontrado.value = null
                _mensajeUi.value = MensajeUi("Jornalero no encontrado con DNI: $dni", TipoMensaje.ERROR)
            }
        }
    }

    /** Busca un jornalero por el código QR de su fotocheck. */
    fun obtenerJornaleroPorQr(codigoQr: String) {
        viewModelScope.launch {
            val jornalero = jornaleroDao.obtenerJornaleroPorQr(codigoQr)
            if (jornalero != null) {
                _jornaleroEncontrado.value = jornalero
                _metodoRegistro.value = "QR_SCAN"
                _mensajeUi.value = null
            } else {
                _jornaleroEncontrado.value = null
                _mensajeUi.value = MensajeUi("QR no reconocido: $codigoQr", TipoMensaje.ERROR)
            }
        }
    }

    /**
     * Registra la asistencia del jornalero actualmente seleccionado.
     * Clasifica automáticamente como PUNTUAL (<= 06:00) o TARDANZA (06:01-06:30).
     */
    fun registrarAsistencia() {
        val jornalero = _jornaleroEncontrado.value ?: return
        viewModelScope.launch {
            val ahora = LocalTime.now()
            val horaIngreso = ahora.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            // Clasificación automática según hora
            val estado = if (ahora.hour < 6 || (ahora.hour == 6 && ahora.minute == 0 && ahora.second == 0)) {
                "PUNTUAL"
            } else {
                "TARDANZA"
            }

            val asistencia = AsistenciaEntity(
                jornaleroId = jornalero.id,
                fecha = fechaHoy,
                horaIngreso = horaIngreso,
                estadoAsistencia = estado,
                metodoRegistro = _metodoRegistro.value
            )

            val resultado = asistenciaDao.registrarAsistenciaSiNoExiste(asistencia)

            if (resultado != -1L) {
                val res = ResultadoAsistencia(
                    nombreJornalero = jornalero.nombreCompleto(),
                    estado = estado,
                    hora = horaIngreso,
                    exitoso = true
                )
                _registroExitoso.emit(res)
                _jornaleroEncontrado.value = null
                _mensajeUi.value = MensajeUi(
                    "✅ Asistencia registrada: ${jornalero.nombreCompleto()} - $estado",
                    TipoMensaje.EXITO
                )
            } else {
                _mensajeUi.value = MensajeUi(
                    "⚠️ ${jornalero.nombreCompleto()} ya tiene asistencia registrada hoy",
                    TipoMensaje.ADVERTENCIA
                )
                _registroExitoso.emit(ResultadoAsistencia(exitoso = false, yaRegistrado = true))
            }
        }
    }

    /** Limpia el jornalero seleccionado para una nueva búsqueda. */
    fun limpiarSeleccion() {
        _jornaleroEncontrado.value = null
        _mensajeUi.value = null
    }

    /** Determina el estado de asistencia que se asignaría a la hora actual (para preview en UI). */
    fun obtenerEstadoActual(): String {
        val ahora = LocalTime.now()
        return if (ahora.hour < 6 || (ahora.hour == 6 && ahora.minute == 0 && ahora.second == 0)) {
            "PUNTUAL"
        } else {
            "TARDANZA"
        }
    }

    // ─── Modelos internos ───────────────────────────────────

    data class MensajeUi(val texto: String, val tipo: TipoMensaje)
    enum class TipoMensaje { EXITO, ADVERTENCIA, ERROR }

    data class ResultadoAsistencia(
        val nombreJornalero: String = "",
        val estado: String = "",
        val hora: String = "",
        val exitoso: Boolean = false,
        val yaRegistrado: Boolean = false
    )

    // ─── Factory ────────────────────────────────────────────

    class Factory(
        private val jornaleroDao: JornaleroDao,
        private val asistenciaDao: AsistenciaDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AsistenciaViewModel(
                SavedStateHandle(),
                jornaleroDao,
                asistenciaDao
            ) as T
        }
    }
}
