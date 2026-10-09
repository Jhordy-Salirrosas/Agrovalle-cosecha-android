package com.dapm.android.agrovalle.ui.pesaje

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dapm.android.agrovalle.AgroValleApp
import com.dapm.android.agrovalle.R
import com.dapm.android.agrovalle.databinding.FragmentCalibracionTaraBinding
import com.dapm.android.agrovalle.util.SonidoVibracionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment de Calibración de Tara (AGV-015).
 * Permite ingresar el peso de 5 jabas vacías de muestra,
 * calcula el promedio y valida el rango [0.350 – 0.400] kg.
 */
class CalibracionTaraFragment : Fragment() {

    private var _binding: FragmentCalibracionTaraBinding? = null
    private val binding get() = _binding!!

    private lateinit var sonidoHelper: SonidoVibracionHelper

    private val viewModel: CalibracionTaraViewModel by viewModels {
        val db = (requireActivity().application as AgroValleApp).database
        CalibracionTaraViewModel.Factory(db.calibracionTaraDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalibracionTaraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sonidoHelper = SonidoVibracionHelper(requireContext())

        configurarToolbar()
        configurarBotonCalibrar()
        observarEstados()
    }

    private fun configurarToolbar() {
        binding.toolbarCalibracion.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarBotonCalibrar() {
        binding.btnCalibrar.setOnClickListener {
            val pesoTexto = binding.etPesoMuestra.text?.toString()?.trim()
            val peso = pesoTexto?.toDoubleOrNull()

            if (peso == null || peso <= 0) {
                binding.tilPesoMuestra.error = "Ingrese un peso válido"
                sonidoHelper.notificarError()
                return@setOnClickListener
            }

            binding.tilPesoMuestra.error = null
            viewModel.calibrarTara(peso)
        }
    }

    private fun observarEstados() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Resultado de calibración
                launch {
                    viewModel.resultadoCalibracion.collectLatest { resultado ->
                        if (resultado != null) {
                            binding.cardResultadoCalibracion.visibility = View.VISIBLE
                            binding.tvTaraPromedio.text = "${"%.3f".format(resultado.taraPromedio)} kg"

                            if (resultado.estaAprobado) {
                                binding.chipEstadoCalibracion.text = "✅ APROBADA"
                                binding.chipEstadoCalibracion.setChipBackgroundColorResource(R.color.primaryColor)
                                binding.chipEstadoCalibracion.setTextColor(
                                    resources.getColor(R.color.colorOnPrimary, null)
                                )
                                binding.tvTaraPromedio.setTextColor(
                                    resources.getColor(R.color.primaryColor, null)
                                )
                                binding.cardResultadoCalibracion.strokeColor =
                                    resources.getColor(R.color.primaryColor, null)
                            } else {
                                binding.chipEstadoCalibracion.text = "❌ RECHAZADA"
                                binding.chipEstadoCalibracion.setChipBackgroundColorResource(android.R.color.holo_red_dark)
                                binding.chipEstadoCalibracion.setTextColor(
                                    resources.getColor(R.color.colorOnPrimary, null)
                                )
                                binding.tvTaraPromedio.setTextColor(
                                    resources.getColor(android.R.color.holo_red_dark, null)
                                )
                                binding.cardResultadoCalibracion.strokeColor =
                                    resources.getColor(android.R.color.holo_red_dark, null)
                            }
                        } else {
                            binding.cardResultadoCalibracion.visibility = View.GONE
                        }
                    }
                }

                // Confirmación multisensorial
                launch {
                    viewModel.calibracionExitosa.collectLatest { aprobada ->
                        if (aprobada) {
                            sonidoHelper.confirmarExito()
                        } else {
                            sonidoHelper.alertarAdvertencia()
                        }
                    }
                }

                // Calibración vigente
                launch {
                    viewModel.calibracionVigente.collectLatest { calibracion ->
                        if (calibracion != null) {
                            val fecha = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm", Locale.getDefault()
                            ).format(Date(calibracion.marcaTiempo))
                            binding.tvCalibracionVigente.text =
                                "Tara: ${"%.3f".format(calibracion.taraPromedioCalculada)} kg · " +
                                "Asistente: ${calibracion.nombreAsistente}\n" +
                                "Fecha: $fecha"
                        } else {
                            binding.tvCalibracionVigente.text = "Sin calibración vigente"
                        }
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
