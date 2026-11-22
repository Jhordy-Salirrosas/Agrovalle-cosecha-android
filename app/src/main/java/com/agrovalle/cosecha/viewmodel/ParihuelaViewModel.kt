package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.repository.AvanceParihuela
import com.agrovalle.cosecha.data.repository.EstadoTiempoParihuela
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

/**
 * Cuartel y variedad derivados para un pesaje a partir del jornalero seleccionado
 * y el catálogo de contexto agronómico ([DatosMemoria.mapaCuarteles]), sin texto libre (H-06).
 */
data class ContextoPesajeDerivado(
    val cuartel: String? = null,
    val variedad: String? = null
) {
    val estaCompleto: Boolean get() = !cuartel.isNullOrBlank() && !variedad.isNullOrBlank()
}

data class ParihuelaUiState(
    val parihuelaAbierta: ParihuelaEntity? = null,
    val avance: AvanceParihuela? = null,
    val estadoTiempo: EstadoTiempoParihuela? = null,
    val jornalerosDisponibles: List<JornaleroAsistenciaOpcion> = emptyList(),
    val jornaleroSeleccionado: String? = null,
    val contextoPesajeDerivado: ContextoPesajeDerivado = ContextoPesajeDerivado(),
    val mensaje: String? = null,
    val esError: Boolean = false
)

/**
 * ViewModel para la pantalla principal de gestión de parihuela (Sprint 2).
 * Consume únicamente [ParihuelaRepository] y [SelectorJornalerosAsistencia].
 */
class ParihuelaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Deriva cuartel y variedad del pesaje desde el jornalero seleccionado
         * ([JornaleroAsistenciaOpcion.cuartelActual]) y el catálogo [catalogoCuarteles]
         * (por defecto [DatosMemoria.mapaCuarteles]) sin entrada de texto libre (H-06).
         */
        fun derivarContextoPesaje(
            disponibles: List<JornaleroAsistenciaOpcion>,
            jornaleroSeleccionado: String?,
            catalogoCuarteles: Map<String, String> = DatosMemoria.mapaCuarteles
        ): ContextoPesajeDerivado {
            val nombre = jornaleroSeleccionado?.trim()?.takeIf { it.isNotEmpty() }
                ?: return ContextoPesajeDerivado()
            val opcion = disponibles.firstOrNull {
                it.nombreCompleto.equals(nombre, ignoreCase = true)
            } ?: return ContextoPesajeDerivado()
            val cuartelCrudo = opcion.cuartelActual?.trim()?.takeIf { it.isNotEmpty() }
                ?: return ContextoPesajeDerivado()
            val entradaCatalogo = catalogoCuarteles.entries.firstOrNull { (cuartelCatalogo, _) ->
                cuartelCatalogo.equals(cuartelCrudo, ignoreCase = true)
            }
            return if (entradaCatalogo != null) {
                ContextoPesajeDerivado(
                    cuartel = entradaCatalogo.key,
                    variedad = entradaCatalogo.value
                )
            } else {
                ContextoPesajeDerivado(
                    cuartel = cuartelCrudo,
                    variedad = null
                )
            }
        }

        /**
         * Valida variedad y cuartel contra el catálogo [catalogoCuarteles] para la apertura
         * de una parihuela (H-06), devolviendo `(variedadCanonica, cuartelCanonico)`.
         */
        fun validarCatalogoApertura(
            variedad: String,
            cuartel: String,
            catalogoCuarteles: Map<String, String> = DatosMemoria.mapaCuarteles
        ): Pair<String, String> {
            val variedadLimpia = variedad.trim()
            val cuartelLimpio = cuartel.trim()
            val entradaCuartel = catalogoCuarteles.entries.firstOrNull { (k, _) ->
                k.equals(cuartelLimpio, ignoreCase = true)
            } ?: throw IllegalArgumentException(
                "El cuartel '$cuartelLimpio' no está en el catálogo válido (${catalogoCuarteles.keys.joinToString(", ")})."
            )
            val variedadesValidas = catalogoCuarteles.values.distinct()
            val variedadCanonica = variedadesValidas.firstOrNull {
                it.equals(variedadLimpia, ignoreCase = true)
            } ?: throw IllegalArgumentException(
                "La variedad '$variedadLimpia' no está en el catálogo válido (${variedadesValidas.joinToString(", ")})."
            )
            if (!entradaCuartel.value.equals(variedadCanonica, ignoreCase = true)) {
                throw IllegalArgumentException(
                    "La variedad '$variedadCanonica' no corresponde al cuartel '${entradaCuartel.key}' (variedad de catálogo: '${entradaCuartel.value}')."
                )
            }
            return variedadCanonica to entradaCuartel.key
        }
    }

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val refreshTiempoTrigger = MutableStateFlow(0L)
    private val _uiState = MutableStateFlow(ParihuelaUiState())
    val uiState: StateFlow<ParihuelaUiState> = _uiState.asStateFlow()
    private var ultimaParihuelaConAlertaId: Long? = null

    init {
        actualizarListaJornaleros()
        viewModelScope.launch {
            combine(
                repository.observarParihuelaAbierta(),
                repository.observarAvanceParihuelaAbierta(),
                refreshTiempoTrigger
            ) { abierta, avance, _ ->
                val estadoTiempo = abierta?.let {
                    repository.obtenerEstadoTiempoParihuela(it.id)
                }
                Triple(abierta, avance, estadoTiempo)
            }.collect { (abierta, avance, estadoTiempo) ->
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
                        avance = avance,
                        estadoTiempo = estadoTiempo
                    )
                }
            }
        }
    }

    fun refrescarTiempoRetencion() {
        refreshTiempoTrigger.update { it + 1L }
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
                jornaleroSeleccionado = seleccionadoVigente,
                contextoPesajeDerivado = derivarContextoPesaje(lista, seleccionadoVigente)
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
            actual.copy(
                jornaleroSeleccionado = valido,
                contextoPesajeDerivado = derivarContextoPesaje(actual.jornalerosDisponibles, valido)
            )
        }
    }

    fun abrirNuevaParihuela(
        variedad: String,
        cuartel: String,
        clp: String,
        cuadrilla: String
    ) {
        val v = variedad.trim()
        val c = cuartel.trim()
        val clpLimpio = clp.trim()
        val cuadrillaLimpia = cuadrilla.trim()
        if (v.isEmpty() || c.isEmpty() || clpLimpio.isEmpty() || cuadrillaLimpia.isEmpty()) {
            _uiState.update {
                it.copy(
                    mensaje = "Debe ingresar variedad, cuartel, CLP y cuadrilla para abrir la parihuela.",
                    esError = true
                )
            }
            return
        }
        val (variedadCanonica, cuartelCanonico) = try {
            validarCatalogoApertura(v, c)
        } catch (e: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    mensaje = e.message ?: "Variedad o cuartel fuera de catálogo.",
                    esError = true
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                val nueva = repository.abrirNuevaParihuela(
                    variedadCanonica,
                    cuartelCanonico,
                    clpLimpio,
                    cuadrillaLimpia
                )
                _uiState.update {
                    it.copy(
                        mensaje = "Parihuela ${nueva.codigo} abierta (${nueva.variedad} - ${nueva.cuartel} • CLP ${nueva.clp} • Cuadrilla ${nueva.cuadrilla})",
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
        pesoKg: Double
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
        val contexto = derivarContextoPesaje(disponibles, jornalero)
        val cuartelDerivado = contexto.cuartel
        val variedadDerivada = contexto.variedad
        if (cuartelDerivado.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    mensaje = "El jornalero seleccionado no tiene cuartel asignado en el contexto agronómico.",
                    esError = true
                )
            }
            return
        }
        if (variedadDerivada.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    mensaje = "El cuartel '$cuartelDerivado' del jornalero no está en el catálogo válido (${DatosMemoria.mapaCuarteles.keys.joinToString(", ")}).",
                    esError = true
                )
            }
            return
        }
        if (!pesoKg.isFinite() || pesoKg !in ParihuelaRepository.PESO_MIN_KG..ParihuelaRepository.PESO_MAX_KG) {
            _uiState.update {
                it.copy(
                    mensaje = "El peso debe estar entre 0.10 y 60.00 kg.",
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
                        pesoKg = pesoKg,
                        variedad = variedadDerivada,
                        cuartel = cuartelDerivado
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
                        refrescarTiempoRetencion()
                        val msg = if (res.resultadoJabas.selladaPorLimite) {
                            val nueva = res.resultadoJabas.nuevaParihuela?.codigo ?: ""
                            "Parihuela ${res.resultadoJabas.parihuela.codigo} sellada al llegar a 120 jabas (${res.pesaje.pesoKg} kg en último pesaje). Se abrió automáticamente $nueva."
                        } else {
                            "Pesaje registrado: +${res.pesaje.cantidadJabas} jabas (${res.pesaje.pesoKg} kg) para ${res.pesaje.jornalero}."
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

    fun sellarEnMeta() {
        val abierta = _uiState.value.parihuelaAbierta ?: return
        viewModelScope.launch {
            try {
                val sellada = repository.sellarEnMeta(abierta.id)
                _uiState.update {
                    it.copy(
                        mensaje = "Parihuela ${sellada.codigo} sellada en meta (${sellada.conteoJabas} jabas · ${sellada.pisos} pisos).",
                        esError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(mensaje = e.message ?: "Error al sellar la parihuela", esError = true)
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
