package com.agrovalle.cosecha.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityBusquedaDniBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.AsistenciaViewModel

/**
 * Pantalla 04 - Busqueda por DNI (AGV-016, plan B de AGV-001).
 *
 * El capataz ingresa los ULTIMOS 4 DIGITOS con el selector numerico, elige el
 * motivo del ingreso manual y confirma la asistencia.
 */
class BusquedaDNIActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusquedaDniBinding
    private val viewModel: AsistenciaViewModel by viewModels()
    private var ignorarSeleccionInicial = true

    companion object {
        const val EXTRA_JORNALERO = "extra_jornalero"
        const val EXTRA_NOMBRE_JORNALERO = "extra_nombre_jornalero"
        const val EXTRA_DNI_JORNALERO = "extra_dni_jornalero"
        const val EXTRA_MOTIVO_MANUAL = "extra_motivo_manual"

        private const val MAX_DIGITOS = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusquedaDniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSelectorMotivo()
        configurarTeclado()
        configurarEventos()
        observarEstado()
        actualizarDisplay()

        val recuperado = viewModel.jornaleroEncontrado
        if (recuperado != null) {
            mostrarResultado(recuperado)
        } else if (viewModel.ultimosDigitos.isNotEmpty()) {
            // Tras una rotacion se repite la busqueda con los digitos conservados.
            viewModel.buscarPorUltimosDigitos(viewModel.ultimosDigitos)
        }
    }

    private fun configurarSelectorMotivo() {
        val opciones = listOf(AsistenciaViewModel.MOTIVO_MANUAL_PROMPT) +
            AsistenciaViewModel.MOTIVOS_INGRESO_MANUAL
        binding.spnMotivoManual.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opciones
        )
        val idxGuardado = opciones.indexOf(viewModel.motivoManual)
        if (idxGuardado >= 0) {
            binding.spnMotivoManual.setSelection(idxGuardado, false)
        }
        ignorarSeleccionInicial = false

        binding.spnMotivoManual.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (ignorarSeleccionInicial) return
                val elegido = opciones.getOrNull(position).orEmpty()
                viewModel.motivoManual = if (position == 0) "" else elegido
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (!ignorarSeleccionInicial) viewModel.motivoManual = ""
            }
        }
    }

    private fun configurarTeclado() {
        val botonesNumericos = listOf(
            binding.btnKey0 to "0",
            binding.btnKey1 to "1",
            binding.btnKey2 to "2",
            binding.btnKey3 to "3",
            binding.btnKey4 to "4",
            binding.btnKey5 to "5",
            binding.btnKey6 to "6",
            binding.btnKey7 to "7",
            binding.btnKey8 to "8",
            binding.btnKey9 to "9"
        )

        for ((boton, digito) in botonesNumericos) {
            boton.setOnClickListener {
                val actuales = viewModel.ultimosDigitos
                if (actuales.length < MAX_DIGITOS) {
                    viewModel.ultimosDigitos = actuales + digito
                    actualizarDisplay()
                }
            }
        }

        binding.btnKeyClear.setOnClickListener {
            val actuales = viewModel.ultimosDigitos
            if (actuales.isNotEmpty()) {
                viewModel.ultimosDigitos = actuales.dropLast(1)
                if (viewModel.ultimosDigitos.isEmpty()) limpiarResultado()
                actualizarDisplay()
            }
        }

        binding.btnKeyBuscar.setOnClickListener { ejecutarBusqueda() }
    }

    private fun configurarEventos() {
        binding.btnVolverDni.setOnClickListener { finish() }

        binding.btnConfirmarAsistencia.setOnClickListener {
            val jornalero = viewModel.jornaleroEncontrado ?: return@setOnClickListener
            val motivo = viewModel.motivoManual.trim()
            if (motivo.isEmpty() || motivo == AsistenciaViewModel.MOTIVO_MANUAL_PROMPT) {
                Toast.makeText(
                    this,
                    "Seleccione el motivo del ingreso manual",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (callingActivity != null) {
                val resultado = Intent().apply {
                    putExtra(EXTRA_JORNALERO, jornalero)
                    putExtra(EXTRA_NOMBRE_JORNALERO, jornalero.nombreCompleto)
                    putExtra(EXTRA_DNI_JORNALERO, jornalero.dni)
                    putExtra(EXTRA_MOTIVO_MANUAL, motivo)
                }
                setResult(Activity.RESULT_OK, resultado)
                finish()
            } else {
                // Acceso directo desde MenuActivity: registra la asistencia manual + bitácora aquí
                viewModel.registrarManual(jornalero, motivo)
            }
        }
    }

    private fun observarEstado() {
        viewModel.coincidencias.observe(this) { lista ->
            if (viewModel.ultimosDigitos.isEmpty()) return@observe
            val primero = lista.firstOrNull()
            if (primero != null) {
                viewModel.jornaleroEncontrado = primero
                mostrarResultado(primero)
            } else {
                viewModel.jornaleroEncontrado = null
                mostrarResultadoVacio()
            }
        }

        viewModel.resultado.observe(this) { res ->
            if (res == null) return@observe
            if (res.exitoso && res.jornalero != null) {
                FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)
                Toast.makeText(this, "Asistencia registrada", Toast.LENGTH_SHORT).show()
                viewModel.consumirResultado()
                val intent = Intent(this, ContextoAgronomicoActivity::class.java).apply {
                    putExtra(ContextoAgronomicoActivity.EXTRA_JORNALERO_ID, res.jornalero.id)
                }
                startActivity(intent)
                finish()
            } else {
                FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
                Toast.makeText(this, res.mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirResultado()
            }
        }
    }

    private fun actualizarDisplay() {
        val digitos = viewModel.ultimosDigitos
        if (digitos.isEmpty()) {
            binding.tvDisplayDni.text = "____"
            binding.btnKeyBuscar.isEnabled = false
        } else {
            binding.tvDisplayDni.text = digitos
            binding.btnKeyBuscar.isEnabled = true
        }
    }

    private fun ejecutarBusqueda() {
        val digitos = viewModel.ultimosDigitos
        if (digitos.isEmpty()) return
        viewModel.buscarPorUltimosDigitos(digitos)
    }

    private fun mostrarResultado(jornalero: Jornalero) {
        binding.tvNombreJornalero.text = jornalero.nombreCompleto
        binding.tvDniCompleto.text =
            "DNI: ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo}"
        binding.cardResultado.visibility = View.VISIBLE
        binding.tvEstadoVacio.visibility = View.GONE
    }

    private fun mostrarResultadoVacio() {
        binding.cardResultado.visibility = View.GONE
        binding.tvEstadoVacio.visibility = View.VISIBLE
        binding.tvEstadoVacio.text =
            "No se encontró ningún jornalero con DNI que termine en '${viewModel.ultimosDigitos}'"
    }

    private fun limpiarResultado() {
        viewModel.jornaleroEncontrado = null
        binding.cardResultado.visibility = View.GONE
        binding.tvEstadoVacio.visibility = View.VISIBLE
        binding.tvEstadoVacio.text = "Ingrese dígitos y presione Buscar"
    }
}
