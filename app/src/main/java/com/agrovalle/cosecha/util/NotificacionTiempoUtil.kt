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

    data class ResultadoNotificacion(
        val mostrada: Boolean,
        val motivoFallo: String? = null
    )

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

    /** Verifica permiso en Android 13+ y estado general de notificaciones del sistema. */
    internal fun evaluarDisponibilidadNotificacion(
        sdkInt: Int,
        permisoPostNotificationsConcedido: Boolean,
        notificacionesHabilitadasSistema: Boolean
    ): String? {
        if (sdkInt >= Build.VERSION_CODES.TIRAMISU && !permisoPostNotificationsConcedido) {
            return "Permiso POST_NOTIFICATIONS no concedido"
        }
        if (!notificacionesHabilitadasSistema) {
            return "Notificaciones deshabilitadas en el sistema"
        }
        return null
    }

    fun mostrarAlertaPorVencer(context: Context, parihuela: ParihuelaEntity): ResultadoNotificacion =
        enviarNotificacion(
            context = context,
            notificacionId = parihuela.id.toInt(),
            titulo = "Parihuela por vencer: ${parihuela.codigo}",
            contenido = "Quedan 5 minutos o menos (${parihuela.variedad} - ${parihuela.cuartel})."
        )

    fun mostrarAlertaRetencionExcedida(
        context: Context,
        parihuela: ParihuelaEntity
    ): ResultadoNotificacion =
        enviarNotificacion(
            context = context,
            notificacionId = 10_000 + parihuela.id.toInt(),
            titulo = "Retención excedida: ${parihuela.codigo}",
            contenido = "La parihuela superó el tiempo máximo de espera (${parihuela.variedad} - ${parihuela.cuartel})."
        )

    private fun enviarNotificacion(
        context: Context,
        notificacionId: Int,
        titulo: String,
        contenido: String
    ): ResultadoNotificacion {
        crearCanal(context)
        val permisoConcedido = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val manager = NotificationManagerCompat.from(context)
        val motivoBloqueo = evaluarDisponibilidadNotificacion(
            sdkInt = Build.VERSION.SDK_INT,
            permisoPostNotificationsConcedido = permisoConcedido,
            notificacionesHabilitadasSistema = manager.areNotificationsEnabled()
        )
        if (motivoBloqueo != null) {
            return ResultadoNotificacion(mostrada = false, motivoFallo = motivoBloqueo)
        }

        val notification = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(contenido)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        return try {
            manager.notify(notificacionId, notification)
            ResultadoNotificacion(mostrada = true)
        } catch (se: SecurityException) {
            ResultadoNotificacion(
                mostrada = false,
                motivoFallo = se.message ?: "SecurityException al mostrar notificación"
            )
        }
    }
}
