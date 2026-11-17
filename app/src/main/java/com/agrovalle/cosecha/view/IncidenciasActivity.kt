package com.agrovalle.cosecha.view

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.data.local.entity.EstadoIncidencia
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.databinding.ActivityIncidenciasBinding
import com.agrovalle.cosecha.viewmodel.IncidenciasUiState
import com.agrovalle.cosecha.viewmodel.IncidenciasViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla de listado y gestión de incidencias de mezcla (Sprint 2 - AGV-007).
 */
class IncidenciasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidenciasBinding
    private val viewModel: IncidenciasViewModel by viewModels()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidenciasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderizar(state)
                }
            }
        }
    }

    private fun renderizar(state: IncidenciasUiState) {
        val revisadas = state.incidencias.count { it.estado == EstadoIncidencia.REVISADA }
        binding.tvContadorPendientes.text =
            "Pendientes: ${state.conteoPendientes} | Revisadas: $revisadas"

        if (state.mensaje != null) {
            binding.tvMensajeIncidencias.visibility = View.VISIBLE
            binding.tvMensajeIncidencias.text = state.mensaje
            val colorRes = if (state.esError) android.R.color.holo_red_dark else R.color.verde_agricola
            binding.tvMensajeIncidencias.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            binding.tvMensajeIncidencias.visibility = View.GONE
        }

        binding.containerRetenidasCalidad.removeAllViews()
        if (state.parihuelasRetenidas.isEmpty()) {
            binding.containerRetenidasCalidad.visibility = View.GONE
        } else {
            binding.containerRetenidasCalidad.visibility = View.VISIBLE
            state.parihuelasRetenidas.forEach { retenida ->
                val banner = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 18, 24, 18)
                    setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    layoutParams = params
                }
                val tvRet = TextView(this).apply {
                    text = "RETENIDA POR CALIDAD: ${retenida.codigo}\n" +
                        "${retenida.variedad} • ${retenida.cuartel} • ${retenida.conteoJabas}/120 jabas (no se puede sellar)"
                    setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                    textSize = 14f
                }
                val btnLiberar = MaterialButton(this).apply {
                    text = "Liberar parihuela (${ParihuelaRepository.ACCION_LIBERAR})"
                    setOnClickListener {
                        mostrarDialogoLiberarParihuela(retenida.id)
                    }
                }
                banner.addView(tvRet)
                banner.addView(btnLiberar)
                binding.containerRetenidasCalidad.addView(banner)
            }
        }

        binding.containerIncidencias.removeAllViews()
        binding.tvSinIncidencias.visibility =
            if (state.incidencias.isEmpty()) View.VISIBLE else View.GONE

        state.incidencias.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                layoutParams = params
            }

            val esPendiente = item.estado == EstadoIncidencia.PENDIENTE
            val estaRetenida = item.parihuelaId in state.parihuelasRetenidasIds
            val estadoEtiqueta = if (esPendiente) "Pendiente" else "Revisada"

            val tv = TextView(this).apply {
                text = buildString {
                    appendLine(
                        "Parihuela #${item.parihuelaId} • ${item.tipo} • [$estadoEtiqueta]" +
                            if (estaRetenida) " • RETENIDA POR CALIDAD" else ""
                    )
                    appendLine(item.detalle)
                    appendLine("Detectada: ${formatoFecha.format(item.fechaHora)}")
                    if (!esPendiente) {
                        appendLine("Acción: ${item.accionTomada ?: "-"}")
                        appendLine("Responsable: ${item.responsable ?: "-"}")
                        append(
                            "Hora revisión: ${item.horaRevision?.let { formatoFecha.format(it) } ?: "-"}"
                        )
                    }
                }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (esPendiente || estaRetenida) android.R.color.holo_red_dark else R.color.text_primary
                    )
                )
                textSize = 14f
            }
            card.addView(tv)

            if (esPendiente) {
                val btnResolver = MaterialButton(this).apply {
                    text = "Elegir acción y confirmar"
                    setOnClickListener {
                        mostrarDialogoResolverIncidencia(item)
                    }
                }
                card.addView(btnResolver)
            }

            binding.containerIncidencias.addView(card)
        }
    }

    private fun mostrarDialogoResolverIncidencia(incidencia: IncidenciaEntity) {
        val opciones = ParihuelaRepository.ACCIONES_INCIDENCIA.toTypedArray()
        var indiceElegido = 0

        AlertDialog.Builder(this)
            .setTitle("Resolver incidencia #${incidencia.id}")
            .setSingleChoiceItems(opciones, 0) { _, which ->
                indiceElegido = which
            }
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.resolverIncidencia(incidencia.id, opciones[indiceElegido])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoLiberarParihuela(parihuelaId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Liberar parihuela #$parihuelaId")
            .setMessage("¿Confirmar la acción '${ParihuelaRepository.ACCION_LIBERAR}' para devolver la parihuela al estado ABIERTA?")
            .setPositiveButton("Liberar") { _, _ ->
                viewModel.liberarParihuela(parihuelaId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
