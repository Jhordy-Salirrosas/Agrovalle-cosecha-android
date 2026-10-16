package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.AgroValleApp
import com.agrovalle.cosecha.data.local.entity.CalibracionTaraEntity
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno
import kotlinx.coroutines.launch

/**
 * AGV-015: calibracion de tara al inicio del turno.
 *
 * Se pesan 5 jabas vacias; la tara del turno es el promedio por jaba. Si el
 * promedio cae fuera del rango valido (350-400 g) la calibracion se marca como
 * no aprobada y se conserva la tara por defecto.
 */
class CalibracionTaraViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as AgroValleApp).repository

    private val pesos = mutableListOf<Double>()

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

    /** Carga la tara vigente guardada en Room (o la de defecto si no hay). */
    fun cargarTaraVigente() {
        viewModelScope.launch {
            val ultima = repo.getUltimaCalibracion()
            _taraVigente.value = if (ultima != null && ultima.aprobado) {
                ultima.taraPromedioCalculada
            } else {
                Constantes.TARA_POR_DEFECTO
            }
        }
    }

    fun registrarPesoMuestra(peso: Double) {
        if (pesos.size >= Constantes.MUESTRAS_CALIBRACION) return
        pesos.add(peso)
        _pesosMuestra.value = pesos.toList()
        _pasoActual.value = pesos.size
        calcularPromedio()
    }

    fun quitarUltimaMuestra() {
        if (pesos.isEmpty()) return
        pesos.removeAt(pesos.size - 1)
        _pesosMuestra.value = pesos.toList()
        _pasoActual.value = pesos.size
        calcularPromedio()
    }

    fun reiniciar() {
        pesos.clear()
        _pesosMuestra.value = emptyList()
        _pasoActual.value = 0
        _calibracionValida.value = false
        _taraPromedio.value = Constantes.TARA_POR_DEFECTO
    }

    private fun calcularPromedio() {
        if (pesos.isEmpty()) {
            _taraPromedio.value = Constantes.TARA_POR_DEFECTO
            _calibracionValida.value = false
            return
        }
        val promedio = pesos.average()
        val redondeado = redondear3(promedio)
        _taraPromedio.value = redondeado
        _calibracionValida.value = pesos.size == Constantes.MUESTRAS_CALIBRACION &&
            redondeado in Constantes.TARA_MIN..Constantes.TARA_MAX
    }

    /** AGV-015: guarda la tara del turno en Room con fecha, hora y responsable. */
    fun confirmarYGuardarTara(onGuardado: (Double) -> Unit) {
        viewModelScope.launch {
            val promedio = _taraPromedio.value ?: Constantes.TARA_POR_DEFECTO
            val totalMuestra = redondear3(pesos.sum())
            val aprobado = _calibracionValida.value == true

            repo.guardarCalibracion(
                CalibracionTaraEntity(
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
    }

    /**
     * Omite la calibracion y registra el uso de la tara por defecto (AGV-003,
     * escenario 2: el pesaje debe quedar marcado como tara por defecto).
     */
    fun usarTaraPorDefecto(onListo: (Double) -> Unit) {
        viewModelScope.launch {
            repo.guardarCalibracion(
                CalibracionTaraEntity(
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
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0
}
