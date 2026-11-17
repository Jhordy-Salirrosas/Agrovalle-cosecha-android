package com.agrovalle.cosecha.view

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityCierreTurnoBinding
import com.agrovalle.cosecha.viewmodel.CierreTurnoUiState
import com.agrovalle.cosecha.viewmodel.CierreTurnoViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Reporte de ingresos manuales por DNI al cierre de turno (AGV-016 - Escenario 2.0).
 * Opera exclusivamente con datos locales en memoria.
 */
class CierreTurnoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCierreTurnoBinding
    private val viewModel: CierreTurnoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCierreTurnoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverCierreTurno.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderizar(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarReporte()
    }

    private fun renderizar(state: CierreTurnoUiState) {
        binding.tvEncabezadoCierreTurno.text =
            "Fecha: ${state.fechaJornada} • ${state.jornada} • ${state.fundo}\n" +
                "Responsable en sesión: ${state.capataz}"

        binding.containerResumenCuadrillas.removeAllViews()
        if (state.resumenPorCuadrilla.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "Sin cuadrillas registradas."
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 14f
            }
            binding.containerResumenCuadrillas.addView(tvVacio)
        } else {
            state.resumenPorCuadrilla.forEach { resumen ->
                val pctTexto = String.format(Locale.US, "%.2f%%", resumen.porcentajeIngresosManuales)
                val tvCuadrilla = TextView(this).apply {
                    text = buildString {
                        appendLine("Cuadrilla: ${resumen.cuadrillaCodigo}")
                        appendLine("Total de asistencias del día: ${resumen.totalAsistencias}")
                        appendLine("Ingresos manuales (DNI): ${resumen.cantidadIngresosManuales}")
                        append("Porcentaje de ingresos manuales: $pctTexto")
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 14f
                    setPadding(0, 8, 0, 12)
                }
                binding.containerResumenCuadrillas.addView(tvCuadrilla)
            }
        }

        binding.containerBitacoraManual.removeAllViews()
        if (state.bitacoraIngresosManuales.isEmpty()) {
            binding.tvSinIngresosManuales.visibility = View.VISIBLE
        } else {
            binding.tvSinIngresosManuales.visibility = View.GONE
            state.bitacoraIngresosManuales.forEach { item ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 18, 24, 18)
                    setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    layoutParams = params
                }
                val tv = TextView(this).apply {
                    text = buildString {
                        appendLine("Hora: ${item.hora} • Cuadrilla ${item.cuadrillaCodigo}")
                        appendLine("Jornalero: ${item.jornaleroNombre} (DNI ${item.dni})")
                        appendLine("Motivo: ${item.motivo}")
                        append("Capataz: ${item.capatazResponsable}")
                    }
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 14f
                }
                card.addView(tv)
                binding.containerBitacoraManual.addView(card)
            }
        }
    }
}
