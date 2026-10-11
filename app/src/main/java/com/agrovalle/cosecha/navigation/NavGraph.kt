package com.agrovalle.cosecha.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agrovalle.cosecha.ui.screens.*
import com.agrovalle.cosecha.ui.viewmodels.*

/**
 * Grafo de navegación principal de AgroValle Cosecha.
 *
 * Flujo:
 *   Login → MainMenu
 *     ├── (Capataz)    → AttendanceQR ↔ SearchByDNI
 *     └── (Asistente)  → AgronomicContext → TareCalibration → Weighing → WeighingConfirmation
 *                                                                ↑              │
 *                                                                └── Siguiente ──┘
 *                                                                │
 *                                                                └── X → MainMenu
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    attendanceViewModel: AttendanceViewModel,
    harvestContextViewModel: HarvestContextViewModel,
    tareCalibrationViewModel: TareCalibrationViewModel,
    weighingViewModel: WeighingViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // ── 1. Login / Selección de Rol ──
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onIniciarTurno = {
                    navController.navigate(Screen.MainMenu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── 2. Menú Principal ──
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                authViewModel = authViewModel,
                onNavigateToAttendance = {
                    navController.navigate(Screen.AttendanceQR.route)
                },
                onNavigateToSearchDni = {
                    navController.navigate(Screen.SearchByDNI.route)
                },
                onNavigateToAgronomicContext = {
                    navController.navigate(Screen.AgronomicContext.route)
                },
                onNavigateToTareCalibration = {
                    navController.navigate(Screen.TareCalibration.route)
                },
                onNavigateToWeighing = {
                    navController.navigate(Screen.Weighing.route)
                },
                onCerrarTurno = {
                    authViewModel.cerrarTurno()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── 3. Asistencia QR (AGV-001) ──
        composable(Screen.AttendanceQR.route) {
            AttendanceQRScreen(
                viewModel = attendanceViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSearchDni = {
                    navController.navigate(Screen.SearchByDNI.route)
                }
            )
        }

        // ── 4. Búsqueda por DNI (AGV-016) ──
        composable(Screen.SearchByDNI.route) {
            SearchByDNIScreen(
                viewModel = attendanceViewModel,
                onBack = {
                    attendanceViewModel.limpiarBusqueda()
                    navController.popBackStack()
                },
                onAsistenciaConfirmada = {
                    attendanceViewModel.limpiarBusqueda()
                    // Volver a la pantalla de asistencia QR
                    navController.popBackStack()
                }
            )
        }

        // ── 5. Contexto del Turno (AGV-002) ──
        composable(Screen.AgronomicContext.route) {
            AgronomicContextScreen(
                viewModel = harvestContextViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── 6. Calibrar Tara (AGV-015) ──
        composable(Screen.TareCalibration.route) {
            TareCalibrationScreen(
                viewModel = tareCalibrationViewModel,
                onBack = { navController.popBackStack() },
                onCalibrationDone = {
                    navController.popBackStack()
                }
            )
        }

        // ── 7. Registrar Pesaje (AGV-003) ──
        composable(Screen.Weighing.route) {
            WeighingScreen(
                viewModel = weighingViewModel,
                onBack = { navController.popBackStack() },
                onPesajeGuardado = {
                    navController.navigate(Screen.WeighingConfirmation.route)
                }
            )
        }

        // ── 8. Confirmación de Pesaje (AGV-004) ──
        composable(Screen.WeighingConfirmation.route) {
            WeighingConfirmationScreen(
                viewModel = weighingViewModel,
                onClose = {
                    // X → vuelve al Menú Principal (no al pesaje)
                    weighingViewModel.limpiarParaSiguiente()
                    navController.popBackStack(Screen.MainMenu.route, inclusive = false)
                },
                onSiguienteJornalero = {
                    // Siguiente Jornalero → limpia y vuelve a pesaje
                    weighingViewModel.limpiarParaSiguiente()
                    navController.popBackStack(Screen.Weighing.route, inclusive = false)
                }
            )
        }
    }
}
