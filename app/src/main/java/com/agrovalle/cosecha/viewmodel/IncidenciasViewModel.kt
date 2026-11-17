package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.EstadoIncidencia
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.util.SesionTurno
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidenciasUiState(
    val incidencias: List<IncidenciaEntity> = emptyList(),
    val conteoPendientes: Int = 0,
    val parihuelasRetenidas: List<ParihuelaEntity> = emptyList(),
    val parihuelasRetenidasIds: Set<Long> = emptySet(),
    val mensaje: String? = null,
    val esError: Boolean = false
)

/**
 * ViewModel para la pantalla de incidencias de mezcla (Sprint 2 - AGV-007).
 */
class IncidenciasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val _uiState = MutableStateFlow(IncidenciasUiState())
    val uiState: StateFlow<IncidenciasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observarTodasLasIncidencias(),
                repository.observarParihuelasRetenidas()
            ) { lista, retenidas ->
                lista to retenidas
            }.collect { (lista, retenidas) ->
                _uiState.update {
                    it.copy(
                        incidencias = lista,
                        conteoPendientes = lista.count { inc -> inc.estado == EstadoIncidencia.PENDIENTE },
                        parihuelasRetenidas = retenidas,
                        parihuelasRetenidasIds = retenidas.map(ParihuelaEntity::id).toSet()
                    )
                }
            }
        }
    }

    fun resolverIncidencia(incidenciaId: Long, accion: String) {
        val responsable = SesionTurno.operador.value ?: SesionTurno.rol.value ?: "Capataz"
        viewModelScope.launch {
            try {
                repository.resolverIncidencia(
                    incidenciaId = incidenciaId,
                    accion = accion,
                    responsable = responsable
                )
                _uiState.update {
                    it.copy(
                        mensaje = "Incidencia #$incidenciaId marcada como REVISADA ($accion).",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mensaje = e.message ?: "No se pudo resolver la incidencia",
                        esError = true
                    )
                }
            }
        }
    }

    fun liberarParihuela(parihuelaId: Long) {
        val responsable = SesionTurno.operador.value ?: SesionTurno.rol.value ?: "Capataz"
        viewModelScope.launch {
            try {
                val liberada = repository.liberarParihuelaRetenida(
                    parihuelaId = parihuelaId,
                    responsable = responsable
                )
                _uiState.update {
                    it.copy(
                        mensaje = "Parihuela ${liberada.codigo} liberada por $responsable.",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mensaje = e.message ?: "No se pudo liberar la parihuela",
                        esError = true
                    )
                }
            }
        }
    }
}
