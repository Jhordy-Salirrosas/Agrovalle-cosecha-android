package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.ReasignacionEntity
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.data.repository.ResumenPanelCapataz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PanelCapatazUiState(
    val resumen: ResumenPanelCapataz = ResumenPanelCapataz(),
    val rendimientoPorJornalero: Map<String, Int> = emptyMap(),
    val reasignaciones: List<ReasignacionEntity> = emptyList(),
    val cuartelesDisponibles: List<String> = DatosMemoria.mapaCuarteles.keys.toList(),
    val mensaje: String? = null,
    val esError: Boolean = false
)

/**
 * ViewModel para el panel del capataz (Sprint 2 - AGV-005).
 */
class PanelCapatazViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val refreshTrigger = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(PanelCapatazUiState())
    val uiState: StateFlow<PanelCapatazUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observarResumenPanel(),
                repository.observarRendimientoPorJornalero(),
                repository.observarReasignaciones(),
                refreshTrigger
            ) { resumen, rendimiento, reasignaciones, _ ->
                Triple(resumen, rendimiento, reasignaciones)
            }.collect { (resumen, rendimiento, reasignaciones) ->
                val cuarteles = (
                    DatosMemoria.mapaCuarteles.keys +
                        resumen.jabasPorCuartel.keys +
                        resumen.jornalerosPorCuartel.keys
                    ).distinct().sorted()
                _uiState.update { current ->
                    current.copy(
                        resumen = resumen,
                        rendimientoPorJornalero = rendimiento,
                        reasignaciones = reasignaciones,
                        cuartelesDisponibles = cuarteles
                    )
                }
            }
        }
    }

    fun refrescar() {
        refreshTrigger.update { it + 1 }
    }

    fun reasignarJornalero(
        jornalero: String,
        cuartelOrigen: String,
        cuartelDestino: String,
        motivo: String
    ) {
        viewModelScope.launch {
            try {
                val registrada = repository.reasignarJornaleroACuartel(
                    jornalero = jornalero,
                    cuartelDestino = cuartelDestino,
                    motivo = motivo,
                    cuartelOrigen = cuartelOrigen
                )
                refreshTrigger.update { it + 1 }
                _uiState.update {
                    it.copy(
                        mensaje = "${registrada.jornalero} reasignado de ${registrada.cuartelOrigen} a ${registrada.cuartelDestino} (${registrada.motivo}).",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mensaje = e.message ?: "No se pudo completar la reasignación",
                        esError = true
                    )
                }
            }
        }
    }
}
