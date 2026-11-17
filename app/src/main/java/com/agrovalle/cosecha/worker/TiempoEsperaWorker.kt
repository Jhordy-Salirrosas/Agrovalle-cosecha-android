package com.agrovalle.cosecha.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.util.NotificacionTiempoUtil
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
    }
}
