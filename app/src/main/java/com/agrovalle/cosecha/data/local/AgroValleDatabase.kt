package com.agrovalle.cosecha.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agrovalle.cosecha.data.local.dao.*
import com.agrovalle.cosecha.data.local.entities.*

/**
 * Base de datos Room de AgroValle Cosecha.
 *
 * TODO [Coder 1]: Inicializar Room en AgroValleApp y reemplazar MockAgroValleRepository
 * por una implementación real que use estos DAOs.
 *
 * Entidades:
 * - JornaleroEntity: Trabajadores de campo
 * - CuadrillaEntity: Grupos de trabajo
 * - AsistenciaEntity: Registros de asistencia
 * - ParihuelaEntity: Bandejas de cosecha con contexto agronómico
 * - PesajeEntity: Pesajes individuales
 * - CalibracionTaraEntity: Calibración del peso de jabas vacías
 */
@Database(
    entities = [
        JornaleroEntity::class,
        CuadrillaEntity::class,
        AsistenciaEntity::class,
        ParihuelaEntity::class,
        PesajeEntity::class,
        CalibracionTaraEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgroValleDatabase : RoomDatabase() {
    abstract fun jornaleroDao(): JornaleroDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun pesajeDao(): PesajeDao
    abstract fun calibracionTaraDao(): CalibracionTaraDao
}
