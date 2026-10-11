package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.data.model.Rol
import com.agrovalle.cosecha.ui.components.BigButtonOutlined
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.AuthViewModel

/**
 * Pantalla 3.2: Menú Principal.
 * Muestra botones grandes según el rol seleccionado.
 * - Capataz: Registrar Asistencia, Buscar por DNI, Panel Resumen (opcional).
 * - Asistente: Contexto Agronómico, Calibrar Tara, Registrar Pesaje.
 * - Chofer: Próximo Sprint.
 * Botón "Cerrar Turno" → vuelve a Login.
 */
@Composable
fun MainMenuScreen(
    authViewModel: AuthViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSearchDni: () -> Unit,
    onNavigateToAgronomicContext: () -> Unit,
    onNavigateToTareCalibration: () -> Unit,
    onNavigateToWeighing: () -> Unit,
    onCerrarTurno: () -> Unit
) {
    val rol by authViewModel.rolSeleccionado.collectAsState()
    val jornada by authViewModel.jornadaSeleccionada.collectAsState()
    val fundo by authViewModel.fundoSeleccionado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── Encabezado con info de sesión ──
        Text(
            text = "AgroValle Cosecha",
            style = MaterialTheme.typography.headlineMedium,
            color = AgroTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${rol?.displayName ?: "—"} · ${jornada?.displayName ?: "—"} · ${fundo ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = AgroTextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Menú Principal",
            style = MaterialTheme.typography.titleLarge,
            color = AgroTextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Botones según rol ──
        when (rol) {
            Rol.CAPATAZ -> {
                MenuCard(
                    title = "Registrar Asistencia",
                    subtitle = "Escanear QR de fotochecks",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = onNavigateToAttendance
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuCard(
                    title = "Buscar por DNI",
                    subtitle = "Búsqueda manual de jornaleros",
                    icon = Icons.Default.Search,
                    onClick = onNavigateToSearchDni
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuCard(
                    title = "Panel Resumen",
                    subtitle = "Vista general del turno (próximamente)",
                    icon = Icons.Default.Dashboard,
                    onClick = { /* Sprint 2 */ },
                    enabled = false
                )
            }

            Rol.ASISTENTE_PESAJE -> {
                MenuCard(
                    title = "Contexto Agronómico",
                    subtitle = "Cuartel, CLP y variedad",
                    icon = Icons.Default.Eco,
                    onClick = onNavigateToAgronomicContext
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuCard(
                    title = "Calibrar Tara",
                    subtitle = "Pesar 5 jabas vacías",
                    icon = Icons.Default.Scale,
                    onClick = onNavigateToTareCalibration
                )
                Spacer(modifier = Modifier.height(14.dp))
                MenuCard(
                    title = "Registrar Pesaje",
                    subtitle = "Pesar cosecha por jornalero",
                    icon = Icons.Default.MonitorWeight,
                    onClick = onNavigateToWeighing
                )
            }

            Rol.CHOFER -> {
                MenuCard(
                    title = "Módulo Chofer",
                    subtitle = "Disponible en próximo Sprint",
                    icon = Icons.Default.LocalShipping,
                    onClick = { },
                    enabled = false
                )
            }

            null -> {
                Text(
                    text = "No se ha seleccionado un rol.",
                    color = AgroTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Botón "Cerrar Turno" ──
        Spacer(modifier = Modifier.height(32.dp))

        BigButtonOutlined(
            text = "Cerrar Turno",
            onClick = onCerrarTurno
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Tarjeta de menú con ícono, título y subtítulo.
 */
@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = AgroSurface,
            disabledContainerColor = AgroSurface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgroSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) AgroPrimary else AgroTextHint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) AgroTextPrimary else AgroTextHint,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AgroTextHint
            )
        }
    }
}
