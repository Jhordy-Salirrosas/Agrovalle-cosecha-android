package com.agrovalle.cosecha

import android.app.Application

/**
 * Clase Application de AgroValle Cosecha.
 *
 * Sprint 1: Sin inicialización de Room (se usa MockRepository).
 * TODO [Coder 1]: Inicializar Room Database aquí:
 *   val database by lazy {
 *       Room.databaseBuilder(this, AgroValleDatabase::class.java, "agrovalle_db").build()
 *   }
 */
class AgroValleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Room se inicializará aquí en Sprint 2 cuando Coder 1 lo implemente.
    }
}
