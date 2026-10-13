package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityCalibrarTaraBinding
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.TurnoViewModel
import java.util.Locale

class CalibrarTaraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrarTaraBinding
    private val viewModel: TurnoViewModel by viewModels()

    private val listaMuestras = mutableListOf<Double>()
    private var pesoActualMuestra: Double = 0.375

    private lateinit var vistasPasos: List<TextView>

    companion object {
        const val EXTRA_TARA = "extra_tara"
        const val EXTRA_ES_CALIBRADA = "extra_es_calibrada"

        private const val KEY_MUESTRAS = "key_muestras_tara"
        private const val KEY_PESO_MUESTRA = "key_peso_muestra"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrarTaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            savedInstanceState.getDoubleArray(KEY_MUESTRAS)?.let { muestras ->
                listaMuestras.clear()
                listaMuestras.addAll(muestras.toList())
            }
            pesoActualMuestra = savedInstanceState.getDouble(KEY_PESO_MUESTRA, 0.375)
        }

        vistasPasos = listOf(
            binding.tvPaso1,
            binding.tvPaso2,
            binding.tvPaso3,
            binding.tvPaso4,
            binding.tvPaso5
        )

        configurarEventos()
        actualizarDisplayPesoMuestra()
        actualizarUiPasoActual()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putDoubleArray(KEY_MUESTRAS, listaMuestras.toDoubleArray())
        outState.putDouble(KEY_PESO_MUESTRA, pesoActualMuestra)
    }

    private fun configurarEventos() {
        binding.btnVolverTara.setOnClickListener {
            finish()
        }

        binding.btnMasPeso.setOnClickListener {
            pesoActualMuestra = Math.round((pesoActualMuestra + 0.005) * 1000.0) / 1000.0
            actualizarDisplayPesoMuestra()
        }

        binding.btnMenosPeso.setOnClickListener {
            if (pesoActualMuestra > 0.100) {
                pesoActualMuestra = Math.round((pesoActualMuestra - 0.005) * 1000.0) / 1000.0
                actualizarDisplayPesoMuestra()
            }
        }

        binding.btnRegistrarPesoJaba.setOnClickListener {
            registrarMuestraActual()
        }

        binding.btnConfirmarCalibracion.setOnClickListener {
            val promedio = if (listaMuestras.isNotEmpty()) listaMuestras.average() else TurnoViewModel.TARA_POR_DEFECTO
            val promedioRedondeado = Math.round(promedio * 1000.0) / 1000.0
            viewModel.setTaraCalibrada(promedioRedondeado, calibrada = true)

            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)

            val intent = Intent(this, RegistrarPesajeActivity::class.java).apply {
                putExtra(EXTRA_TARA, promedioRedondeado)
                putExtra(EXTRA_ES_CALIBRADA, true)
            }
            startActivity(intent)
            finish()
        }

        binding.btnOmitirCalibracion.setOnClickListener {
            viewModel.setTaraCalibrada(TurnoViewModel.TARA_POR_DEFECTO, calibrada = false)

            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)

            val intent = Intent(this, RegistrarPesajeActivity::class.java).apply {
                putExtra(EXTRA_TARA, TurnoViewModel.TARA_POR_DEFECTO)
                putExtra(EXTRA_ES_CALIBRADA, false)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun actualizarDisplayPesoMuestra() {
        binding.tvPesoMuestra.text = String.format(Locale.US, "%.3f kg", pesoActualMuestra)
    }

    private fun registrarMuestraActual() {
        if (listaMuestras.size < 5) {
            listaMuestras.add(pesoActualMuestra)
            FeedbackUtil.vibrarExito(this)

            // Simular variación realista para la siguiente muestra
            val variaciones = listOf(0.370, 0.380, 0.375, 0.378, 0.372)
            pesoActualMuestra = variaciones[listaMuestras.size % variaciones.size]
            actualizarDisplayPesoMuestra()

            actualizarUiPasoActual()
        }
    }

    private fun actualizarUiPasoActual() {
        val totalRegistradas = listaMuestras.size

        for (i in vistasPasos.indices) {
            val tvPaso = vistasPasos[i]
            when {
                i < totalRegistradas -> {
                    // Completado
                    tvPaso.text = "✓"
                    tvPaso.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_agricola)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
                i == totalRegistradas -> {
                    // Paso activo actual
                    tvPaso.text = "${i + 1}"
                    tvPaso.backgroundTintList = ContextCompat.getColorStateList(this, R.color.verde_claro)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.verde_oscuro))
                }
                else -> {
                    // Pendiente
                    tvPaso.text = "${i + 1}"
                    tvPaso.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_border)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        if (totalRegistradas < 5) {
            binding.tvInstruccionJaba.text = "Coloque la Jaba ${totalRegistradas + 1} vacía en la balanza"
            binding.btnRegistrarPesoJaba.isEnabled = true
            binding.btnConfirmarCalibracion.isEnabled = false
        } else {
            binding.tvInstruccionJaba.text = "¡Muestreo completo! 5 jabas registradas"
            binding.btnRegistrarPesoJaba.isEnabled = false
            binding.btnConfirmarCalibracion.isEnabled = true
        }

        if (listaMuestras.isNotEmpty()) {
            val promedio = listaMuestras.average()
            binding.tvPromedioCalculado.text = String.format(Locale.US, "%.3f kg", promedio)
            binding.tvDetalleMuestras.text = "Muestras registradas: $totalRegistradas de 5 [${listaMuestras.joinToString { String.format(Locale.US, "%.3f", it) }}]"
        } else {
            binding.tvPromedioCalculado.text = "-- kg"
            binding.tvDetalleMuestras.text = "Muestras registradas: 0 de 5"
        }
    }
}
