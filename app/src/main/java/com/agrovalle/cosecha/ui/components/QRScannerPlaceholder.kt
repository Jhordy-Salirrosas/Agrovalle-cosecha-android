package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Visor de cámara QR placeholder con borde discontinuo (dashed).
 * Simula el área de escaneo del fotocheck del jornalero.
 * Muestra ícono de cámara y texto instructivo.
 */
@Composable
fun QRScannerPlaceholder(
    modifier: Modifier = Modifier,
    message: String = "Coloca el código QR dentro del recuadro"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AgroSurfaceVariant)
            .border(
                width = 2.dp,
                color = AgroButtonBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cámara QR",
                tint = AgroTextSecondary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AgroTextSecondary
            )
        }
    }
}
