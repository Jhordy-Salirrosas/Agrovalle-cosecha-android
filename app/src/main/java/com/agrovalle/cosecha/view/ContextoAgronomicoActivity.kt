package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityContextoBinding
import com.agrovalle.cosecha.viewmodel.TurnoViewModel

class ContextoAgronomicoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CUARTEL = "extra_cuartel"
        const val EXTRA_VARIEDAD = "extra_variedad"
    }

    private lateinit var binding: ActivityContextoBinding
    private val viewModel: TurnoViewModel by viewModels()

    private var estaParihuelaAbierta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContextoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinners()
        configurarListeners()
        observarEstado()
    }

    private fun configurarSpinners() {
        val cuarteles = listOf("Seleccione Cuartel...", "Cuartel 01", "Cuartel 05", "Cuartel 12", "Cuartel 18")
        val adapterCuartel = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cuarteles)
        binding.spnCuartel.adapter = adapterCuartel

        val clps = listOf("Seleccione CLP...", "CLP-01 (Virú Central)", "CLP-02 (Virú Sur)", "CLP-03 (Chao Norte)")
        val adapterClp = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clps)
        binding.spnClp.adapter = adapterClp
    }

    private fun configurarListeners() {
        binding.btnVolverContexto.setOnClickListener {
            finish()
        }

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                validarFormulario()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                validarFormulario()
            }
        }

        binding.spnCuartel.onItemSelectedListener = itemSelectedListener
        binding.spnClp.onItemSelectedListener = itemSelectedListener
        binding.rgVariedad.setOnCheckedChangeListener { _, _ ->
            validarFormulario()
        }

        binding.btnAccionParihuela.setOnClickListener {
            if (!estaParihuelaAbierta) {
                abrirParihuela()
            } else {
                cerrarParihuela()
            }
        }
    }

    private fun observarEstado() {
        viewModel.parihuelaAbierta.observe(this) { abierta ->
            estaParihuelaAbierta = abierta
            actualizarUiEstadoParihuela(abierta)
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

        viewModel.setParihuelaContexto(cuartel, clp, variedad, abierta = true)

        // Navegar a CalibrarTaraActivity
        val intent = Intent(this, CalibrarTaraActivity::class.java).apply {
            putExtra(EXTRA_CUARTEL, cuartel)
            putExtra(EXTRA_VARIEDAD, variedad)
        }
        startActivity(intent)
    }

    private fun cerrarParihuela() {
        viewModel.setParihuelaContexto("", "", "", abierta = false)
        binding.spnCuartel.setSelection(0)
        binding.spnClp.setSelection(0)
        binding.rgVariedad.clearCheck()
        validarFormulario()
    }

    private fun actualizarUiEstadoParihuela(abierta: Boolean) {
        if (abierta) {
            binding.tvEstadoParihuela.text = "Estado: PARIHUELA ABIERTA (MARB-2024-001)"
            binding.tvMarbeteInfo.text = "Contexto bloqueado. Parihuela activa lista para recepción de jabas."
            binding.btnAccionParihuela.text = getString(R.string.cerrar_parihuela)

            // Bloquear selectores
            binding.spnCuartel.isEnabled = false
            binding.spnClp.isEnabled = false
            binding.rbBiloxi.isEnabled = false
            binding.rbVentura.isEnabled = false
        } else {
            binding.tvEstadoParihuela.text = "Estado: PARIHUELA NO INICIADA"
            binding.tvMarbeteInfo.text = "Seleccione los parámetros agronómicos para habilitar el pesaje."
            binding.btnAccionParihuela.text = getString(R.string.abrir_parihuela)

            // Habilitar selectores
            binding.spnCuartel.isEnabled = true
            binding.spnClp.isEnabled = true
            binding.rbBiloxi.isEnabled = true
            binding.rbVentura.isEnabled = true
            validarFormulario()
        }
    }
}
