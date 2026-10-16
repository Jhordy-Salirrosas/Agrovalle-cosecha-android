package com.agrovalle.cosecha.data.repository

import androidx.room.withTransaction
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.AsistenciaEntity
import com.agrovalle.cosecha.data.local.entity.CalibracionTaraEntity
import com.agrovalle.cosecha.data.local.entity.JornaleroEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity

/**
 * Repositorio unico de acceso a datos locales (Room).
 * Centraliza las operaciones para que los ViewModels no conozcan los DAOs.
 */
class AgroValleRepository(private val db: AgroValleDatabase) {

    // ---------------------------------------------------------------- Padron
    suspend fun contarJornaleros(): Int = db.jornaleroDao().contar()

    suspend fun buscarJornaleroPorQr(codigoQr: String): JornaleroEntity? =
        db.jornaleroDao().buscarPorQr(codigoQr.trim())

    suspend fun buscarJornaleroPorDni(dni: String): JornaleroEntity? =
        db.jornaleroDao().buscarPorDni(dni.trim())

    /** AGV-016: coincidencias por ultimos digitos dentro del padron local. */
    suspend fun buscarJornalerosPorUltimosDigitos(ultimosDigitos: String): List<JornaleroEntity> =
        db.jornaleroDao().buscarPorUltimosDigitosDni(ultimosDigitos.trim())

    suspend fun listarJornalerosPorCuadrilla(codigoCuadrilla: String): List<JornaleroEntity> =
        db.jornaleroDao().listarPorCuadrilla(codigoCuadrilla)

    // ----------------------------------------------------------- Asistencias
    suspend fun buscarAsistencia(jornaleroId: Long, fecha: String): AsistenciaEntity? =
        db.asistenciaDao().buscarPorJornaleroYFecha(jornaleroId, fecha)

    suspend fun contarAsistencias(fecha: String): Int =
        db.asistenciaDao().contarPorFecha(fecha)

    suspend fun contarIngresosManuales(fecha: String): Int =
        db.asistenciaDao().contarIngresosManualesPorFecha(fecha)

    /** Devuelve el id insertado o -1 si el jornalero ya tenia asistencia hoy. */
    suspend fun registrarAsistencia(asistencia: AsistenciaEntity): Long =
        db.asistenciaDao().insertar(asistencia)

    suspend fun listarAsistencias(fecha: String): List<AsistenciaEntity> =
        db.asistenciaDao().listarPorFecha(fecha)

    // --------------------------------------------------- Calibracion de tara
    suspend fun getUltimaCalibracion(): CalibracionTaraEntity? =
        db.calibracionTaraDao().getUltimaCalibracion()

    suspend fun guardarCalibracion(calibracion: CalibracionTaraEntity): Long =
        db.calibracionTaraDao().insertarCalibracion(calibracion)

    // ------------------------------------------------------------- Parihuela
    suspend fun getParihuelaAbierta(): ParihuelaEntity? =
        db.parihuelaDao().getParihuelaAbierta()

    suspend fun contarParihuelas(): Int = db.parihuelaDao().contar()

    suspend fun abrirParihuela(parihuela: ParihuelaEntity): Long =
        db.parihuelaDao().insertar(parihuela)

    suspend fun cerrarParihuela(parihuela: ParihuelaEntity) =
        db.parihuelaDao().actualizar(parihuela.copy(estado = ParihuelaEntity.ESTADO_SELLADA))

    suspend fun sumarJabasAParihuela(parihuelaId: Long, jabas: Int) =
        db.parihuelaDao().sumarJabas(parihuelaId, jabas)

    // ---------------------------------------------------------------- Pesaje
    suspend fun registrarPesaje(pesaje: PesajeEntity): Long =
        db.pesajeDao().insertarPesaje(pesaje)

    /**
     * Persistencia ATOMICA del pesaje (AGV-003): inserta la entrega y suma las
     * jabas a la parihuela dentro de una sola transaccion SQLite.
     */
    suspend fun registrarPesajeAtomico(pesaje: PesajeEntity): Long =
        db.withTransaction {
            val id = db.pesajeDao().insertarPesaje(pesaje)
            pesaje.parihuelaId?.let { parihuelaId ->
                db.parihuelaDao().sumarJabas(parihuelaId, pesaje.cantidadJabas)
            }
            id
        }

    /** Acumulado diario del cosechador (AGV-004). */
    suspend fun getAcumuladoDiario(jornaleroId: Long, desde: Long, hasta: Long): Double =
        db.pesajeDao().getAcumuladoDiarioId(jornaleroId, desde, hasta) ?: 0.0

    suspend fun getPesajesDeJornalero(jornaleroId: Long): List<PesajeEntity> =
        db.pesajeDao().getPesajesPorJornaleroId(jornaleroId)
}
