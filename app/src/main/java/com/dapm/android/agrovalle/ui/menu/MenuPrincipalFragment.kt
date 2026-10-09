package com.dapm.android.agrovalle.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.dapm.android.agrovalle.AgroValleApp
import com.dapm.android.agrovalle.R
import com.dapm.android.agrovalle.databinding.FragmentMenuPrincipalBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Fragment del Menú Principal de la aplicación.
 * Muestra información del turno activo, contadores rápidos
 * y botones de navegación a Asistencia, Pesaje y Calibración.
 */
class MenuPrincipalFragment : Fragment() {

    private var _binding: FragmentMenuPrincipalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuPrincipalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = (requireActivity().application as AgroValleApp).database
        val fechaHoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // ─── Navegación ─────────────────────────────────────
        binding.btnAsistencia.setOnClickListener {
            findNavController().navigate(R.id.action_menu_to_asistencia)
        }
        binding.btnPesaje.setOnClickListener {
            findNavController().navigate(R.id.action_menu_to_pesaje)
        }
        binding.btnCalibracion.setOnClickListener {
            findNavController().navigate(R.id.action_menu_to_calibracion)
        }

        // ─── Cargar info del turno ──────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            val turno = db.turnoContextoDao().obtenerTurnoAbierto()
            if (turno != null) {
                binding.tvInfoTurno.text = "Fundo ${turno.fundo} · ${turno.cuartel} · ${turno.variedad}"
                binding.tvCapataz.text = "Capataz: ${turno.nombreCapataz}"
            }
        }

        // ─── Contadores reactivos ───────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            db.asistenciaDao().contarAsistenciasDelDia(fechaHoy).collectLatest { total ->
                binding.tvContadorAsistencias.text = total.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.parihuelaDao().observarParihuelaAbierta().collectLatest { parihuela ->
                if (parihuela != null) {
                    binding.tvContadorPesajes.text = "${parihuela.totalJabas}/120"
                } else {
                    binding.tvContadorPesajes.text = "—"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
