package com.agrovalle.cosecha.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.repository.JornaleroAsistenciaOpcion
import com.agrovalle.cosecha.data.repository.SelectorJornalerosAsistencia
import com.agrovalle.cosecha.databinding.ActivityParihuelaBinding
import com.agrovalle.cosecha.util.NotificacionTiempoUtil
import com.agrovalle.cosecha.viewmodel.ParihuelaUiState
import com.agrovalle.cosecha.viewmodel.ParihuelaViewModel
import com.agrovalle.cosecha.worker.TiempoEsperaWorker
import kotlinx.coroutines.launch

/**
 * Pantalla principal de gestión de parihuela (Sprint 2).
 */
class ParihuelaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParihuelaBinding
    private val viewModel: ParihuelaViewModel by viewModels()
    private val permisoNotificacionesLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private var ultimasEtiquetasSpinner: List<String> = emptyList()
    private var opcionesActuales: List<JornaleroAsistenciaOpcion> = emptyList()

    companion object {
        private const val PROMPT_SELECCIONE_JORNALERO = "Seleccione un jornalero..."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParihuelaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificacionTiempoUtil.crearCanal(this)
        solicitarPermisoNotificacionesSiAplica()
        TiempoEsperaWorker.programarRevisionPeriodica(this)

        configurarEventos()
        observarEstado()
    }

    override fun onResume() {
        super.onResume()
        viewModel.actualizarListaJornaleros()
    }

    private fun solicitarPermisoNotificacionesSiAplica() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val estado = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (estado != PackageManager.PERMISSION_GRANTED) {
                permisoNotificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun configurarEventos() {
        binding.btnAbrirParihuela.setOnClickListener {
            mostrarDialogoAbrirParihuela()
        }

        binding.btnSelladoAnticipado.setOnClickListener {
            mostrarDialogoSelladoAnticipado()
        }

        binding.btnActualizarListaJornaleros.setOnClickListener {
            viewModel.actualizarListaJornaleros()
        }

        binding.btnActualizarListaConDatos.setOnClickListener {
            viewModel.actualizarListaJornaleros()
        }

        binding.spnJornalero.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val elegido = if (position <= 0) {
                    null
                } else {
                    opcionesActuales.getOrNull(position - 1)?.nombreCompleto
                }
                viewModel.seleccionarJornalero(elegido)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.seleccionarJornalero(null)
            }
        }

        binding.btnRegistrarPesajeParihuela.setOnClickListener {
            val cantidad = binding.etCantidadJabas.text?.toString()?.toIntOrNull() ?: 0
            val variedad = binding.etVariedadPesaje.text?.toString().orEmpty()
            val cuartel = binding.etCuartelPesaje.text?.toString().orEmpty()

            viewModel.registrarPesaje(
                cantidadJabas = cantidad,
                variedad = variedad,
                cuartel = cuartel
            )
        }
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderizar(state)
                }
            }
        }
    }

    private fun renderizar(state: ParihuelaUiState) {
        renderizarSelectorJornalero(state)

        val tieneJornaleroValido = SelectorJornalerosAsistencia.puedeRegistrarPesaje(
            state.jornalerosDisponibles,
            state.jornaleroSeleccionado
        )

        val abierta = state.parihuelaAbierta
        val avance = state.avance

        if (abierta == null) {
            binding.tvCodigoParihuela.text = "Sin parihuela abierta"
            binding.tvDetalleParihuela.text = "Abra una parihuela para comenzar el registro."
            binding.tvConteoJabas.text = "0 / 120 jabas"
            binding.progressParihuela.progress = 0
            binding.tvFaltantes.text = "Faltan 100 para 100 | 120 para 120 (0%)"
            binding.btnAbrirParihuela.visibility = View.VISIBLE
            binding.btnSelladoAnticipado.visibility = View.GONE
            binding.btnRegistrarPesajeParihuela.isEnabled = tieneJornaleroValido
            binding.btnRegistrarPesajeParihuela.text = "Registrar pesaje"
        } else {
            val retenida = abierta.estado == EstadoParihuela.RETENIDA_CALIDAD
            binding.tvCodigoParihuela.text = if (retenida) {
                "Parihuela: ${abierta.codigo} [RETENIDA POR CALIDAD]"
            } else {
                "Parihuela: ${abierta.codigo}"
            }
            binding.tvDetalleParihuela.text = "Variedad: ${abierta.variedad} • Cuartel: ${abierta.cuartel}"
            val jabas = avance?.jabasActuales ?: abierta.conteoJabas
            val f100 = avance?.faltanPara100 ?: (100 - jabas).coerceAtLeast(0)
            val f120 = avance?.faltanPara120 ?: (120 - jabas).coerceAtLeast(0)
            val pct = avance?.porcentajeHacia120 ?: ((jabas * 100) / 120).coerceIn(0, 100)
            binding.tvConteoJabas.text = "$jabas / 120 jabas"
            binding.progressParihuela.progress = pct
            if (retenida) {
                binding.tvFaltantes.text =
                    "Parihuela retenida por calidad — libérela desde Incidencias antes de continuar o sellar."
                binding.btnRegistrarPesajeParihuela.isEnabled = false
                binding.btnRegistrarPesajeParihuela.text = "Retenida por calidad"
                binding.btnSelladoAnticipado.isEnabled = false
                binding.btnSelladoAnticipado.text = "Sellado bloqueado (Retenida por Calidad)"
            } else if (f120 < 3) {
                binding.tvFaltantes.text =
                    "Faltan $f120 para 120 ($pct%) — Menos de 3 jabas restantes: proceda a sellar la parihuela."
                binding.btnRegistrarPesajeParihuela.isEnabled = false
                binding.btnRegistrarPesajeParihuela.text = "Proceda a sellar la parihuela"
                binding.btnSelladoAnticipado.isEnabled = true
                binding.btnSelladoAnticipado.text = "Sellado anticipado"
            } else {
                binding.tvFaltantes.text = "Faltan $f100 para 100 | $f120 para 120 ($pct%)"
                binding.btnRegistrarPesajeParihuela.isEnabled = tieneJornaleroValido
                binding.btnRegistrarPesajeParihuela.text = "Registrar pesaje"
                binding.btnSelladoAnticipado.isEnabled = true
                binding.btnSelladoAnticipado.text = "Sellado anticipado"
            }
            binding.btnAbrirParihuela.visibility = View.GONE
            binding.btnSelladoAnticipado.visibility = View.VISIBLE
        }

        if (state.mensaje != null) {
            binding.tvMensajeEstado.visibility = View.VISIBLE
            binding.tvMensajeEstado.text = state.mensaje
            val colorRes = if (state.esError) android.R.color.holo_red_dark else R.color.verde_agricola
            binding.tvMensajeEstado.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            binding.tvMensajeEstado.visibility = View.GONE
        }
    }

    private fun renderizarSelectorJornalero(state: ParihuelaUiState) {
        val disponibles = state.jornalerosDisponibles
        if (disponibles.isEmpty()) {
            binding.layoutSelectorJornalero.visibility = View.GONE
            binding.layoutSinAsistencias.visibility = View.VISIBLE
            opcionesActuales = emptyList()
            ultimasEtiquetasSpinner = emptyList()
            return
        }

        binding.layoutSelectorJornalero.visibility = View.VISIBLE
        binding.layoutSinAsistencias.visibility = View.GONE

        val nuevasEtiquetas = listOf(PROMPT_SELECCIONE_JORNALERO) + disponibles.map { it.etiqueta }
        if (nuevasEtiquetas != ultimasEtiquetasSpinner) {
            opcionesActuales = disponibles
            ultimasEtiquetasSpinner = nuevasEtiquetas
            binding.spnJornalero.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                nuevasEtiquetas
            )
            val idx = disponibles.indexOfFirst {
                it.nombreCompleto.equals(state.jornaleroSeleccionado, ignoreCase = true)
            }
            binding.spnJornalero.setSelection(if (idx >= 0) idx + 1 else 0, false)
        }
    }

    private fun mostrarDialogoAbrirParihuela() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val etVariedad = EditText(this).apply { hint = "Variedad (obligatorio)" }
        val etCuartel = EditText(this).apply { hint = "Cuartel (obligatorio)" }
        layout.addView(etVariedad)
        layout.addView(etCuartel)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Abrir nueva parihuela")
            .setView(layout)
            .setPositiveButton("Abrir", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val v = etVariedad.text?.toString()?.trim().orEmpty()
                val c = etCuartel.text?.toString()?.trim().orEmpty()
                if (v.isEmpty() || c.isEmpty()) {
                    if (v.isEmpty()) etVariedad.error = "La variedad es obligatoria"
                    if (c.isEmpty()) etCuartel.error = "El cuartel es obligatorio"
                    viewModel.abrirNuevaParihuela(v, c)
                } else {
                    viewModel.abrirNuevaParihuela(v, c)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun mostrarDialogoSelladoAnticipado() {
        val etMotivo = EditText(this).apply {
            hint = "Motivo obligatorio del sellado anticipado"
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(etMotivo)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar sellado anticipado")
            .setMessage("Ingrese el motivo para sellar la parihuela antes de las 120 jabas.")
            .setView(container)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val motivo = etMotivo.text.toString().trim()
                if (motivo.isEmpty()) {
                    etMotivo.error = "El motivo es obligatorio"
                } else {
                    viewModel.sellarAnticipadamente(motivo)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}
