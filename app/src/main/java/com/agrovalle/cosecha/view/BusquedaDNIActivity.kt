package com.agrovalle.cosecha.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.databinding.ActivityBusquedaDniBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.viewmodel.AsistenciaViewModel

/**
 * Pantalla 04 - Busqueda por DNI (AGV-016, plan B de AGV-001).
 *
 * El capataz ingresa los ULTIMOS 4 DIGITOS con el selector numerico y el sistema
 * busca coincidencias en el padron EN MEMORIA (DatosMemoria). Al confirmar se
 * devuelve el jornalero para registrar el ingreso manual.
 *
 * Los digitos y el jornalero encontrado viven en el SavedStateHandle del
 * ViewModel, de modo que la busqueda sobrevive a la rotacion de pantalla.
 */
class BusquedaDNIActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusquedaDniBinding
    private val viewModel: AsistenciaViewModel by viewModels()

    companion object {
        const val EXTRA_JORNALERO = "extra_jornalero"
        const val EXTRA_NOMBRE_JORNALERO = "extra_nombre_jornalero"
        const val EXTRA_DNI_JORNALERO = "extra_dni_jornalero"

        private const val MAX_DIGITOS = 4
        private const val KEY_JORNALERO = "key_jornalero_busqueda"
    }

    private var jornaleroEncontrado: Jornalero? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusquedaDniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            jornaleroEncontrado = buscarJornaleroGuardado(savedInstanceState)
        }

        configurarTeclado()
        configurarEventos()
        observarCoincidencias()
        actualizarDisplay()

        val recuperado = jornaleroEncontrado
        if (recuperado != null) {
            mostrarResultado(recuperado)
        } else if (viewModel.ultimosDigitos.isNotEmpty()) {
            // Tras una rotacion se repite la busqueda con los digitos conservados.
            viewModel.buscarPorUltimosDigitos(viewModel.ultimosDigitos)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_JORNALERO, jornaleroEncontrado?.id ?: 0L)
    }

    /** Recupera el jornalero desde el padron en memoria usando su id. */
    private fun buscarJornaleroGuardado(state: Bundle): Jornalero? {
        val id = state.getLong(KEY_JORNALERO, 0L)
        if (id == 0L) return null
        return DatosMemoria.buscarJornaleroPorId(id)
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
            jornaleroEncontrado?.let { jornalero ->
                val resultado = Intent().apply {
                    putExtra(EXTRA_JORNALERO, jornalero)
                    putExtra(EXTRA_NOMBRE_JORNALERO, jornalero.nombreCompleto)
                    putExtra(EXTRA_DNI_JORNALERO, jornalero.dni)
                }
                setResult(Activity.RESULT_OK, resultado)
                finish()
            }
        }
    }

    private fun observarCoincidencias() {
        viewModel.coincidencias.observe(this) { lista ->
            if (viewModel.ultimosDigitos.isEmpty()) return@observe
            val primero = lista.firstOrNull()
            if (primero != null) {
                jornaleroEncontrado = primero
                mostrarResultado(primero)
            } else {
                jornaleroEncontrado = null
                mostrarResultadoVacio()
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
        jornaleroEncontrado = null
        binding.cardResultado.visibility = View.GONE
        binding.tvEstadoVacio.visibility = View.VISIBLE
        binding.tvEstadoVacio.text = "Ingrese dígitos y presione Buscar"
    }
}
