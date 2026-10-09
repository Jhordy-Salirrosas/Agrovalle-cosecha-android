package com.dapm.android.agrovalle.ui.asistencia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.dapm.android.agrovalle.databinding.FragmentEscanerQrBinding
import com.dapm.android.agrovalle.util.SonidoVibracionHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Fragment para escaneo de código QR del fotocheck del jornalero.
 * Usa CameraX para captura y ML Kit Barcode Scanning para decodificación.
 * Al detectar un QR válido, emite el resultado y regresa al fragment anterior.
 */
class EscanerQrFragment : Fragment() {

    private var _binding: FragmentEscanerQrBinding? = null
    private val binding get() = _binding!!

    private lateinit var sonidoHelper: SonidoVibracionHelper
    private var qrDetectado = false

    private val permisoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            iniciarCamara()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEscanerQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sonidoHelper = SonidoVibracionHelper(requireContext())

        binding.btnCancelarEscaneo.setOnClickListener {
            findNavController().navigateUp()
        }

        verificarPermisoCamara()
    }

    private fun verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamara()
        } else {
            permisoLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            // Análisis de imagen con ML Kit
            val analizador = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        AnalizadorQr { codigoQr ->
                            if (!qrDetectado) {
                                qrDetectado = true
                                activity?.runOnUiThread {
                                    procesarCodigoQr(codigoQr)
                                }
                            }
                        }
                    )
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analizador
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun procesarCodigoQr(codigoQr: String) {
        sonidoHelper.beepEscaneo()
        setFragmentResult("resultado_qr", bundleOf("codigo_qr" to codigoQr))
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Analizador de imagen que usa ML Kit para detectar códigos QR.
     */
    private class AnalizadorQr(
        private val alDetectar: (String) -> Unit
    ) : ImageAnalysis.Analyzer {

        private val escaner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                escaner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_TEXT ||
                                barcode.valueType == Barcode.TYPE_UNKNOWN
                            ) {
                                barcode.rawValue?.let { valor ->
                                    alDetectar(valor)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
}
