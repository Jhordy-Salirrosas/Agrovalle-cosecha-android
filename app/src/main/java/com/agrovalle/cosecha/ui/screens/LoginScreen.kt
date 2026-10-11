package com.agrovalle.cosecha.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.data.model.Jornada
import com.agrovalle.cosecha.data.model.Rol
import com.agrovalle.cosecha.ui.components.BigButtonPrimary
import com.agrovalle.cosecha.ui.components.DropdownSelector
import com.agrovalle.cosecha.ui.theme.*
import com.agrovalle.cosecha.ui.viewmodels.AuthViewModel

/**
 * Pantalla 3.1: Login / Selección de Rol.
 * - Selector de rol: Capataz, Asistente de Pesaje, Chofer.
 * - Selector de jornada: Mañana, Tarde, Noche.
 * - Selector de fundo/sector.
 * - Botón grande "Iniciar Turno".
 * - No tiene botón de volver.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onIniciarTurno: () -> Unit
) {
    val rolSeleccionado by viewModel.rolSeleccionado.collectAsState()
    val jornadaSeleccionada by viewModel.jornadaSeleccionada.collectAsState()
    val fundoSeleccionado by viewModel.fundoSeleccionado.collectAsState()
    val sectorSeleccionado by viewModel.sectorSeleccionado.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgroBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // ── Logo / Título ──
        Icon(
            imageVector = Icons.Default.Agriculture,
            contentDescription = "AgroValle",
            tint = AgroPrimary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AgroValle Cosecha",
            style = MaterialTheme.typography.headlineLarge,
            color = AgroTextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Sistema de control de cosecha",
            style = MaterialTheme.typography.bodyMedium,
            color = AgroTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // ── Selector de Rol ──
        Text(
            text = "Selecciona tu rol",
            style = MaterialTheme.typography.titleSmall,
            color = AgroTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Rol.entries.forEach { rol ->
                val isSelected = rolSeleccionado == rol
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AgroPrimary else AgroSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AgroPrimary else AgroButtonBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.seleccionarRol(rol) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rol.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AgroOnPrimary else AgroTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Selector de Jornada ──
        Text(
            text = "Jornada",
            style = MaterialTheme.typography.titleSmall,
            color = AgroTextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Jornada.entries.forEach { jornada ->
                val isSelected = jornadaSeleccionada == jornada
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AgroPrimary else AgroSurface)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AgroPrimary else AgroButtonBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.seleccionarJornada(jornada) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = jornada.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AgroOnPrimary else AgroTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Selector de Fundo ──
        DropdownSelector(
            label = "Fundo",
            selectedValue = fundoSeleccionado,
            options = viewModel.fundos,
            onOptionSelected = { viewModel.seleccionarFundo(it) },
            placeholder = "Seleccionar fundo"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Selector de Sector ──
        DropdownSelector(
            label = "Sector",
            selectedValue = sectorSeleccionado,
            options = viewModel.sectores,
            onOptionSelected = { viewModel.seleccionarSector(it) },
            placeholder = "Seleccionar sector"
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Botón "Iniciar Turno" ──
        BigButtonPrimary(
            text = "Iniciar Turno",
            onClick = {
                viewModel.iniciarTurno()
                onIniciarTurno()
            },
            enabled = viewModel.formularioValido
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
