package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.WeighingViewModel
import com.agrovalle.cosecha.util.SonidoVibracionHelper

/**
 * Pantalla 3.8: Confirmación de Pesaje (AGV-004).
 *
 * Estado 01 – Registro Correcto: Check verde + "¡Pesaje Exitoso!"
 * Estado 02 – Tara por Defecto: Triángulo amarillo ⚠️ + advertencia.
 *
 * - Header con botón cerrar (X) → vuelve a Menú Principal.
 * - Peso Neto y Acumulado Diario destacados.
 * - Feedback sensorial: ToneGenerator + Vibrator al entrar.
 * - Botón "Siguiente Jornalero →" → vuelve a pesaje con campos limpios.
 */
@Composable
fun WeighingConfirmationScreen(
    viewModel: WeighingViewModel,
    onClose: () -> Unit,
    onSiguienteJornalero: () -> Unit
) {
    val pesajeResultado by viewModel.pesajeResultado.collectAsState()
    val context = LocalContext.current

    // ── Feedback sensorial al entrar (Regla 11) ──
    LaunchedEffect(Unit) {
        val helper = SonidoVibracionHelper(context)
        if (pesajeResultado?.esTaraPorDefecto == true) {
            helper.alertarAdvertencia()
        } else {
            helper.confirmarExito()
        }
    }

    val resultado = pesajeResultado

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
            // ── Header con botón X ──
            HeaderBar(
                title = "Pesaje Registrado",
                onClose = onClose
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Ícono de estado ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (resultado?.esTaraPorDefecto == true) {
                    // Estado 02: Tara por Defecto → Triángulo amarillo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AgroWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Advertencia",
                            tint = AgroWarning,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    // Estado 01: Registro Correcto → Check verde
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AgroSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = AgroSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Pesaje Exitoso!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AgroTextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "El registro se guardó correctamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AgroTextSecondary,
                    textAlign = TextAlign.Center
                )

                // Advertencia si tara por defecto
                if (resultado?.esTaraPorDefecto == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AgroWarning.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "⚠️ Pesaje registrado con tara por defecto (0.375 kg). Se recomienda calibrar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroWarning,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Tarjeta PESO NETO ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PESO NETO",
                        style = MaterialTheme.typography.labelMedium,
                        color = AgroTextSecondary
                    )
                    Text(
                        text = "${String.format("%.2f", resultado?.pesoNeto ?: 0.0)} kg",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroPrimary
                    )

                    if (resultado != null) {
                        Text(
                            text = "${resultado.jornaleroNombre} · ${resultado.cantidadJabas} jabas",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Tarjeta ACUMULADO DIARIO ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Σ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ACUMULADO DIARIO",
                            style = MaterialTheme.typography.labelMedium,
                            color = AgroTextSecondary
                        )
                        Text(
                            text = "${String.format("%.2f", resultado?.acumuladoDiario ?: 0.0)} kg",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Indicador sensorial ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Sonido",
                    tint = AgroTextHint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = "Vibración",
                    tint = AgroTextHint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sonido y vibración activados",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroTextHint
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Botón "Siguiente Jornalero →" ──
            BigButtonPrimary(
                text = "Siguiente Jornalero →",
                onClick = onSiguienteJornalero
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Texto "Volver a la pantalla de pesaje" ──
            Text(
                text = "Volver a la pantalla de pesaje",
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
