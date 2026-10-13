package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityRegistrarPesajeBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.TurnoViewModel
import java.util.Locale

class RegistrarPesajeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarPesajeBinding
    private val viewModel: TurnoViewModel by viewModels()

    private var jornaleroActual: Jornalero? = null
    private var entradaPesoBruto: String = ""
    private var cantidadJabas: Int = 0
    private var valorTara: Double = TurnoViewModel.TARA_POR_DEFECTO
    private var esTaraCalibrada: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrarPesajeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        valorTara = intent.getDoubleExtra("EXTRA_TARA", TurnoViewModel.TARA_POR_DEFECTO)
        esTaraCalibrada = intent.getBooleanExtra("EXTRA_ES_CALIBRADA", false)

        configurarHeader()
        configurarEscanerFotocheck()
        configurarBotonesJabas()
        configurarTecladoPesaje()
        configurarBotonGuardar()

        // Estado inicial "Esperando Escaneo"
        limpiarCampos()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Al regresar con "Siguiente Jornalero", limpiar para el siguiente pesaje
        limpiarCampos()
    }

    private fun configurarHeader() {
        binding.btnVolverPesaje.setOnClickListener {
            finish()
        }

        binding.tvChipTara.text = String.format(Locale.US, "Tara: %.3f kg %s",
            valorTara, if (esTaraCalibrada) "(Calibrada)" else "(Defecto)")
    }

    private fun configurarEscanerFotocheck() {
        // Tocar el visor QR simula escaneo inmediato del fotocheck
        binding.btnEscanearFotocheck.setOnClickListener {
            identificarJornaleroSimulado()
        }
        binding.cardJornaleroPesaje.setOnClickListener {
            identificarJornaleroSimulado()
        }
    }

    private fun identificarJornaleroSimulado() {
        FeedbackUtil.vibrarExito(this)

        val demo = Jornalero(
            id = 4L,
            dni = "45678901",
            nombres = "Luis",
            apellidos = "Pérez Ramírez",
            codigoQr = "QR_FOTOCHECK_004",
            cuadrillaCodigo = "CUA-01"
        )
        jornaleroActual = demo

        binding.tvNombreJornaleroPesaje.text = "Fotocheck identificado: ${demo.nombreCompleto}"
        binding.tvEstadoAsistenciaJornalero.text = "✓ Asistencia registrada (Cuadrilla ${demo.cuadrillaCodigo})"

        // Pre-cargar valores de muestra inicial sugeridos (9.50 kg y 4 jabas)
        entradaPesoBruto = "9.50"
        cantidadJabas = 4
        actualizarCalculos()
    }

    private fun configurarBotonesJabas() {
        binding.btnJabaMas1.setOnClickListener {
            if (jornaleroActual != null) {
                cantidadJabas += 1
                actualizarCalculos()
            }
        }

        binding.btnJabaMas4.setOnClickListener {
            if (jornaleroActual != null) {
                cantidadJabas += 4
                actualizarCalculos()
            }
        }

        binding.btnJabaMas6.setOnClickListener {
            if (jornaleroActual != null) {
                cantidadJabas += 6
                actualizarCalculos()
            }
        }

        binding.btnJabaMenos1.setOnClickListener {
            if (jornaleroActual != null && cantidadJabas > 0) {
                cantidadJabas -= 1
                actualizarCalculos()
            }
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
        val taraTotal = valorTara * cantidadJabas
        val pesoNeto = (pesoBruto - taraTotal).coerceAtLeast(0.0)

        binding.tvPesoBrutoValor.text = if (entradaPesoBruto.isEmpty()) "0.00" else entradaPesoBruto
        binding.tvCantidadJabas.text = cantidadJabas.toString()

        binding.tvTaraAplicada.text = String.format(Locale.US, "Tara aplicada: %.3f kg por jaba", valorTara)
        binding.tvTaraTotalCalculada.text = String.format(Locale.US, "Tara total: %.2f kg (%d x %.3f)", taraTotal, cantidadJabas, valorTara)

        binding.tvPesoNetoResultado.text = String.format(Locale.US, "%.2f kg", pesoNeto)
        binding.tvFormulaCalculo.text = String.format(Locale.US, "%.2f - %.2f = %.2f kg", pesoBruto, taraTotal, pesoNeto)

        // Validación: Habilitado solo si Peso Bruto > 0 y Cantidad de Jabas > 0
        binding.btnGuardarPesaje.isEnabled = (jornaleroActual != null && pesoBruto > 0.0 && cantidadJabas > 0)
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarPesaje.setOnClickListener {
            val pesoBruto = entradaPesoBruto.toDoubleOrNull() ?: 0.0
            val netoCalculado = viewModel.registrarPesaje(pesoBruto, cantidadJabas)
            val acumuladoActual = viewModel.acumuladoDiario.value ?: 45.50

            val intent = Intent(this, ConfirmacionActivity::class.java).apply {
                putExtra("EXTRA_PESO_NETO", netoCalculado)
                putExtra("EXTRA_ACUMULADO_DIARIO", acumuladoActual)
                putExtra("EXTRA_ES_TARA_CALIBRADA", esTaraCalibrada)
            }
            startActivity(intent)
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
        binding.tvTaraTotalCalculada.text = String.format(Locale.US, "Tara total: 0.00 kg (0 x %.3f)", valorTara)
        binding.btnGuardarPesaje.isEnabled = false
    }
}
