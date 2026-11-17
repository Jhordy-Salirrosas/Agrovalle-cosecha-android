package com.agrovalle.cosecha.view

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.agrovalle.cosecha.R
import com.agrovalle.cosecha.databinding.ActivityIncidenciasBinding
import com.agrovalle.cosecha.viewmodel.IncidenciasViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla de listado de incidencias de mezcla (Sprint 2).
 */
class IncidenciasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidenciasBinding
    private val viewModel: IncidenciasViewModel by viewModels()
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidenciasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.incidencias.collect { lista ->
                    binding.containerIncidencias.removeAllViews()
                    binding.tvSinIncidencias.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE

                    lista.forEach { item ->
                        val tv = TextView(this@IncidenciasActivity).apply {
                            text = "Parihuela #${item.parihuelaId} • ${item.tipo}\n${item.detalle}\n${formatoFecha.format(item.fechaHora)}"
                            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                            textSize = 14f
                            setPadding(24, 20, 24, 20)
                            setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                        }
                        binding.containerIncidencias.addView(tv)
                    }
                }
            }
        }
    }
}
