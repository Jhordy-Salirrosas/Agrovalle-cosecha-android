package com.agrovalle.cosecha.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Calibrar Tara (AGV-015).
 * Gestiona la secuencia de 5 jabas vacías y el cálculo del promedio.
 */
class TareCalibrationViewModel(
    private val repository: AgroValleRepository
) : ViewModel() {

    companion object {
        const val TOTAL_JABAS = 5
        const val TARA_POR_DEFECTO = 0.375
    }

    // ── Estado de la secuencia de calibración ──
    private val _pesosRegistrados = MutableStateFlow<List<Double>>(emptyList())
    val pesosRegistrados: StateFlow<List<Double>> = _pesosRegistrados.asStateFlow()

    private val _pasoActual = MutableStateFlow(1)
    val pasoActual: StateFlow<Int> = _pasoActual.asStateFlow()

    private val _pesoActualSimulado = MutableStateFlow(TARA_POR_DEFECTO)
    val pesoActualSimulado: StateFlow<Double> = _pesoActualSimulado.asStateFlow()

    private val _pesoEstable = MutableStateFlow(true)
    val pesoEstable: StateFlow<Boolean> = _pesoEstable.asStateFlow()

    private val _calibracionCompleta = MutableStateFlow(false)
    val calibracionCompleta: StateFlow<Boolean> = _calibracionCompleta.asStateFlow()

    private val _taraPromedio = MutableStateFlow<Double?>(null)
    val taraPromedio: StateFlow<Double?> = _taraPromedio.asStateFlow()

    private val _calibracionOmitida = MutableStateFlow(false)
    val calibracionOmitida: StateFlow<Boolean> = _calibracionOmitida.asStateFlow()

    // Simulación de pesos ligeramente variados para cada jaba
    private val pesosSimulados = listOf(0.370, 0.380, 0.375, 0.372, 0.378)

    val completedSteps: Int
        get() = _pesosRegistrados.value.size

    /** Registra el peso de la jaba actual y avanza al siguiente paso. */
    fun registrarPeso() {
        val pesos = _pesosRegistrados.value.toMutableList()
        val indice = pesos.size
        if (indice >= TOTAL_JABAS) return

        val pesoActual = pesosSimulados.getOrElse(indice) { TARA_POR_DEFECTO }
        pesos.add(pesoActual)
        _pesosRegistrados.value = pesos

        if (pesos.size >= TOTAL_JABAS) {
            _calibracionCompleta.value = true
            _taraPromedio.value = pesos.average()
        } else {
            _pasoActual.value = pesos.size + 1
            // Simular peso de la siguiente jaba
            _pesoActualSimulado.value = pesosSimulados.getOrElse(pesos.size) { TARA_POR_DEFECTO }
        }
    }

    /** Confirma la calibración y guarda el promedio en el repositorio. */
    fun confirmarCalibracion() {
        viewModelScope.launch {
            val pesos = _pesosRegistrados.value
            if (pesos.size == TOTAL_JABAS) {
                repository.guardarCalibracionTara(pesos)
            }
        }
    }

    /** Omite la calibración y usa tara por defecto (0.375 kg). */
    fun omitirCalibracion() {
        viewModelScope.launch {
            repository.usarTaraPorDefecto()
            _calibracionOmitida.value = true
        }
    }

    /** Reinicia el estado de calibración para una nueva sesión. */
    fun reiniciar() {
        _pesosRegistrados.value = emptyList()
        _pasoActual.value = 1
        _pesoActualSimulado.value = TARA_POR_DEFECTO
        _calibracionCompleta.value = false
        _taraPromedio.value = null
        _calibracionOmitida.value = false
    }
}
