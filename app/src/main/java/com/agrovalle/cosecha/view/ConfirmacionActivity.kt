package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityConfirmacionBinding
import com.agrovalle.cosecha.util.FeedbackUtil
import java.util.Locale

class ConfirmacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pesoNeto = intent.getDoubleExtra(RegistrarPesajeActivity.EXTRA_PESO_NETO, 8.00)
        val acumuladoDiario = intent.getDoubleExtra(RegistrarPesajeActivity.EXTRA_ACUMULADO_DIARIO, 45.50)
        val esTaraCalibrada = intent.getBooleanExtra(RegistrarPesajeActivity.EXTRA_ES_TARA_CALIBRADA, true)

        configurarEstadoVisual(esTaraCalibrada, pesoNeto, acumuladoDiario)
        if (savedInstanceState == null) {
            activarFeedbackSensorial(esTaraCalibrada)
        }
        configurarNavegacion()
    }

    private fun configurarEstadoVisual(esTaraCalibrada: Boolean, pesoNeto: Double, acumuladoDiario: Double) {
        binding.tvPesoNetoValor.text = String.format(Locale.US, "%.2f kg", pesoNeto)
        binding.tvAcumuladoDiarioValor.text = String.format(Locale.US, "%.2f kg", acumuladoDiario)

        if (esTaraCalibrada) {
            // Estado 01 - Registro Correcto
            binding.ivIconoEstado.setImageResource(R.drawable.ic_check_circle)
            binding.tvTituloEstado.text = getString(R.string.pesaje_exitoso)
            binding.tvTituloEstado.setTextColor(ContextCompat.getColor(this, R.color.color_success))
            binding.tvMensajeEstado.text = "El registro se guardó correctamente en el dispositivo"
            binding.tvMensajeEstado.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            // Estado 02 - Tara por Defecto (Advertencia)
            binding.ivIconoEstado.setImageResource(R.drawable.ic_warning)
            binding.tvTituloEstado.text = "¡Pesaje Registrado con Advertencia!"
            binding.tvTituloEstado.setTextColor(ContextCompat.getColor(this, R.color.color_warning))
            binding.tvMensajeEstado.text = getString(R.string.tara_defecto_warning)
            binding.tvMensajeEstado.setTextColor(ContextCompat.getColor(this, R.color.color_warning))
        }
    }

    private fun activarFeedbackSensorial(esTaraCalibrada: Boolean) {
        // Al entrar a esta pantalla, activar sonido (ToneGenerator) y vibración (Vibrator)
        FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = !esTaraCalibrada)
    }

    private fun configurarNavegacion() {
        // Al presionar la X, se cierra esta pantalla y se vuelve al Menú Principal (no al pesaje)
        binding.btnCerrarX.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        // Al presionar "Siguiente Jornalero", se regresa a Registrar Pesaje y se limpian todos los campos
        binding.btnSiguienteJornalero.setOnClickListener {
            val intent = Intent(this, RegistrarPesajeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
