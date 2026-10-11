package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.AttendanceViewModel
import com.agrovalle.cosecha.util.SonidoVibracionHelper

/**
 * Pantalla 3.4: Búsqueda por DNI (AGV-016).
 * - Header: "Buscar Jornalero" + flecha de volver.
 * - Visor de 4 dígitos: _ _ _ _
 * - Teclado numérico grande: 0-9, "Borrar", "Buscar".
 * - Lista de resultados con foto, nombre y "Confirmar Asistencia".
 * - Estado vacío con mensaje informativo.
 */
@Composable
fun SearchByDNIScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit,
    onAsistenciaConfirmada: () -> Unit
) {
    val digitos by viewModel.digitosDni.collectAsState()
    val resultados by viewModel.resultadosBusqueda.collectAsState()
    val hasBuscado by viewModel.hasBuscado.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val esError by viewModel.esError.collectAsState()
    val context = LocalContext.current
    val helper = remember(context) { SonidoVibracionHelper(context) }

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
                title = "Buscar Jornalero",
                onBack = onBack
            )

            // ── Visor de DNI con instrucción ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ingresa el DNI del jornalero",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Visor de 4 dígitos: _ _ _ _
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val char = digitos.getOrNull(i)?.toString() ?: "—"
                            Text(
                                text = char,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (char == "—") AgroTextHint else AgroTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Teclado numérico ──
            NumericKeypad(
                onKeyPress = { viewModel.agregarDigito(it) },
                onDelete = { viewModel.borrarDigito() },
                onAction = { viewModel.buscarPorDni() },
                actionLabel = "Buscar",
                actionEnabled = digitos.isNotEmpty()
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

            // ── Resultados ──
            Text(
                text = "Resultados",
                style = MaterialTheme.typography.titleSmall,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (hasBuscado && resultados.isEmpty()) {
                // Estado vacío
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = AgroSurface)
                ) {
                    Text(
                        text = "No se encontraron jornaleros con esos dígitos. Verifica el DNI o intenta escanear el QR nuevamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroTextSecondary,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Lista de resultados
                resultados.forEach { jornalero ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = AgroSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Foto circular placeholder
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AgroSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = AgroTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = jornalero.nombreCompleto(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AgroTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "DNI: ${jornalero.dni}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AgroTextSecondary
                                )
                            }

                            SmallButton(
                                text = "Confirmar Asistencia",
                                onClick = {
                                    viewModel.confirmarAsistenciaPorDni(
                                        jornalero = jornalero,
                                        onSuccess = { helper.confirmarExito() },
                                        onError = { helper.notificarError() }
                                    )
                                    onAsistenciaConfirmada()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Botón "Cancelar" ──
            BigButtonOutlined(
                text = "Cancelar",
                onClick = onBack
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Volver a la pantalla de escaneo QR",
                style = MaterialTheme.typography.bodySmall,
                color = AgroTextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Footer offline ──
        OfflineFooter()
    }
}
