package com.dapm.android.agrovalle.ui.pesaje

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dapm.android.agrovalle.data.local.dao.*
import com.dapm.android.agrovalle.data.local.entities.EntregaPesajeEntity
import com.dapm.android.agrovalle.data.local.entities.JornaleroEntity
import com.dapm.android.agrovalle.data.local.entities.ParihuelaEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para el módulo de Pesaje Ágil (AGV-002, AGV-003, AGV-004).
 * Gestiona la búsqueda de jornalero, selección de jabas, cálculo
 * automático de tara y peso neto, y persistencia con inmutabilidad SENASA.
 */
class PesajeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val jornaleroDao: JornaleroDao,
    private val parihuelaDao: ParihuelaDao,
    private val entregaPesajeDao: EntregaPesajeDao,
    private val calibracionTaraDao: CalibracionTaraDao
) : ViewModel() {

    // ─── Estado de UI ───────────────────────────────────────
    private val _jornaleroSeleccionado = MutableStateFlow<JornaleroEntity?>(null)
    val jornaleroSeleccionado: StateFlow<JornaleroEntity?> = _jornaleroSeleccionado.asStateFlow()

    private val _cantidadJabas = MutableStateFlow(4)
    val cantidadJabas: StateFlow<Int> = _cantidadJabas.asStateFlow()

    private val _pesoBruto = MutableStateFlow(0.0)

    private val _taraActual = MutableStateFlow(0.375) // Nominal por defecto

    private val _calculos = MutableStateFlow<CalculosPesaje?>(null)
    val calculos: StateFlow<CalculosPesaje?> = _calculos.asStateFlow()

    private val _mensajeUi = MutableStateFlow<MensajeUi?>(null)
    val mensajeUi: StateFlow<MensajeUi?> = _mensajeUi.asStateFlow()

    private val _pesajeGuardado = MutableSharedFlow<ResultadoPesaje>()
    val pesajeGuardado: SharedFlow<ResultadoPesaje> = _pesajeGuardado.asSharedFlow()

    // ─── Parihuela activa reactiva ──────────────────────────
    val parihuelaActiva: StateFlow<ParihuelaEntity?> = parihuelaDao
        .observarParihuelaAbierta()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ─── Motivos de observación disponibles ─────────────────
    val motivosObservacion = listOf(
        "ERROR_CONTEO",
        "JABA_DANADA",
        "FRUTA_DEFECTO",
        "RECLAMO_JORNALERO"
    )

    init {
        // Cargar tara calibrada vigente
        viewModelScope.launch {
            val tara = calibracionTaraDao.obtenerTaraActual()
            if (tara != null) {
                _taraActual.value = tara
            }
        }
    }

    // ─── Operaciones ────────────────────────────────────────

    /** Busca jornalero por DNI para asignar la entrega. */
    fun buscarJornaleroPorDni(dni: String) {
        if (dni.length != 8) return
        viewModelScope.launch {
            val jornalero = jornaleroDao.buscarJornaleroPorDni(dni)
            _jornaleroSeleccionado.value = jornalero
            if (jornalero == null) {
                _mensajeUi.value = MensajeUi("Jornalero no encontrado", TipoMensaje.ERROR)
            } else {
                _mensajeUi.value = null
            }
        }
    }

    /** Establece la cantidad de jabas seleccionada (3, 4 o 6). */
    fun seleccionarCantidadJabas(cantidad: Int) {
        _cantidadJabas.value = cantidad
        recalcular()
    }

    /** Establece el peso bruto ingresado y recalcula automáticamente. */
    fun establecerPesoBruto(peso: Double) {
        _pesoBruto.value = peso
        recalcular()
    }

    /** Recalcula tara total y peso neto con la tara calibrada vigente. */
    private fun recalcular() {
        val pesoBruto = _pesoBruto.value
        if (pesoBruto <= 0) {
            _calculos.value = null
            return
        }
        val jabas = _cantidadJabas.value
        val tara = _taraActual.value
        val taraTotal = Math.round(jabas * tara * 1000.0) / 1000.0
        val pesoNeto = Math.round((pesoBruto - taraTotal) * 1000.0) / 1000.0

        _calculos.value = CalculosPesaje(
            cantidadJabas = jabas,
            taraPorJaba = tara,
            taraTotal = taraTotal,
            pesoNeto = pesoNeto
        )
    }

    /**
     * Guarda el pesaje aplicando regla de inmutabilidad SENASA.
     * Los registros nunca se borran ni editan; las correcciones
     * se hacen con registros compensatorios.
     */
    fun guardarPesaje(
        estaObservado: Boolean = false,
        motivoObservacion: String? = null
    ) {
        val jornalero = _jornaleroSeleccionado.value
        val parihuela = parihuelaActiva.value
        val calc = _calculos.value

        if (jornalero == null || parihuela == null || calc == null) {
            _mensajeUi.value = MensajeUi("Complete todos los campos", TipoMensaje.ERROR)
            return
        }

        if (calc.pesoNeto <= 0) {
            _mensajeUi.value = MensajeUi("El peso neto debe ser mayor a 0", TipoMensaje.ERROR)
            return
        }

        viewModelScope.launch {
            val entrega = EntregaPesajeEntity(
                jornaleroId = jornalero.id,
                parihuelaId = parihuela.id,
                cantidadJabas = calc.cantidadJabas,
                pesoBruto = _pesoBruto.value,
                taraTotal = calc.taraTotal,
                pesoNeto = calc.pesoNeto,
                estaObservado = estaObservado,
                motivoObservacion = if (estaObservado) motivoObservacion else null
            )

            val resultado = entregaPesajeDao.guardarPesaje(entrega, parihuelaDao)

            if (resultado != -1L) {
                _pesajeGuardado.emit(
                    ResultadoPesaje(
                        nombreJornalero = jornalero.nombreCompleto(),
                        pesoNeto = calc.pesoNeto,
                        jabas = calc.cantidadJabas,
                        exitoso = true
                    )
                )
                limpiarFormulario()
                _mensajeUi.value = MensajeUi(
                    "✅ Pesaje guardado: ${jornalero.apellidos} · ${calc.pesoNeto} kg",
                    TipoMensaje.EXITO
                )
            } else {
                _mensajeUi.value = MensajeUi(
                    "⚠️ No se pudo guardar. La parihuela puede estar sellada.",
                    TipoMensaje.ERROR
                )
                _pesajeGuardado.emit(ResultadoPesaje(exitoso = false))
            }
        }
    }

    /** Limpia el formulario para un nuevo pesaje. */
    fun limpiarFormulario() {
        _jornaleroSeleccionado.value = null
        _pesoBruto.value = 0.0
        _cantidadJabas.value = 4
        _calculos.value = null
    }

    // ─── Modelos internos ───────────────────────────────────

    data class CalculosPesaje(
        val cantidadJabas: Int,
        val taraPorJaba: Double,
        val taraTotal: Double,
        val pesoNeto: Double
    )

    data class MensajeUi(val texto: String, val tipo: TipoMensaje)
    enum class TipoMensaje { EXITO, ADVERTENCIA, ERROR }

    data class ResultadoPesaje(
        val nombreJornalero: String = "",
        val pesoNeto: Double = 0.0,
        val jabas: Int = 0,
        val exitoso: Boolean = false
    )

    // ─── Factory ────────────────────────────────────────────

    class Factory(
        private val jornaleroDao: JornaleroDao,
        private val parihuelaDao: ParihuelaDao,
        private val entregaPesajeDao: EntregaPesajeDao,
        private val calibracionTaraDao: CalibracionTaraDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PesajeViewModel(
                SavedStateHandle(),
                jornaleroDao,
                parihuelaDao,
                entregaPesajeDao,
                calibracionTaraDao
            ) as T
        }
    }
}
