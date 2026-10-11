package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrovalle.cosecha.data.model.Variedad
import com.agrovalle.cosecha.ui.components.*
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.HarvestContextViewModel

/**
 * Pantalla 3.5: Contexto del Turno (AGV-002).
 * Estado 1 (Sin Parihuela Abierta):
 *   - Selectores de Cuartel, CLP, Variedad.
 *   - Botón "Abrir Parihuela" (deshabilitado hasta seleccionar los 3 campos).
 * Estado 2 (Parihuela Abierta):
 *   - Banner verde con info de parihuela activa.
 *   - Selectores bloqueados. Botón "Cerrar Parihuela".
 */
@Composable
fun AgronomicContextScreen(
    viewModel: HarvestContextViewModel,
    onBack: () -> Unit,
    onNavigateToTareCalibration: () -> Unit = {}
) {
    val cuartelSeleccionado by viewModel.cuartelSeleccionado.collectAsState()
    val clpSeleccionado by viewModel.clpSeleccionado.collectAsState()
    val variedadSeleccionada by viewModel.variedadSeleccionada.collectAsState()
    val parihuelaActiva by viewModel.parihuelaActiva.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()

    val hayParihuelaAbierta = parihuelaActiva != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // ── Header ──
        HeaderBar(
            title = "Contexto del Turno",
            onBack = onBack
        )

        // ── Estado de parihuela ──
        if (hayParihuelaAbierta) {
            // Banner verde: Parihuela Activa
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AgroSuccess.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Parihuela Activa",
                        style = MaterialTheme.typography.titleSmall,
                        color = AgroSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${parihuelaActiva!!.codigo} - ${parihuelaActiva!!.cuartel} - ${parihuelaActiva!!.variedad.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AgroSuccess
                    )
                }
            }
        } else {
            // Texto estado sin parihuela
            Text(
                text = "Sin parihuela abierta",
                style = MaterialTheme.typography.titleMedium,
                color = AgroTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Selecciona el contexto de cosecha.",
                style = MaterialTheme.typography.bodyMedium,
                color = AgroTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Selector de Cuartel ──
        DropdownSelector(
            label = "Cuartel",
            selectedValue = if (hayParihuelaAbierta) parihuelaActiva?.cuartel else cuartelSeleccionado,
            options = viewModel.cuarteles,
            onOptionSelected = { viewModel.seleccionarCuartel(it) },
            enabled = !hayParihuelaAbierta,
            placeholder = "Seleccionar cuartel"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Selector de CLP ──
        DropdownSelector(
            label = "CLP",
            selectedValue = if (hayParihuelaAbierta) parihuelaActiva?.clp else clpSeleccionado,
            options = viewModel.clps,
            onOptionSelected = { viewModel.seleccionarClp(it) },
            enabled = !hayParihuelaAbierta,
            placeholder = "Seleccionar CLP vigente"
        )

        Text(
            text = "Código de Lugar de Producción vigente.",
            style = MaterialTheme.typography.bodySmall,
            color = AgroTextHint,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Selector de Variedad (radio buttons) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Variedad",
                style = MaterialTheme.typography.titleSmall,
                color = AgroTextPrimary
            )
            Text(
                text = "Selecciona una",
                style = MaterialTheme.typography.bodySmall,
                color = AgroTextHint
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Variedad.entries.forEach { variedad ->
                val currentVariedad = if (hayParihuelaAbierta) parihuelaActiva?.variedad else variedadSeleccionada
                val isSelected = currentVariedad == variedad

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AgroSurface else AgroBackground)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) AgroTextPrimary else AgroButtonBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .then(
                            if (!hayParihuelaAbierta) Modifier.clickable {
                                viewModel.seleccionarVariedad(variedad)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = if (!hayParihuelaAbierta) {
                                { viewModel.seleccionarVariedad(variedad) }
                            } else null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AgroTextPrimary,
                                unselectedColor = AgroTextHint
                            )
                        )
                        Text(
                            text = variedad.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (hayParihuelaAbierta) AgroTextHint else AgroTextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Advertencia ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = AgroSurface)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AgroWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Si hay una parihuela abierta, no se puede cambiar el contexto",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Debes sellar la parihuela abierta antes de cambiar el contexto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AgroTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Mensaje de feedback ──
        if (mensaje != null) {
            Text(
                text = mensaje ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = AgroSuccess,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Al abrir la parihuela, se guarda el contexto y se asigna automáticamente a cada pesaje.",
            style = MaterialTheme.typography.bodySmall,
            color = AgroTextHint
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Botón Abrir / Cerrar Parihuela ──
        if (hayParihuelaAbierta) {
            BigButtonPrimary(
                text = "Cerrar Parihuela",
                onClick = { viewModel.cerrarParihuela() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            BigButtonOutlined(
                text = "Continuar a Calibrar Tara →",
                onClick = onNavigateToTareCalibration
            )
        } else {
            BigButtonPrimary(
                text = "Abrir Parihuela",
                onClick = {
                    viewModel.abrirParihuela(onSuccess = onNavigateToTareCalibration)
                },
                enabled = viewModel.formularioCompleto
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
