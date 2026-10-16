package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.AgroValleApp
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.mapper.aDominio
import com.agrovalle.cosecha.model.Parihuela
import com.agrovalle.cosecha.util.Constantes
import kotlinx.coroutines.launch

/**
 * AGV-002: contexto agronomico del turno (cuartel, CLP, variedad y parihuela).
 *
 * Regla de negocio: mientras exista una parihuela ABIERTA no se puede cambiar
 * el contexto; primero debe sellarse.
 */
class ContextoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as AgroValleApp).repository

    private val _parihuelaAbierta = MutableLiveData<Parihuela?>(null)
    val parihuelaAbierta: LiveData<Parihuela?> = _parihuelaAbierta

    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    fun cargarEstado() {
        viewModelScope.launch {
            _parihuelaAbierta.value = repo.getParihuelaAbierta()?.aDominio()
        }
    }

    /**
     * Abre una parihuela nueva con el contexto elegido. [onAbierta] recibe la
     * parihuela creada para continuar con la calibracion de tara.
     */
    fun abrirParihuela(
        cuartel: String,
        clp: String,
        variedad: String,
        onAbierta: (Parihuela) -> Unit
    ) {
        viewModelScope.launch {
            if (repo.getParihuelaAbierta() != null) {
                _mensaje.value =
                    "Ya existe una parihuela abierta. Selle la parihuela en curso para continuar."
                return@launch
            }

            val correlativo = repo.contarParihuelas() + 1
            val codigoMarbete = "MARB-%03d".format(correlativo)

            val id = repo.abrirParihuela(
                ParihuelaEntity(
                    codigoMarbete = codigoMarbete,
                    cuartel = cuartel,
                    variedad = variedad,
                    clp = clp,
                    totalJabas = 0,
                    estado = ParihuelaEntity.ESTADO_ABIERTA
                )
            )

            val creada = Parihuela(
                id = id,
                codigoMarbete = codigoMarbete,
                cuartel = cuartel,
                variedad = variedad,
                clp = clp,
                totalJabas = 0,
                estado = ParihuelaEntity.ESTADO_ABIERTA
            )
            _parihuelaAbierta.value = creada
            _mensaje.value = "Parihuela $codigoMarbete abierta. Continúe con la calibración de tara."
            onAbierta(creada)
        }
    }

    fun cerrarParihuela() {
        viewModelScope.launch {
            val abierta = repo.getParihuelaAbierta() ?: return@launch
            repo.cerrarParihuela(abierta)
            _parihuelaAbierta.value = null
            _mensaje.value = "Parihuela ${abierta.codigoMarbete} sellada correctamente."
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }

    companion object {
        /** Estandar de estiba usado por la HU (100 a 120 jabas). */
        const val JABAS_MIN = Constantes.JABAS_ESTANDAR_MIN
        const val JABAS_MAX = Constantes.JABAS_ESTANDAR_MAX
    }
}
