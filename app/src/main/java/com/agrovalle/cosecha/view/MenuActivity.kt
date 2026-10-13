package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityMenuBinding
import com.agrovalle.cosecha.viewmodel.TurnoViewModel

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private val viewModel: TurnoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rol = intent.getStringExtra(LoginActivity.EXTRA_ROL) ?: LoginActivity.ROL_CAPATAZ
        val jornada = intent.getStringExtra(LoginActivity.EXTRA_JORNADA) ?: "Jornada Mañana"
        val fundo = intent.getStringExtra(LoginActivity.EXTRA_FUNDO) ?: "Fundo Principal Virú"

        viewModel.setContextoLogin(rol, jornada, fundo)

        configurarEncabezado(rol, jornada, fundo)
        configurarVisibilidadPorRol(rol)
        configurarNavegacion(rol)
    }

    private fun configurarEncabezado(rol: String, jornada: String, fundo: String) {
        binding.tvRolActivo.text = "Operador: $rol de Cuadrilla"
        binding.tvDetalleTurno.text = "$jornada • $fundo"
    }

    private fun configurarVisibilidadPorRol(rol: String) {
        if (rol == LoginActivity.ROL_CAPATAZ) {
            binding.layoutOpcionesCapataz.visibility = View.VISIBLE
            binding.layoutOpcionesAsistente.visibility = View.GONE
        } else {
            binding.layoutOpcionesCapataz.visibility = View.GONE
            binding.layoutOpcionesAsistente.visibility = View.VISIBLE
        }
    }

    private fun configurarNavegacion(rol: String) {
        // Módulo Capataz
        binding.btnRegistrarAsistencia.setOnClickListener {
            val intent = Intent(this, AsistenciaQRActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_ROL, rol)
            }
            startActivity(intent)
        }

        binding.btnBuscarDni.setOnClickListener {
            val intent = Intent(this, BusquedaDNIActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_ROL, rol)
            }
            startActivity(intent)
        }

        // Módulo Asistente
        binding.btnContextoTurno.setOnClickListener {
            val intent = Intent(this, ContextoAgronomicoActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_ROL, rol)
            }
            startActivity(intent)
        }

        binding.btnRegistrarPesaje.setOnClickListener {
            val intent = Intent(this, RegistrarPesajeActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_ROL, rol)
            }
            startActivity(intent)
        }

        // Cerrar Turno -> vuelve a Login
        binding.btnCerrarTurno.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
