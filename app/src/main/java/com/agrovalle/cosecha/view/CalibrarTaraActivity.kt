package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityCalibrarTaraBinding
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.CalibracionTaraViewModel
import java.util.Locale

/**
 * Pantalla 06 - Calibracion de tara (AGV-015) - SIN Room.
 *
 * Se pesan 5 jabas vacias; la tara promedio por jaba se guarda en memoria
 * (DatosMemoria.calibraciones) como tara del turno, con fecha, hora y
 * responsable, y se aplica a todos los pesajes.
 *
 * Las muestras registradas viven en el SavedStateHandle del ViewModel, asi que
 * no se pierden al rotar la pantalla.
 */
class CalibrarTaraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalibrarTaraBinding
    private val viewModel: CalibracionTaraViewModel by viewModels()

    private lateinit var vistasPasos: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalibrarTaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vistasPasos = listOf(
            binding.tvPaso1,
            binding.tvPaso2,
            binding.tvPaso3,
            binding.tvPaso4,
            binding.tvPaso5
        )

        configurarEventos()
        observarEstado()
        viewModel.cargarTaraVigente()
        actualizarDisplayPesoMuestra()
        actualizarUiPasoActual(viewModel.pesosMuestra.value?.size ?: 0)
    }

    private fun observarEstado() {
        viewModel.pesosMuestra.observe(this) { muestras ->
            actualizarUiPasoActual(muestras.size)
        }
        viewModel.mensaje.observe(this) { mensaje ->
            if (!mensaje.isNullOrBlank()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                viewModel.consumirMensaje()
            }
        }
    }

    private fun configurarEventos() {
        binding.btnVolverTara.setOnClickListener { finish() }

        binding.btnMasPeso.setOnClickListener {
            viewModel.pesoActualMuestra = redondear3(viewModel.pesoActualMuestra + 0.005)
            actualizarDisplayPesoMuestra()
        }

        binding.btnMenosPeso.setOnClickListener {
            if (viewModel.pesoActualMuestra > 0.100) {
                viewModel.pesoActualMuestra = redondear3(viewModel.pesoActualMuestra - 0.005)
                actualizarDisplayPesoMuestra()
            }
        }

        binding.btnRegistrarPesoJaba.setOnClickListener {
            FeedbackUtil.vibrarExito(this)
            viewModel.registrarPesoMuestra(viewModel.pesoActualMuestra)
        }

        // AGV-015: guarda la tara del turno en memoria
        binding.btnConfirmarCalibracion.setOnClickListener {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)
            viewModel.confirmarYGuardarTara { irARegistrarPesaje() }
        }

        // AGV-003 escenario 2: se omite la calibracion y se usa la tara por defecto
        binding.btnOmitirCalibracion.setOnClickListener {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
            viewModel.usarTaraPorDefecto { irARegistrarPesaje() }
        }
    }

    private fun irARegistrarPesaje() {
        startActivity(Intent(this, RegistrarPesajeActivity::class.java))
        finish()
    }

    private fun actualizarDisplayPesoMuestra() {
        binding.tvPesoMuestra.text =
            String.format(Locale.US, "%.3f kg", viewModel.pesoActualMuestra)
    }

    private fun actualizarUiPasoActual(totalRegistradas: Int) {
        for (i in vistasPasos.indices) {
            val tvPaso = vistasPasos[i]
            when {
                i < totalRegistradas -> {
                    tvPaso.text = "✓"
                    tvPaso.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.verde_agricola)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
                i == totalRegistradas -> {
                    tvPaso.text = "${i + 1}"
                    tvPaso.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.verde_claro)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.verde_oscuro))
                }
                else -> {
                    tvPaso.text = "${i + 1}"
                    tvPaso.backgroundTintList =
                        ContextCompat.getColorStateList(this, R.color.card_border)
                    tvPaso.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        val total = Constantes.MUESTRAS_CALIBRACION
        if (totalRegistradas < total) {
            binding.tvInstruccionJaba.text =
                "Coloque la Jaba ${totalRegistradas + 1} vacía en la balanza"
            binding.btnRegistrarPesoJaba.isEnabled = true
            binding.btnConfirmarCalibracion.isEnabled = false
        } else {
            binding.tvInstruccionJaba.text = "¡Muestreo completo! $total jabas registradas"
            binding.btnRegistrarPesoJaba.isEnabled = false
            binding.btnConfirmarCalibracion.isEnabled = true
        }

        val muestras = viewModel.pesosMuestra.value ?: emptyList()
        if (muestras.isNotEmpty()) {
            binding.tvPromedioCalculado.text =
                String.format(Locale.US, "%.3f kg", muestras.average())
            binding.tvDetalleMuestras.text =
                "Muestras registradas: $totalRegistradas de $total " +
                    "[${muestras.joinToString { String.format(Locale.US, "%.3f", it) }}]"
        } else {
            binding.tvPromedioCalculado.text = "-- kg"
            binding.tvDetalleMuestras.text = "Muestras registradas: 0 de $total"
        }
    }

    private fun redondear3(valor: Double): Double = Math.round(valor * 1000.0) / 1000.0
}
