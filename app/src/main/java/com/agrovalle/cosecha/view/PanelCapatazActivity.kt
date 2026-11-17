package com.agrovalle.cosecha.view

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.data.repository.EstadoTiempoEspera
import com.agrovalle.cosecha.data.repository.JornaleroCuartelResumen
import com.agrovalle.cosecha.data.repository.ParihuelaRepository
import com.agrovalle.cosecha.databinding.ActivityPanelCapatazBinding
import com.agrovalle.cosecha.viewmodel.PanelCapatazUiState
import com.agrovalle.cosecha.viewmodel.PanelCapatazViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla del panel del capataz (Sprint 2 - AGV-005).
 */
class PanelCapatazActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanelCapatazBinding
    private val viewModel: PanelCapatazViewModel by viewModels()
    private val formatoHora = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    private val formatoSoloHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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

    override fun onResume() {
        super.onResume()
        viewModel.refrescar()
    }

    private fun formatearKg(kg: Double): String = String.format(Locale.US, "%.2f", kg)

    private fun renderizar(state: PanelCapatazUiState) {
        if (state.mensaje != null) {
            binding.tvMensajePanel.visibility = View.VISIBLE
            binding.tvMensajePanel.text = state.mensaje
            val colorRes = if (state.esError) android.R.color.holo_red_dark else R.color.verde_agricola
            binding.tvMensajePanel.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            binding.tvMensajePanel.visibility = View.GONE
        }

        val resumen = state.resumen
        binding.tvTotalesPanel.text = buildString {
            appendLine("Abiertas: ${resumen.totalAbiertas}")
            appendLine("Retenidas por Calidad: ${resumen.totalRetenidasCalidad}")
            appendLine("Selladas: ${resumen.totalSelladas}")
            appendLine("Selladas anticipadas: ${resumen.totalSelladasAnticipadas}")
            appendLine("Total jabas: ${resumen.totalJabas}")
            appendLine("Total kilos netos: ${formatearKg(resumen.totalKilosNetos)} kg")
            append("Jornaleros activos: ${resumen.totalJornalerosActivos}")
        }

        val porVariedad = if (resumen.jabasPorVariedad.isEmpty()) {
            "Sin datos"
        } else {
            resumen.jabasPorVariedad.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        }
        val porCuartel = if (resumen.jabasPorCuartel.isEmpty()) {
            "Sin datos"
        } else {
            resumen.jabasPorCuartel.entries.joinToString(", ") { (cuartel, jabas) ->
                val kg = resumen.kilosPorCuartel[cuartel] ?: 0.0
                "$cuartel: $jabas jabas (${formatearKg(kg)} kg)"
            }
        }
        binding.tvAgrupacionesPanel.text = "Variedad: $porVariedad\nCuartel: $porCuartel"

        binding.tvRendimientoPanel.text = if (resumen.rendimientoDetallado.isEmpty()) {
            "Sin pesajes registrados."
        } else {
            resumen.rendimientoDetallado.joinToString("\n") { r ->
                val horaTxt = formatoSoloHora.format(r.horaPrimerPesaje)
                val tasaTxt = r.tasaKgPorHora?.let { "${formatearKg(it)} kg/h" }
                    ?: "Tiempo insuficiente"
                val alertaTxt = if (r.bajoRendimiento) " [Bajo rendimiento]" else ""
                "• ${r.jornalero}: ${formatearKg(r.totalKilos)} kg • ${r.totalJabas} jabas • " +
                    "1er pesaje: $horaTxt • $tasaTxt$alertaTxt"
            }
        }

        renderizarJornalerosPorCuartel(state)

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

    private fun renderizarJornalerosPorCuartel(state: PanelCapatazUiState) {
        binding.containerJornalerosCuartel.removeAllViews()
        val agrupados = state.resumen.jornalerosPorCuartel
        if (agrupados.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "Sin jornaleros asignados o con pesajes todavía."
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 14f
            }
            binding.containerJornalerosCuartel.addView(tvVacio)
        } else {
            agrupados.forEach { (cuartel, jornaleros) ->
                val kilosCuartel = jornaleros.sumOf { it.kilosNetos }
                val tvCuartelHeader = TextView(this).apply {
                    text = "$cuartel (${jornaleros.size} activos • ${formatearKg(kilosCuartel)} kg)"
                    setTextColor(ContextCompat.getColor(context, R.color.verde_agricola))
                    textSize = 14f
                    setPadding(0, 12, 0, 4)
                    setTypeface(null, Typeface.BOLD)
                }
                binding.containerJornalerosCuartel.addView(tvCuartelHeader)

                jornaleros.forEach { item ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(8, 8, 8, 8)
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val colInfo = LinearLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        orientation = LinearLayout.VERTICAL
                    }
                    val primerTxt = item.horaPrimerPesaje?.let {
                        "1er pesaje: ${formatoSoloHora.format(it)}"
                    } ?: "Sin pesajes"
                    val tasaTxt = item.tasaKgPorHora?.let { "${formatearKg(it)} kg/h" }
                        ?: if (item.horaPrimerPesaje == null) "-" else "Tiempo insuficiente"
                    val tvInfo = TextView(this).apply {
                        text = "${item.nombre}\n${formatearKg(item.kilosNetos)} kg • " +
                            "${item.jabasAcumuladas} jabas • $primerTxt • $tasaTxt"
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        textSize = 13f
                    }
                    colInfo.addView(tvInfo)

                    if (item.bajoRendimiento) {
                        val tvBajoRendimiento = TextView(this).apply {
                            text = "Bajo rendimiento (< ${ParihuelaRepository.UMBRAL_BAJO_RENDIMIENTO_KG_HORA} kg/h)"
                            setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                            textSize = 12f
                            setTypeface(null, Typeface.BOLD)
                        }
                        colInfo.addView(tvBajoRendimiento)
                    }
                    row.addView(colInfo)

                    if (item.bajoRendimiento) {
                        val btnReasignar = MaterialButton(this).apply {
                            text = "Reasignar"
                            setOnClickListener {
                                mostrarDialogoReasignar(item, state.cuartelesDisponibles)
                            }
                        }
                        row.addView(btnReasignar)
                    }
                    binding.containerJornalerosCuartel.addView(row)
                }
            }
        }

        if (state.reasignaciones.isEmpty()) {
            binding.tvHistorialReasignaciones.visibility = View.GONE
        } else {
            binding.tvHistorialReasignaciones.visibility = View.VISIBLE
            binding.tvHistorialReasignaciones.text = buildString {
                appendLine("Últimas reasignaciones:")
                state.reasignaciones.take(5).forEach { r ->
                    val syncTxt = if (r.sincronizado) "Sincronizado" else "Pendiente sync"
                    appendLine(
                        "• ${r.jornalero}: ${r.cuartelOrigen} → ${r.cuartelDestino} • " +
                            "${r.motivo} (${formatoHora.format(r.hora)}) [$syncTxt]"
                    )
                }
            }.trimEnd()
        }
    }

    private fun mostrarDialogoReasignar(
        item: JornaleroCuartelResumen,
        cuartelesDisponibles: List<String>
    ) {
        val destinos = cuartelesDisponibles.filter {
            !it.equals(item.cuartelActual, ignoreCase = true)
        }
        if (destinos.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sin otros cuarteles")
                .setMessage("No hay otro cuartel disponible distinto a ${item.cuartelActual}.")
                .setPositiveButton("Aceptar", null)
                .show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val tvOrigen = TextView(this).apply {
            text = "Cuartel origen: ${item.cuartelActual}"
            textSize = 14f
        }
        val tvLblDestino = TextView(this).apply {
            text = "Cuartel destino:"
            setPadding(0, 16, 0, 4)
            textSize = 14f
        }
        val spnDestino = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@PanelCapatazActivity,
                android.R.layout.simple_spinner_dropdown_item,
                destinos
            )
        }
        val tvLblMotivo = TextView(this).apply {
            text = "Motivo de reasignación (1 toque):"
            setPadding(0, 16, 0, 4)
            textSize = 14f
        }
        val radioGroup = RadioGroup(this)
        ParihuelaRepository.MOTIVOS_REASIGNACION.forEachIndexed { idx, motivo ->
            val rb = RadioButton(this).apply {
                id = idx + 1
                text = motivo
            }
            radioGroup.addView(rb)
        }

        container.addView(tvOrigen)
        container.addView(tvLblDestino)
        container.addView(spnDestino)
        container.addView(tvLblMotivo)
        container.addView(radioGroup)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reasignar a ${item.nombre}")
            .setView(container)
            .setPositiveButton("Confirmar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val selectedId = radioGroup.checkedRadioButtonId
                if (selectedId <= 0) {
                    val rb = radioGroup.findViewById<RadioButton>(1)
                    rb?.error = "Elija un motivo"
                    return@setOnClickListener
                }
                val motivo = ParihuelaRepository.MOTIVOS_REASIGNACION[selectedId - 1]
                val destino = spnDestino.selectedItem?.toString().orEmpty()
                viewModel.reasignarJornalero(
                    jornalero = item.nombre,
                    cuartelOrigen = item.cuartelActual,
                    cuartelDestino = destino,
                    motivo = motivo
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}
