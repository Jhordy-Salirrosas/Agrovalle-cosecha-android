package com.agrovalle.cosecha.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.model.AsistenciaRegistro
import com.agrovalle.cosecha.data.model.Jornalero
import com.agrovalle.cosecha.data.model.TipoIngreso
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para las pantallas de Asistencia QR (AGV-001) y Búsqueda por DNI (AGV-016).
 * Gestiona el contador de asistentes, escaneo QR simulado y búsqueda por DNI.
 */
class AttendanceViewModel(
    private val repository: AgroValleRepository
) : ViewModel() {

    // ── Contador de asistencia (reactivo desde repositorio) ──
    val contadorAsistencia: StateFlow<Int> = repository.contadorAsistencia
    val totalTrabajadores: Int = repository.totalTrabajadores
    val ultimoRegistro: StateFlow<AsistenciaRegistro?> = repository.ultimoRegistroAsistencia

    // ── Búsqueda por DNI ──
    private val _digitosDni = MutableStateFlow("")
    val digitosDni: StateFlow<String> = _digitosDni.asStateFlow()

    private val _resultadosBusqueda = MutableStateFlow<List<Jornalero>>(emptyList())
    val resultadosBusqueda: StateFlow<List<Jornalero>> = _resultadosBusqueda.asStateFlow()

    private val _buscando = MutableStateFlow(false)
    val buscando: StateFlow<Boolean> = _buscando.asStateFlow()

    private val _hasBuscado = MutableStateFlow(false)
    val hasBuscado: StateFlow<Boolean> = _hasBuscado.asStateFlow()

    // ── Mensajes de feedback ──
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _esError = MutableStateFlow(false)
    val esError: StateFlow<Boolean> = _esError.asStateFlow()

    /** Simula un escaneo QR exitoso con un jornalero aleatorio del padrón. */
    fun simularEscaneoQr(
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // Simular escaneo del siguiente jornalero disponible
            val codigosQr = listOf("QR-001", "QR-002", "QR-003", "QR-004", "QR-005",
                                    "QR-006", "QR-007", "QR-008", "QR-009", "QR-010")
            val indice = contadorAsistencia.value % codigosQr.size
            val codigoQr = codigosQr[indice]

            val jornalero = repository.buscarJornaleroPorQr(codigoQr)
            if (jornalero != null) {
                val resultado = repository.registrarAsistencia(jornalero.id, TipoIngreso.QR)
                resultado.fold(
                    onSuccess = {
                        _mensaje.value = "✓ ${it.nombreCompleto} registrado a las ${it.horaRegistro}"
                        _esError.value = false
                        onSuccess()
                    },
                    onFailure = {
                        _mensaje.value = it.message
                        _esError.value = true
                        onError()
                    }
                )
            } else {
                _mensaje.value = "QR no reconocido"
                _esError.value = true
                onError()
            }
        }
    }

    /** Agrega un dígito al campo de búsqueda DNI (máximo 4). */
    fun agregarDigito(digito: String) {
        if (_digitosDni.value.length < 4) {
            _digitosDni.value += digito
        }
    }

    /** Elimina el último dígito del campo de búsqueda. */
    fun borrarDigito() {
        if (_digitosDni.value.isNotEmpty()) {
            _digitosDni.value = _digitosDni.value.dropLast(1)
        }
    }

    /** Ejecuta la búsqueda por DNI contra el padrón mock. */
    fun buscarPorDni() {
        val digitos = _digitosDni.value
        if (digitos.isBlank()) return

        _buscando.value = true
        _hasBuscado.value = true
        viewModelScope.launch {
            repository.buscarJornaleroPorDni(digitos).collect { resultados ->
                _resultadosBusqueda.value = resultados
                _buscando.value = false
            }
        }
    }

    /** Confirma la asistencia de un jornalero encontrado por DNI. */
    fun confirmarAsistenciaPorDni(
        jornalero: Jornalero,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val resultado = repository.registrarAsistencia(jornalero.id, TipoIngreso.MANUAL)
            resultado.fold(
                onSuccess = {
                    _mensaje.value = "✓ ${it.nombreCompleto} registrado manualmente"
                    _esError.value = false
                    onSuccess()
                },
                onFailure = {
                    _mensaje.value = it.message
                    _esError.value = true
                    onError()
                }
            )
        }
    }

    /** Limpia el estado de búsqueda al salir de la pantalla de DNI. */
    fun limpiarBusqueda() {
        _digitosDni.value = ""
        _resultadosBusqueda.value = emptyList()
        _hasBuscado.value = false
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}
