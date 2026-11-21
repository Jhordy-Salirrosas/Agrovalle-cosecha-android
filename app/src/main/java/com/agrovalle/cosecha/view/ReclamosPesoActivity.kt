package com.agrovalle.cosecha.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityReclamosPesoBinding
import com.agrovalle.cosecha.model.EstadoReclamoPeso
import com.agrovalle.cosecha.util.CalculadoraReclamoPeso
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.ReclamoPesoUiState
import com.agrovalle.cosecha.viewmodel.ReclamoPesoViewModel
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Pantalla de reclamos de peso y consulta de su bitácora (AGV-015 escenarios 2.0 y 3.0).
 */
class ReclamosPesoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PESAJE_ID = "extra_reclamo_pesaje_id"
    }

    private lateinit var binding: ActivityReclamosPesoBinding
    private val viewModel: ReclamoPesoViewModel by viewModels()

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
            Toast.makeText(this, "Escaneo de credencial cancelado.", Toast.LENGTH_SHORT).show()
        } else {
            val brutoTxt = binding.etNuevoPesoBrutoReclamo.text?.toString().orEmpty()
            viewModel.registrarReclamoConQrCapataz(brutoTxt, contenido)
            binding.etNuevoPesoBrutoReclamo.setText("")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReclamosPesoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolverReclamos.setOnClickListener { finish() }

        binding.etNuevoPesoBrutoReclamo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.actualizarPesoBrutoReclamo(s?.toString().orEmpty())
            }
        })

        binding.btnRegistrarReclamoSimple.setOnClickListener {
            val brutoTxt = binding.etNuevoPesoBrutoReclamo.text?.toString().orEmpty()
            viewModel.registrarReclamoDentroTolerancia(brutoTxt)
            binding.etNuevoPesoBrutoReclamo.setText("")
        }

        binding.btnAutorizarQrCompensatorio.setOnClickListener {
            val brutoTxt = binding.etNuevoPesoBrutoReclamo.text?.toString().orEmpty()
            if (viewModel.validarListoParaAutorizacionQr(brutoTxt)) {
                solicitarCamaraYAbrirEscaner()
            }
        }

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
        val preseleccionadoId = intent.getLongExtra(EXTRA_PESAJE_ID, 0L).takeIf { it > 0L }
        viewModel.cargarDatos(preseleccionadoId)
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
            setPrompt("Escanee la credencial QR del capataz (ej. QR_CAPATAZ_001)")
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setBeepEnabled(false)
            setOrientationLocked(false)
        }
        escanerQrCapatazLauncher.launch(opciones)
    }

    private fun renderizar(state: ReclamoPesoUiState) {
        if (!state.mensaje.isNullOrBlank()) {
            binding.tvMensajeReclamo.visibility = View.VISIBLE
            binding.tvMensajeReclamo.text = state.mensaje
            val colorRes = if (state.esError) R.color.color_error else R.color.verde_agricola
            binding.tvMensajeReclamo.setTextColor(ContextCompat.getColor(this, colorRes))
            Toast.makeText(this, state.mensaje, Toast.LENGTH_LONG).show()
            viewModel.consumirMensaje()
        }

        renderizarPesajes(state)
        renderizarFormularioReclamo(state)
        renderizarBitacora(state)
    }

    private fun renderizarPesajes(state: ReclamoPesoUiState) {
        binding.containerPesajesRegistrados.removeAllViews()
        if (state.pesajesRegistrados.isEmpty()) {
            binding.tvSinPesajesRegistrados.visibility = View.VISIBLE
            return
        }
        binding.tvSinPesajesRegistrados.visibility = View.GONE

        state.pesajesRegistrados.forEach { item ->
            val seleccionado = state.pesajeSeleccionado?.pesaje?.id == item.pesaje.id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 14, 20, 14)
                val bgColor = if (seleccionado) R.color.verde_suave else R.color.fondo_app
                setBackgroundColor(ContextCompat.getColor(context, bgColor))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10 }
                layoutParams = params
            }

            val tvInfo = TextView(this).apply {
                text = String.format(
                    Locale.US,
                    "Pesaje #%d • %s • %s\nJabas: %d • Bruto: %.2f kg • Tara: %.2f kg • Neto: %.2f kg",
                    item.pesaje.id,
                    item.horaTexto,
                    item.jornaleroNombre,
                    item.pesaje.cantidadJabas,
                    item.pesaje.pesoBruto,
                    item.pesaje.taraTotal,
                    item.pesaje.pesoNeto
                )
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 13f
            }

            val btnSeleccionar = MaterialButton(this).apply {
                text = if (seleccionado) "Seleccionado para reclamo" else "Iniciar reclamo"
                isEnabled = !seleccionado
                setOnClickListener {
                    viewModel.seleccionarPesaje(item.pesaje.id)
                    binding.etNuevoPesoBrutoReclamo.setText("")
                }
            }

            card.addView(tvInfo)
            card.addView(btnSeleccionar)
            binding.containerPesajesRegistrados.addView(card)
        }
    }

    private fun renderizarFormularioReclamo(state: ReclamoPesoUiState) {
        val sel = state.pesajeSeleccionado
        if (sel == null) {
            binding.cardFormularioReclamo.visibility = View.GONE
            return
        }
        binding.cardFormularioReclamo.visibility = View.VISIBLE
        val taraTotalAplicada = CalculadoraReclamoPeso.resolverTaraTotal(
            taraTotalOriginalKg = sel.pesaje.taraTotal,
            cantidadJabas = sel.pesaje.cantidadJabas,
            taraPorJabaRespaldoKg = state.taraVigenteKg
        )
        binding.tvDetallePesajeSeleccionado.text = String.format(
            Locale.US,
            "Original #%d (%s) • %d jabas • Neto original: %.2f kg\nTara total del pesaje: %.2f kg %s",
            sel.pesaje.id,
            sel.jornaleroNombre,
            sel.pesaje.cantidadJabas,
            sel.pesaje.pesoNeto,
            taraTotalAplicada,
            if (sel.pesaje.esTaraPorDefecto) "(Defecto)" else "(Calibrada)"
        )

        val eval = state.evaluacionEnCurso
        if (eval == null) {
            binding.tvResultadoComparacionReclamo.text =
                "Ingrese el nuevo peso bruto para evaluar la diferencia respecto al original."
            binding.tvResultadoComparacionReclamo.setTextColor(
                ContextCompat.getColor(this, R.color.text_secondary)
            )
            binding.btnRegistrarReclamoSimple.visibility = View.VISIBLE
            binding.btnAutorizarQrCompensatorio.visibility = View.GONE
        } else if (!eval.excedeTolerancia) {
            binding.tvResultadoComparacionReclamo.text = String.format(
                Locale.US,
                "Nuevo neto: %.2f kg • Diferencia: %+.0f g (<= 20 g).\nDentro de tolerancia: se mantiene el pesaje original y se registra en bitácora.",
                eval.pesoNuevoNetoKg,
                eval.diferenciaGramos
            )
            binding.tvResultadoComparacionReclamo.setTextColor(
                ContextCompat.getColor(this, R.color.color_success)
            )
            binding.btnRegistrarReclamoSimple.visibility = View.VISIBLE
            binding.btnAutorizarQrCompensatorio.visibility = View.GONE
        } else {
            binding.tvResultadoComparacionReclamo.text = String.format(
                Locale.US,
                "Nuevo neto: %.2f kg • Diferencia: %+.0f g (> 20 g).\nSupera tolerancia: se registrará compensatorio de %.2f kg. Exige autorización QR del capataz.",
                eval.pesoNuevoNetoKg,
                eval.diferenciaGramos,
                eval.pesoCompensatorioKg
            )
            binding.tvResultadoComparacionReclamo.setTextColor(
                ContextCompat.getColor(this, R.color.color_error)
            )
            binding.btnRegistrarReclamoSimple.visibility = View.GONE
            binding.btnAutorizarQrCompensatorio.visibility = View.VISIBLE
        }
    }

    private fun renderizarBitacora(state: ReclamoPesoUiState) {
        binding.containerBitacoraReclamos.removeAllViews()
        if (state.bitacoraReclamos.isEmpty()) {
            binding.tvSinReclamosRegistrados.visibility = View.VISIBLE
            return
        }
        binding.tvSinReclamosRegistrados.visibility = View.GONE

        state.bitacoraReclamos.forEach { reg ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 18, 24, 18)
                setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                layoutParams = params
            }

            val detalleCompensatorio = if (reg.estado == EstadoReclamoPeso.COMPENSADO) {
                String.format(
                    Locale.US,
                    "\nPesaje compensatorio #%d: %.2f kg (vinculado a #%d)\nAutorizado por: %s (%s)",
                    reg.pesajeCompensatorioId ?: 0L,
                    reg.pesoCompensatorioKg ?: 0.0,
                    reg.pesajeOriginalId,
                    reg.capatazAutorizador ?: "-",
                    reg.codigoQrCapataz ?: "-"
                )
            } else {
                "\nEstado: Dentro de tolerancia (pesaje original mantenido)"
            }

            val tv = TextView(this).apply {
                text = String.format(
                    Locale.US,
                    "%s %s • Jornalero: %s\nPesaje original #%d: %.3f kg -> Nuevo neto: %.3f kg\nDiferencia: %+.0f g (%+.3f kg)%s",
                    reg.fecha,
                    reg.hora,
                    reg.jornaleroNombre,
                    reg.pesajeOriginalId,
                    reg.pesoOriginalNetoKg,
                    reg.pesoNuevoNetoKg,
                    reg.diferenciaGramos,
                    reg.diferenciaKg,
                    detalleCompensatorio
                )
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 13f
            }
            card.addView(tv)
            binding.containerBitacoraReclamos.addView(card)
        }
    }
}
