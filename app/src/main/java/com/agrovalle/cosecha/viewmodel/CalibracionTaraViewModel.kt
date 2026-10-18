package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.CalibracionTara
import com.agrovalle.cosecha.util.CalculadoraPesaje
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno

/**
 * AGV-015: calibracion de tara al inicio del turno - SIN Room.
 *
 * Se pesan 5 jabas vacias; la tara del turno es el promedio por jaba. Si el
 * promedio cae fuera del rango valido (350-400 g) la calibracion se marca como
 * no aprobada y se conserva la tara por defecto.
 *
 * Las muestras se guardan en [SavedStateHandle] (sobreviven a la rotacion) y el
 * historial de calibraciones en [DatosMemoria].
 */
class CalibracionTaraViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _pasoActual = MutableLiveData(0)
    val pasoActual: LiveData<Int> = _pasoActual

    private val _pesosMuestra = MutableLiveData<List<Double>>(emptyList())
    val pesosMuestra: LiveData<List<Double>> = _pesosMuestra

    private val _taraPromedio = MutableLiveData(Constantes.TARA_POR_DEFECTO)
    val taraPromedio: LiveData<Double> = _taraPromedio

    private val _calibracionValida = MutableLiveData(false)
    val calibracionValida: LiveData<Boolean> = _calibracionValida

    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    private val _taraVigente = MutableLiveData(Constantes.TARA_POR_DEFECTO)
    val taraVigente: LiveData<Double> = _taraVigente

    /** Muestras registradas; viven en SavedStateHandle para sobrevivir a la rotacion. */
    private var pesos: List<Double>
        get() = savedStateHandle[KEY_MUESTRAS] ?: emptyList()
        set(value) {
            savedStateHandle[KEY_MUESTRAS] = ArrayList(value)
        }

    init {
        // Al recrear la Activity (rotacion) se restaura lo ya muestreado.
        val restauradas = pesos
        if (restauradas.isNotEmpty()) {
            _pesosMuestra.value = restauradas
            _pasoActual.value = restauradas.size
            calcularPromedio()
        }
    }

    /** Carga la tara vigente del turno (o la de defecto si no hay calibracion). */
    fun cargarTaraVigente() {
        val ultima = DatosMemoria.ultimaCalibracion()
        _taraVigente.value = if (ultima != null && ultima.aprobado) {
            ultima.taraPromedioCalculada
        } else {
            Constantes.TARA_POR_DEFECTO
        }
    }

    fun registrarPesoMuestra(peso: Double) {
        val actuales = pesos
        if (actuales.size >= Constantes.MUESTRAS_CALIBRACION) return
        val nuevas = actuales + peso
        pesos = nuevas
        _pesosMuestra.value = nuevas
        _pasoActual.value = nuevas.size
        calcularPromedio()
    }

    fun quitarUltimaMuestra() {
        val actuales = pesos
        if (actuales.isEmpty()) return
        val nuevas = actuales.dropLast(1)
        pesos = nuevas
        _pesosMuestra.value = nuevas
        _pasoActual.value = nuevas.size
        calcularPromedio()
    }

    fun reiniciar() {
        pesos = emptyList()
        _pesosMuestra.value = emptyList()
        _pasoActual.value = 0
        _calibracionValida.value = false
        _taraPromedio.value = Constantes.TARA_POR_DEFECTO
    }

    private fun calcularPromedio() {
        val actuales = pesos
        if (actuales.isEmpty()) {
            _taraPromedio.value = Constantes.TARA_POR_DEFECTO
            _calibracionValida.value = false
            return
        }
        val redondeado = CalculadoraPesaje.promedioMuestras(actuales)
        _taraPromedio.value = redondeado
        _calibracionValida.value = actuales.size == Constantes.MUESTRAS_CALIBRACION &&
            CalculadoraPesaje.taraValida(redondeado)
    }

    /** AGV-015: guarda la tara del turno en memoria con fecha, hora y responsable. */
    fun confirmarYGuardarTara(onGuardado: (Double) -> Unit) {
        val promedio = _taraPromedio.value ?: Constantes.TARA_POR_DEFECTO
        val totalMuestra = redondear3(pesos.sum())
        val aprobado = _calibracionValida.value == true

        DatosMemoria.guardarCalibracion(
            CalibracionTara(
                taraPromedioCalculada = promedio,
                pesoMuestra5Jabas = totalMuestra,
                asistenteNombre = SesionTurno.operador.value ?: "Asistente de pesaje",
                timestamp = FechaHoraUtil.timestampActual(),
                aprobado = aprobado
            )
        )
        _taraVigente.value = promedio
        _mensaje.value = if (aprobado) {
            "Tara calibrada: ${"%.3f".format(promedio)} kg por jaba"
        } else {
            "Muestreo incompleto o fuera de rango: se usará la tara de ${Constantes.TARA_POR_DEFECTO} kg"
        }
        onGuardado(promedio)
    }

    /**
     * Omite la calibracion y registra el uso de la tara por defecto (AGV-003,
     * escenario 2: el pesaje debe quedar marcado como tara por defecto).
     */
    fun usarTaraPorDefecto(onListo: (Double) -> Unit) {
        DatosMemoria.guardarCalibracion(
            CalibracionTara(
                taraPromedioCalculada = Constantes.TARA_POR_DEFECTO,
                pesoMuestra5Jabas = 0.0,
                asistenteNombre = SesionTurno.operador.value ?: "Asistente de pesaje",
                timestamp = FechaHoraUtil.timestampActual(),
                aprobado = false
            )
        )
        _taraVigente.value = Constantes.TARA_POR_DEFECTO
        _mensaje.value = "Se usará la tara por defecto de ${Constantes.TARA_POR_DEFECTO} kg por jaba"
        onListo(Constantes.TARA_POR_DEFECTO)
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0

    companion object {
        private const val KEY_MUESTRAS = "key_calibracion_muestras"
    }
}
