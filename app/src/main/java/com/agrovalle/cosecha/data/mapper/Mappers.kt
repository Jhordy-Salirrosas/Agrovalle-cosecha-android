package com.agrovalle.cosecha.data.mapper

import com.agrovalle.cosecha.data.local.entity.AsistenciaEntity
import com.agrovalle.cosecha.data.local.entity.CalibracionTaraEntity
import com.agrovalle.cosecha.data.local.entity.JornaleroEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import com.agrovalle.cosecha.model.Asistencia
import com.agrovalle.cosecha.model.CalibracionTara
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.model.Parihuela
import com.agrovalle.cosecha.model.Pesaje

/**
 * Mapeadores Entidad (Room) -> Modelo de dominio.
 * Mantienen el desacoplamiento MVVM: la UI nunca depende de las entidades.
 */

fun JornaleroEntity.aDominio(): Jornalero = Jornalero(
    id = id,
    dni = dni,
    nombres = nombres,
    apellidos = apellidos,
    codigoQr = codigoQr,
    cuadrillaCodigo = cuadrillaCodigo
)

fun AsistenciaEntity.aDominio(): Asistencia = Asistencia(
    id = id,
    jornaleroId = jornaleroId,
    fecha = fecha,
    horaIngreso = horaIngreso,
    estado = estado,
    metodo = metodo
)

fun ParihuelaEntity.aDominio(): Parihuela = Parihuela(
    id = id,
    codigoMarbete = codigoMarbete,
    cuartel = cuartel,
    variedad = variedad,
    clp = clp,
    totalJabas = totalJabas,
    estado = estado
)

fun PesajeEntity.aDominio(): Pesaje = Pesaje(
    id = id,
    jornaleroId = jornaleroId,
    parihuelaId = parihuelaId,
    cantidadJabas = cantidadJabas,
    pesoBruto = pesoBruto,
    taraTotal = taraTotal,
    pesoNeto = pesoNeto,
    timestamp = timestamp,
    esTaraPorDefecto = esTaraPorDefecto
)

fun CalibracionTaraEntity.aDominio(): CalibracionTara = CalibracionTara(
    id = id,
    taraPromedioCalculada = taraPromedioCalculada,
    pesoMuestra5Jabas = pesoMuestra5Jabas,
    asistenteNombre = asistenteNombre,
    timestamp = timestamp,
    aprobado = aprobado
)
