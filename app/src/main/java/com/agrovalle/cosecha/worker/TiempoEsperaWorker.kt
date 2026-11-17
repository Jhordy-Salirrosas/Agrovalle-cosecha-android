package com.agrovalle.cosecha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.util.NotificacionTiempoUtil
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Worker periódico que evalúa las parihuelas abiertas desde la BD
 * y dispara alertas de alta prioridad cuando entran en POR_VENCER.
 */
class TiempoEsperaWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AgroValleDatabase.getInstance(applicationContext)
        val repository = ParihuelaRepository(
            parihuelaDao = db.parihuelaDao(),
            bitacoraDao = db.bitacoraDao(),
            incidenciaDao = db.incidenciaDao(),
            pesajeDao = db.pesajeDao()
        )
        val porNotificar = repository.evaluarAlertasTiempo()
        porNotificar.forEach { parihuela ->
            NotificacionTiempoUtil.mostrarAlertaPorVencer(applicationContext, parihuela)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "revision_tiempo_espera_parihuela"
        const val KEY_PARIHUELA_ID = "parihuela_id"

        fun programarRevisionPeriodica(context: Context) {
            val request = PeriodicWorkRequestBuilder<TiempoEsperaWorker>(
                15,
                TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun programarAlertaExacta(
            context: Context,
            parihuelaId: Long,
            horaInicio: Date,
            ahora: Date = Date()
        ) {
            val delayMs = ParihuelaRepository.calcularDemoraAlertaExactaMs(horaInicio, ahora)
            val tag = "alerta_parihuela_$parihuelaId"
            val request = OneTimeWorkRequestBuilder<TiempoEsperaWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_PARIHUELA_ID to parihuelaId))
                .addTag(tag)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "alerta_exacta_parihuela_$parihuelaId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
