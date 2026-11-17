package com.agrovalle.cosecha.util

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificacionTiempoUtilTest {

    @Test
    fun `en Android 13+ sin permiso POST_NOTIFICATIONS reporta bloqueo`() {
        val motivo = NotificacionTiempoUtil.evaluarDisponibilidadNotificacion(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            permisoPostNotificationsConcedido = false,
            notificacionesHabilitadasSistema = true
        )

        assertTrue(motivo != null && motivo.contains("POST_NOTIFICATIONS"))
    }

    @Test
    fun `con notificaciones deshabilitadas en el sistema reporta bloqueo incluso sin exigir TIRAMISU`() {
        val motivo = NotificacionTiempoUtil.evaluarDisponibilidadNotificacion(
            sdkInt = Build.VERSION_CODES.O,
            permisoPostNotificationsConcedido = true,
            notificacionesHabilitadasSistema = false
        )

        assertEquals("Notificaciones deshabilitadas en el sistema", motivo)
    }

    @Test
    fun `con permiso y sistema habilitado permite mostrar notificacion`() {
        val motivo = NotificacionTiempoUtil.evaluarDisponibilidadNotificacion(
            sdkInt = Build.VERSION_CODES.TIRAMISU,
            permisoPostNotificationsConcedido = true,
            notificacionesHabilitadasSistema = true
        )

        assertNull(motivo)
    }
}
