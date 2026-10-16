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
 * Pantalla 07 - Registro de pesaje en campo (AGV-003).
 *
 * Peso Neto = Peso Bruto - (Tara * Jabas). La tara sale de la calibracion
 * guardada en Room; el pesaje se persiste de forma ATOMICA y las jabas se
 * suman a la parihuela abierta.
 */
class RegistrarPesajeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarPesajeBinding
    private val viewModel: PesajeViewModel by viewModels()

    private var jornaleroActual: Jornalero? = null
    private var entradaPesoBruto: String = ""
    private var cantidadJabas: Int = 0
    private var taraAplicada: Double = Constantes.TARA_POR_DEFECTO
    private var esTaraPorDefecto: Boolean = true

    companion object {
        const val EXTRA_PESO_NETO = "extra_peso_neto"
        const val EXTRA_ACUMULADO_DIARIO = "extra_acumulado_diario"
        const val EXTRA_ES_TARA_CALIBRADA = "extra_es_tara_calibrada"

        private const val KEY_JORNALERO = "key_jornalero_pesaje"
        private const val KEY_ENTRADA_PESO = "key_entrada_peso"
        private const val KEY_CANTIDAD_JABAS = "key_cantidad_jabas"
    }

    /** Escaneo REAL del fotocheck del jornalero (ZXing). */
    private val escanerLauncher = registerForActivityResult(ScanContract()) { result ->
        val contenido = result.contents
        if (!contenido.isNullOrBlank()) identificarPorQr(contenido)
    }

    @Suppress("DEPRECATION")
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

        if (savedInstanceState != null) {
            entradaPesoBruto = savedInstanceState.getString(KEY_ENTRADA_PESO) ?: ""
            cantidadJabas = savedInstanceState.getInt(KEY_CANTIDAD_JABAS, 0)
            val jornalero = savedInstanceState.getSerializable(KEY_JORNALERO) as? Jornalero
            jornalero?.let { mostrarJornalero(it) }
        } else {
            limpiarCampos()
        }

        // Tara vigente del turno: calibrada (Room) o por defecto
        viewModel.cargarTaraVigente()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_JORNALERO, jornaleroActual)
        outState.putString(KEY_ENTRADA_PESO, entradaPesoBruto)
        outState.putInt(KEY_CANTIDAD_JABAS, cantidadJabas)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // "Siguiente jornalero": limpia el formulario sin apilar Activities
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
        jornaleroActual = jornalero
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
            if (jornaleroActual != null && cantidadJabas > 0) {
                cantidadJabas -= 1
                actualizarCalculos()
            }
        }
    }

    private fun sumarJabas(cantidad: Int) {
        if (jornaleroActual != null) {
            cantidadJabas += cantidad
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
                if (jornaleroActual != null) {
                    if (char == "." && entradaPesoBruto.contains(".")) return@setOnClickListener
                    if (entradaPesoBruto.length < 6) {
                        entradaPesoBruto += char
                        actualizarCalculos()
                    }
                }
            }
        }

        binding.btnPClear.setOnClickListener {
            if (jornaleroActual != null) {
                entradaPesoBruto = ""
                actualizarCalculos()
            }
        }
    }

    private fun actualizarCalculos() {
        val pesoBruto = entradaPesoBruto.toDoubleOrNull() ?: 0.0
        val taraTotal = viewModel.calcularTaraTotal(cantidadJabas)
        val pesoNeto = viewModel.calcularPesoNeto(pesoBruto, cantidadJabas)

        binding.tvPesoBrutoValor.text = if (entradaPesoBruto.isEmpty()) "0.00" else entradaPesoBruto
        binding.tvCantidadJabas.text = cantidadJabas.toString()

        binding.tvTaraAplicada.text =
            String.format(Locale.US, "Tara aplicada: %.3f kg por jaba", taraAplicada)
        binding.tvTaraTotalCalculada.text =
            String.format(Locale.US, "Tara total: %.2f kg (%d x %.3f)", taraTotal, cantidadJabas, taraAplicada)

        binding.tvPesoNetoResultado.text = String.format(Locale.US, "%.2f kg", pesoNeto)
        binding.tvFormulaCalculo.text =
            String.format(Locale.US, "%.2f - %.2f = %.2f kg", pesoBruto, taraTotal, pesoNeto)

        binding.btnGuardarPesaje.isEnabled =
            jornaleroActual != null && viewModel.validarRango(pesoBruto, cantidadJabas)
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarPesaje.setOnClickListener {
            val jornalero = jornaleroActual ?: return@setOnClickListener
            val pesoBruto = entradaPesoBruto.toDoubleOrNull() ?: 0.0

            viewModel.persistirPesaje(jornalero, pesoBruto, cantidadJabas) { pesoNeto, porDefecto ->
                // AGV-004: acumulado diario real del cosechador
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
        jornaleroActual = null
        entradaPesoBruto = ""
        cantidadJabas = 0

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
