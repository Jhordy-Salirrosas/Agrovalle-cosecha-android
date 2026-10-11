package com.agrovalle.cosecha.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.model.ParihuelajState
import com.agrovalle.cosecha.data.model.Variedad
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Contexto del Turno (AGV-002).
 * Gestiona selección de cuartel/CLP/variedad y estado de parihuela.
 */
class HarvestContextViewModel(
    private val repository: AgroValleRepository
) : ViewModel() {

    // ── Selectores de contexto ──
    private val _cuartelSeleccionado = MutableStateFlow<String?>(null)
    val cuartelSeleccionado: StateFlow<String?> = _cuartelSeleccionado.asStateFlow()

    private val _clpSeleccionado = MutableStateFlow<String?>(null)
    val clpSeleccionado: StateFlow<String?> = _clpSeleccionado.asStateFlow()

    private val _variedadSeleccionada = MutableStateFlow<Variedad?>(null)
    val variedadSeleccionada: StateFlow<Variedad?> = _variedadSeleccionada.asStateFlow()

    // ── Estado de parihuela (reactivo desde repositorio) ──
    val parihuelaActiva: StateFlow<ParihuelajState?> = repository.parihuelaActiva

    // ── Datos para selectores ──
    val cuarteles: List<String> = repository.obtenerCuarteles()
    val clps: List<String> = repository.obtenerClps()

    // ── Mensaje de feedback ──
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    /** Validación: los 3 campos deben estar seleccionados para abrir parihuela. */
    val formularioCompleto: Boolean
        get() = _cuartelSeleccionado.value != null &&
                _clpSeleccionado.value != null &&
                _variedadSeleccionada.value != null

    fun seleccionarCuartel(cuartel: String) {
        _cuartelSeleccionado.value = cuartel
    }

    fun seleccionarClp(clp: String) {
        _clpSeleccionado.value = clp
    }

    fun seleccionarVariedad(variedad: Variedad) {
        _variedadSeleccionada.value = variedad
    }

    /** Abre una nueva parihuela con el contexto seleccionado. */
    fun abrirParihuela(onSuccess: () -> Unit = {}) {
        val cuartel = _cuartelSeleccionado.value ?: return
        val clp = _clpSeleccionado.value ?: return
        val variedad = _variedadSeleccionada.value ?: return

        viewModelScope.launch {
            val resultado = repository.abrirParihuela(cuartel, clp, variedad)
            resultado.fold(
                onSuccess = {
                    _mensaje.value = "Parihuela ${it.codigo} abierta correctamente"
                    onSuccess()
                },
                onFailure = {
                    _mensaje.value = it.message
                }
            )
        }
    }

    /** Cierra la parihuela activa. */
    fun cerrarParihuela() {
        viewModelScope.launch {
            val resultado = repository.cerrarParihuela()
            resultado.fold(
                onSuccess = {
                    _mensaje.value = "Parihuela cerrada correctamente"
                    // Limpiar selectores para permitir nuevo contexto
                    _cuartelSeleccionado.value = null
                    _clpSeleccionado.value = null
                    _variedadSeleccionada.value = null
                },
                onFailure = {
                    _mensaje.value = it.message
                }
            )
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}
