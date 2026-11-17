package com.agrovalle.cosecha.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agrovalle.cosecha.data.local.AgroValleDatabase
import com.agrovalle.cosecha.data.repository.ConsultaOrigenParihuela
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la consulta de origen de parihuela por código (Sprint 2).
 */
class ConsultaOrigenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParihuelaRepository by lazy {
        AgroValleDatabase.getInstance(application).crearParihuelaRepository()
    }

    private val _resultado = MutableStateFlow<ConsultaOrigenParihuela?>(null)
    val resultado: StateFlow<ConsultaOrigenParihuela?> = _resultado.asStateFlow()

    fun consultarPorCodigo(codigo: String) {
        viewModelScope.launch {
            _resultado.value = repository.consultarOrigenPorCodigo(codigo)
        }
    }
}
