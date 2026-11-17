package com.agrovalle.cosecha.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.data.repository.ConsultaOrigenParihuela
import com.agrovalle.cosecha.databinding.ActivityConsultaOrigenBinding
import com.agrovalle.cosecha.viewmodel.ConsultaOrigenViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultaOrigenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConsultarOrigen.setOnClickListener {
            val codigo = binding.etCodigoConsulta.text?.toString().orEmpty()
            viewModel.consultarPorCodigo(codigo)
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
                        }

                        is ConsultaOrigenParihuela.Encontrado -> {
                            val o = resultado.origen
                            binding.tvResultadoOrigen.text = buildString {
                                appendLine("Código: ${o.codigo}")
                                appendLine("Estado: ${o.estado}")
                                appendLine("Variedad: ${o.variedad}")
                                appendLine("Cuartel: ${o.cuartel}")
                                appendLine("Jabas: ${o.conteoJabas} | Pisos: ${o.pisos}")
                                appendLine("Inicio: ${formatoFecha.format(o.horaInicio)}")
                                appendLine("Sellado: ${o.horaSellado?.let { formatoFecha.format(it) } ?: "Pendiente"}")
                                if (!o.motivoSelladoAnticipado.isNullOrBlank()) {
                                    append("Motivo sellado anticipado: ${o.motivoSelladoAnticipado}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
