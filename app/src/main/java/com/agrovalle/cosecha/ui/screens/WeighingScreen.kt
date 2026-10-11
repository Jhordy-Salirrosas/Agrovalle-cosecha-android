package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.WeighingViewModel

/**
 * Pantalla 3.7: Registrar Pesaje (AGV-003).
 * - Visor QR pequeño con jornalero identificado.
 * - Campo "Peso Bruto (kg)" controlado por teclado numérico.
 * - Sección "Cantidad de Jabas" con botones +1, +4, +6.
 * - Cálculo explícito de Peso Neto = Peso Bruto – (Tara * Jabas).
 * - Botón "Guardar Pesaje" (solo si peso > 0 y jabas > 0).
 */
@Composable
fun WeighingScreen(
    viewModel: WeighingViewModel,
    onBack: () -> Unit,
    onPesajeGuardado: () -> Unit
) {
    val jornalero by viewModel.jornaleroEscaneado.collectAsState()
    val pesoBrutoTexto by viewModel.pesoBrutoTexto.collectAsState()
    val cantidadJabas by viewModel.cantidadJabas.collectAsState()
    val taraActual by viewModel.taraActual.collectAsState()
    val esTaraPorDefecto by viewModel.esTaraPorDefecto.collectAsState()
    val pesajeResultado by viewModel.pesajeResultado.collectAsState()

    val pesoBruto = pesoBrutoTexto.toDoubleOrNull() ?: 0.0
    val taraTotal = taraActual * cantidadJabas
    val pesoNeto = if (pesoBruto > taraTotal) pesoBruto - taraTotal else 0.0
    val formularioValido = pesoBruto > 0 && cantidadJabas > 0 && jornalero != null

    // Navegar a confirmación cuando se guarda
    LaunchedEffect(pesajeResultado) {
        if (pesajeResultado != null) {
            onPesajeGuardado()
        }
    }

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
                title = "Registrar Pesaje",
                onBack = onBack
            )

            // ── Visor QR / Fotocheck identificado ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                if (jornalero != null) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AgroSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fotocheck identificado: ${jornalero!!.nombreCompleto()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AgroTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "✓ Asistencia registrada",
                                style = MaterialTheme.typography.bodySmall,
                                color = AgroSuccess
                            )
                        }
                    }
                } else {
                    // Estado inicial: Esperando Escaneo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AgroTextHint,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Escanea el fotocheck para comenzar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AgroTextHint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Botón simulación de escaneo
            if (jornalero == null) {
                Spacer(modifier = Modifier.height(10.dp))
                BigButtonDark(
                    text = "📷  Simular Escaneo Fotocheck",
                    onClick = { viewModel.simularEscaneoFotocheck() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Peso Bruto ──
            Text(
                text = "Peso Bruto (kg)",
                style = MaterialTheme.typography.titleSmall,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                Text(
                    text = if (pesoBrutoTexto.isNotEmpty()) pesoBrutoTexto else "0.00",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pesoBrutoTexto.isNotEmpty()) AgroTextPrimary else AgroTextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cantidad de Jabas ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cantidad de Jabas",
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroTextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$cantidadJabas",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AgroPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(1, 4, 6).forEach { n ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AgroSurfaceVariant)
                            .border(1.dp, AgroButtonBorder, RoundedCornerShape(10.dp))
                            .clickable(enabled = jornalero != null) {
                                viewModel.agregarJabas(n)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$n",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroTextPrimary
                        )
                    }
                }

                // Botón resetear jabas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AgroSurfaceVariant)
                        .border(1.dp, AgroButtonBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.setJabas(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "C",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Detalle de Tara ──
            Text(
                text = "Tara aplicada: ${String.format("%.3f", taraActual)} kg por jaba" +
                       if (esTaraPorDefecto) " (por defecto)" else " (calibrada)",
                style = MaterialTheme.typography.bodySmall,
                color = if (esTaraPorDefecto) AgroWarning else AgroTextSecondary
            )
            Text(
                text = "Tara total: ${String.format("%.2f", taraTotal)} kg ($cantidadJabas × ${String.format("%.3f", taraActual)})",
                style = MaterialTheme.typography.bodySmall,
                color = AgroTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Peso Neto (cálculo explícito) ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSuccess.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Peso Neto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroTextSecondary
                    )
                    Text(
                        text = "${String.format("%.2f", pesoNeto)} kg",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgroSuccess
                    )
                    Text(
                        text = "${String.format("%.2f", pesoBruto)} - ${String.format("%.2f", taraTotal)} = ${String.format("%.2f", pesoNeto)} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Teclado numérico para peso ──
            NumericKeypad(
                onKeyPress = { viewModel.agregarDigitoPeso(it) },
                onDelete = { viewModel.borrarDigitoPeso() },
                showDecimal = true,
                enabled = jornalero != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón "Guardar Pesaje" ──
            BigButtonDark(
                text = "Guardar Pesaje",
                onClick = { viewModel.guardarPesaje() },
                enabled = formularioValido
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Al guardar, la pantalla se limpia para el siguiente jornalero en menos de 10 segundos.",
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
