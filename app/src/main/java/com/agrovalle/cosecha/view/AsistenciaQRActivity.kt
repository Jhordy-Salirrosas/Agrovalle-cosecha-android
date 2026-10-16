package com.agrovalle.cosecha.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityAsistenciaQrBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.AsistenciaViewModel
import com.agrovalle.cosecha.viewmodel.ResultadoAsistencia
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Pantalla 03 - Asistencia QR (AGV-001) + acceso al plan B por DNI (AGV-016).
 *
 * El escaneo es REAL: usa ZXing (journeyapps), que no es CameraX ni ML Kit.
 * Toda la validacion (padron, duplicados, tardanza) la resuelve el ViewModel
 * contra la base Room.
 */
class AsistenciaQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenciaQrBinding
    private val viewModel: AsistenciaViewModel by viewModels()

    /** AGV-001: lector real de QR / Code 128 con la camara del terminal. */
    private val escanerQrLauncher = registerForActivityResult(ScanContract()) { result ->
        val contenido = result.contents
        if (contenido.isNullOrBlank()) {
            binding.tvEstadoEscaneo.text = "Escaneo cancelado"
        } else {
            viewModel.registrarPorQr(contenido)
        }
    }

    /** AGV-016: resultado de la pantalla "Buscar por DNI". */
    @Suppress("DEPRECATION")
    private val busquedaDniLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val jornalero = result.data?.getSerializableExtra(BusquedaDNIActivity.EXTRA_JORNALERO) as? Jornalero
            if (jornalero != null) viewModel.registrarManual(jornalero)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsistenciaQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarEstado()
        configurarEventos()
        viewModel.actualizarContador()
    }

    private fun observarEstado() {
        viewModel.contador.observe(this) { cantidad ->
            binding.tvContadorCuadrilla.text = "$cantidad / ${Constantes.TOTAL_CUADRILLA}"
        }
        viewModel.resultado.observe(this) { resultado ->
            if (resultado != null) mostrarResultado(resultado)
        }
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener { finish() }

        binding.btnEscanearQr.setOnClickListener { lanzarEscaner() }

        binding.btnBuscarPorDni.setOnClickListener {
            busquedaDniLauncher.launch(Intent(this, BusquedaDNIActivity::class.java))
        }
    }

    private fun lanzarEscaner() {
        val opciones = ScanOptions().apply {
            setPrompt("Apunte la cámara al fotocheck del jornalero")
            setBeepEnabled(false) // el feedback sensorial lo da FeedbackUtil
            setOrientationLocked(false)
        }
        escanerQrLauncher.launch(opciones)
    }

    private fun mostrarResultado(resultado: ResultadoAsistencia) {
        val jornalero = resultado.jornalero
        if (resultado.exitoso && jornalero != null) {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)

            binding.tvUltimoJornaleroRegistrado.text = "✓ Registrado: ${jornalero.nombreCompleto}"
            binding.tvDetalleRegistro.text =
                "DNI: ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo} • " +
                    "${resultado.horaIngreso} • ${resultado.estado}"
            binding.tvEstadoEscaneo.text =
                if (resultado.metodo == AsistenciaViewModel.METODO_DNI) {
                    "Ingreso manual registrado (búsqueda por DNI)"
                } else {
                    "Último escaneo exitoso: ${jornalero.codigoQr}"
                }
        } else {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
            binding.tvEstadoEscaneo.text = resultado.mensaje
        }
        viewModel.consumirResultado()
    }
}
