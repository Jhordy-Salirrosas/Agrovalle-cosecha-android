package com.agrovalle.cosecha.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.agrovalle.cosecha.databinding.ActivityMenuBinding
import com.agrovalle.cosecha.util.SesionTurno

/**
 * Pantalla 02 - Menu principal (hub por rol).
 * Lee el rol/jornada/fundo desde la sesion compartida, no de Intents, para que
 * el estado del turno no se pierda al navegar entre pantallas.
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rol = SesionTurno.rol.value ?: SesionTurno.ROL_CAPATAZ
        val jornada = SesionTurno.jornada.value ?: SesionTurno.JORNADA_MANANA_TXT
        val fundo = SesionTurno.fundo.value ?: "Fundo Principal Virú"

        configurarEncabezado(rol, jornada, fundo)
        configurarVisibilidadPorRol(rol)
        configurarNavegacion()
    }

    private fun configurarEncabezado(rol: String, jornada: String, fundo: String) {
        binding.tvRolActivo.text = "Operador: $rol"
        binding.tvDetalleTurno.text = "$jornada • $fundo"
    }

    private fun configurarVisibilidadPorRol(rol: String) {
        val esCapataz = rol == SesionTurno.ROL_CAPATAZ
        binding.layoutOpcionesCapataz.visibility = if (esCapataz) View.VISIBLE else View.GONE
        binding.layoutOpcionesAsistente.visibility = if (esCapataz) View.GONE else View.VISIBLE
    }

    private fun configurarNavegacion() {
        // Modulo Capataz
        binding.btnRegistrarAsistencia.setOnClickListener {
            startActivity(Intent(this, AsistenciaQRActivity::class.java))
        }
        binding.btnBuscarDni.setOnClickListener {
            startActivity(Intent(this, BusquedaDNIActivity::class.java))
        }

        // Modulo Asistente
        binding.btnContextoTurno.setOnClickListener {
            startActivity(Intent(this, ContextoAgronomicoActivity::class.java))
        }
        binding.btnRegistrarPesaje.setOnClickListener {
            startActivity(Intent(this, RegistrarPesajeActivity::class.java))
        }

        // Cerrar turno -> vuelve a Login limpiando la pila
        binding.btnCerrarTurno.setOnClickListener {
            SesionTurno.cerrar()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
