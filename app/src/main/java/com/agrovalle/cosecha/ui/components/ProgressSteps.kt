package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Indicador horizontal de progreso con 5 pasos circulares numerados.
 * Usado en la calibración de tara (5 jabas vacías).
 * Los pasos completados muestran un check verde.
 */
@Composable
fun ProgressSteps(
    totalSteps: Int = 5,
    completedSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            val isCompleted = step <= completedSteps
            val isCurrent = step == currentStep

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> AgroSuccess
                            isCurrent -> AgroPrimary
                            else -> AgroSurfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completado",
                        tint = AgroTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "$step",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) AgroOnPrimary else AgroTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
