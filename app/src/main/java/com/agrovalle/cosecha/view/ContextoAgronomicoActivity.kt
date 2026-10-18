package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityContextoBinding
import com.agrovalle.cosecha.viewmodel.ContextoViewModel

/**
 * Pantalla 05 - Contexto agronomico del turno (AGV-002) - SIN Room.
 *
 * Recibe el ID del jornalero por Intent (`EXTRA_JORNALERO_ID`) desde la pantalla
 * de asistencia. Al elegir un cuartel, la VARIEDAD se autocompleta desde
 * [com.agrovalle.cosecha.data.DatosMemoria.mapaCuarteles] (ya no se elige a mano).
 *
 * Al presionar "ASIGNAR Y FINALIZAR" se guarda en
 * [com.agrovalle.cosecha.data.DatosMemoria.asignaciones] que jornalero va a que
 * cuartel y se vuelve al escaneo con FLAG_ACTIVITY_CLEAR_TOP para recibir al
 * siguiente jornalero.
 */
class ContextoAgronomicoActivity : AppCompatActivity() {

    companion object {
        /** ID del jornalero enviado desde AsistenciaQRActivity. */
        const val EXTRA_JORNALERO_ID = "EXTRA_JORNALERO_ID"
    }

    private lateinit var binding: ActivityContextoBinding
    private val viewModel: ContextoViewModel by viewModels()

    /**
     * El Spinner dispara `onItemSelected` una vez al inicializarse. Mientras
     * este en `true` se ignoran esos eventos para no pisar el cuartel que se
     * recupera desde el SavedStateHandle.
     */
    private var ignorarSeleccionInicial = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContextoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarSpinnerCuartel()
        configurarListeners()
        observarEstado()

        // 1) Se recupera el jornalero enviado por el Intent.
        val jornaleroId = intent.getLongExtra(EXTRA_JORNALERO_ID, 0L)
        viewModel.cargarJornalero(jornaleroId)

        // 2) Se pinta la seleccion guardada (cuartel + variedad).
        sincronizarDesdeViewModel()

        // 3) A partir de aqui el usuario ya manda: los cambios del Spinner cuentan.
        ignorarSeleccionInicial = false
    }

    private fun configurarSpinnerCuartel() {
        binding.spnCuartel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            viewModel.cuarteles()
        )
    }

    private fun configurarListeners() {
        binding.btnVolverContexto.setOnClickListener { finish() }

        binding.spnCuartel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Se descarta el evento automatico del arranque.
                if (ignorarSeleccionInicial) return

                val cuartel = parent?.getItemAtPosition(position)?.toString() ?: return
                viewModel.seleccionarCuartel(cuartel)
                mostrarVariedad(viewModel.variedadActual())
                actualizarBotonAsignar()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (ignorarSeleccionInicial) return
                actualizarBotonAsignar()
            }
        }

        binding.btnAsignar.setOnClickListener { asignarYFinalizar() }
    }

    private fun observarEstado() {
        viewModel.jornalero.observe(this) { jornalero ->
            if (jornalero == null) {
                binding.tvEstadoAsignacion.text = "Estado: JORNALERO NO ENCONTRADO"
                binding.tvNombreJornaleroContexto.text =
                    "Vuelva a escanear el fotocheck del jornalero."
            } else {
                binding.tvEstadoAsignacion.text = "Estado: JORNALERO ESCANEADO"
                binding.tvNombreJornaleroContexto.text =
                    "${jornalero.nombreCompleto} • DNI ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo}"
            }
            actualizarBotonAsignar()
        }
        viewModel.mensaje.observe(this) { mensaje ->
            if (!mensaje.isNullOrBlank()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                viewModel.consumirMensaje()
            }
        }
    }

    /** Coloca el Spinner en el cuartel guardado en el ViewModel y pinta la variedad. */
    private fun sincronizarDesdeViewModel() {
        val cuartel = viewModel.cuartelSeleccionado
        val posicion = viewModel.cuarteles().indexOf(cuartel)
        if (posicion >= 0) {
            binding.spnCuartel.setSelection(posicion)
        }
        mostrarVariedad(viewModel.variedadActual())
        actualizarBotonAsignar()
    }

    /** Pinta la variedad autocompletada o el placeholder. */
    private fun mostrarVariedad(variedad: String?) {
        binding.tvVariedadAuto.text = variedad ?: "--"
    }

    private fun actualizarBotonAsignar() {
        binding.btnAsignar.isEnabled = viewModel.puedeAsignar()
    }

    /**
     * AGV-002: guarda la asignacion jornalero -> cuartel y vuelve al escaneo
     * con FLAG_ACTIVITY_CLEAR_TOP para recibir al siguiente jornalero.
     */
    private fun asignarYFinalizar() {
        if (!viewModel.asignarYFinalizar()) return

        val intent = Intent(this, AsistenciaQRActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}
