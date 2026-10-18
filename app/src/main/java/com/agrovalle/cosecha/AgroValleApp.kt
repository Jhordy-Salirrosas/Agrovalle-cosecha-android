package com.agrovalle.cosecha

import android.app.Application
import com.agrovalle.cosecha.data.DatosMemoria

/**
 * Application de AgroValle Cosecha - Sprint 1.
 *
 * Ya NO inicializa ninguna base de datos: la app trabaja enteramente en memoria
 * a traves del singleton [com.agrovalle.cosecha.data.DatosMemoria], que se
 * precarga solo con tocarse (inicializacion perezosa del objeto Kotlin).
 *
 * Se conserva la clase Application porque el AndroidManifest la declara y
 * porque es el punto natural para futuras configuraciones globales.
 */
class AgroValleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Tocar el singleton garantiza que el padron de 30 jornaleros quede
        // cargado en memoria desde el arranque de la aplicacion.
        DatosMemoria
    }
}
