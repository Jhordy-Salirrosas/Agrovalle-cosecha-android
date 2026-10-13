package com.agrovalle.cosecha.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityBusquedaDniBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.viewmodel.TurnoViewModel

class BusquedaDNIActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusquedaDniBinding
    private val viewModel: TurnoViewModel by viewModels()

    private var digitosIngresados: String = ""
    private var jornaleroEncontrado: Jornalero? = null

    companion object {
        const val EXTRA_NOMBRE_JORNALERO = "extra_nombre_jornalero"
        const val EXTRA_DNI_JORNALERO = "extra_dni_jornalero"

        private const val KEY_DIGITOS = "key_digitos"
        private const val KEY_JORNALERO = "key_jornalero"
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusquedaDniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            digitosIngresados = savedInstanceState.getString(KEY_DIGITOS) ?: ""
            jornaleroEncontrado = savedInstanceState.getSerializable(KEY_JORNALERO) as? Jornalero
        }

        configurarTeclado()
        configurarEventos()
        actualizarDisplay()

        jornaleroEncontrado?.let { mostrarResultado(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_DIGITOS, digitosIngresados)
        outState.putSerializable(KEY_JORNALERO, jornaleroEncontrado)
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
                if (digitosIngresados.length < 8) {
                    digitosIngresados += digito
                    actualizarDisplay()
                }
            }
        }

        binding.btnKeyClear.setOnClickListener {
            if (digitosIngresados.isNotEmpty()) {
                digitosIngresados = digitosIngresados.dropLast(1)
                actualizarDisplay()
            }
        }

        binding.btnKeyBuscar.setOnClickListener {
            ejecutarBusqueda()
        }
    }

    private fun configurarEventos() {
        binding.btnVolverDni.setOnClickListener {
            finish()
        }

        binding.btnConfirmarAsistencia.setOnClickListener {
            jornaleroEncontrado?.let { j ->
                val resultadoIntent = Intent().apply {
                    putExtra(EXTRA_NOMBRE_JORNALERO, j.nombreCompleto)
                    putExtra(EXTRA_DNI_JORNALERO, j.dni)
                }
                setResult(Activity.RESULT_OK, resultadoIntent)
                finish()
            }
        }
    }

    private fun actualizarDisplay() {
        if (digitosIngresados.isEmpty()) {
            binding.tvDisplayDni.text = "____"
            binding.btnKeyBuscar.isEnabled = false
        } else {
            binding.tvDisplayDni.text = digitosIngresados
            binding.btnKeyBuscar.isEnabled = true
        }
    }

    private fun ejecutarBusqueda() {
        if (digitosIngresados.isEmpty()) return

        val resultados = viewModel.buscarJornaleroPorUltimosDigitosDni(digitosIngresados)
        if (resultados.isNotEmpty()) {
            val primero = resultados.first()
            jornaleroEncontrado = primero
            mostrarResultado(primero)
        } else {
            jornaleroEncontrado = null
            mostrarResultadoVacio()
        }
    }

    private fun mostrarResultado(jornalero: Jornalero) {
        binding.tvNombreJornalero.text = jornalero.nombreCompleto
        binding.tvDniCompleto.text = "DNI: ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo}"
        binding.cardResultado.visibility = View.VISIBLE
        binding.tvEstadoVacio.visibility = View.GONE
    }

    private fun mostrarResultadoVacio() {
        binding.cardResultado.visibility = View.GONE
        binding.tvEstadoVacio.visibility = View.VISIBLE
        binding.tvEstadoVacio.text = "No se encontró ningún jornalero con DNI que coincida con '$digitosIngresados'"
    }
}
