package com.agrovalle.cosecha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.agrovalle.cosecha.data.repository.MockAgroValleRepository
import com.agrovalle.cosecha.navigation.NavGraph
import com.agrovalle.cosecha.ui.theme.AgroBackground
import com.agrovalle.cosecha.ui.theme.AgroValleCosechaTheme
import com.agrovalle.cosecha.ui.viewmodels.*

/**
 * Activity principal (Single Activity Architecture).
 * Toda la UI se maneja con Jetpack Compose y Navigation Compose.
 *
 * Sprint 1: Los ViewModels se crean manualmente con MockRepository.
 * TODO [Futuro]: Usar Hilt/Koin para inyección de dependencias
 *                cuando se escale a producción.
 */
class MainActivity : ComponentActivity() {

    // ── Repositorio Mock (será reemplazado por Room en Sprint 2) ──
    private val repository = MockAgroValleRepository()

    // ── ViewModels instanciados manualmente (sin DI por ahora) ──
    private val authViewModel by lazy { AuthViewModel(repository) }
    private val attendanceViewModel by lazy { AttendanceViewModel(repository) }
    private val harvestContextViewModel by lazy { HarvestContextViewModel(repository) }
    private val tareCalibrationViewModel by lazy { TareCalibrationViewModel(repository) }
    private val weighingViewModel by lazy { WeighingViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AgroValleCosechaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AgroBackground
                ) {
                    val navController = rememberNavController()

                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        attendanceViewModel = attendanceViewModel,
                        harvestContextViewModel = harvestContextViewModel,
                        tareCalibrationViewModel = tareCalibrationViewModel,
                        weighingViewModel = weighingViewModel
                    )
                }
            }
        }
    }
}
