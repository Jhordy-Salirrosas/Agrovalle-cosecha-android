package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityLoginBinding
import com.agrovalle.cosecha.viewmodel.TurnoViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: TurnoViewModel by viewModels()

    companion object {
        const val EXTRA_ROL = "extra_rol"
        const val EXTRA_JORNADA = "extra_jornada"
        const val EXTRA_FUNDO = "extra_fundo"

        const val ROL_CAPATAZ = "Capataz"
        const val ROL_ASISTENTE = "Asistente"

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
        val adapterRol = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spnRol.adapter = adapterRol

        val jornadas = listOf("Jornada Mañana (06:00 - 14:00)", "Jornada Tarde (14:00 - 22:00)")
        val adapterJornada = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, jornadas)
        binding.spnJornada.adapter = adapterJornada

        val fundos = listOf("Fundo Principal Virú", "Fundo Chao Norte")
        val adapterFundo = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fundos)
        binding.spnFundo.adapter = adapterFundo
    }

    private fun configurarBotonIniciar() {
        binding.btnIniciarTurno.setOnClickListener {
            val posicionRol = binding.spnRol.selectedItemPosition
            val rolNormalizado = if (posicionRol == 0) ROL_CAPATAZ else ROL_ASISTENTE
            val jornadaSeleccionada = binding.spnJornada.selectedItem.toString()
            val fundoSeleccionado = binding.spnFundo.selectedItem.toString()

            viewModel.setContextoLogin(rolNormalizado, jornadaSeleccionada, fundoSeleccionado)

            val intent = Intent(this, MenuActivity::class.java).apply {
                putExtra(EXTRA_ROL, rolNormalizado)
                putExtra(EXTRA_JORNADA, jornadaSeleccionada)
                putExtra(EXTRA_FUNDO, fundoSeleccionado)
            }
            startActivity(intent)
        }
    }
}
