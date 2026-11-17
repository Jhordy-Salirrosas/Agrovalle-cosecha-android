package com.agrovalle.cosecha.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.repository.ConsultaOrigenParihuela
import com.agrovalle.cosecha.databinding.ActivityConsultaOrigenBinding
import com.agrovalle.cosecha.util.QrUtil
import com.agrovalle.cosecha.viewmodel.ConsultaOrigenViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla de consulta de origen de parihuela por código (Sprint 2).
 */
class ConsultaOrigenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsultaOrigenBinding
    private val viewModel: ConsultaOrigenViewModel by viewModels()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    private val escanerLauncher = registerForActivityResult(ScanContract()) { result ->
        val codigo = result.contents
        if (!codigo.isNullOrBlank()) {
            binding.etCodigoConsulta.setText(codigo)
            viewModel.consultarPorCodigo(codigo)
        }
    }

    private val permisoCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) lanzarEscaner()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultaOrigenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConsultarOrigen.setOnClickListener {
            val codigo = binding.etCodigoConsulta.text?.toString().orEmpty()
            viewModel.consultarPorCodigo(codigo)
        }

        binding.btnEscanearQr.setOnClickListener {
            iniciarEscaneoConPermiso()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resultado.collect { resultado ->
                    if (resultado == null) {
                        binding.cardResultadoOrigen.visibility = View.GONE
                        return@collect
                    }

                    binding.cardResultadoOrigen.visibility = View.VISIBLE
                    when (resultado) {
                        is ConsultaOrigenParihuela.NoEncontrado -> {
                            binding.tvResultadoOrigen.text = "No se encontró ninguna parihuela con ese código."
                            binding.ivQrParihuela.visibility = View.GONE
                        }

                        is ConsultaOrigenParihuela.Encontrado -> {
                            val o = resultado.origen
                            val estadoLegible = when (o.estado) {
                                EstadoParihuela.RETENIDA_CALIDAD -> "RETENIDA POR CALIDAD"
                                else -> o.estado.name
                            }
                            binding.tvResultadoOrigen.text = buildString {
                                appendLine("Código: ${o.codigo}")
                                appendLine("Estado: $estadoLegible")
                                if (o.estado == EstadoParihuela.RETENIDA_CALIDAD) {
                                    appendLine("Aviso: Retenida por calidad (no se puede sellar hasta liberar)")
                                }
                                appendLine("Variedad: ${o.variedad}")
                                appendLine("Cuartel: ${o.cuartel}")
                                appendLine("Jabas: ${o.conteoJabas} | Pisos: ${o.pisos}")
                                appendLine("Inicio: ${o.horaInicio?.let { formatoFecha.format(it) } ?: "Sin iniciar (sin pesajes)"}")
                                appendLine("Sellado: ${o.horaSellado?.let { formatoFecha.format(it) } ?: "Pendiente"}")
                                if (!o.motivoSelladoAnticipado.isNullOrBlank()) {
                                    append("Motivo sellado anticipado: ${o.motivoSelladoAnticipado}")
                                }
                            }
                            if (o.estado == EstadoParihuela.SELLADA ||
                                o.estado == EstadoParihuela.SELLADA_ANTICIPADA
                            ) {
                                binding.ivQrParihuela.setImageBitmap(QrUtil.generarQrBitmap(o.codigo))
                                binding.ivQrParihuela.visibility = View.VISIBLE
                            } else {
                                binding.ivQrParihuela.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun iniciarEscaneoConPermiso() {
        val estado = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (estado == PackageManager.PERMISSION_GRANTED) {
            lanzarEscaner()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun lanzarEscaner() {
        val opciones = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanee el código QR de la parihuela")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }
        escanerLauncher.launch(opciones)
    }
}
