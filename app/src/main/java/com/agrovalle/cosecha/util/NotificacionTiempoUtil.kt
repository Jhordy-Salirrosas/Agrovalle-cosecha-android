package com.agrovalle.cosecha.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity

/**
 * Utilidad para crear el canal de alta prioridad y mostrar alertas de tiempo de parihuela.
 */
object NotificacionTiempoUtil {

    const val CANAL_ID = "canal_alerta_parihuela"
    private const val CANAL_NOMBRE = "Alertas de Tiempo de Parihuela"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de alta prioridad cuando una parihuela está por vencer"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(canal)
        }
    }

    fun mostrarAlertaPorVencer(context: Context, parihuela: ParihuelaEntity) {
        crearCanal(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        val notification = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Parihuela por vencer: ${parihuela.codigo}")
            .setContentText("Quedan 5 minutos o menos (${parihuela.variedad} - ${parihuela.cuartel}).")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(parihuela.id.toInt(), notification)
    }
}
