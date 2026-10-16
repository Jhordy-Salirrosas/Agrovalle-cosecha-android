package com.agrovalle.cosecha

import android.app.Application
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.PadronSeeder
import com.agrovalle.cosecha.data.repository.AgroValleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application de AgroValle Cosecha.
 * Provee la base de datos Room y el repositorio a toda la app, y precarga
 * el padron local de la cuadrilla la primera vez que se abre.
 */
class AgroValleApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AgroValleDatabase by lazy { AgroValleDatabase.getInstance(this) }

    val repository: AgroValleRepository by lazy { AgroValleRepository(database) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            PadronSeeder.sembrarSiVacio(database)
        }
    }
}
