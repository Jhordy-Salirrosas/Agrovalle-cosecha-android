package com.agrovalle.cosecha.navigation

/**
 * Rutas de navegación tipadas para Compose Navigation.
 * Cada pantalla del Sprint 1 tiene su ruta única.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object MainMenu : Screen("main_menu")
    object AttendanceQR : Screen("attendance_qr")
    object SearchByDNI : Screen("search_dni")
    object AgronomicContext : Screen("agronomic_context")
    object TareCalibration : Screen("tare_calibration")
    object Weighing : Screen("weighing")
    object WeighingConfirmation : Screen("weighing_confirmation")
}
