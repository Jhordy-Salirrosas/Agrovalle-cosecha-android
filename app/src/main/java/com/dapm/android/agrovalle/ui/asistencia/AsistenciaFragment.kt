package com.dapm.android.agrovalle.ui.asistencia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dapm.android.agrovalle.AgroValleApp
import com.dapm.android.agrovalle.R
import com.dapm.android.agrovalle.databinding.FragmentAsistenciaBinding
import com.dapm.android.agrovalle.util.SonidoVibracionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment de Registro de Asistencia (AGV-001, AGV-016).
 * Permite identificar un jornalero por QR (fotocheck) o búsqueda DNI,
 * registrar su asistencia con clasificación PUNTUAL/TARDANZA,
 * y confirmar con retroalimentación multisensorial.
 */
class AsistenciaFragment : Fragment() {

    private var _binding: FragmentAsistenciaBinding? = null
    private val binding get() = _binding!!

    private lateinit var sonidoHelper: SonidoVibracionHelper

    private val viewModel: AsistenciaViewModel by viewModels {
        val db = (requireActivity().application as AgroValleApp).database
        AsistenciaViewModel.Factory(db.jornaleroDao(), db.asistenciaDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAsistenciaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sonidoHelper = SonidoVibracionHelper(requireContext())

        configurarToolbar()
        configurarBusquedaDni()
        configurarBotonQr()
        configurarBotonRegistrar()
        escucharResultadoQr()
        observarEstados()
    }

    private fun configurarToolbar() {
        binding.toolbarAsistencia.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarBusquedaDni() {
        binding.etDni.doAfterTextChanged { texto ->
            val dni = texto?.toString()?.trim() ?: ""
            if (dni.length == 8) {
                viewModel.buscarJornaleroPorDni(dni)
            } else {
                viewModel.limpiarSeleccion()
            }
        }
    }

    private fun configurarBotonQr() {
        binding.btnEscanearQr.setOnClickListener {
            findNavController().navigate(R.id.action_asistencia_to_escanerQr)
        }
    }

    private fun configurarBotonRegistrar() {
        binding.btnRegistrarAsistencia.setOnClickListener {
            viewModel.registrarAsistencia()
        }
    }

    /** Escucha el resultado QR devuelto por EscanerQrFragment. */
    private fun escucharResultadoQr() {
        setFragmentResultListener("resultado_qr") { _, bundle ->
            val codigoQr = bundle.getString("codigo_qr") ?: return@setFragmentResultListener
            viewModel.obtenerJornaleroPorQr(codigoQr)
        }
    }

    private fun observarEstados() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Jornalero encontrado
                launch {
                    viewModel.jornaleroEncontrado.collectLatest { jornalero ->
                        if (jornalero != null) {
                            binding.cardResultado.visibility = View.VISIBLE
                            binding.cardMensaje.visibility = View.GONE
                            binding.tvNombreJornalero.text = jornalero.nombreCompleto()
                            binding.tvDniJornalero.text = "DNI: ${jornalero.dni}"
                            binding.tvCuadrillaJornalero.text = "Cuadrilla: ${jornalero.codigoCuadrilla}"
                            binding.chipEstadoAsistencia.text = viewModel.obtenerEstadoActual()

                            val esPuntual = viewModel.obtenerEstadoActual() == "PUNTUAL"
                            binding.chipEstadoAsistencia.setChipBackgroundColorResource(
                                if (esPuntual) R.color.primaryColor else R.color.secondaryColor
                            )
                            binding.btnRegistrarAsistencia.isEnabled = true
                        } else {
                            binding.cardResultado.visibility = View.GONE
                            binding.btnRegistrarAsistencia.isEnabled = false
                        }
                    }
                }

                // Mensajes de UI
                launch {
                    viewModel.mensajeUi.collectLatest { mensaje ->
                        if (mensaje != null) {
                            binding.cardMensaje.visibility = View.VISIBLE
                            binding.tvMensaje.text = mensaje.texto

                            val colorFondo = when (mensaje.tipo) {
                                AsistenciaViewModel.TipoMensaje.EXITO -> "#E8F5E9"
                                AsistenciaViewModel.TipoMensaje.ADVERTENCIA -> "#FFF3E0"
                                AsistenciaViewModel.TipoMensaje.ERROR -> "#FFEBEE"
                            }
                            binding.cardMensaje.setCardBackgroundColor(
                                android.graphics.Color.parseColor(colorFondo)
                            )
                        } else {
                            binding.cardMensaje.visibility = View.GONE
                        }
                    }
                }

                // Confirmación multisensorial (AGV-004)
                launch {
                    viewModel.registroExitoso.collectLatest { resultado ->
                        if (resultado.exitoso) {
                            sonidoHelper.confirmarExito()
                            binding.etDni.text?.clear()
                        } else if (resultado.yaRegistrado) {
                            sonidoHelper.alertarAdvertencia()
                        }
                    }
                }

                // Contadores del día
                launch {
                    viewModel.contadorAsistencias.collectLatest { total ->
                        val puntuales = viewModel.contadorPuntuales.value
                        val tardanzas = viewModel.contadorTardanzas.value
                        binding.tvContadorDia.text =
                            "Registros hoy: $total (Puntuales: $puntuales · Tardanzas: $tardanzas)"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
