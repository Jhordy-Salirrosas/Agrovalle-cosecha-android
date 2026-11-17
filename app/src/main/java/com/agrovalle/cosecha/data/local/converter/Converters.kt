package com.agrovalle.cosecha.data.local.converter

import androidx.room.TypeConverter
import com.agrovalle.cosecha.data.local.entity.EstadoIncidencia
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import java.util.Date

/**
 * Conversores de tipos para Room (fechas y enumeraciones).
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromEstadoParihuela(value: String?): EstadoParihuela? =
        value?.let { EstadoParihuela.valueOf(it) }

    @TypeConverter
    fun estadoParihuelaToString(estado: EstadoParihuela?): String? = estado?.name

    @TypeConverter
    fun fromEstadoIncidencia(value: String?): EstadoIncidencia? =
        value?.let { EstadoIncidencia.valueOf(it) }

    @TypeConverter
    fun estadoIncidenciaToString(estado: EstadoIncidencia?): String? = estado?.name
}
