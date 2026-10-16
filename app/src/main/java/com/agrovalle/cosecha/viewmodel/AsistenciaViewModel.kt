package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.AgroValleApp
import com.agrovalle.cosecha.data.local.entity.AsistenciaEntity
import com.agrovalle.cosecha.data.mapper.aDominio
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno
import kotlinx.coroutines.launch

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
 * Logica de la asistencia diaria:
 *  - AGV-001: registro por lectura de fotocheck, marca horaria exacta, deteccion
 *    de tardanza, rechazo de credenciales invalidas y de duplicados.
 *  - AGV-016: plan B por ultimos digitos del DNI (ingreso manual).
 */
class AsistenciaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as AgroValleApp).repository

    private val _contador = MutableLiveData(0)
    val contador: LiveData<Int> = _contador

    private val _resultado = MutableLiveData<ResultadoAsistencia?>(null)
    val resultado: LiveData<ResultadoAsistencia?> = _resultado

    private val _coincidencias = MutableLiveData<List<Jornalero>>(emptyList())
    val coincidencias: LiveData<List<Jornalero>> = _coincidencias

    private fun fechaTurno(): String =
        SesionTurno.fechaJornada.value ?: FechaHoraUtil.hoy()

    fun actualizarContador() {
        viewModelScope.launch {
            _contador.value = repo.contarAsistencias(fechaTurno())
        }
    }

    /** AGV-001: el capataz escanea el QR / Code 128 del fotocheck. */
    fun registrarPorQr(codigoQr: String) {
        viewModelScope.launch {
            val jornalero = repo.buscarJornaleroPorQr(codigoQr)
            if (jornalero == null) {
                _resultado.value = ResultadoAsistencia(
                    exitoso = false,
                    mensaje = "Credencial no válida: el código no pertenece al padrón de la cuadrilla"
                )
                return@launch
            }
            registrar(jornalero.id, jornalero.aDominio(), "QR_SCAN")
        }
    }

    /** AGV-016: ingreso manual confirmado desde la pantalla de búsqueda por DNI. */
    fun registrarManual(jornalero: Jornalero) {
        viewModelScope.launch {
            registrar(jornalero.id, jornalero, "DNI_MANUAL")
        }
    }

    private suspend fun registrar(jornaleroId: Long, jornalero: Jornalero, metodo: String) {
        val fecha = fechaTurno()

        val existente = repo.buscarAsistencia(jornaleroId, fecha)
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

        val insertado = repo.registrarAsistencia(
            AsistenciaEntity(
                jornaleroId = jornaleroId,
                fecha = fecha,
                horaIngreso = hora,
                estado = estado,
                metodo = metodo
            )
        )

        if (insertado == -1L) {
            _resultado.value = ResultadoAsistencia(
                exitoso = false,
                mensaje = "Asistencia duplicada: ${jornalero.nombreCompleto} ya fue registrado hoy"
            )
            return
        }

        _contador.value = repo.contarAsistencias(fecha)
        _resultado.value = ResultadoAsistencia(
            exitoso = true,
            mensaje = "Asistencia registrada",
            jornalero = jornalero,
            horaIngreso = hora,
            estado = estado,
            metodo = metodo
        )
    }

    /** AGV-016: coincidencias en el padrón local por los últimos dígitos del DNI. */
    fun buscarPorUltimosDigitos(digitos: String) {
        viewModelScope.launch {
            val lista = if (digitos.isBlank()) {
                emptyList()
            } else {
                repo.buscarJornalerosPorUltimosDigitos(digitos).map { it.aDominio() }
            }
            _coincidencias.value = lista
        }
    }

    fun consumirResultado() {
        _resultado.value = null
    }

    companion object {
        const val ESTADO_PUNTUAL = "PUNTUAL"
        const val ESTADO_TARDANZA = "TARDANZA"
        const val METODO_QR = "QR_SCAN"
        const val METODO_DNI = "DNI_MANUAL"
    }
}
