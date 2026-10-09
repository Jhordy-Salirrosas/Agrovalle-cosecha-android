package com.dapm.android.agrovalle

import android.app.Application
import com.dapm.android.agrovalle.data.local.AgroValleDatabase

/**
 * Clase Application personalizada que inicializa la base de datos Room
 * de forma lazy (al primer acceso). Provee acceso global a la instancia
 * de la base de datos para ViewModels y Fragments.
 */
class AgroValleApp : Application() {

    /** Instancia lazy de la base de datos Room. */
    val database: AgroValleDatabase by lazy {
        AgroValleDatabase.obtenerInstancia(this)
    }

    override fun onCreate() {
        super.onCreate()
        instancia = this
    }

    companion object {
        lateinit var instancia: AgroValleApp
            private set
    }
}
