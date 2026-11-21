package com.agrovalle.cosecha.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados de migraciones Room (1->2, 2->3, 3->4, 4->5 y 5->6)
 * validados contra los esquemas exportados en `app/schemas`.
 */
@RunWith(AndroidJUnit4::class)
class AgroValleDatabaseMigrationTest {

    companion object {
        private const val TEST_DB = "agrovalle_migration_test.db"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AgroValleDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-100-0001', 'ABIERTA', 'Biloxi', 'Cuartel 01', 10, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            AgroValleDatabase.MIGRATION_1_2
        )
        migratedDb.execSQL(
            """
            INSERT INTO pesajes (parihuela_id, jornalero, cantidad_jabas, fecha_hora)
            VALUES (1, 'Juan Perez', 5, 1700000010000)
            """.trimIndent()
        )
        migratedDb.query("SELECT jornalero, cantidad_jabas FROM pesajes WHERE parihuela_id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Juan Perez", cursor.getString(0))
            assertEquals(5, cursor.getInt(1))
        }
        migratedDb.close()
    }

    @Test
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-200-0001', 'ABIERTA', 'Ventura', 'Cuartel 05', 20, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO incidencias (id, parihuela_id, tipo, detalle, fecha_hora)
                VALUES (1, 1, 'MEZCLA_VARIEDAD', 'Biloxi en lugar de Ventura', 1700000020000)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO pesajes (id, parihuela_id, jornalero, cantidad_jabas, fecha_hora)
                VALUES (1, 1, 'Juan Perez', 10, 1700000021000)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO bitacora (id, parihuela_id, evento, detalle, fecha_hora)
                VALUES (1, 1, 'ALERTA_POR_VENCER', 'Quedan 5 minutos', 1700000022000)
                """.trimIndent()
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            AgroValleDatabase.MIGRATION_2_3
        )
        migratedDb.query("SELECT id, codigo FROM parihuelas WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("PAR-200-0001", cursor.getString(1))
        }
        migratedDb.query(
            "SELECT parihuela_id, estado, accion_tomada, responsable FROM incidencias WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("PENDIENTE", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
        migratedDb.query(
            "SELECT parihuela_id, jornalero, cantidad_jabas FROM pesajes WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Juan Perez", cursor.getString(1))
            assertEquals(10, cursor.getInt(2))
        }
        migratedDb.query(
            "SELECT parihuela_id, evento, detalle FROM bitacora WHERE id = 1"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("ALERTA_POR_VENCER", cursor.getString(1))
            assertEquals("Quedan 5 minutos", cursor.getString(2))
        }
        migratedDb.close()
    }

    @Test
    fun migrate3To4() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-300-0001', 'ABIERTA', 'Biloxi', 'Cuartel 01', 15, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            AgroValleDatabase.MIGRATION_3_4
        )
        migratedDb.execSQL(
            """
            INSERT INTO reasignaciones (jornalero, cuartel_origen, cuartel_destino, motivo, hora, sincronizado)
            VALUES ('Maria Lopez', 'Cuartel 01', 'Cuartel 05', 'Refuerzo en cuartel con alta carga', 1700000030000, 0)
            """.trimIndent()
        )
        migratedDb.query("SELECT cuartel_destino, sincronizado FROM reasignaciones").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Cuartel 05", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        migratedDb.close()
    }

    @Test
    fun migrate4To5() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-400-0001', 'ABIERTA', 'Biloxi', 'Cuartel 01', 10, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO pesajes (id, parihuela_id, jornalero, cantidad_jabas, fecha_hora)
                VALUES (1, 1, 'Carlos Alva', 10, 1700000040000)
                """.trimIndent()
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AgroValleDatabase.MIGRATION_4_5
        )
        migratedDb.query("SELECT peso_kg FROM pesajes WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0.0, cursor.getDouble(0), 0.0001)
        }
        migratedDb.close()
    }

    @Test
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-500-0001', 'ABIERTA', 'Ventura', 'Cuartel 18', 25, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            AgroValleDatabase.MIGRATION_5_6
        )
        migratedDb.query("SELECT codigo, clp, cuadrilla FROM parihuelas WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("PAR-500-0001", cursor.getString(0))
            assertEquals("", cursor.getString(1))
            assertEquals("", cursor.getString(2))
        }
        migratedDb.close()
    }

    @Test
    fun migrateAll1To6() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO parihuelas (id, codigo, estado, variedad, cuartel, conteo_jabas, pisos, hora_inicio, hora_sellado, motivo_sellado_anticipado)
                VALUES (1, 'PAR-CHAIN-0001', 'ABIERTA', 'Biloxi', 'Cuartel 01', 12, 0, 1700000000000, NULL, NULL)
                """.trimIndent()
            )
        }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AgroValleDatabase::class.java,
            TEST_DB
        )
            .addMigrations(
                AgroValleDatabase.MIGRATION_1_2,
                AgroValleDatabase.MIGRATION_2_3,
                AgroValleDatabase.MIGRATION_3_4,
                AgroValleDatabase.MIGRATION_4_5,
                AgroValleDatabase.MIGRATION_5_6
            )
            .build()
            .apply {
                openHelper.writableDatabase.close()
            }
    }
}
