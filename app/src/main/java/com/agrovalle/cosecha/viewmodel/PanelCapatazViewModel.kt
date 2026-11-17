package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.data.repository.ResumenPanelCapataz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PanelCapatazUiState(
    val resumen: ResumenPanelCapataz = ResumenPanelCapataz(),
    val rendimientoPorJornalero: Map<String, Int> = emptyMap()
)

/**
 * ViewModel para el panel del capataz (Sprint 2).
 */
class PanelCapatazViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val _uiState = MutableStateFlow(PanelCapatazUiState())
    val uiState: StateFlow<PanelCapatazUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observarResumenPanel(),
                repository.observarRendimientoPorJornalero()
            ) { resumen, rendimiento ->
                PanelCapatazUiState(
                    resumen = resumen,
                    rendimientoPorJornalero = rendimiento
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
