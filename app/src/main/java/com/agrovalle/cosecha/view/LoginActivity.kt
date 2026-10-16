package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityLoginBinding
import com.agrovalle.cosecha.util.SesionTurno

/**
 * Pantalla 01 - Login / Seleccion de Rol (NavGraph Sprint 1).
 * Configura el turno (rol, jornada y fundo) y lo guarda en la sesion compartida
 * [SesionTurno], de modo que todas las pantallas lo conserven.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    companion object {
        const val ROL_CAPATAZ = SesionTurno.ROL_CAPATAZ
        const val ROL_ASISTENTE = SesionTurno.ROL_ASISTENTE

        private const val KEY_POS_ROL = "key_pos_rol"
        private const val KEY_POS_JORNADA = "key_pos_jornada"
        private const val KEY_POS_FUNDO = "key_pos_fundo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinners()

        if (savedInstanceState != null) {
            binding.spnRol.setSelection(savedInstanceState.getInt(KEY_POS_ROL, 0))
            binding.spnJornada.setSelection(savedInstanceState.getInt(KEY_POS_JORNADA, 0))
            binding.spnFundo.setSelection(savedInstanceState.getInt(KEY_POS_FUNDO, 0))
        }

        configurarBotonIniciar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_POS_ROL, binding.spnRol.selectedItemPosition)
        outState.putInt(KEY_POS_JORNADA, binding.spnJornada.selectedItemPosition)
        outState.putInt(KEY_POS_FUNDO, binding.spnFundo.selectedItemPosition)
    }

    private fun configurarSpinners() {
        val roles = listOf("Capataz de Cuadrilla", "Asistente de Pesaje")
        binding.spnRol.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        val jornadas = listOf(SesionTurno.JORNADA_MANANA_TXT, SesionTurno.JORNADA_TARDE_TXT)
        binding.spnJornada.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jornadas)

        val fundos = listOf("Fundo Principal Virú", "Fundo Chao Norte")
        binding.spnFundo.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fundos)
    }

    private fun configurarBotonIniciar() {
        binding.btnIniciarTurno.setOnClickListener {
            val rolNormalizado =
                if (binding.spnRol.selectedItemPosition == 0) ROL_CAPATAZ else ROL_ASISTENTE
            val jornadaSeleccionada = binding.spnJornada.selectedItem.toString()
            val fundoSeleccionado = binding.spnFundo.selectedItem.toString()

            // Estado de turno COMPARTIDO para toda la aplicacion.
            SesionTurno.iniciar(
                rol = rolNormalizado,
                jornada = jornadaSeleccionada,
                fundo = fundoSeleccionado,
                operador = if (rolNormalizado == ROL_CAPATAZ) {
                    "Capataz de cuadrilla"
                } else {
                    "Asistente de pesaje"
                }
            )

            startActivity(Intent(this, MenuActivity::class.java))
        }
    }
}
