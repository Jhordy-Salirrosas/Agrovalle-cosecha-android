package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityContextoBinding
import com.agrovalle.cosecha.model.Parihuela
import com.agrovalle.cosecha.viewmodel.ContextoViewModel

/**
 * Pantalla 05 - Contexto agronomico del turno (AGV-002).
 *
 * Cuartel, CLP y variedad se guardan en la parihuela digital (Room). Mientras
 * la parihuela este ABIERTA el contexto queda bloqueado: para cambiar de cuartel
 * o variedad hay que sellarla primero.
 */
class ContextoAgronomicoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CUARTEL = "extra_cuartel"
        const val EXTRA_VARIEDAD = "extra_variedad"
        const val EXTRA_PARIHUELA_ID = "extra_parihuela_id"
        const val EXTRA_MARBETE = "extra_marbete"

        private const val KEY_POS_CUARTEL = "key_pos_cuartel"
        private const val KEY_POS_CLP = "key_pos_clp"
        private const val KEY_BILOXI = "key_biloxi"
        private const val KEY_VENTURA = "key_ventura"
    }

    private lateinit var binding: ActivityContextoBinding
    private val viewModel: ContextoViewModel by viewModels()

    private var estaParihuelaAbierta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContextoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinners()
        configurarListeners()

        if (savedInstanceState != null) {
            binding.spnCuartel.setSelection(savedInstanceState.getInt(KEY_POS_CUARTEL, 0))
            binding.spnClp.setSelection(savedInstanceState.getInt(KEY_POS_CLP, 0))
            binding.rbBiloxi.isChecked = savedInstanceState.getBoolean(KEY_BILOXI, false)
            binding.rbVentura.isChecked = savedInstanceState.getBoolean(KEY_VENTURA, false)
        }

        observarEstado()
        viewModel.cargarEstado()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_POS_CUARTEL, binding.spnCuartel.selectedItemPosition)
        outState.putInt(KEY_POS_CLP, binding.spnClp.selectedItemPosition)
        outState.putBoolean(KEY_BILOXI, binding.rbBiloxi.isChecked)
        outState.putBoolean(KEY_VENTURA, binding.rbVentura.isChecked)
    }

    private fun configurarSpinners() {
        val cuarteles = listOf(
            "Seleccione Cuartel...", "Cuartel 01", "Cuartel 05", "Cuartel 12", "Cuartel 18"
        )
        binding.spnCuartel.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cuarteles)

        val clps = listOf(
            "Seleccione CLP...", "CLP-01 (Virú Central)", "CLP-02 (Virú Sur)", "CLP-03 (Chao Norte)"
        )
        binding.spnClp.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clps)
    }

    private fun configurarListeners() {
        binding.btnVolverContexto.setOnClickListener { finish() }

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                validarFormulario()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                validarFormulario()
            }
        }

        binding.spnCuartel.onItemSelectedListener = itemSelectedListener
        binding.spnClp.onItemSelectedListener = itemSelectedListener
        binding.rgVariedad.setOnCheckedChangeListener { _, _ -> validarFormulario() }

        binding.btnAccionParihuela.setOnClickListener {
            if (estaParihuelaAbierta) cerrarParihuela() else abrirParihuela()
        }
    }

    private fun observarEstado() {
        viewModel.parihuelaAbierta.observe(this) { parihuela ->
            estaParihuelaAbierta = parihuela != null
            actualizarUiEstadoParihuela(parihuela)
        }
        viewModel.mensaje.observe(this) { mensaje ->
            if (!mensaje.isNullOrBlank()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirMensaje()
            }
        }
    }

    private fun validarFormulario() {
        if (estaParihuelaAbierta) {
            binding.btnAccionParihuela.isEnabled = true
            return
        }
        val cuartelValido = binding.spnCuartel.selectedItemPosition > 0
        val clpValido = binding.spnClp.selectedItemPosition > 0
        val variedadValida = binding.rbBiloxi.isChecked || binding.rbVentura.isChecked
        binding.btnAccionParihuela.isEnabled = cuartelValido && clpValido && variedadValida
    }

    private fun abrirParihuela() {
        val cuartel = binding.spnCuartel.selectedItem.toString()
        val clp = binding.spnClp.selectedItem.toString()
        val variedad = if (binding.rbBiloxi.isChecked) "Biloxi" else "Ventura"

        viewModel.abrirParihuela(cuartel, clp, variedad) { parihuela ->
            val intent = Intent(this, CalibrarTaraActivity::class.java).apply {
                putExtra(EXTRA_CUARTEL, parihuela.cuartel)
                putExtra(EXTRA_VARIEDAD, parihuela.variedad)
                putExtra(EXTRA_PARIHUELA_ID, parihuela.id)
                putExtra(EXTRA_MARBETE, parihuela.codigoMarbete)
            }
            startActivity(intent)
        }
    }

    private fun cerrarParihuela() {
        viewModel.cerrarParihuela()
        binding.spnCuartel.setSelection(0)
        binding.spnClp.setSelection(0)
        binding.rgVariedad.clearCheck()
        validarFormulario()
    }

    private fun actualizarUiEstadoParihuela(parihuela: Parihuela?) {
        if (parihuela != null) {
            binding.tvEstadoParihuela.text =
                "Estado: PARIHUELA ABIERTA (${parihuela.codigoMarbete})"
            binding.tvMarbeteInfo.text =
                "Contexto bloqueado: ${parihuela.cuartel} • ${parihuela.variedad} • ${parihuela.totalJabas} jabas"
            binding.btnAccionParihuela.text = getString(R.string.cerrar_parihuela)

            binding.spnCuartel.isEnabled = false
            binding.spnClp.isEnabled = false
            binding.rbBiloxi.isEnabled = false
            binding.rbVentura.isEnabled = false
        } else {
            binding.tvEstadoParihuela.text = "Estado: PARIHUELA NO INICIADA"
            binding.tvMarbeteInfo.text =
                "Seleccione los parámetros agronómicos para habilitar el pesaje."
            binding.btnAccionParihuela.text = getString(R.string.abrir_parihuela)

            binding.spnCuartel.isEnabled = true
            binding.spnClp.isEnabled = true
            binding.rbBiloxi.isEnabled = true
            binding.rbVentura.isEnabled = true
            validarFormulario()
        }
    }
}
