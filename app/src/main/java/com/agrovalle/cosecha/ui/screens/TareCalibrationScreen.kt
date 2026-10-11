package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.TareCalibrationViewModel

/**
 * Pantalla 3.6: Calibrar Tara (AGV-015).
 * - Indicador de progreso: 5 círculos numerados 1-5.
 * - Área de peso actual simulado.
 * - Botón "Registrar Peso" / "Omitir y usar tara por defecto".
 * - Tara Promedio y botón "Confirmar Calibración".
 */
@Composable
fun TareCalibrationScreen(
    viewModel: TareCalibrationViewModel,
    onBack: () -> Unit,
    onCalibrationDone: () -> Unit
) {
    val pesosRegistrados by viewModel.pesosRegistrados.collectAsState()
    val pasoActual by viewModel.pasoActual.collectAsState()
    val pesoActual by viewModel.pesoActualSimulado.collectAsState()
    val pesoEstable by viewModel.pesoEstable.collectAsState()
    val calibracionCompleta by viewModel.calibracionCompleta.collectAsState()
    val taraPromedio by viewModel.taraPromedio.collectAsState()

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
                title = "Calibrar Tara",
                onBack = onBack
            )

            // ── Instrucciones ──
            Text(
                text = "Pesa cinco jabas vacías",
                style = MaterialTheme.typography.titleMedium,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coloca una jaba vacía en la balanza.",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroTextSecondary
            )
            Text(
                text = "60 segundos o menos · Sin conexión",
                style = MaterialTheme.typography.bodySmall,
                color = AgroTextHint
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Indicador de progreso y contador ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jabas vacías",
                    style = MaterialTheme.typography.titleSmall,
                    color = AgroTextPrimary
                )
                Text(
                    text = "${pesosRegistrados.size} de 5 registradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgroTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ProgressSteps(
                totalSteps = 5,
                completedSteps = pesosRegistrados.size,
                currentStep = pasoActual
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Área de peso actual ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AgroSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Peso actual",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!calibracionCompleta) {
                        Text(
                            text = "Jaba ${pasoActual} de 5: ${String.format("%.3f", pesoActual)} kg",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (pesoEstable) "Peso listo para registrar"
                                   else "Espera a que el peso se estabilice",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pesoEstable) AgroSuccess else AgroWarning
                        )
                    } else {
                        Text(
                            text = "✓ Calibración completa",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgroSuccess,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Lista de pesos registrados ──
            if (pesosRegistrados.isNotEmpty()) {
                pesosRegistrados.forEachIndexed { index, peso ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jaba ${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroTextSecondary
                        )
                        Text(
                            text = "${String.format("%.3f", peso)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = AgroSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Botón "Registrar Peso" ──
            if (!calibracionCompleta) {
                BigButtonPrimary(
                    text = "Registrar Peso",
                    onClick = { viewModel.registrarPeso() },
                    enabled = pesoEstable
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Botón "Omitir" ──
                BigButtonOutlined(
                    text = "Omitir y usar tara por defecto (0.375 kg)",
                    onClick = {
                        viewModel.omitirCalibracion()
                        onCalibrationDone()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tara Promedio ──
            HorizontalDivider(color = AgroDivider, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tara Promedio: ${if (taraPromedio != null) String.format("%.3f", taraPromedio) else "--"} kg",
                style = MaterialTheme.typography.titleMedium,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Al confirmar las cinco jabas, el promedio se guardará como tara del turno.",
                style = MaterialTheme.typography.bodySmall,
                color = AgroTextHint
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Botón "Confirmar Calibración" ──
            BigButtonPrimary(
                text = "Confirmar Calibración",
                onClick = {
                    viewModel.confirmarCalibracion()
                    onCalibrationDone()
                },
                enabled = calibracionCompleta
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Footer offline ──
        OfflineFooter()
    }
}
