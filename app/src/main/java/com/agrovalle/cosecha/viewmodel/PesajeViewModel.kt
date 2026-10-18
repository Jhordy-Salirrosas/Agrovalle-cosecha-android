package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.model.Pesaje
import com.agrovalle.cosecha.util.CalculadoraPesaje
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FechaHoraUtil

/**
 * AGV-003 / AGV-004: ciclo critico de pesaje (< 10 s) - SIN Room.
 *
 *  - Peso Neto = Peso Bruto - (Tara * Jabas)
 *  - Validacion de rango de peso bruto (anti errores de tipeo)
 *  - Persistencia en la bitacora en memoria de [DatosMemoria]
 *  - Acumulado diario del cosechador
 *
 * El peso en curso y la cantidad de jabas viven en [SavedStateHandle] para
 * demostrar el manejo del ciclo de vida (rotacion / muerte del proceso).
 */
class PesajeViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _taraAplicada = MutableLiveData(Constantes.TARA_POR_DEFECTO)
    val taraAplicada: LiveData<Double> = _taraAplicada

    private val _esTaraPorDefecto = MutableLiveData(true)
    val esTaraPorDefecto: LiveData<Boolean> = _esTaraPorDefecto

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Jornalero identificado por escaneo. Su id se guarda en SavedStateHandle. */
    var jornaleroId: Long
        get() = savedStateHandle[KEY_JORNALERO_ID] ?: 0L
        set(value) {
            savedStateHandle[KEY_JORNALERO_ID] = value
        }

    /** Entrada del teclado numerico del peso bruto (en curso). */
    var entradaPesoBruto: String
        get() = savedStateHandle[KEY_ENTRADA_PESO] ?: ""
        set(value) {
            savedStateHandle[KEY_ENTRADA_PESO] = value
        }

    /** Cantidad de jabas acumuladas para el pesaje en curso. */
    var cantidadJabas: Int
        get() = savedStateHandle[KEY_CANTIDAD_JABAS] ?: 0
        set(value) {
            savedStateHandle[KEY_CANTIDAD_JABAS] = value
        }

    /** Peso bruto numerico derivado de la entrada del teclado. */
    fun pesoBrutoActual(): Double = entradaPesoBruto.toDoubleOrNull() ?: 0.0

    /** Jornalero actualmente cargado (se resuelve desde DatosMemoria). */
    fun jornaleroActual(): Jornalero? =
        DatosMemoria.buscarJornaleroPorId(jornaleroId)

    /** Por defecto sin jornalero el formulario esta bloqueado. */
    fun hayJornalero(): Boolean = jornaleroId != 0L

    /** Tara vigente del turno: la ultima calibracion aprobada o la de defecto. */
    fun cargarTaraVigente() {
        val ultima = DatosMemoria.ultimaCalibracion()
        val hayCalibracion = ultima != null && ultima.aprobado
        _taraAplicada.value = if (hayCalibracion) {
            ultima!!.taraPromedioCalculada
        } else {
            Constantes.TARA_POR_DEFECTO
        }
        _esTaraPorDefecto.value = !hayCalibracion
    }

    /** AGV-003: Peso Neto = Bruto - (Tara * Jabas), nunca negativo. */
    fun calcularPesoNeto(pesoBruto: Double, cantidadJabas: Int): Double {
        val tara = _taraAplicada.value ?: Constantes.TARA_POR_DEFECTO
        return CalculadoraPesaje.calcularPesoNeto(pesoBruto, cantidadJabas, tara)
    }

    fun calcularTaraTotal(cantidadJabas: Int): Double {
        val tara = _taraAplicada.value ?: Constantes.TARA_POR_DEFECTO
        return CalculadoraPesaje.calcularTaraTotal(cantidadJabas, tara)
    }

    /** Validacion de rangos para evitar errores de tipeo (AGV-003). */
    fun validarRango(pesoBruto: Double, cantidadJabas: Int): Boolean =
        CalculadoraPesaje.rangoValido(pesoBruto, cantidadJabas)

    /**
     * Registra el pesaje en la bitacora en memoria y devuelve el peso neto.
     * [onGuardado] se invoca con el neto y si la tara fue la de defecto.
     */
    fun persistirPesaje(
        jornalero: Jornalero,
        pesoBruto: Double,
        cantidadJabas: Int,
        onGuardado: (pesoNeto: Double, esTaraPorDefecto: Boolean) -> Unit
    ) {
        if (!validarRango(pesoBruto, cantidadJabas)) {
            _error.value = "Peso fuera de rango válido (${Constantes.PESO_BRUTO_MIN} a ${Constantes.PESO_BRUTO_MAX} kg)"
            return
        }

        val tara = _taraAplicada.value ?: Constantes.TARA_POR_DEFECTO
        val taraTotal = CalculadoraPesaje.calcularTaraTotal(cantidadJabas, tara)
        val neto = CalculadoraPesaje.calcularPesoNeto(pesoBruto, cantidadJabas, tara)
        val porDefecto = _esTaraPorDefecto.value == true

        DatosMemoria.registrarPesaje(
            Pesaje(
                jornaleroId = jornalero.id,
                cantidadJabas = cantidadJabas,
                pesoBruto = redondear2(pesoBruto),
                taraTotal = taraTotal,
                pesoNeto = neto,
                timestamp = FechaHoraUtil.timestampActual(),
                esTaraPorDefecto = porDefecto
            )
        )

        // El pesaje quedo guardado: se reinicia el formulario en curso.
        limpiarPesajeEnCurso()
        onGuardado(neto, porDefecto)
    }

    /** AGV-004: acumulado diario del cosechador (suma de sus pesajes del dia). */
    fun cargarAcumuladoDiario(jornaleroId: Long, onAcumulado: (Double) -> Unit) {
        val acumulado = DatosMemoria.acumuladoDiario(
            jornaleroId = jornaleroId,
            desde = FechaHoraUtil.inicioDelDia(),
            hasta = FechaHoraUtil.finDelDia()
        )
        onAcumulado(redondear2(acumulado))
    }

    fun getPesajesDeJornalero(jornaleroId: Long, onLista: (List<Pesaje>) -> Unit) {
        onLista(DatosMemoria.pesajesDeJornalero(jornaleroId))
    }

    fun consumirError() {
        _error.value = null
    }

    /** Identifica al jornalero por el codigo leido del fotocheck (AGV-003). */
    fun buscarJornaleroPorQr(codigoQr: String, onEncontrado: (Jornalero?) -> Unit) {
        val jornalero = DatosMemoria.buscarJornaleroPorQr(codigoQr)
        if (jornalero != null) {
            jornaleroId = jornalero.id
            entradaPesoBruto = ""
            cantidadJabas = 0
        }
        onEncontrado(jornalero)
    }

    /** Limpia los datos en curso para el siguiente jornalero. */
    fun limpiarPesajeEnCurso() {
        jornaleroId = 0L
        entradaPesoBruto = ""
        cantidadJabas = 0
    }

    private fun redondear2(valor: Double): Double = Math.round(valor * 100.0) / 100.0

    companion object {
        private const val KEY_JORNALERO_ID = "key_pesaje_jornalero_id"
        private const val KEY_ENTRADA_PESO = "key_pesaje_entrada_peso"
        private const val KEY_CANTIDAD_JABAS = "key_pesaje_cantidad_jabas"
    }
}
