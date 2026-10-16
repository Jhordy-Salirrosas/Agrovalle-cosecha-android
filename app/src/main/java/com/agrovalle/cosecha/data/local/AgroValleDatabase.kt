package com.agrovalle.cosecha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agrovalle.cosecha.data.local.dao.AsistenciaDao
import com.agrovalle.cosecha.data.local.dao.CalibracionTaraDao
import com.agrovalle.cosecha.data.local.dao.CuadrillaDao
import com.agrovalle.cosecha.data.local.dao.JornaleroDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.entity.AsistenciaEntity
import com.agrovalle.cosecha.data.local.entity.CalibracionTaraEntity
import com.agrovalle.cosecha.data.local.entity.CuadrillaEntity
import com.agrovalle.cosecha.data.local.entity.JornaleroEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity

/**
 * Base de datos local (SQLite) de AgroValle Cosecha - 100% offline-first.
 * Sprint 1: persistencia de asistencia, padron, calibracion de tara y pesaje.
 */
@Database(
    entities = [
        CuadrillaEntity::class,
        JornaleroEntity::class,
        AsistenciaEntity::class,
        ParihuelaEntity::class,
        PesajeEntity::class,
        CalibracionTaraEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgroValleDatabase : RoomDatabase() {

    abstract fun cuadrillaDao(): CuadrillaDao
    abstract fun jornaleroDao(): JornaleroDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun pesajeDao(): PesajeDao
    abstract fun calibracionTaraDao(): CalibracionTaraDao

    companion object {
        private const val NOMBRE_BD = "agrovalle_cosecha.db"

        @Volatile
        private var instancia: AgroValleDatabase? = null

        fun getInstance(context: Context): AgroValleDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgroValleDatabase::class.java,
                    NOMBRE_BD
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
