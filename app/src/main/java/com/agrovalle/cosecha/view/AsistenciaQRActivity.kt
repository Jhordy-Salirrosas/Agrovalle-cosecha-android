package com.agrovalle.cosecha.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
 * La validacion (padron, duplicados, tardanza) la resuelve el ViewModel contra
 * las listas en memoria de [com.agrovalle.cosecha.data.DatosMemoria].
 *
 * Al registrar una asistencia exitosa se muestra el Toast "Asistencia
 * registrada" y se navega a [ContextoAgronomicoActivity] enviando el ID del
 * jornalero en el extra `EXTRA_JORNALERO_ID`.
 *
 * PERMISO DE CAMARA: aunque ZXing ya solicita el permiso por su cuenta, aqui se
 * valida y se solicita EXPLICITAMENTE antes de abrir el escaner, de modo que si
 * el usuario deniega el permiso la app no crashea y se informa con un Toast.
 */
class AsistenciaQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenciaQrBinding
    private val viewModel: AsistenciaViewModel by viewModels()

    /**
     * Fix 2: se pide el permiso de CAMARA en tiempo de ejecucion.
     * Si se concede se abre el escaner; si se deniega se avisa con un Toast y la
     * pantalla sigue funcionando (el plan B por DNI no necesita camara).
     */
    private val permisoCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirEscaner()
        } else {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
            Toast.makeText(
                this,
                "Permiso de cámara denegado. Use \"Buscar por DNI\" para registrar la asistencia.",
                Toast.LENGTH_LONG
            ).show()
            binding.tvEstadoEscaneo.text = "Sin permiso de cámara"
        }
    }

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
            val jornalero =
                result.data?.getSerializableExtra(BusquedaDNIActivity.EXTRA_JORNALERO) as? Jornalero
            if (jornalero != null) viewModel.registrarManual(jornalero)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsistenciaQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarEstado()
        configurarEventos()
    }

    override fun onResume() {
        super.onResume()
        // Al volver desde ContextoAgronomicoActivity (FLAG_ACTIVITY_CLEAR_TOP) el
        // contador se refresca con el estado actual de la memoria.
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

        binding.btnEscanearQr.setOnClickListener { solicitarCamaraYAbrirEscaner() }

        binding.btnBuscarPorDni.setOnClickListener {
            busquedaDniLauncher.launch(Intent(this, BusquedaDNIActivity::class.java))
        }
    }

    /** Fix 2: verifica el permiso y, si falta, lo solicita; luego abre el escaner. */
    private fun solicitarCamaraYAbrirEscaner() {
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (concedido) {
            abrirEscaner()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirEscaner() {
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

            Toast.makeText(this, "Asistencia registrada", Toast.LENGTH_SHORT).show()
            viewModel.consumirResultado()
            irAContextoAgronomico(jornalero.id)
        } else {
            FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
            binding.tvEstadoEscaneo.text = resultado.mensaje
            viewModel.consumirResultado()
        }
    }

    /** Navega al contexto agronomico enviando el ID del jornalero por el Intent. */
    private fun irAContextoAgronomico(jornaleroId: Long) {
        val intent = Intent(this, ContextoAgronomicoActivity::class.java).apply {
            putExtra(ContextoAgronomicoActivity.EXTRA_JORNALERO_ID, jornaleroId)
        }
        startActivity(intent)
    }
}
