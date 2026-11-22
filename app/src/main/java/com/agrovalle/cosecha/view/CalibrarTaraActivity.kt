package com.agrovalle.cosecha.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityCalibrarTaraBinding
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.CalibracionTaraViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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

    private enum class AccionAutorizacionTara {
        CONFIRMAR_CALIBRACION,
        RESTAURAR_POR_DEFECTO
    }

    private lateinit var binding: ActivityCalibrarTaraBinding
    private val viewModel: CalibracionTaraViewModel by viewModels()

    private lateinit var vistasPasos: List<TextView>
    private var accionPendienteQr: AccionAutorizacionTara = AccionAutorizacionTara.CONFIRMAR_CALIBRACION

    private val permisoCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirEscanerCapataz()
        } else {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
            Toast.makeText(
                this,
                "Permiso de cámara denegado: no se puede leer la credencial QR del capataz.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val escanerQrCapatazLauncher = registerForActivityResult(ScanContract()) { result ->
        val contenido = result.contents
        if (contenido.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Escaneo de credencial cancelado. La tara no fue modificada.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            when (accionPendienteQr) {
                AccionAutorizacionTara.CONFIRMAR_CALIBRACION -> {
                    viewModel.confirmarYGuardarTara(contenido) {
                        FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)
                        irARegistrarPesaje()
                    }
                }

                AccionAutorizacionTara.RESTAURAR_POR_DEFECTO -> {
                    viewModel.usarTaraPorDefecto(contenido) {
                        FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
                        irARegistrarPesaje()
                    }
                }
            }
        }
    }

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

        // AGV-015 / RNF-015.1 (H-12): exige credencial QR de capataz/supervisor antes de guardar
        binding.btnConfirmarCalibracion.setOnClickListener {
            if (viewModel.validarListoParaAutorizacionQr()) {
                accionPendienteQr = AccionAutorizacionTara.CONFIRMAR_CALIBRACION
                solicitarCamaraYAbrirEscaner()
            }
        }

        // AGV-003 escenario 2: se omite la calibracion y se usa la tara por defecto
        // Si ya había una tara calibrada aprobada, exige autorización QR para modificarla (H-12)
        binding.btnOmitirCalibracion.setOnClickListener {
            if (viewModel.requiereAutorizacionParaOmitir()) {
                accionPendienteQr = AccionAutorizacionTara.RESTAURAR_POR_DEFECTO
                solicitarCamaraYAbrirEscaner()
            } else {
                FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
                viewModel.usarTaraPorDefecto { irARegistrarPesaje() }
            }
        }
    }

    private fun solicitarCamaraYAbrirEscaner() {
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (concedido) {
            abrirEscanerCapataz()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirEscanerCapataz() {
        val opciones = ScanOptions().apply {
            setPrompt("Escanee la credencial QR del capataz o supervisor (ej. QR_CAPATAZ_001)")
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
        escanerQrCapatazLauncher.launch(opciones)
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
