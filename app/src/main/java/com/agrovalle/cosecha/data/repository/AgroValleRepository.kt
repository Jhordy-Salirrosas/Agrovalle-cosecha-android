package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// ============================================================================
// AgroValle Cosecha - Interfaz del Repositorio
//
// CONTRATO PRINCIPAL entre la capa de datos y la capa de UI/ViewModels.
// Sprint 1: Implementada por MockAgroValleRepository.
// Futuro [Coder 1]: Implementar con Room real → RoomAgroValleRepository.
// ============================================================================

interface AgroValleRepository {

    // ── Jornaleros ──
    /** Busca jornaleros cuyo DNI contiene los dígitos ingresados. */
    fun buscarJornaleroPorDni(digitos: String): Flow<List<Jornalero>>

    /** Busca un jornalero por su código QR de fotocheck. */
    suspend fun buscarJornaleroPorQr(codigoQr: String): Jornalero?

    // ── Asistencia ──
    /** Registra asistencia de un jornalero (por QR o manual). */
    suspend fun registrarAsistencia(jornaleroId: Long, tipo: TipoIngreso): Result<AsistenciaRegistro>

    /** Contador reactivo de asistentes registrados en el turno actual. */
    val contadorAsistencia: StateFlow<Int>

    /** Total de trabajadores en la cuadrilla. */
    val totalTrabajadores: Int

    /** Último registro de asistencia del turno. */
    val ultimoRegistroAsistencia: StateFlow<AsistenciaRegistro?>

    // ── Parihuela (Contexto Agronómico) ──
    /** Estado reactivo de la parihuela activa (null si no hay abierta). */
    val parihuelaActiva: StateFlow<ParihuelajState?>

    /** Abre una nueva parihuela con el contexto seleccionado. */
    suspend fun abrirParihuela(cuartel: String, clp: String, variedad: Variedad): Result<ParihuelajState>

    /** Cierra la parihuela activa. */
    suspend fun cerrarParihuela(): Result<Unit>

    // ── Calibración de Tara ──
    /** Guarda una calibración con los 5 pesos y retorna el promedio. */
    suspend fun guardarCalibracionTara(pesos: List<Double>): Double

    /** Marca que se omitió la calibración; retorna la tara por defecto. */
    suspend fun usarTaraPorDefecto(): Double

    /** Tara actual del turno. */
    val taraActual: StateFlow<Double>

    /** Indica si la tara actual es la por defecto (sin calibración). */
    val esTaraPorDefecto: StateFlow<Boolean>

    // ── Pesaje ──
    /** Registra un pesaje y retorna el resultado con peso neto calculado. */
    suspend fun registrarPesaje(
        jornaleroId: Long,
        jornaleroNombre: String,
        pesoBruto: Double,
        cantidadJabas: Int
    ): Result<PesajeResultado>

    /** Obtiene el acumulado diario de un jornalero. */
    suspend fun obtenerAcumuladoDiario(jornaleroId: Long): Double

    // ── Listas para selectores ──
    /** Lista de cuarteles disponibles. */
    fun obtenerCuarteles(): List<String>

    /** Lista de CLPs disponibles. */
    fun obtenerClps(): List<String>

    /** Lista de fundos disponibles. */
    fun obtenerFundos(): List<String>

    /** Lista de sectores disponibles. */
    fun obtenerSectores(): List<String>

    /** Reinicia el estado del turno (para cerrar sesión). */
    fun reiniciarTurno()
}
