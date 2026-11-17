package com.agrovalle.cosecha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agrovalle.cosecha.data.local.converter.Converters
import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity

/**
 * Base de datos Room de AgroValle Cosecha (Sprint 2).
 */
@Database(
    entities = [
        ParihuelaEntity::class,
        IncidenciaEntity::class,
        BitacoraEntity::class,
        PesajeEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgroValleDatabase : RoomDatabase() {

    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun bitacoraDao(): BitacoraDao
    abstract fun pesajeDao(): PesajeDao

    companion object {
        private const val DATABASE_NAME = "agrovalle_cosecha.db"

        @Volatile
        private var instance: AgroValleDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pesajes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `parihuela_id` INTEGER NOT NULL,
                        `jornalero` TEXT NOT NULL,
                        `cantidad_jabas` INTEGER NOT NULL,
                        `fecha_hora` INTEGER NOT NULL,
                        FOREIGN KEY(`parihuela_id`) REFERENCES `parihuelas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pesajes_parihuela_id` ON `pesajes` (`parihuela_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pesajes_jornalero` ON `pesajes` (`jornalero`)"
                )
            }
        }

        fun getInstance(context: Context): AgroValleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgroValleDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
