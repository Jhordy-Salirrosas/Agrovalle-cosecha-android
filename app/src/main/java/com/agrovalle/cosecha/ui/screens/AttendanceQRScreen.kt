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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.AttendanceViewModel

/**
 * Pantalla 3.3: Asistencia QR (AGV-001).
 * - Header: "Asistencia - Cuadrilla A" + flecha de volver.
 * - Contador: "Trabajadores registrados 0/35".
 * - Visor QR con borde punteado.
 * - Botón "Escanear QR" (negro, grande).
 * - Botón "Buscar por DNI" (blanco con borde).
 * - Área "Último registro".
 * - Footer "Sin conexión".
 */
@Composable
fun AttendanceQRScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit,
    onNavigateToSearchDni: () -> Unit
) {
    val contador by viewModel.contadorAsistencia.collectAsState()
    val ultimoRegistro by viewModel.ultimoRegistro.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val esError by viewModel.esError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ── Header ──
            HeaderBar(
                title = "Asistencia - Cuadrilla A",
                onBack = onBack
            )

            // ── Contador de trabajadores registrados ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trabajadores registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroTextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$contador/${viewModel.totalTrabajadores}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sonido",
                        tint = AgroTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Vibración",
                        tint = AgroTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Visor QR placeholder ──
            QRScannerPlaceholder()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón "Escanear QR" ──
            BigButtonDark(
                text = "📷  Escanear QR",
                onClick = { viewModel.simularEscaneoQr() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Botón "Buscar por DNI" ──
            BigButtonOutlined(
                text = "🔍  Buscar por DNI",
                onClick = onNavigateToSearchDni
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Mensaje de feedback ──
            if (mensaje != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (esError) AgroError.copy(alpha = 0.15f)
                                         else AgroSuccess.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = mensaje ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (esError) AgroError else AgroSuccess,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Último registro ──
            Text(
                text = "Último registro",
                style = MaterialTheme.typography.titleSmall,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                if (ultimoRegistro != null) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(AgroSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AgroTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = ultimoRegistro!!.nombreCompleto,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AgroTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${ultimoRegistro!!.horaRegistro} · ${ultimoRegistro!!.tipoIngreso.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroTextSecondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Aún no hay registros",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroTextHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Footer offline ──
        OfflineFooter()
    }
}
