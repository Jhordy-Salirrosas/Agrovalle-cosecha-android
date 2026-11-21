package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.model.Pesaje
import com.agrovalle.cosecha.model.ReclamoPeso
import com.agrovalle.cosecha.util.CalculadoraReclamoPeso
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.EvaluacionReclamoPeso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PesajeRegistradoItemUi(
    val pesaje: Pesaje,
    val jornaleroNombre: String,
    val horaTexto: String
)

data class ReclamoPesoUiState(
    val pesajesRegistrados: List<PesajeRegistradoItemUi> = emptyList(),
    val pesajeSeleccionado: PesajeRegistradoItemUi? = null,
    val taraVigenteKg: Double = Constantes.TARA_POR_DEFECTO,
    val esTaraPorDefecto: Boolean = true,
    val evaluacionEnCurso: EvaluacionReclamoPeso? = null,
    val bitacoraReclamos: List<ReclamoPeso> = emptyList(),
    val mensaje: String? = null,
    val esError: Boolean = false
)

/**
 * ViewModel para el flujo de reclamos de peso y consulta de su bitácora (AGV-015 escenarios 2.0 y 3.0).
 */
class ReclamoPesoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReclamoPesoUiState())
    val uiState: StateFlow<ReclamoPesoUiState> = _uiState.asStateFlow()

    fun cargarDatos(pesajePreseleccionadoId: Long? = null) {
        val ultimaCal = DatosMemoria.ultimaCalibracion()
        val hayCalibracion = ultimaCal != null && ultimaCal.aprobado
        val tara = if (hayCalibracion) ultimaCal!!.taraPromedioCalculada else Constantes.TARA_POR_DEFECTO

        val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.US)
        val items = DatosMemoria.pesajesOriginales().map { pesaje ->
            val jornalero = DatosMemoria.buscarJornaleroPorId(pesaje.jornaleroId)
            PesajeRegistradoItemUi(
                pesaje = pesaje,
                jornaleroNombre = jornalero?.nombreCompleto ?: "Jornalero #${pesaje.jornaleroId}",
                horaTexto = formatoHora.format(Date(pesaje.timestamp))
            )
        }

        val idPreferido = pesajePreseleccionadoId?.takeIf { it > 0L }
            ?: _uiState.value.pesajeSeleccionado?.pesaje?.id
        val seleccionado = items.firstOrNull { it.pesaje.id == idPreferido } ?: items.firstOrNull()

        _uiState.update { actual ->
            actual.copy(
                pesajesRegistrados = items,
                pesajeSeleccionado = seleccionado,
                taraVigenteKg = tara,
                esTaraPorDefecto = !hayCalibracion,
                bitacoraReclamos = DatosMemoria.obtenerBitacoraReclamos()
            )
        }
    }

    fun seleccionarPesaje(pesajeId: Long) {
        val encontrado = _uiState.value.pesajesRegistrados.firstOrNull { it.pesaje.id == pesajeId }
        _uiState.update {
            it.copy(
                pesajeSeleccionado = encontrado,
                evaluacionEnCurso = null
            )
        }
    }

    fun actualizarPesoBrutoReclamo(pesoBrutoTexto: String) {
        val seleccionado = _uiState.value.pesajeSeleccionado
        val bruto = pesoBrutoTexto.trim().toDoubleOrNull()
        if (seleccionado == null || bruto == null ||
            bruto !in Constantes.PESO_BRUTO_MIN..Constantes.PESO_BRUTO_MAX
        ) {
            _uiState.update { it.copy(evaluacionEnCurso = null) }
            return
        }
        val nuevoNeto = runCatching {
            CalculadoraReclamoPeso.calcularPesoNeto(
                pesoBrutoKg = bruto,
                cantidadJabas = seleccionado.pesaje.cantidadJabas,
                taraPorJabaKg = _uiState.value.taraVigenteKg
            )
        }.getOrNull()
        if (nuevoNeto == null) {
            _uiState.update { it.copy(evaluacionEnCurso = null) }
            return
        }
        val evaluacion = CalculadoraReclamoPeso.evaluarReclamo(
            pesoOriginalNetoKg = seleccionado.pesaje.pesoNeto,
            pesoNuevoNetoKg = nuevoNeto
        )
        _uiState.update { it.copy(evaluacionEnCurso = evaluacion) }
    }

    fun validarListoParaAutorizacionQr(pesoBrutoTexto: String): Boolean {
        val bruto = parsearPesoBrutoValido(pesoBrutoTexto) ?: return false
        val seleccionado = _uiState.value.pesajeSeleccionado ?: return false
        val nuevoNeto = try {
            CalculadoraReclamoPeso.calcularPesoNeto(
                pesoBrutoKg = bruto,
                cantidadJabas = seleccionado.pesaje.cantidadJabas,
                taraPorJabaKg = _uiState.value.taraVigenteKg
            )
        } catch (e: IllegalArgumentException) {
            _uiState.update {
                it.copy(mensaje = e.message ?: "Peso bruto inválido para reclamo", esError = true)
            }
            return false
        }
        val evaluacion = CalculadoraReclamoPeso.evaluarReclamo(
            pesoOriginalNetoKg = seleccionado.pesaje.pesoNeto,
            pesoNuevoNetoKg = nuevoNeto
        )
        if (!evaluacion.excedeTolerancia) {
            _uiState.update {
                it.copy(
                    mensaje = "La diferencia (${evaluacion.diferenciaAbsolutaGramos.toInt()} g) no supera 20 g; registre el reclamo manteniendo el pesaje original.",
                    esError = false
                )
            }
            return false
        }
        return true
    }

    fun registrarReclamoDentroTolerancia(pesoBrutoTexto: String) {
        val bruto = parsearPesoBrutoValido(pesoBrutoTexto) ?: return
        val seleccionado = _uiState.value.pesajeSeleccionado ?: return
        try {
            val resultado = DatosMemoria.registrarReclamoPeso(
                pesajeOriginalId = seleccionado.pesaje.id,
                nuevoPesoBrutoKg = bruto,
                taraPorJabaKg = _uiState.value.taraVigenteKg,
                codigoQrCapataz = null
            )
            cargarDatos(seleccionado.pesaje.id)
            if (resultado is DatosMemoria.ResultadoRegistroReclamo.DentroDeTolerancia) {
                val diffTxt = String.format(Locale.US, "%+.0f g", resultado.reclamo.diferenciaGramos)
                _uiState.update {
                    it.copy(
                        evaluacionEnCurso = null,
                        mensaje = "Reclamo dentro de tolerancia ($diffTxt). Se mantuvo el pesaje original #${seleccionado.pesaje.id}.",
                        esError = false
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    mensaje = e.message ?: "No se pudo registrar el reclamo",
                    esError = true
                )
            }
        }
    }

    fun registrarReclamoConQrCapataz(pesoBrutoTexto: String, codigoQrCapataz: String) {
        val bruto = parsearPesoBrutoValido(pesoBrutoTexto) ?: return
        val seleccionado = _uiState.value.pesajeSeleccionado ?: return
        try {
            when (
                val resultado = DatosMemoria.registrarReclamoPeso(
                    pesajeOriginalId = seleccionado.pesaje.id,
                    nuevoPesoBrutoKg = bruto,
                    taraPorJabaKg = _uiState.value.taraVigenteKg,
                    codigoQrCapataz = codigoQrCapataz
                )
            ) {
                is DatosMemoria.ResultadoRegistroReclamo.CompensacionRegistrada -> {
                    cargarDatos(seleccionado.pesaje.id)
                    val compTxt = String.format(Locale.US, "%+.3f kg", resultado.pesajeCompensatorio.pesoNeto)
                    _uiState.update {
                        it.copy(
                            evaluacionEnCurso = null,
                            mensaje = "Compensatorio #${resultado.pesajeCompensatorio.id} ($compTxt) registrado y autorizado por ${resultado.reclamo.capatazAutorizador}. Original #${seleccionado.pesaje.id} intacto.",
                            esError = false
                        )
                    }
                }

                is DatosMemoria.ResultadoRegistroReclamo.DentroDeTolerancia -> {
                    cargarDatos(seleccionado.pesaje.id)
                    _uiState.update {
                        it.copy(
                            evaluacionEnCurso = null,
                            mensaje = "Reclamo dentro de tolerancia (<= 20 g). Se mantuvo el pesaje original.",
                            esError = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    mensaje = e.message ?: "No se pudo autorizar el pesaje compensatorio",
                    esError = true
                )
            }
        }
    }

    fun consumirMensaje() {
        _uiState.update { it.copy(mensaje = null) }
    }

    private fun parsearPesoBrutoValido(pesoBrutoTexto: String): Double? {
        if (_uiState.value.pesajeSeleccionado == null) {
            _uiState.update {
                it.copy(mensaje = "Seleccione un pesaje registrado primero.", esError = true)
            }
            return null
        }
        val bruto = pesoBrutoTexto.trim().toDoubleOrNull()
        if (bruto == null || bruto !in Constantes.PESO_BRUTO_MIN..Constantes.PESO_BRUTO_MAX) {
            _uiState.update {
                it.copy(
                    mensaje = "Ingrese un nuevo peso bruto válido (${Constantes.PESO_BRUTO_MIN} a ${Constantes.PESO_BRUTO_MAX} kg).",
                    esError = true
                )
            }
            return null
        }
        return bruto
    }
}
