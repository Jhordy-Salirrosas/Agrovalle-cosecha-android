package com.agrovalle.cosecha.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.model.Jornalero
import com.agrovalle.cosecha.data.model.PesajeResultado
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para las pantallas de Registrar Pesaje (AGV-003) y Confirmación (AGV-004).
 * Gestiona el flujo completo: escaneo de fotocheck → ingreso de peso/jabas → cálculo → guardado.
 */
class WeighingViewModel(
    private val repository: AgroValleRepository
) : ViewModel() {

    // ── Estado del jornalero escaneado ──
    private val _jornaleroEscaneado = MutableStateFlow<Jornalero?>(null)
    val jornaleroEscaneado: StateFlow<Jornalero?> = _jornaleroEscaneado.asStateFlow()

    // ── Campos de entrada ──
    private val _pesoBrutoTexto = MutableStateFlow("")
    val pesoBrutoTexto: StateFlow<String> = _pesoBrutoTexto.asStateFlow()

    private val _cantidadJabas = MutableStateFlow(0)
    val cantidadJabas: StateFlow<Int> = _cantidadJabas.asStateFlow()

    // ── Tara (reactiva desde repositorio) ──
    val taraActual: StateFlow<Double> = repository.taraActual
    val esTaraPorDefecto: StateFlow<Boolean> = repository.esTaraPorDefecto

    // ── Cálculos en vivo ──
    val pesoBruto: Double
        get() = _pesoBrutoTexto.value.toDoubleOrNull() ?: 0.0

    val taraTotal: Double
        get() = taraActual.value * _cantidadJabas.value

    val pesoNeto: Double
        get() {
            val neto = pesoBruto - taraTotal
            return if (neto > 0) neto else 0.0
        }

    /** Validación: botón "Guardar Pesaje" solo habilitado si peso > 0 y jabas > 0. */
    val formularioValido: Boolean
        get() = pesoBruto > 0 && _cantidadJabas.value > 0 && _jornaleroEscaneado.value != null

    // ── Resultado del pesaje guardado ──
    private val _pesajeResultado = MutableStateFlow<PesajeResultado?>(null)
    val pesajeResultado: StateFlow<PesajeResultado?> = _pesajeResultado.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    // ── Jornaleros mock para simulación de escaneo ──
    private val jornalerosMockPesaje = listOf(
        Jornalero(1, "1234", "Luis", "Pérez", "QR-001", "CUAD-A"),
        Jornalero(2, "5678", "María", "López", "QR-002", "CUAD-A"),
        Jornalero(3, "9012", "Pedro", "García", "QR-003", "CUAD-A")
    )
    private var indiceFotocheck = 0

    /** Simula el escaneo del fotocheck de un jornalero. */
    fun simularEscaneoFotocheck() {
        val jornalero = jornalerosMockPesaje[indiceFotocheck % jornalerosMockPesaje.size]
        _jornaleroEscaneado.value = jornalero
        indiceFotocheck++

        // Precargar valores del wireframe para visualización inmediata si están vacíos
        if (_pesoBrutoTexto.value.isEmpty()) {
            _pesoBrutoTexto.value = "9.50"
        }
        if (_cantidadJabas.value == 0) {
            _cantidadJabas.value = 4
        }
    }

    // ── Teclado numérico de peso ──
    fun agregarDigitoPeso(digito: String) {
        if (_jornaleroEscaneado.value == null) return
        val actual = _pesoBrutoTexto.value
        // Evitar múltiples puntos decimales
        if (digito == "." && actual.contains(".")) return
        // Limitar a 2 decimales
        if (actual.contains(".") && actual.substringAfter(".").length >= 2) return
        // Limitar longitud total
        if (actual.length >= 6) return
        _pesoBrutoTexto.value = actual + digito
    }

    fun borrarDigitoPeso() {
        if (_pesoBrutoTexto.value.isNotEmpty()) {
            _pesoBrutoTexto.value = _pesoBrutoTexto.value.dropLast(1)
        }
    }

    // ── Cantidad de jabas ──
    fun agregarJabas(cantidad: Int) {
        val nuevo = _cantidadJabas.value + cantidad
        if (nuevo >= 0) {
            _cantidadJabas.value = nuevo
        }
    }

    fun setJabas(cantidad: Int) {
        if (cantidad >= 0) {
            _cantidadJabas.value = cantidad
        }
    }

    /** Guarda el pesaje y genera el resultado con cálculos. */
    fun guardarPesaje() {
        val jornalero = _jornaleroEscaneado.value ?: return
        val peso = pesoBruto
        val jabas = _cantidadJabas.value

        if (peso <= 0 || jabas <= 0) return

        viewModelScope.launch {
            val resultado = repository.registrarPesaje(
                jornaleroId = jornalero.id,
                jornaleroNombre = jornalero.nombreCompleto(),
                pesoBruto = peso,
                cantidadJabas = jabas
            )
            resultado.fold(
                onSuccess = { _pesajeResultado.value = it },
                onFailure = { _mensaje.value = it.message }
            )
        }
    }

    /**
     * Limpia todos los campos para el siguiente jornalero.
     * Regla 9: "Al presionar Siguiente Jornalero, limpiar Peso Bruto, Jabas y QR."
     */
    fun limpiarParaSiguiente() {
        _jornaleroEscaneado.value = null
        _pesoBrutoTexto.value = ""
        _cantidadJabas.value = 0
        _pesajeResultado.value = null
        _mensaje.value = null
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }
}
