package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de incidencias de mezcla (Sprint 2).
 */
class IncidenciasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        val db = AgroValleDatabase.getInstance(application)
        ParihuelaRepository(
            parihuelaDao = db.parihuelaDao(),
            bitacoraDao = db.bitacoraDao(),
            incidenciaDao = db.incidenciaDao(),
            pesajeDao = db.pesajeDao()
        )
    }

    private val _incidencias = MutableStateFlow<List<IncidenciaEntity>>(emptyList())
    val incidencias: StateFlow<List<IncidenciaEntity>> = _incidencias.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observarTodasLasIncidencias().collect { lista ->
                _incidencias.value = lista
            }
        }
    }
}
