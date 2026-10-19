package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.util.SesionTurno

/**
 * Pantalla 01 - Login / Seleccion de Rol (Sprint 1) - SIN Room.
 *
 * Guarda en [SavedStateHandle] la posicion de los tres Spinners de configuracion
 * del turno (rol, jornada y fundo), de modo que la seleccion sobreviva a la
 * rotacion de pantalla sin usar `onSaveInstanceState`.
 *
 * Al confirmar el turno, el estado se publica en la sesion compartida
 * [SesionTurno] para que el resto de pantallas lo conserven.
 */
class LoginViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ------------------------------------------------------------------------
    // Opciones de los Spinners (fuente unica de verdad)
    // ------------------------------------------------------------------------

    val roles: List<String> = listOf(ROL_CAPATAZ_TXT, ROL_ASISTENTE_TXT)

    val jornadas: List<String> = listOf(
        SesionTurno.JORNADA_MANANA_TXT,
        SesionTurno.JORNADA_TARDE_TXT
    )

    val fundos: List<String> = listOf(FUNDO_PRINCIPAL, FUNDO_CHAO)

    // ------------------------------------------------------------------------
    // Posiciones seleccionadas; viven en SavedStateHandle
    // ------------------------------------------------------------------------

    /** Posicion elegida en el Spinner de Rol. */
    var posicionRol: Int
        get() = savedStateHandle[KEY_POS_ROL] ?: 0
        set(value) {
            savedStateHandle[KEY_POS_ROL] = value
        }

    /** Posicion elegida en el Spinner de Jornada. */
    var posicionJornada: Int
        get() = savedStateHandle[KEY_POS_JORNADA] ?: 0
        set(value) {
            savedStateHandle[KEY_POS_JORNADA] = value
        }

    /** Posicion elegida en el Spinner de Fundo. */
    var posicionFundo: Int
        get() = savedStateHandle[KEY_POS_FUNDO] ?: 0
        set(value) {
            savedStateHandle[KEY_POS_FUNDO] = value
        }

    // ------------------------------------------------------------------------
    // Derivados
    // ------------------------------------------------------------------------

    /** Rol normalizado (Capataz / Asistente) segun la posicion del Spinner. */
    fun rolSeleccionado(): String =
        if (posicionRol == 0) SesionTurno.ROL_CAPATAZ else SesionTurno.ROL_ASISTENTE

    /** Texto de la jornada elegida. */
    fun jornadaSeleccionada(): String =
        jornadas.getOrElse(posicionJornada) { SesionTurno.JORNADA_MANANA_TXT }

    /** Texto del fundo elegido. */
    fun fundoSeleccionado(): String =
        fundos.getOrElse(posicionFundo) { FUNDO_PRINCIPAL }

    /**
     * Guarda el turno en la sesion compartida [SesionTurno].
     * @return la jornada y el fundo publicados, para que la Activity navegue.
     */
    fun iniciarTurno(): SesionIniciada {
        val rol = rolSeleccionado()
        val jornada = jornadaSeleccionada()
        val fundo = fundoSeleccionado()

        SesionTurno.iniciar(
            rol = rol,
            jornada = jornada,
            fundo = fundo,
            operador = if (rol == SesionTurno.ROL_CAPATAZ) {
                "Capataz de cuadrilla"
            } else {
                "Asistente de pesaje"
            }
        )
        return SesionIniciada(rol, jornada, fundo)
    }

    /** Datos del turno ya confirmado (usado por la Activity para navegar). */
    data class SesionIniciada(
        val rol: String,
        val jornada: String,
        val fundo: String
    )

    companion object {
        const val ROL_CAPATAZ_TXT = "Capataz de Cuadrilla"
        const val ROL_ASISTENTE_TXT = "Asistente de Pesaje"

        private const val FUNDO_PRINCIPAL = "Fundo Principal Virú"
        private const val FUNDO_CHAO = "Fundo Chao Norte"

        private const val KEY_POS_ROL = "key_pos_rol"
        private const val KEY_POS_JORNADA = "key_pos_jornada"
        private const val KEY_POS_FUNDO = "key_pos_fundo"
    }
}
