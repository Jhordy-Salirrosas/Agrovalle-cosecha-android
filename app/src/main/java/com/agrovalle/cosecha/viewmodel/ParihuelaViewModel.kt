package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.repository.AvanceParihuela
import com.agrovalle.cosecha.data.repository.JornaleroAsistenciaOpcion
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.data.repository.ResultadoPesaje
import com.agrovalle.cosecha.data.repository.SelectorJornalerosAsistencia
import com.agrovalle.cosecha.util.FechaHoraUtil
import com.agrovalle.cosecha.util.SesionTurno
import com.agrovalle.cosecha.worker.TiempoEsperaWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParihuelaUiState(
    val parihuelaAbierta: ParihuelaEntity? = null,
    val avance: AvanceParihuela? = null,
    val jornalerosDisponibles: List<JornaleroAsistenciaOpcion> = emptyList(),
    val jornaleroSeleccionado: String? = null,
    val mensaje: String? = null,
    val esError: Boolean = false
)

/**
 * ViewModel para la pantalla principal de gestión de parihuela (Sprint 2).
 * Consume únicamente [ParihuelaRepository] y [SelectorJornalerosAsistencia].
 */
class ParihuelaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val _uiState = MutableStateFlow(ParihuelaUiState())
    val uiState: StateFlow<ParihuelaUiState> = _uiState.asStateFlow()
    private var ultimaParihuelaConAlertaId: Long? = null

    init {
        actualizarListaJornaleros()
        viewModelScope.launch {
            combine(
                repository.observarParihuelaAbierta(),
                repository.observarAvanceParihuelaAbierta()
            ) { abierta, avance ->
                abierta to avance
            }.collect { (abierta, avance) ->
                val inicio = abierta?.horaInicio
                if (abierta != null && inicio != null && abierta.id != ultimaParihuelaConAlertaId) {
                    ultimaParihuelaConAlertaId = abierta.id
                    TiempoEsperaWorker.programarAlertaExacta(
                        context = getApplication(),
                        parihuelaId = abierta.id,
                        horaInicio = inicio
                    )
                }
                _uiState.update {
                    it.copy(
                        parihuelaAbierta = abierta,
                        avance = avance
                    )
                }
            }
        }
    }

    fun actualizarListaJornaleros() {
        val fechaHoy = SesionTurno.fechaJornada.value ?: FechaHoraUtil.hoy()
        val lista = SelectorJornalerosAsistencia.cargarParaFecha(fechaHoy)
        _uiState.update { actual ->
            val seleccionadoVigente = actual.jornaleroSeleccionado?.takeIf { sel ->
                SelectorJornalerosAsistencia.puedeRegistrarPesaje(lista, sel)
            }
            actual.copy(
                jornalerosDisponibles = lista,
                jornaleroSeleccionado = seleccionadoVigente
            )
        }
    }

    fun seleccionarJornalero(nombreCompleto: String?) {
        val limpio = nombreCompleto?.trim()?.takeIf { it.isNotEmpty() }
        _uiState.update { actual ->
            val valido = if (limpio != null &&
                SelectorJornalerosAsistencia.puedeRegistrarPesaje(actual.jornalerosDisponibles, limpio)
            ) {
                limpio
            } else {
                null
            }
            actual.copy(jornaleroSeleccionado = valido)
        }
    }

    fun abrirNuevaParihuela(variedad: String, cuartel: String) {
        val v = variedad.trim()
        val c = cuartel.trim()
        if (v.isEmpty() || c.isEmpty()) {
            _uiState.update {
                it.copy(
                    mensaje = "Debe ingresar variedad y cuartel para abrir la parihuela.",
                    esError = true
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                val nueva = repository.abrirNuevaParihuela(v, c)
                _uiState.update {
                    it.copy(
                        mensaje = "Parihuela ${nueva.codigo} abierta (${nueva.variedad} - ${nueva.cuartel})",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mensaje = e.message ?: "No se pudo abrir la parihuela",
                        esError = true
                    )
                }
            }
        }
    }

    fun registrarPesaje(
        jornalero: String = _uiState.value.jornaleroSeleccionado.orEmpty(),
        cantidadJabas: Int,
        variedad: String,
        cuartel: String
    ) {
        val disponibles = _uiState.value.jornalerosDisponibles
        if (!SelectorJornalerosAsistencia.puedeRegistrarPesaje(disponibles, jornalero)) {
            _uiState.update {
                it.copy(
                    mensaje = "Debe seleccionar un jornalero con asistencia registrada hoy.",
                    esError = true
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                when (
                    val res = repository.registrarPesaje(
                        jornalero = jornalero,
                        cantidadJabas = cantidadJabas,
                        variedad = variedad,
                        cuartel = cuartel
                    )
                ) {
                    is ResultadoPesaje.SinParihuelaAbierta -> {
                        _uiState.update {
                            it.copy(
                                mensaje = "No hay ninguna parihuela abierta. Abra una antes de registrar.",
                                esError = true
                            )
                        }
                    }

                    is ResultadoPesaje.RechazadoPorMezcla -> {
                        _uiState.update {
                            it.copy(
                                mensaje = "Registro rechazado (${res.incidencia.tipo}): ${res.incidencia.detalle}. Se registró la incidencia.",
                                esError = true
                            )
                        }
                    }

                    is ResultadoPesaje.RechazadoPorCapacidad -> {
                        _uiState.update {
                            it.copy(
                                mensaje = "Registro rechazado por capacidad: solo faltan ${res.jabasFaltantes} jabas para llegar a ${ParihuelaRepository.LIMITE_JABAS}.",
                                esError = true
                            )
                        }
                    }

                    is ResultadoPesaje.Registrado -> {
                        val msg = if (res.resultadoJabas.selladaPorLimite) {
                            val nueva = res.resultadoJabas.nuevaParihuela?.codigo ?: ""
                            "Parihuela ${res.resultadoJabas.parihuela.codigo} sellada al llegar a 120 jabas. Se abrió automáticamente $nueva."
                        } else {
                            "Pesaje registrado: +${res.pesaje.cantidadJabas} jabas para ${res.pesaje.jornalero}."
                        }
                        _uiState.update { it.copy(mensaje = msg, esError = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(mensaje = e.message ?: "Error al registrar pesaje", esError = true)
                }
            }
        }
    }

    fun sellarAnticipadamente(motivo: String) {
        val abierta = _uiState.value.parihuelaAbierta ?: return
        viewModelScope.launch {
            try {
                val sellada = repository.sellarAnticipadamente(abierta.id, motivo)
                _uiState.update {
                    it.copy(
                        mensaje = "Parihuela ${sellada.codigo} sellada anticipadamente.",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(mensaje = e.message ?: "Error al sellar anticipadamente", esError = true)
                }
            }
        }
    }
}
