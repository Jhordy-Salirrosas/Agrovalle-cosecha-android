package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityRegistrarPesajeBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.Constantes
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.PesajeViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale

/**
 * Pantalla 07 - Registro de pesaje en campo (AGV-003) - SIN Room.
 *
 * Peso Neto = Peso Bruto - (Tara * Jabas). La tara sale de la ultima calibracion
 * guardada en memoria (DatosMemoria.calibraciones); el pesaje se agrega a la
 * bitacora en memoria y el acumulado diario se calcula sumando esos pesajes.
 *
 * El peso en curso y la cantidad de jabas viven en el SavedStateHandle del
 * ViewModel (AGV-003 + ciclo de vida).
 */
class RegistrarPesajeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarPesajeBinding
    private val viewModel: PesajeViewModel by viewModels()

    private var taraAplicada: Double = Constantes.TARA_POR_DEFECTO
    private var esTaraPorDefecto: Boolean = true

    companion object {
        const val EXTRA_PESO_NETO = "extra_peso_neto"
        const val EXTRA_ACUMULADO_DIARIO = "extra_acumulado_diario"
        const val EXTRA_ES_TARA_CALIBRADA = "extra_es_tara_calibrada"
    }

    /** Escaneo REAL del fotocheck del jornalero (ZXing). */
    private val escanerLauncher = registerForActivityResult(ScanContract()) { result ->
        val contenido = result.contents
        if (!contenido.isNullOrBlank()) identificarPorQr(contenido)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarPesajeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarHeader()
        configurarEscanerFotocheck()
        configurarBotonesJabas()
        configurarTecladoPesaje()
        configurarBotonGuardar()
        observarViewModel()

        // Tara vigente del turno: calibrada (en memoria) o por defecto.
        viewModel.cargarTaraVigente()

        // Restaura el jornalero y los valores en curso tras una rotacion.
        val jornalero = viewModel.jornaleroActual()
        if (jornalero != null) {
            mostrarJornalero(jornalero)
        } else {
            limpiarCampos()
        }
        actualizarCalculos()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // "Siguiente jornalero": limpia el formulario sin apilar Activities.
        viewModel.limpiarPesajeEnCurso()
        limpiarCampos()
    }

    private fun observarViewModel() {
        viewModel.taraAplicada.observe(this) { tara ->
            taraAplicada = tara
            actualizarHeaderTara()
            actualizarCalculos()
        }
        viewModel.esTaraPorDefecto.observe(this) { porDefecto ->
            esTaraPorDefecto = porDefecto
            actualizarHeaderTara()
        }
        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.consumirError()
            }
        }
    }

    private fun configurarHeader() {
        binding.btnVolverPesaje.setOnClickListener { finish() }
        actualizarHeaderTara()
    }

    private fun actualizarHeaderTara() {
        binding.tvChipTara.text = String.format(
            Locale.US,
            "Tara: %.3f kg %s",
            taraAplicada,
            if (esTaraPorDefecto) "(Defecto)" else "(Calibrada)"
        )
    }

    private fun configurarEscanerFotocheck() {
        binding.btnEscanearFotocheck.setOnClickListener { lanzarEscaner() }
        binding.cardJornaleroPesaje.setOnClickListener { lanzarEscaner() }
    }

    private fun lanzarEscaner() {
        val opciones = ScanOptions().apply {
            setPrompt("Apunte la cámara al fotocheck del jornalero")
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
        escanerLauncher.launch(opciones)
    }

    private fun identificarPorQr(codigoQr: String) {
        viewModel.buscarJornaleroPorQr(codigoQr) { jornalero ->
            if (jornalero == null) {
                FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = true)
                Toast.makeText(this, "Fotocheck no reconocido en el padrón", Toast.LENGTH_LONG)
                    .show()
            } else {
                FeedbackUtil.vibrarExito(this)
                mostrarJornalero(jornalero)
                actualizarCalculos()
            }
        }
    }

    private fun mostrarJornalero(jornalero: Jornalero) {
        binding.tvNombreJornaleroPesaje.text =
            "Fotocheck identificado: ${jornalero.nombreCompleto}"
        binding.tvEstadoAsistenciaJornalero.text =
            "DNI ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo}"
    }

    private fun configurarBotonesJabas() {
        binding.btnJabaMas1.setOnClickListener { sumarJabas(1) }
        binding.btnJabaMas4.setOnClickListener { sumarJabas(4) }
        binding.btnJabaMas6.setOnClickListener { sumarJabas(6) }
        binding.btnJabaMenos1.setOnClickListener {
            if (viewModel.hayJornalero() && viewModel.cantidadJabas > 0) {
                viewModel.cantidadJabas = viewModel.cantidadJabas - 1
                actualizarCalculos()
            }
        }
    }

    private fun sumarJabas(cantidad: Int) {
        if (viewModel.hayJornalero()) {
            viewModel.cantidadJabas = viewModel.cantidadJabas + cantidad
            actualizarCalculos()
        }
    }

    private fun configurarTecladoPesaje() {
        val teclas = listOf(
            binding.btnP0 to "0",
            binding.btnP1 to "1",
            binding.btnP2 to "2",
            binding.btnP3 to "3",
            binding.btnP4 to "4",
            binding.btnP5 to "5",
            binding.btnP6 to "6",
            binding.btnP7 to "7",
            binding.btnP8 to "8",
            binding.btnP9 to "9",
            binding.btnPDot to "."
        )

        for ((btn, char) in teclas) {
            btn.setOnClickListener {
                if (viewModel.hayJornalero()) {
                    val entrada = viewModel.entradaPesoBruto
                    if (char == "." && entrada.contains(".")) return@setOnClickListener
                    if (entrada.length < 6) {
                        viewModel.entradaPesoBruto = entrada + char
                        actualizarCalculos()
                    }
                }
            }
        }

        binding.btnPClear.setOnClickListener {
            if (viewModel.hayJornalero()) {
                viewModel.entradaPesoBruto = ""
                actualizarCalculos()
            }
        }
    }

    private fun actualizarCalculos() {
        val pesoBruto = viewModel.pesoBrutoActual()
        val cantidadJabas = viewModel.cantidadJabas
        val taraTotal = viewModel.calcularTaraTotal(cantidadJabas)
        val pesoNeto = viewModel.calcularPesoNeto(pesoBruto, cantidadJabas)

        binding.tvPesoBrutoValor.text =
            if (viewModel.entradaPesoBruto.isEmpty()) "0.00" else viewModel.entradaPesoBruto
        binding.tvCantidadJabas.text = cantidadJabas.toString()

        binding.tvTaraAplicada.text =
            String.format(Locale.US, "Tara aplicada: %.3f kg por jaba", taraAplicada)
        binding.tvTaraTotalCalculada.text =
            String.format(Locale.US, "Tara total: %.2f kg (%d x %.3f)", taraTotal, cantidadJabas, taraAplicada)

        binding.tvPesoNetoResultado.text = String.format(Locale.US, "%.2f kg", pesoNeto)
        binding.tvFormulaCalculo.text =
            String.format(Locale.US, "%.2f - %.2f = %.2f kg", pesoBruto, taraTotal, pesoNeto)

        binding.btnGuardarPesaje.isEnabled =
            viewModel.hayJornalero() && viewModel.validarRango(pesoBruto, cantidadJabas)
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarPesaje.setOnClickListener {
            val jornalero = viewModel.jornaleroActual() ?: return@setOnClickListener
            val pesoBruto = viewModel.pesoBrutoActual()
            val cantidadJabas = viewModel.cantidadJabas

            viewModel.persistirPesaje(jornalero, pesoBruto, cantidadJabas) { pesoNeto, porDefecto ->
                // AGV-004: acumulado diario real del cosechador (suma en memoria).
                viewModel.cargarAcumuladoDiario(jornalero.id) { acumulado ->
                    val intent = Intent(this, ConfirmacionActivity::class.java).apply {
                        putExtra(EXTRA_PESO_NETO, pesoNeto)
                        putExtra(EXTRA_ACUMULADO_DIARIO, acumulado)
                        putExtra(EXTRA_ES_TARA_CALIBRADA, !porDefecto)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    private fun limpiarCampos() {
        binding.tvNombreJornaleroPesaje.text = "Escanea el fotocheck para comenzar"
        binding.tvEstadoAsistenciaJornalero.text = "Esperando escaneo..."
        binding.tvPesoBrutoValor.text = "0.00"
        binding.tvCantidadJabas.text = "0"
        binding.tvPesoNetoResultado.text = "0.00 kg"
        binding.tvFormulaCalculo.text = "0.00 - 0.00 = 0.00 kg"
        binding.tvTaraTotalCalculada.text =
            String.format(Locale.US, "Tara total: 0.00 kg (0 x %.3f)", taraAplicada)
        binding.btnGuardarPesaje.isEnabled = false
    }
}
