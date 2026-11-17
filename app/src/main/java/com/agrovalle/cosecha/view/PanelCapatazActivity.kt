package com.agrovalle.cosecha.view

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.data.repository.EstadoTiempoEspera
import com.agrovalle.cosecha.databinding.ActivityPanelCapatazBinding
import com.agrovalle.cosecha.viewmodel.PanelCapatazUiState
import com.agrovalle.cosecha.viewmodel.PanelCapatazViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla del panel del capataz (Sprint 2).
 */
class PanelCapatazActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelCapatazBinding
    private val viewModel: PanelCapatazViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanelCapatazBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderizar(state)
                }
            }
        }
    }

    private fun renderizar(state: PanelCapatazUiState) {
        val resumen = state.resumen
        binding.tvTotalesPanel.text = buildString {
            appendLine("Abiertas: ${resumen.totalAbiertas}")
            appendLine("Retenidas por Calidad: ${resumen.totalRetenidasCalidad}")
            appendLine("Selladas: ${resumen.totalSelladas}")
            appendLine("Selladas anticipadas: ${resumen.totalSelladasAnticipadas}")
            append("Total jabas: ${resumen.totalJabas}")
        }

        val porVariedad = if (resumen.jabasPorVariedad.isEmpty()) {
            "Sin datos"
        } else {
            resumen.jabasPorVariedad.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        val porCuartel = if (resumen.jabasPorCuartel.isEmpty()) {
            "Sin datos"
        } else {
            resumen.jabasPorCuartel.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        binding.tvAgrupacionesPanel.text = "Variedad: $porVariedad\nCuartel: $porCuartel"

        binding.tvRendimientoPanel.text = if (state.rendimientoPorJornalero.isEmpty()) {
            "Sin pesajes registrados."
        } else {
            state.rendimientoPorJornalero.entries.joinToString("\n") { "${it.key}: ${it.value} jabas" }
        }

        binding.containerAbiertasUrgencia.removeAllViews()
        resumen.retenidasCalidad.forEach { retenida ->
            val tvRetenida = TextView(this).apply {
                text = "${retenida.codigo} • ${retenida.variedad} (${retenida.cuartel})\n" +
                    "Jabas: ${retenida.conteoJabas}/120 • Estado: RETENIDA POR CALIDAD (No se puede sellar)"
                setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                textSize = 14f
                setPadding(24, 18, 24, 18)
                setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            }
            binding.containerAbiertasUrgencia.addView(tvRetenida)
        }
        if (resumen.abiertasPorUrgencia.isEmpty() && resumen.retenidasCalidad.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No hay parihuelas abiertas."
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 14f
            }
            binding.containerAbiertasUrgencia.addView(tv)
        } else {
            resumen.abiertasPorUrgencia.forEach { item ->
                val minutosRestantes = item.tiempoEspera.restanteMs / 60_000L
                val estadoTiempo = item.tiempoEspera.estado
                val esUrgente = estadoTiempo == EstadoTiempoEspera.POR_VENCER ||
                    estadoTiempo == EstadoTiempoEspera.EXCEDIDA

                val restanteTxt = if (item.tiempoEspera.cronometroIniciado) {
                    "Restante: $minutosRestantes min"
                } else {
                    "Cronómetro: sin iniciar ($minutosRestantes min al 1er pesaje)"
                }

                val tv = TextView(this).apply {
                    text = "${item.parihuela.codigo} • ${item.parihuela.variedad} (${item.parihuela.cuartel})\n" +
                        "Jabas: ${item.parihuela.conteoJabas}/120 • $restanteTxt • Estado: $estadoTiempo"
                    setTextColor(
                        ContextCompat.getColor(
                            context,
                            if (esUrgente) android.R.color.holo_red_dark else R.color.text_primary
                        )
                    )
                    textSize = 14f
                    setPadding(24, 18, 24, 18)
                    setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                }
                binding.containerAbiertasUrgencia.addView(tv)
            }
        }
    }
}
