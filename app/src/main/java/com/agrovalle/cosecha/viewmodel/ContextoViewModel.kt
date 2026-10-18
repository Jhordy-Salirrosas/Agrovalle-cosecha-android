package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.Jornalero

/**
 * AGV-002: contexto agronomico del turno - SIN Room.
 *
 * La variedad se AUTOCOMPLETA a partir del cuartel (DatosMemoria.mapaCuarteles)
 * y al presionar "ASIGNAR Y FINALIZAR" se guarda en
 * [DatosMemoria.asignaciones] que jornalero va a que cuartel.
 *
 * El estado del formulario (cuartel elegido) se conserva en
 * [SavedStateHandle] para sobrevivir a la rotacion o a la muerte del proceso.
 */
class ContextoViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _jornalero = MutableLiveData<Jornalero?>(null)
    val jornalero: LiveData<Jornalero?> = _jornalero

    private val _variedadAuto = MutableLiveData<String?>(null)
    val variedadAuto: LiveData<String?> = _variedadAuto

    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    /** Cuartel elegido en el Spinner; se persiste entre rotaciones. */
    var cuartelSeleccionado: String
        get() = savedStateHandle[KEY_CUARTEL] ?: CUARTEL_VACIO
        set(value) {
            savedStateHandle[KEY_CUARTEL] = value
            _variedadAuto.value = DatosMemoria.obtenerVariedadPorCuartel(value)
        }

    /** ID recibido por Intent (EXTRA_JORNALERO_ID). */
    var jornaleroId: Long
        get() = savedStateHandle[KEY_JORNALERO_ID] ?: 0L
        set(value) {
            savedStateHandle[KEY_JORNALERO_ID] = value
        }

    /** Opciones del Spinner de cuartel. */
    fun cuarteles(): List<String> = DatosMemoria.opcionesCuartel

    /** Carga el jornalero recibido y pre-selecciona su cuartel si ya tenia uno. */
    fun cargarJornalero(id: Long) {
        jornaleroId = id
        val encontrado = DatosMemoria.buscarJornaleroPorId(id)
        _jornalero.value = encontrado

        val asignado = DatosMemoria.obtenerCuartelAsignado(id)
        if (asignado != null) {
            cuartelSeleccionado = asignado
        } else {
            _variedadAuto.value = DatosMemoria.obtenerVariedadPorCuartel(cuartelSeleccionado)
        }
    }

    /**
     * Cambia el cuartel y autocompleta la variedad.
     * @return la variedad asociada, o `null` si el cuartel no es valido.
     */
    fun seleccionarCuartel(cuartel: String): String? {
        cuartelSeleccionado = cuartel
        return _variedadAuto.value
    }

    /** La variedad actual ya calculada. */
    fun variedadActual(): String? = _variedadAuto.value

    /** Habilita el boton solo si el cuartel es valido y hay jornalero cargado. */
    fun puedeAsignar(): Boolean = esCuartelValido() && _jornalero.value != null

    private fun esCuartelValido(): Boolean = cuartelSeleccionado != CUARTEL_VACIO

    /**
     * AGV-002: guarda la asignacion jornalero -> cuartel en memoria.
     * @return `true` si se guardo correctamente.
     */
    fun asignarYFinalizar(): Boolean {
        val jornaleroActual = _jornalero.value
        if (jornaleroActual == null) {
            _mensaje.value = "No se recibió un jornalero válido. Vuelva a escanear su fotocheck."
            return false
        }
        if (!esCuartelValido()) {
            _mensaje.value = "Seleccione un cuartel para continuar."
            return false
        }

        DatosMemoria.asignarCuartel(jornaleroActual.id, cuartelSeleccionado)
        val variedad = DatosMemoria.obtenerVariedadPorCuartel(cuartelSeleccionado) ?: "-"
        _mensaje.value =
            "${jornaleroActual.nombreCompleto} asignado a $cuartelSeleccionado ($variedad)"
        return true
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    companion object {
        /** Texto del prompt del Spinner; nunca es un cuartel seleccionable. */
        const val CUARTEL_VACIO = "Seleccione Cuartel..."

        private const val KEY_CUARTEL = "key_contexto_cuartel"
        private const val KEY_JORNALERO_ID = "key_contexto_jornalero_id"
    }
}
