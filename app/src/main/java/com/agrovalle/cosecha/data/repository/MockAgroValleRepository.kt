package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// AgroValle Cosecha - Repositorio Mock para Sprint 1
//
// Datos en memoria para que la UI funcione sin base de datos real.
// TODO [Coder 1]: Reemplazar por RoomAgroValleRepository cuando se conecte Room.
// ============================================================================

class MockAgroValleRepository : AgroValleRepository {

    // ── Datos Mock de Jornaleros ──
    private val jornalerosMock = listOf(
        Jornalero(1, "1234", "Juan", "Pérez", "QR-001", "CUAD-A"),
        Jornalero(2, "5678", "María", "López", "QR-002", "CUAD-A"),
        Jornalero(3, "9012", "Pedro", "García", "QR-003", "CUAD-A"),
        Jornalero(4, "3456", "Ana", "Martínez", "QR-004", "CUAD-A"),
        Jornalero(5, "7890", "Carlos", "Rodríguez", "QR-005", "CUAD-A"),
        Jornalero(6, "2345", "Rosa", "Hernández", "QR-006", "CUAD-A"),
        Jornalero(7, "6789", "Luis", "Flores", "QR-007", "CUAD-A"),
        Jornalero(8, "0123", "Carmen", "Torres", "QR-008", "CUAD-A"),
        Jornalero(9, "4567", "Miguel", "Vásquez", "QR-009", "CUAD-A"),
        Jornalero(10, "8901", "Elena", "Díaz", "QR-010", "CUAD-A")
    )

    // ── Estado reactivo de asistencia ──
    private val _contadorAsistencia = MutableStateFlow(0)
    override val contadorAsistencia: StateFlow<Int> = _contadorAsistencia

    override val totalTrabajadores: Int = 35

    private val _ultimoRegistro = MutableStateFlow<AsistenciaRegistro?>(null)
    override val ultimoRegistroAsistencia: StateFlow<AsistenciaRegistro?> = _ultimoRegistro

    private val asistenciasRegistradas = mutableSetOf<Long>() // IDs ya registrados

    // ── Estado reactivo de parihuela ──
    private val _parihuelaActiva = MutableStateFlow<ParihuelajState?>(null)
    override val parihuelaActiva: StateFlow<ParihuelajState?> = _parihuelaActiva

    // ── Estado reactivo de tara ──
    private val _taraActual = MutableStateFlow(0.375)
    override val taraActual: StateFlow<Double> = _taraActual

    private val _esTaraPorDefecto = MutableStateFlow(true)
    override val esTaraPorDefecto: StateFlow<Boolean> = _esTaraPorDefecto

    // ── Acumulado diario simulado ──
    private var acumuladoDiarioSimulado = 45.50

    private var contadorParihuela = 0

    // ── Implementación de métodos ──

    override fun buscarJornaleroPorDni(digitos: String): Flow<List<Jornalero>> = flow {
        if (digitos.isBlank()) {
            emit(emptyList())
        } else {
            val resultados = jornalerosMock.filter { it.dni.contains(digitos) }
            emit(resultados)
        }
    }

    override suspend fun buscarJornaleroPorQr(codigoQr: String): Jornalero? {
        return jornalerosMock.find { it.codigoQr == codigoQr }
    }

    override suspend fun registrarAsistencia(
        jornaleroId: Long,
        tipo: TipoIngreso
    ): Result<AsistenciaRegistro> {
        val jornalero = jornalerosMock.find { it.id == jornaleroId }
            ?: return Result.failure(Exception("Jornalero no encontrado"))

        // Verificar duplicado
        if (asistenciasRegistradas.contains(jornaleroId)) {
            return Result.failure(Exception("Asistencia ya registrada para ${jornalero.nombreCompleto()}"))
        }

        asistenciasRegistradas.add(jornaleroId)
        _contadorAsistencia.value = asistenciasRegistradas.size

        val registro = AsistenciaRegistro(
            jornaleroId = jornaleroId,
            nombreCompleto = jornalero.nombreCompleto(),
            dni = jornalero.dni,
            horaRegistro = obtenerHoraActual(),
            tipoIngreso = tipo
        )
        _ultimoRegistro.value = registro

        return Result.success(registro)
    }

