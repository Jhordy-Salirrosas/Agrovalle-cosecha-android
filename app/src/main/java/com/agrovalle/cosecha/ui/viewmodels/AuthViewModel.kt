package com.agrovalle.cosecha.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.model.Jornada
import com.agrovalle.cosecha.data.model.Rol
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel compartido para la sesión/turno del usuario.
 * Gestiona el rol seleccionado, jornada, fundo y sector.
 * Se comparte entre todas las pantallas vía el NavGraph.
 */
class AuthViewModel(
    private val repository: AgroValleRepository
) : ViewModel() {

    // ── Estado del formulario de login ──
    private val _rolSeleccionado = MutableStateFlow<Rol?>(null)
    val rolSeleccionado: StateFlow<Rol?> = _rolSeleccionado.asStateFlow()

    private val _jornadaSeleccionada = MutableStateFlow<Jornada?>(null)
    val jornadaSeleccionada: StateFlow<Jornada?> = _jornadaSeleccionada.asStateFlow()

    private val _fundoSeleccionado = MutableStateFlow<String?>(null)
    val fundoSeleccionado: StateFlow<String?> = _fundoSeleccionado.asStateFlow()

    private val _sectorSeleccionado = MutableStateFlow<String?>(null)
    val sectorSeleccionado: StateFlow<String?> = _sectorSeleccionado.asStateFlow()

    // ── Sesión activa ──
    private val _sesionActiva = MutableStateFlow(false)
    val sesionActiva: StateFlow<Boolean> = _sesionActiva.asStateFlow()

    // ── Datos para selectores ──
    val fundos: List<String> = repository.obtenerFundos()
    val sectores: List<String> = repository.obtenerSectores()

    // ── Validación del formulario ──
    val formularioValido: Boolean
        get() = _rolSeleccionado.value != null &&
                _jornadaSeleccionada.value != null &&
                _fundoSeleccionado.value != null &&
                _sectorSeleccionado.value != null

    fun seleccionarRol(rol: Rol) {
        _rolSeleccionado.value = rol
    }

    fun seleccionarJornada(jornada: Jornada) {
        _jornadaSeleccionada.value = jornada
    }

    fun seleccionarFundo(fundo: String) {
        _fundoSeleccionado.value = fundo
    }

    fun seleccionarSector(sector: String) {
        _sectorSeleccionado.value = sector
    }

    /** Inicia el turno y navega al menú principal. */
    fun iniciarTurno() {
        _sesionActiva.value = true
    }

    /** Cierra el turno y limpia toda la sesión. */
    fun cerrarTurno() {
        _sesionActiva.value = false
        _rolSeleccionado.value = null
        _jornadaSeleccionada.value = null
        _fundoSeleccionado.value = null
        _sectorSeleccionado.value = null
        repository.reiniciarTurno()
    }
}
