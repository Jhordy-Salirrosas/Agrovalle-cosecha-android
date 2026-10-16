package com.agrovalle.cosecha.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Estado de la sesion de turno COMPARTIDO entre todas las Activities.
 *
 * Resuelve el problema detectado en la auditoria: cada Activity tenia su propio
 * ViewModel y el rol/jornada/fundo se reiniciaban al navegar. Aqui el estado del
 * turno es unico para toda la aplicacion (patron singleton observable).
 */
object SesionTurno {

    const val ROL_CAPATAZ = "Capataz"
    const val ROL_ASISTENTE = "Asistente"

    private const val JORNADA_MANANA = "Jornada Mañana (06:00 - 14:00)"
    private const val JORNADA_TARDE = "Jornada Tarde (14:00 - 22:00)"
    private const val FUNDO_DEFECTO = "Fundo Principal Virú"

    private val _rol = MutableLiveData(ROL_CAPATAZ)
    val rol: LiveData<String> = _rol

    private val _jornada = MutableLiveData(JORNADA_MANANA)
    val jornada: LiveData<String> = _jornada

    private val _fundo = MutableLiveData(FUNDO_DEFECTO)
    val fundo: LiveData<String> = _fundo

    private val _operador = MutableLiveData("Operador")
    val operador: LiveData<String> = _operador

    private val _fechaJornada = MutableLiveData(FechaHoraUtil.hoy())
    val fechaJornada: LiveData<String> = _fechaJornada

    const val JORNADA_MANANA_TXT = JORNADA_MANANA
    const val JORNADA_TARDE_TXT = JORNADA_TARDE

    val esJornadaTarde: Boolean
        get() = _jornada.value?.contains("Tarde", ignoreCase = true) == true

    fun iniciar(
        rol: String,
        jornada: String,
        fundo: String,
        operador: String? = null
    ) {
        _rol.value = rol
        _jornada.value = jornada
        _fundo.value = fundo
        _operador.value = operador ?: rol
        _fechaJornada.value = FechaHoraUtil.hoy()
    }

    fun cerrar() {
        _rol.value = ROL_CAPATAZ
        _jornada.value = JORNADA_MANANA
        _fundo.value = FUNDO_DEFECTO
        _operador.value = "Operador"
        _fechaJornada.value = FechaHoraUtil.hoy()
    }
}
