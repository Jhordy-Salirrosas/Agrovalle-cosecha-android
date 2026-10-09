package com.dapm.android.agrovalle.ui.pesaje

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dapm.android.agrovalle.data.local.dao.CalibracionTaraDao
import com.dapm.android.agrovalle.data.local.entities.CalibracionTaraEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para la calibración de tara de jabas (AGV-015).
 * Calcula el promedio de 5 jabas vacías y valida el rango [0.350, 0.400] kg.
 */
class CalibracionTaraViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val calibracionTaraDao: CalibracionTaraDao
) : ViewModel() {

    // ─── Estado de UI ───────────────────────────────────────
    private val _resultadoCalibracion = MutableStateFlow<ResultadoCalibracion?>(null)
    val resultadoCalibracion: StateFlow<ResultadoCalibracion?> = _resultadoCalibracion.asStateFlow()

    private val _mensajeUi = MutableStateFlow<String?>(null)
    val mensajeUi: StateFlow<String?> = _mensajeUi.asStateFlow()

    private val _calibracionExitosa = MutableSharedFlow<Boolean>()
    val calibracionExitosa: SharedFlow<Boolean> = _calibracionExitosa.asSharedFlow()

    // ─── Calibración vigente reactiva ───────────────────────
    val calibracionVigente: StateFlow<CalibracionTaraEntity?> = calibracionTaraDao
        .observarUltimaCalibracion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ─── Operaciones ────────────────────────────────────────

    /**
     * Ejecuta la calibración de tara:
     * 1. Calcula promedio = pesoMuestra / 5
     * 2. Valida que esté en rango [0.350, 0.400] kg
     * 3. Persiste el resultado
     */
    fun calibrarTara(pesoMuestra5Jabas: Double) {
        if (pesoMuestra5Jabas <= 0) {
            _mensajeUi.value = "Ingrese un peso válido mayor a 0"
            return
        }

        val promedio = Math.round((pesoMuestra5Jabas / 5.0) * 1000.0) / 1000.0
        val estaAprobado = promedio in 0.350..0.400

        viewModelScope.launch {
            val calibracion = calibracionTaraDao.calibrarTara(
                pesoMuestra5Jabas = pesoMuestra5Jabas,
                nombreAsistente = "Maribel Cárdenas Tello"
            )

            _resultadoCalibracion.value = ResultadoCalibracion(
                taraPromedio = promedio,
                estaAprobado = estaAprobado,
                pesoMuestra = pesoMuestra5Jabas
            )

            if (estaAprobado) {
                _mensajeUi.value = "✅ Calibración aprobada: $promedio kg por jaba"
                _calibracionExitosa.emit(true)
            } else {
                _mensajeUi.value = "❌ Calibración rechazada: $promedio kg fuera de rango [0.350 – 0.400]"
                _calibracionExitosa.emit(false)
            }
        }
    }

    /** Limpia el resultado para una nueva calibración. */
    fun limpiarResultado() {
        _resultadoCalibracion.value = null
        _mensajeUi.value = null
    }

    // ─── Modelos internos ───────────────────────────────────

    data class ResultadoCalibracion(
        val taraPromedio: Double,
        val estaAprobado: Boolean,
        val pesoMuestra: Double
    )

    // ─── Factory ────────────────────────────────────────────

    class Factory(
        private val calibracionTaraDao: CalibracionTaraDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalibracionTaraViewModel(
                SavedStateHandle(),
                calibracionTaraDao
            ) as T
        }
    }
}
