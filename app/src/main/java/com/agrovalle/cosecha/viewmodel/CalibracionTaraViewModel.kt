package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.CalibracionTara
import com.agrovalle.cosecha.model.CredencialCapataz
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

    /**
     * Peso de la muestra que se esta ajustando con +/- antes de registrarla.
     * Vive en SavedStateHandle (no en la Activity) para sobrevivir a la rotacion.
     */
    var pesoActualMuestra: Double
        get() = savedStateHandle[KEY_PESO_MUESTRA] ?: Constantes.TARA_POR_DEFECTO
        set(value) {
            savedStateHandle[KEY_PESO_MUESTRA] = value
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
        pesoActualMuestra = Constantes.TARA_POR_DEFECTO
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

    /** Verifica que el muestreo esté listo antes de abrir el escáner de QR del capataz (H-12). */
    fun validarListoParaAutorizacionQr(): Boolean {
        if (pesos.size < Constantes.MUESTRAS_CALIBRACION) {
            _mensaje.value = "Complete las ${Constantes.MUESTRAS_CALIBRACION} muestras antes de guardar la tara."
            return false
        }
        return true
    }

    /**
     * Indica si omitir la calibración modificaría una tara previamente aprobada,
     * en cuyo caso también exige autorización QR de capataz/supervisor (RNF-015.1 / H-12).
     */
    fun requiereAutorizacionParaOmitir(): Boolean =
        DatosMemoria.ultimaCalibracion()?.aprobado == true

    /**
     * AGV-015 / RNF-015.1 (H-12): guarda la tara del turno en memoria exigiendo
     * autorización previa mediante credencial QR válida de capataz o supervisor.
     * Sin autorización válida no se guarda nada.
     */
    fun confirmarYGuardarTara(
        codigoQrCapataz: String?,
        onGuardado: (Double) -> Unit
    ) {
        val guardada = try {
            guardarCalibracionAutorizada(
                pesosMuestras = pesos,
                codigoQrCapataz = codigoQrCapataz,
                asistenteNombre = SesionTurno.operador.value ?: "Asistente de pesaje",
                timestamp = FechaHoraUtil.timestampActual()
            )
        } catch (e: Exception) {
            _mensaje.value = e.message ?: "Se requiere autorización de capataz o supervisor para modificar la tara."
            return
        }
        val promedio = guardada.taraPromedioCalculada
        _taraVigente.value = if (guardada.aprobado) promedio else Constantes.TARA_POR_DEFECTO
        _mensaje.value = if (guardada.aprobado) {
            "Tara calibrada: ${"%.3f".format(promedio)} kg por jaba (autorizado por ${guardada.capatazAutorizador})"
        } else {
            "Muestreo incompleto o fuera de rango: se usará la tara de ${Constantes.TARA_POR_DEFECTO} kg"
        }
        onGuardado(promedio)
    }

    /**
     * Omite la calibracion y registra el uso de la tara por defecto (AGV-003,
     * escenario 2: el pesaje debe quedar marcado como tara por defecto).
     * Si ya existía una tara calibrada aprobada, exige credencial QR de capataz/supervisor (H-12).
     */
    fun usarTaraPorDefecto(
        codigoQrCapataz: String? = null,
        onListo: (Double) -> Unit
    ) {
        val capataz = if (requiereAutorizacionParaOmitir()) {
            try {
                validarCredencialCapataz(codigoQrCapataz)
            } catch (e: Exception) {
                _mensaje.value = e.message ?: "Se requiere autorización de capataz o supervisor para modificar la tara."
                return
            }
        } else {
            null
        }
        DatosMemoria.guardarCalibracion(
            CalibracionTara(
                taraPromedioCalculada = Constantes.TARA_POR_DEFECTO,
                pesoMuestra5Jabas = 0.0,
                asistenteNombre = SesionTurno.operador.value ?: "Asistente de pesaje",
                timestamp = FechaHoraUtil.timestampActual(),
                aprobado = false,
                capatazAutorizador = capataz?.nombre,
                codigoQrCapataz = capataz?.codigoQr
            )
        )
        _taraVigente.value = Constantes.TARA_POR_DEFECTO
        _mensaje.value = "Se usará la tara por defecto de ${Constantes.TARA_POR_DEFECTO} kg por jaba"
        onListo(Constantes.TARA_POR_DEFECTO)
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    companion object {
        private const val KEY_MUESTRAS = "key_calibracion_muestras"
        private const val KEY_PESO_MUESTRA = "key_peso_muestra_actual"

        private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0

        /**
         * Compuerta de autorización (RNF-015.1 / H-12) reutilizando el catálogo de credenciales QR
         * de capataz/supervisor de [DatosMemoria.buscarCapatazPorQr].
         */
        fun validarCredencialCapataz(codigoQrCapataz: String?): CredencialCapataz {
            val qrLimpio = codigoQrCapataz?.trim().orEmpty()
            check(qrLimpio.isNotEmpty()) {
                "Modificar la tara exige autorización escaneando el QR del capataz o supervisor"
            }
            return DatosMemoria.buscarCapatazPorQr(qrLimpio)
                ?: throw IllegalArgumentException(
                    "Credencial QR no válida para autorización de capataz o supervisor"
                )
        }

        /**
         * Valida la credencial QR de capataz/supervisor y, solo si es válida, guarda
         * la calibración de tara en [DatosMemoria]. Sin autorización válida no guarda nada (H-12).
         */
        fun guardarCalibracionAutorizada(
            pesosMuestras: List<Double>,
            codigoQrCapataz: String?,
            asistenteNombre: String = SesionTurno.operador.value ?: "Asistente de pesaje",
            timestamp: Long = FechaHoraUtil.timestampActual()
        ): CalibracionTara {
            val capataz = validarCredencialCapataz(codigoQrCapataz)
            val promedio = CalculadoraPesaje.promedioMuestras(pesosMuestras)
            val totalMuestra = redondear3(pesosMuestras.sum())
            val aprobado = pesosMuestras.size == Constantes.MUESTRAS_CALIBRACION &&
                CalculadoraPesaje.taraValida(promedio)

            return DatosMemoria.guardarCalibracion(
                CalibracionTara(
                    taraPromedioCalculada = promedio,
                    pesoMuestra5Jabas = totalMuestra,
                    asistenteNombre = asistenteNombre,
                    timestamp = timestamp,
                    aprobado = aprobado,
                    capatazAutorizador = capataz.nombre,
                    codigoQrCapataz = capataz.codigoQr
                )
            )
        }
    }
}
