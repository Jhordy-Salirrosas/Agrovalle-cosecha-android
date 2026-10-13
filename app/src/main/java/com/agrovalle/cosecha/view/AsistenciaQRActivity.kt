package com.agrovalle.cosecha.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityAsistenciaQrBinding
import com.agrovalle.cosecha.model.Jornalero
import com.agrovalle.cosecha.util.FeedbackUtil
import com.agrovalle.cosecha.viewmodel.TurnoViewModel

class AsistenciaQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenciaQrBinding
    private val viewModel: TurnoViewModel by viewModels()

    private var indiceSimulado = 0

    // Launcher para recibir resultado de Búsqueda por DNI
    private val busquedaDniLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val nombre = result.data?.getStringExtra("EXTRA_NOMBRE_JORNALERO") ?: "Jornalero Registrado"
            val dni = result.data?.getStringExtra("EXTRA_DNI_JORNALERO") ?: ""
            registrarAsistenciaManual(nombre, dni)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsistenciaQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarEstado()
        configurarEventos()
    }

    private fun observarEstado() {
        viewModel.contadorAsistentes.observe(this) { cantidad ->
            binding.tvContadorCuadrilla.text = "$cantidad / ${TurnoViewModel.TOTAL_CUADRILLA}"
        }
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        // Botón Escanear QR (simula lectura de cámara)
        binding.btnEscanearQr.setOnClickListener {
            simularEscaneoQr()
        }

        // Botón Secundario: Buscar por DNI
        binding.btnBuscarPorDni.setOnClickListener {
            val intent = Intent(this, BusquedaDNIActivity::class.java)
            busquedaDniLauncher.launch(intent)
        }
    }

    private fun simularEscaneoQr() {
        // Feedback sensorial haptico y sonoro
        FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)

        val lista = viewModel.listaMockJornaleros
        val jornalero = lista[indiceSimulado % lista.size]
        indiceSimulado++

        viewModel.incrementarAsistentes()

        binding.tvUltimoJornaleroRegistrado.text = "✓ Registrado: ${jornalero.nombreCompleto}"
        binding.tvDetalleRegistro.text = "DNI: ${jornalero.dni} • Cuadrilla ${jornalero.cuadrillaCodigo} • Método: Código QR"
        binding.tvEstadoEscaneo.text = "Último escaneo exitoso: ${jornalero.codigoQr}"
    }

    private fun registrarAsistenciaManual(nombre: String, dni: String) {
        FeedbackUtil.ejecutarFeedbackConfirmacion(this, esAlerta = false)
        viewModel.incrementarAsistentes()

        binding.tvUltimoJornaleroRegistrado.text = "✓ Registrado: $nombre"
        binding.tvDetalleRegistro.text = "DNI: $dni • Método: Búsqueda Manual DNI"
        binding.tvEstadoEscaneo.text = "Asistencia confirmada vía teclado numérico"
    }
}
