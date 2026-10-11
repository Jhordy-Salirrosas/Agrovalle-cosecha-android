package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Botón grande ergonómico (56-64 dp) para uso en campo con guantes.
 * Variante PRIMARIA: fondo amarillo (#FFC107), texto oscuro.
 */
@Composable
fun BigButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AgroPrimary,
            contentColor = AgroOnPrimary,
            disabledContainerColor = AgroPrimary.copy(alpha = 0.3f),
            disabledContentColor = AgroOnPrimary.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Botón grande con fondo OSCURO y texto blanco.
 * Usado para acciones principales como "Escanear QR" y "Guardar Pesaje".
 */
@Composable
fun BigButtonDark(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AgroButtonDark,
            contentColor = AgroTextPrimary,
            disabledContainerColor = AgroButtonDark.copy(alpha = 0.4f),
            disabledContentColor = AgroTextPrimary.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Botón grande con BORDE (outlined), fondo transparente.
 * Usado para acciones secundarias como "Buscar por DNI".
 */
@Composable
fun BigButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) AgroButtonBorder else AgroButtonBorder.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AgroTextPrimary,
            disabledContentColor = AgroTextPrimary.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Botón pequeño para acciones dentro de listas (ej: "Confirmar Asistencia").
 */
@Composable
fun SmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = AgroButtonDark,
    contentColor: Color = AgroTextPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
