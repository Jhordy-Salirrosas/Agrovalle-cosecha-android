package com.agrovalle.cosecha.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.agrovalle.cosecha.model.Jornalero

class TurnoViewModel(private val state: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_ROL = "key_rol"
        private const val KEY_JORNADA = "key_jornada"
        private const val KEY_FUNDO = "key_fundo"
        private const val KEY_CONTADOR_ASISTENTES = "key_contador_asistentes"
        private const val KEY_TARA_CALIBRADA = "key_tara_calibrada"
        private const val KEY_ES_TARA_CALIBRADA = "key_es_tara_calibrada"
        private const val KEY_ACUMULADO_DIARIO = "key_acumulado_diario"
        private const val KEY_ULTIMO_PESO_NETO = "key_ultimo_peso_neto"
        private const val KEY_PARIHUELA_ABIERTA = "key_parihuela_abierta"
        private const val KEY_CUARTEL = "key_cuartel"
        private const val KEY_CLP = "key_clp"
        private const val KEY_VARIEDAD = "key_variedad"

        const val TARA_POR_DEFECTO = 0.375
        const val TOTAL_CUADRILLA = 35
    }

    init {
        // Inicializar valores por defecto si no existen en el SavedStateHandle
        if (!state.contains(KEY_CONTADOR_ASISTENTES)) {
            state[KEY_CONTADOR_ASISTENTES] = 0
        }
        if (!state.contains(KEY_TARA_CALIBRADA)) {
            state[KEY_TARA_CALIBRADA] = TARA_POR_DEFECTO
        }
        if (!state.contains(KEY_ES_TARA_CALIBRADA)) {
            state[KEY_ES_TARA_CALIBRADA] = false
        }
        if (!state.contains(KEY_ACUMULADO_DIARIO)) {
            state[KEY_ACUMULADO_DIARIO] = 45.50
        }
        if (!state.contains(KEY_ULTIMO_PESO_NETO)) {
            state[KEY_ULTIMO_PESO_NETO] = 8.00
        }
        if (!state.contains(KEY_PARIHUELA_ABIERTA)) {
            state[KEY_PARIHUELA_ABIERTA] = false
        }
    }

    // LiveData observables para las Views
    val rol: LiveData<String> = state.getLiveData(KEY_ROL, "Capataz")
    val jornada: LiveData<String> = state.getLiveData(KEY_JORNADA, "Mañana")
    val fundo: LiveData<String> = state.getLiveData(KEY_FUNDO, "Fundo Principal Virú")
    val contadorAsistentes: LiveData<Int> = state.getLiveData(KEY_CONTADOR_ASISTENTES, 0)
    val tara: LiveData<Double> = state.getLiveData(KEY_TARA_CALIBRADA, TARA_POR_DEFECTO)
    val esTaraCalibrada: LiveData<Boolean> = state.getLiveData(KEY_ES_TARA_CALIBRADA, false)
    val acumuladoDiario: LiveData<Double> = state.getLiveData(KEY_ACUMULADO_DIARIO, 45.50)
    val ultimoPesoNeto: LiveData<Double> = state.getLiveData(KEY_ULTIMO_PESO_NETO, 8.00)
    val parihuelaAbierta: LiveData<Boolean> = state.getLiveData(KEY_PARIHUELA_ABIERTA, false)
    val cuartel: LiveData<String> = state.getLiveData(KEY_CUARTEL, "")
    val clp: LiveData<String> = state.getLiveData(KEY_CLP, "")
    val variedad: LiveData<String> = state.getLiveData(KEY_VARIEDAD, "")

    // Lista mock de jornaleros para simulación sin base de datos
    val listaMockJornaleros: List<Jornalero> = listOf(
        Jornalero(1L, "12345678", "Juan", "Pérez Sánchez", "QR_AGV_001", "CUA-01"),
        Jornalero(2L, "23456789", "María", "López Mendoza", "QR_AGV_002", "CUA-01"),
        Jornalero(3L, "34567890", "Pedro", "García Alva", "QR_AGV_003", "CUA-01"),
        Jornalero(4L, "45678901", "Luis", "Pérez Ramírez", "QR_AGV_004", "CUA-01"),
        Jornalero(5L, "56789012", "Ana", "Torres Huamán", "QR_AGV_005", "CUA-01"),
        Jornalero(6L, "71231234", "Carlos", "Díaz Rodríguez", "QR_AGV_006", "CUA-01")
    )

    fun setContextoLogin(nuevoRol: String, nuevaJornada: String, nuevoFundo: String) {
        state[KEY_ROL] = nuevoRol
        state[KEY_JORNADA] = nuevaJornada
        state[KEY_FUNDO] = nuevoFundo
    }

    fun incrementarAsistentes(): Int {
        val actual = state.get<Int>(KEY_CONTADOR_ASISTENTES) ?: 0
        val nuevo = if (actual < TOTAL_CUADRILLA) actual + 1 else actual
        state[KEY_CONTADOR_ASISTENTES] = nuevo
        return nuevo
    }

    fun setTaraCalibrada(valor: Double, calibrada: Boolean) {
        state[KEY_TARA_CALIBRADA] = valor
        state[KEY_ES_TARA_CALIBRADA] = calibrada
    }

    fun registrarPesaje(pesoBruto: Double, cantidadJabas: Int): Double {
        val valorTara = state.get<Double>(KEY_TARA_CALIBRADA) ?: TARA_POR_DEFECTO
        val taraTotal = valorTara * cantidadJabas
        val neto = (pesoBruto - taraTotal).coerceAtLeast(0.0)
        val netoRedondeado = Math.round(neto * 100.0) / 100.0

        val actualAcumulado = state.get<Double>(KEY_ACUMULADO_DIARIO) ?: 0.0
        val nuevoAcumulado = Math.round((actualAcumulado + netoRedondeado) * 100.0) / 100.0

        state[KEY_ULTIMO_PESO_NETO] = netoRedondeado
        state[KEY_ACUMULADO_DIARIO] = nuevoAcumulado
        return netoRedondeado
    }

    fun setParihuelaContexto(cuartel: String, clp: String, variedad: String, abierta: Boolean) {
        state[KEY_CUARTEL] = cuartel
        state[KEY_CLP] = clp
        state[KEY_VARIEDAD] = variedad
        state[KEY_PARIHUELA_ABIERTA] = abierta
    }

    fun buscarJornaleroPorUltimosDigitosDni(ultimosDigitos: String): List<Jornalero> {
        if (ultimosDigitos.isBlank()) return emptyList()
        return listaMockJornaleros.filter { it.dni.endsWith(ultimosDigitos) || it.dni.contains(ultimosDigitos) }
    }
}
