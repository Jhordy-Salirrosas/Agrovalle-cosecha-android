package com.dapm.android.agrovalle.ui.pesaje

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dapm.android.agrovalle.AgroValleApp
import com.dapm.android.agrovalle.R
import com.dapm.android.agrovalle.databinding.FragmentPesajeBinding
import com.dapm.android.agrovalle.util.SonidoVibracionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment de Pesaje Ágil de Jabas (AGV-002, AGV-003, AGV-004).
 * Permite seleccionar jornalero, cantidad de jabas (3, 4 o 6),
 * ingresar peso bruto, y el sistema calcula automáticamente
 * la tara y el peso neto. Confirmación con sonido + vibración.
 */
class PesajeFragment : Fragment() {

    private var _binding: FragmentPesajeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sonidoHelper: SonidoVibracionHelper

    private val viewModel: PesajeViewModel by viewModels {
        val db = (requireActivity().application as AgroValleApp).database
        PesajeViewModel.Factory(
            db.jornaleroDao(),
            db.parihuelaDao(),
            db.entregaPesajeDao(),
            db.calibracionTaraDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesajeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sonidoHelper = SonidoVibracionHelper(requireContext())

        configurarToolbar()
        configurarBusquedaDni()
        configurarSelectorJabas()
        configurarPesoBruto()
        configurarObservaciones()
        configurarBotonGuardar()
        observarEstados()
    }

    private fun configurarToolbar() {
        binding.toolbarPesaje.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun configurarBusquedaDni() {
        binding.etDniPesaje.doAfterTextChanged { texto ->
            val dni = texto?.toString()?.trim() ?: ""
            if (dni.length == 8) {
                viewModel.buscarJornaleroPorDni(dni)
            }
        }
    }

    private fun configurarSelectorJabas() {
        binding.toggleJabas.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val cantidad = when (checkedId) {
                    R.id.btn3Jabas -> 3
                    R.id.btn4Jabas -> 4
                    R.id.btn6Jabas -> 6
                    else -> 4
                }
                viewModel.seleccionarCantidadJabas(cantidad)
            }
        }
    }

    private fun configurarPesoBruto() {
        binding.etPesoBruto.doAfterTextChanged { texto ->
            val peso = texto?.toString()?.toDoubleOrNull() ?: 0.0
            viewModel.establecerPesoBruto(peso)
        }
    }

    private fun configurarObservaciones() {
        // Adapter para motivos de observación
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            viewModel.motivosObservacion
        )
        binding.actvMotivo.setAdapter(adapter)

        // Toggle visibilidad del campo motivo
        binding.switchObservado.setOnCheckedChangeListener { _, isChecked ->
            binding.tilMotivoObservacion.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun configurarBotonGuardar() {
        binding.btnGuardarPesaje.setOnClickListener {
            val estaObservado = binding.switchObservado.isChecked
            val motivo = if (estaObservado) {
                binding.actvMotivo.text?.toString()?.takeIf { it.isNotBlank() }
            } else null

            viewModel.guardarPesaje(estaObservado, motivo)
        }
    }

    private fun observarEstados() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Parihuela activa
                launch {
                    viewModel.parihuelaActiva.collectLatest { parihuela ->
                        if (parihuela != null) {
                            binding.tvInfoParihuela.text =
                                "${parihuela.codigoMarbete} · ${parihuela.cuartel} · ${parihuela.variedad}"
                            binding.tvJabasParihuela.text =
                                "Jabas: ${parihuela.totalJabas}/120 · Peso neto: ${"%.2f".format(parihuela.pesoNetoTotal)} kg"
                        }
                    }
                }

                // Jornalero seleccionado
                launch {
                    viewModel.jornaleroSeleccionado.collectLatest { jornalero ->
                        if (jornalero != null) {
                            binding.tvJornaleroEncontrado.visibility = View.VISIBLE
                            binding.tvJornaleroEncontrado.text =
                                "✅ ${jornalero.nombreCompleto()} (${jornalero.codigoCuadrilla})"
                        } else {
                            binding.tvJornaleroEncontrado.visibility = View.GONE
                        }
                    }
                }

                // Cálculos automáticos de tara y peso neto
                launch {
                    viewModel.calculos.collectLatest { calc ->
                        if (calc != null) {
                            binding.cardCalculos.visibility = View.VISIBLE
                            binding.tvTaraTotal.text =
                                "Tara total: ${calc.cantidadJabas} × ${calc.taraPorJaba} = ${"%.3f".format(calc.taraTotal)} kg"
                            binding.tvPesoNeto.text =
                                "PESO NETO: ${"%.3f".format(calc.pesoNeto)} kg"

                            // Habilitar botón solo si hay jornalero y peso neto > 0
                            binding.btnGuardarPesaje.isEnabled =
                                viewModel.jornaleroSeleccionado.value != null && calc.pesoNeto > 0
                        } else {
                            binding.cardCalculos.visibility = View.GONE
                            binding.btnGuardarPesaje.isEnabled = false
                        }
                    }
                }

                // Confirmación multisensorial (AGV-004)
                launch {
                    viewModel.pesajeGuardado.collectLatest { resultado ->
                        if (resultado.exitoso) {
                            sonidoHelper.confirmarExito()
                            binding.etDniPesaje.text?.clear()
                            binding.etPesoBruto.text?.clear()
                            binding.switchObservado.isChecked = false
                        } else {
                            sonidoHelper.notificarError()
                        }
                    }
                }

                // Mensajes de UI
                launch {
                    viewModel.mensajeUi.collectLatest { mensaje ->
                        // Se podría mostrar un Snackbar aquí
                        if (mensaje != null) {
                            com.google.android.material.snackbar.Snackbar.make(
                                binding.root,
                                mensaje.texto,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).show()
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
