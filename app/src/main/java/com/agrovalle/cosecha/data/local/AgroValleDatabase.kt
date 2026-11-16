package com.agrovalle.cosecha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agrovalle.cosecha.data.local.converter.Converters
import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity

/**
 * Base de datos Room de AgroValle Cosecha (Sprint 2).
 */
@Database(
    entities = [
        ParihuelaEntity::class,
        IncidenciaEntity::class,
        BitacoraEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgroValleDatabase : RoomDatabase() {

    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun bitacoraDao(): BitacoraDao

    companion object {
        private const val DATABASE_NAME = "agrovalle_cosecha.db"

        @Volatile
        private var instance: AgroValleDatabase? = null

        fun getInstance(context: Context): AgroValleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgroValleDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
