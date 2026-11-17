package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.RegistroIngresoManual
import com.agrovalle.cosecha.model.ResumenCuadrillaCierre
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CierreTurnoUiState(
    val fechaJornada: String = "",
    val jornada: String = "",
    val fundo: String = "",
    val capataz: String = "",
    val resumenPorCuadrilla: List<ResumenCuadrillaCierre> = emptyList(),
    val bitacoraIngresosManuales: List<RegistroIngresoManual> = emptyList()
)

/**
 * ViewModel del reporte local de ingresos manuales al cierre de turno (AGV-016 - Escenario 2.0).
 */
class CierreTurnoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CierreTurnoUiState())
    val uiState: StateFlow<CierreTurnoUiState> = _uiState.asStateFlow()

    init {
        cargarReporte()
    }

    fun cargarReporte() {
        val fecha = SesionTurno.fechaJornada.value ?: FechaHoraUtil.hoy()
        val capataz = SesionTurno.operador.value?.takeIf { it.isNotBlank() }
            ?: SesionTurno.rol.value
            ?: SesionTurno.ROL_CAPATAZ

        _uiState.value = CierreTurnoUiState(
            fechaJornada = fecha,
            jornada = SesionTurno.jornada.value.orEmpty(),
            fundo = SesionTurno.fundo.value.orEmpty(),
            capataz = capataz,
            resumenPorCuadrilla = DatosMemoria.obtenerResumenCierrePorCuadrilla(fecha),
            bitacoraIngresosManuales = DatosMemoria.obtenerBitacoraIngresosManuales(fecha)
        )
    }
}
