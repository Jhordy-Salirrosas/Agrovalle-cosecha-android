package com.agrovalle.cosecha.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
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
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.data.repository.TransaccionRunner

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
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AgroValleDatabase : RoomDatabase() {

    abstract fun parihuelaDao(): ParihuelaDao
    abstract fun incidenciaDao(): IncidenciaDao
    abstract fun bitacoraDao(): BitacoraDao
    abstract fun pesajeDao(): PesajeDao

    fun crearParihuelaRepository(): ParihuelaRepository {
        return ParihuelaRepository(
            parihuelaDao = parihuelaDao(),
            bitacoraDao = bitacoraDao(),
            incidenciaDao = incidenciaDao(),
            pesajeDao = pesajeDao(),
            transaccionRunner = object : TransaccionRunner {
                override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
                    return withTransaction { bloque() }
                }
            }
        )
    }

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

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `parihuelas_temp` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `codigo` TEXT NOT NULL,
                        `estado` TEXT NOT NULL,
                        `variedad` TEXT NOT NULL,
                        `cuartel` TEXT NOT NULL,
                        `conteo_jabas` INTEGER NOT NULL,
                        `pisos` INTEGER NOT NULL,
                        `hora_inicio` INTEGER,
                        `hora_sellado` INTEGER,
                        `motivo_sellado_anticipado` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `parihuelas_temp` (`id`, `codigo`, `estado`, `variedad`, `cuartel`, `conteo_jabas`, `pisos`, `hora_inicio`, `hora_sellado`, `motivo_sellado_anticipado`)
                    SELECT `id`, `codigo`, `estado`, `variedad`, `cuartel`, `conteo_jabas`, `pisos`, `hora_inicio`, `hora_sellado`, `motivo_sellado_anticipado`
                    FROM `parihuelas`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `parihuelas`")
                db.execSQL("ALTER TABLE `parihuelas_temp` RENAME TO `parihuelas`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_parihuelas_codigo` ON `parihuelas` (`codigo`)"
                )

                db.execSQL(
                    "ALTER TABLE `incidencias` ADD COLUMN `estado` TEXT NOT NULL DEFAULT 'PENDIENTE'"
                )
                db.execSQL(
                    "ALTER TABLE `incidencias` ADD COLUMN `accion_tomada` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `incidencias` ADD COLUMN `hora_revision` INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE `incidencias` ADD COLUMN `responsable` TEXT"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
