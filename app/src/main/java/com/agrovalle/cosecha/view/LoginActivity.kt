package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityLoginBinding
import com.agrovalle.cosecha.util.SesionTurno
import com.agrovalle.cosecha.viewmodel.LoginViewModel

/**
 * Pantalla 01 - Login / Seleccion de Rol (NavGraph Sprint 1).
 * Configura el turno (rol, jornada y fundo) y lo guarda en la sesion compartida
 * [SesionTurno], de modo que todas las pantallas lo conserven.
 *
 * Las posiciones de los Spinners viven en el SavedStateHandle de
 * [LoginViewModel]: la Activity ya no guarda estado en onSaveInstanceState.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    /**
     * Al restaurar las posiciones, `setSelection` dispara `onItemSelected` y
     * machacaria el SavedStateHandle antes de terminar de pintar. Mientras este
     * en `true` se ignoran esos eventos automaticos.
     */
    private var ignorarSeleccionInicial = true

    companion object {
        const val ROL_CAPATAZ = SesionTurno.ROL_CAPATAZ
        const val ROL_ASISTENTE = SesionTurno.ROL_ASISTENTE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinners()
        configurarBotonIniciar()
        restaurarSeleccion()
    }

    private fun configurarSpinners() {
        binding.spnRol.adapter = crearAdapter(viewModel.roles)
        binding.spnJornada.adapter = crearAdapter(viewModel.jornadas)
        binding.spnFundo.adapter = crearAdapter(viewModel.fundos)

        binding.spnRol.onItemSelectedListener = selectorPara { posicion ->
            viewModel.posicionRol = posicion
        }
        binding.spnJornada.onItemSelectedListener = selectorPara { posicion ->
            viewModel.posicionJornada = posicion
        }
        binding.spnFundo.onItemSelectedListener = selectorPara { posicion ->
            viewModel.posicionFundo = posicion
        }
    }

    private fun crearAdapter(opciones: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opciones)

    /** Listener que ignora el primer evento automatico del arranque/restauracion. */
    private fun selectorPara(onCambio: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (ignorarSeleccionInicial) return
                onCambio(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

    /** Pinta en los Spinners las posiciones recuperadas del SavedStateHandle. */
    private fun restaurarSeleccion() {
        binding.spnRol.setSelection(viewModel.posicionRol)
        binding.spnJornada.setSelection(viewModel.posicionJornada)
        binding.spnFundo.setSelection(viewModel.posicionFundo)

        // A partir de aqui el usuario ya manda: los cambios cuentan.
        ignorarSeleccionInicial = false
    }

    private fun configurarBotonIniciar() {
        binding.btnIniciarTurno.setOnClickListener {
            // Guarda el turno en SesionTurno (estado compartido de la app)
            // y vuelca las posiciones actuales al SavedStateHandle.
            sincronizarPosiciones()
            viewModel.iniciarTurno()
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    /**
     * Refresca el SavedStateHandle con lo que se ve en pantalla. Garantiza que
     * el turno iniciado coincida con la seleccion visible aunque el usuario haya
     * cambiado de Spinner sin disparar el listener.
     */
    private fun sincronizarPosiciones() {
        viewModel.posicionRol = binding.spnRol.selectedItemPosition
        viewModel.posicionJornada = binding.spnJornada.selectedItemPosition
        viewModel.posicionFundo = binding.spnFundo.selectedItemPosition
    }
}
