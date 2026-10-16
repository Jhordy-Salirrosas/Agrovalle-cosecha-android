package com.agrovalle.cosecha.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Jornalero del padrón local (tabla `jornaleros`).
 * FK `cuadrilla_codigo` -> `cuadrillas.codigo` (1:N).
 */
@Entity(
    tableName = "jornaleros",
    foreignKeys = [
        ForeignKey(
            entity = CuadrillaEntity::class,
            parentColumns = ["codigo"],
            childColumns = ["cuadrilla_codigo"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dni"]),
        Index(value = ["codigo_qr"], unique = true),
        Index(value = ["cuadrilla_codigo"])
    ]
)
data class JornaleroEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** DNI de 8 dígitos. */
    @ColumnInfo(name = "dni")
    val dni: String,

    /** Código del fotocheck (QR / Code 128). */
    @ColumnInfo(name = "codigo_qr")
    val codigoQr: String,

    @ColumnInfo(name = "nombres")
    val nombres: String,

    @ColumnInfo(name = "apellidos")
    val apellidos: String,

    @ColumnInfo(name = "cuadrilla_codigo")
    val cuadrillaCodigo: String
)