    override suspend fun abrirParihuela(
        cuartel: String,
        clp: String,
        variedad: Variedad
    ): Result<ParihuelajState> {
        if (_parihuelaActiva.value != null) {
            return Result.failure(Exception("Ya hay una parihuela abierta. Ciérrala primero."))
        }

        contadorParihuela++
        val parihuela = ParihuelajState(
            codigo = "#PAR-${String.format("%03d", contadorParihuela)}",
            cuartel = cuartel,
            clp = clp,
            variedad = variedad,
            estado = EstadoParihuela.ABIERTA
        )
        _parihuelaActiva.value = parihuela
        return Result.success(parihuela)
    }

    override suspend fun cerrarParihuela(): Result<Unit> {
        if (_parihuelaActiva.value == null) {
            return Result.failure(Exception("No hay parihuela abierta para cerrar."))
        }
        _parihuelaActiva.value = null
        return Result.success(Unit)
    }

    override suspend fun guardarCalibracionTara(pesos: List<Double>): Double {
        val promedio = if (pesos.isNotEmpty()) pesos.average() else 0.375
        _taraActual.value = promedio
        _esTaraPorDefecto.value = false
        return promedio
    }

    override suspend fun usarTaraPorDefecto(): Double {
        _taraActual.value = 0.375
        _esTaraPorDefecto.value = true
        return 0.375
    }

    override suspend fun registrarPesaje(
        jornaleroId: Long,
        jornaleroNombre: String,
        pesoBruto: Double,
        cantidadJabas: Int
    ): Result<PesajeResultado> {
        val tara = _taraActual.value
        val taraTotal = tara * cantidadJabas
        val pesoNeto = pesoBruto - taraTotal

        if (pesoNeto <= 0) {
            return Result.failure(Exception("El peso neto no puede ser negativo o cero."))
        }

        // Acumular al diario simulado
        acumuladoDiarioSimulado += pesoNeto

        val resultado = PesajeResultado(
            jornaleroNombre = jornaleroNombre,
            pesoBruto = pesoBruto,
            cantidadJabas = cantidadJabas,
            taraUnitaria = tara,
            taraTotal = taraTotal,
            pesoNeto = pesoNeto,
            esTaraPorDefecto = _esTaraPorDefecto.value,
            acumuladoDiario = acumuladoDiarioSimulado
        )

        return Result.success(resultado)
    }

    override suspend fun obtenerAcumuladoDiario(jornaleroId: Long): Double {
        return acumuladoDiarioSimulado
    }

    override fun obtenerCuarteles(): List<String> {
        return listOf("Cuartel 1", "Cuartel 2", "Cuartel 5", "Cuartel 8", "Cuartel 12")
    }

    override fun obtenerClps(): List<String> {
        return listOf("CLP-2024-001", "CLP-2024-002", "CLP-2024-003", "CLP-2024-004")
    }

    override fun obtenerFundos(): List<String> {
        return listOf("Fundo San José", "Fundo La Victoria", "Fundo El Porvenir")
    }

    override fun obtenerSectores(): List<String> {
        return listOf("Sector Norte", "Sector Sur", "Sector Este", "Sector Oeste")
    }

    override fun reiniciarTurno() {
        _contadorAsistencia.value = 0
        _ultimoRegistro.value = null
        _parihuelaActiva.value = null
        _taraActual.value = 0.375
        _esTaraPorDefecto.value = true
        asistenciasRegistradas.clear()
        acumuladoDiarioSimulado = 45.50
        contadorParihuela = 0
    }

    private fun obtenerHoraActual(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}
