package com.agrovalle.cosecha.data.model

// ============================================================================
// AgroValle Cosecha - Modelos de Dominio y Enumeraciones
// Estos modelos representan la lógica de negocio independiente de Room.
// ============================================================================

/** Roles disponibles en el sistema según la especificación. */
enum class Rol(val displayName: String) {
    CAPATAZ("Capataz"),
    ASISTENTE_PESAJE("Asistente de Pesaje"),
    CHOFER("Chofer")
}

/** Jornadas de trabajo disponibles. */
enum class Jornada(val displayName: String) {
    MANANA("Mañana"),
    TARDE("Tarde"),
    NOCHE("Noche")
}

/** Tipo de registro de ingreso a la asistencia. */
enum class TipoIngreso(val displayName: String) {
    QR("Escaneo QR"),
    MANUAL("Búsqueda por DNI")
}

/** Estado de una parihuela. */
enum class EstadoParihuela {
    ABIERTA, CERRADA
}

/** Variedades de arándano disponibles. */
enum class Variedad(val displayName: String) {
    BILOXI("Biloxi"),
    VENTURA("Ventura")
}

/** Representación de un jornalero (trabajador de campo). */
data class Jornalero(
    val id: Long,
    val dni: String,
    val nombres: String,
    val apellidos: String,
    val codigoQr: String,
    val cuadrillaId: String,
    val activo: Boolean = true
) {
    fun nombreCompleto(): String = "$nombres $apellidos"
}

/** Registro de asistencia de un jornalero. */
data class AsistenciaRegistro(
    val jornaleroId: Long,
    val nombreCompleto: String,
    val dni: String,
    val horaRegistro: String,
    val tipoIngreso: TipoIngreso
)

/** Estado de una parihuela (bandeja de cosecha). */
data class ParihuelajState(
    val codigo: String,
    val cuartel: String,
    val clp: String,
    val variedad: Variedad,
    val estado: EstadoParihuela
)

/** Resultado de un pesaje registrado. */
data class PesajeResultado(
    val jornaleroNombre: String,
    val pesoBruto: Double,
    val cantidadJabas: Int,
    val taraUnitaria: Double,
    val taraTotal: Double,
    val pesoNeto: Double,
    val esTaraPorDefecto: Boolean,
    val acumuladoDiario: Double
)

/** Datos de la sesión / turno activo. */
data class SesionTurno(
    val rol: Rol,
    val jornada: Jornada,
    val fundo: String,
    val sector: String
)
