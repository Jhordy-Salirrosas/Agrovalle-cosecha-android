package com.dapm.android.agrovalle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dapm.android.agrovalle.data.local.dao.*
import com.dapm.android.agrovalle.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Base de datos Room principal de AgroValle Cosecha.
 * Incluye pre-poblado automático con 10 jornaleros de prueba,
 * un turno de contexto abierto, una parihuela activa y una calibración de tara aprobada
 * para permitir pruebas 100% offline desde el primer inicio.
 */
@Database(
    entities = [
        JornaleroEntity::class,
        TurnoContextoEntity::class,
        AsistenciaEntity::class,
        ParihuelaEntity::class,
        CalibracionTaraEntity::class,
        EntregaPesajeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgroValleDatabase : RoomDatabase() {

    abstract fun jornaleroDao(): JornaleroDao
    abstract fun turnoContextoDao(): TurnoContextoDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun calibracionTaraDao(): CalibracionTaraDao
    abstract fun entregaPesajeDao(): EntregaPesajeDao

    companion object {
        @Volatile
        private var INSTANCIA: AgroValleDatabase? = null

        fun obtenerInstancia(contexto: Context): AgroValleDatabase {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext,
                    AgroValleDatabase::class.java,
                    "agrovalle_cosecha.db"
                )
                    .addCallback(CallbackPrePoblado())
                    .build()
                INSTANCIA = instancia
                instancia
            }
        }
    }

    /**
     * Callback que pre-puebla la base de datos en la primera creación
     * con datos mock para pruebas offline.
     */
    private class CallbackPrePoblado : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCIA?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prePoblarJornaleros(database.jornaleroDao())
                    prePoblarTurnoContexto(database.turnoContextoDao())
                    prePoblarParihuela(database.parihuelaDao())
                    prePoblarCalibracionTara(database.calibracionTaraDao())
                }
            }
        }
    }
}

// ============================================================
// Funciones de pre-poblado con datos mock peruanos
// ============================================================

private suspend fun prePoblarJornaleros(dao: JornaleroDao) {
    val jornaleros = listOf(
        JornaleroEntity(dni = "70123456", codigoQr = "JOR-001", nombres = "Carlos", apellidos = "Mendoza Ríos", codigoCuadrilla = "CUAD-01"),
        JornaleroEntity(dni = "70234567", codigoQr = "JOR-002", nombres = "María", apellidos = "Flores Gutiérrez", codigoCuadrilla = "CUAD-01"),
        JornaleroEntity(dni = "70345678", codigoQr = "JOR-003", nombres = "José", apellidos = "Huamán López", codigoCuadrilla = "CUAD-01"),
        JornaleroEntity(dni = "70456789", codigoQr = "JOR-004", nombres = "Ana", apellidos = "Quispe Rodríguez", codigoCuadrilla = "CUAD-02"),
        JornaleroEntity(dni = "70567890", codigoQr = "JOR-005", nombres = "Pedro", apellidos = "Sánchez Vargas", codigoCuadrilla = "CUAD-02"),
        JornaleroEntity(dni = "70678901", codigoQr = "JOR-006", nombres = "Rosa", apellidos = "Chávez Medina", codigoCuadrilla = "CUAD-02"),
        JornaleroEntity(dni = "70789012", codigoQr = "JOR-007", nombres = "Miguel", apellidos = "Torres Díaz", codigoCuadrilla = "CUAD-03"),
        JornaleroEntity(dni = "70890123", codigoQr = "JOR-008", nombres = "Lucía", apellidos = "Ramírez Palacios", codigoCuadrilla = "CUAD-03"),
        JornaleroEntity(dni = "70901234", codigoQr = "JOR-009", nombres = "Fernando", apellidos = "Castillo Vega", codigoCuadrilla = "CUAD-03"),
        JornaleroEntity(dni = "71012345", codigoQr = "JOR-010", nombres = "Carmen", apellidos = "Delgado Ruiz", codigoCuadrilla = "CUAD-03")
    )
    dao.insertarTodos(jornaleros)
}

private suspend fun prePoblarTurnoContexto(dao: TurnoContextoDao) {
    val fechaHoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    val turno = TurnoContextoEntity(
        fundo = "Virú",
        cuartel = "Cuartel 04",
        codigoClp = "CLP-VIRU-04",
        variedad = "Biloxi",
        nombreCapataz = "Jacinto Quispe Huamán",
        fechaTurno = fechaHoy,
        estadoTurno = "ABIERTO"
    )
    dao.insertarTurno(turno)
}

private suspend fun prePoblarParihuela(dao: ParihuelaDao) {
    val parihuela = ParihuelaEntity(
        codigoMarbete = "PAL-001",
        cuartel = "Cuartel 04",
        variedad = "Biloxi",
        codigoClp = "CLP-VIRU-04",
        estadoParihuela = "ABIERTA"
    )
    dao.insertarParihuela(parihuela)
}

private suspend fun prePoblarCalibracionTara(dao: CalibracionTaraDao) {
    val calibracion = CalibracionTaraEntity(
        pesoMuestra5Jabas = 1.875,
        taraPromedioCalculada = 0.375,
        estaAprobado = true,
        nombreAsistente = "Maribel Cárdenas Tello"
    )
    dao.insertarCalibracion(calibracion)
}
