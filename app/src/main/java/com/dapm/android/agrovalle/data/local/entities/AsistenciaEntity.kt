package com.dapm.android.agrovalle.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que registra la asistencia diaria de un jornalero.
 * Clasificación automática: PUNTUAL (<= 06:00) o TARDANZA (06:01-06:30).
 * Método de registro: QR_SCAN (fotocheck) o DNI_MANUAL (búsqueda por DNI).
 */
@Entity(
    tableName = "asistencias",
    foreignKeys = [
        ForeignKey(
            entity = JornaleroEntity::class,
            parentColumns = ["id"],
            childColumns = ["jornalero_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["jornalero_id"]),
        Index(value = ["jornalero_id", "fecha"], unique = true)
    ]
)
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "jornalero_id")
    val jornaleroId: Long,

    @ColumnInfo(name = "fecha")
    val fecha: String,

    @ColumnInfo(name = "hora_ingreso")
    val horaIngreso: String,

    @ColumnInfo(name = "marca_tiempo")
    val marcaTiempo: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "estado_asistencia")
    val estadoAsistencia: String,

    @ColumnInfo(name = "metodo_registro")
    val metodoRegistro: String,

    @ColumnInfo(name = "esta_sincronizado")
    val estaSincronizado: Boolean = false
)
