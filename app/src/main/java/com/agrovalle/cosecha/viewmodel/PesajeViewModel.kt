package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.AgroValleApp
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import com.agrovalle.cosecha.data.mapper.aDominio
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.CalculadoraPesaje
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FechaHoraUtil
import kotlinx.coroutines.launch

/**
 * AGV-003 / AGV-004: ciclo critico de pesaje (< 10 s).
 *
 *  - Peso Neto = Peso Bruto - (Tara * Jabas)
 *  - Validacion de rango de peso bruto (anti errores de tipeo)
 *  - Persistencia atomica en Room
 *  - Acumulado diario del cosechador
 */
class PesajeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as AgroValleApp).repository

    private val _taraAplicada = MutableLiveData(Constantes.TARA_POR_DEFECTO)
    val taraAplicada: LiveData<Double> = _taraAplicada

    private val _esTaraPorDefecto = MutableLiveData(true)
    val esTaraPorDefecto: LiveData<Boolean> = _esTaraPorDefecto

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun cargarTaraVigente() {
        viewModelScope.launch {
            val ultima = repo.getUltimaCalibracion()
            val hayCalibracion = ultima != null && ultima.aprobado
            _taraAplicada.value = if (hayCalibracion) {
                ultima!!.taraPromedioCalculada
            } else {
                Constantes.TARA_POR_DEFECTO
            }
            _esTaraPorDefecto.value = !hayCalibracion
        }
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
     * Persiste el pesaje de forma atomica y devuelve el peso neto guardado.
     * [onGuardado] se invoca con el neto y la parihuela usada.
     */
    fun persistirPesaje(
        jornalero: Jornalero,
        pesoBruto: Double,
        cantidadJabas: Int,
        onGuardado: (pesoNeto: Double, esTaraPorDefecto: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            if (!validarRango(pesoBruto, cantidadJabas)) {
                _error.value = "Peso fuera de rango válido (${Constantes.PESO_BRUTO_MIN} a ${Constantes.PESO_BRUTO_MAX} kg)"
                return@launch
            }

            val tara = _taraAplicada.value ?: Constantes.TARA_POR_DEFECTO
            val taraTotal = CalculadoraPesaje.calcularTaraTotal(cantidadJabas, tara)
            val neto = CalculadoraPesaje.calcularPesoNeto(pesoBruto, cantidadJabas, tara)
            val porDefecto = _esTaraPorDefecto.value == true
            val parihuela = repo.getParihuelaAbierta()

            repo.registrarPesajeAtomico(
                PesajeEntity(
                    jornaleroId = jornalero.id,
                    parihuelaId = parihuela?.id,
                    cantidadJabas = cantidadJabas,
                    pesoBruto = redondear2(pesoBruto),
                    taraTotal = taraTotal,
                    pesoNeto = neto,
                    timestamp = FechaHoraUtil.timestampActual(),
                    esTaraPorDefecto = porDefecto
                )
            )
            onGuardado(neto, porDefecto)
        }
    }

    /** AGV-004: acumulado diario del cosechador (suma de sus pesajes del dia). */
    fun cargarAcumuladoDiario(jornaleroId: Long, onAcumulado: (Double) -> Unit) {
        viewModelScope.launch {
            val acumulado = repo.getAcumuladoDiario(
                jornaleroId = jornaleroId,
                desde = FechaHoraUtil.inicioDelDia(),
                hasta = FechaHoraUtil.finDelDia()
            )
            onAcumulado(redondear2(acumulado))
        }
    }

    fun getPesajesDeJornalero(jornaleroId: Long, onLista: (List<com.agrovalle.cosecha.model.Pesaje>) -> Unit) {
        viewModelScope.launch {
            onLista(repo.getPesajesDeJornalero(jornaleroId).map { it.aDominio() })
        }
    }

    fun consumirError() {
        _error.value = null
    }

    /** Identifica al jornalero por el codigo leido del fotocheck (AGV-003). */
    fun buscarJornaleroPorQr(codigoQr: String, onEncontrado: (Jornalero?) -> Unit) {
        viewModelScope.launch {
            onEncontrado(repo.buscarJornaleroPorQr(codigoQr)?.aDominio())
        }
    }

    private fun redondear2(valor: Double): Double = Math.round(valor * 100.0) / 100.0
}
