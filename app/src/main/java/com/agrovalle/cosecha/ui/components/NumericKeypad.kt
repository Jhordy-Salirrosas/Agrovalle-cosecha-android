package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Teclado numérico táctil grande optimizado para uso con guantes en campo.
 * Variantes:
 * - DNI: teclas 0-9, "Borrar", "Buscar"
 * - Pesaje: teclas 0-9, ".", "⌫"
 */
@Composable
fun NumericKeypad(
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onAction: (() -> Unit)? = null,
    actionLabel: String = "Buscar",
    actionEnabled: Boolean = true,
    showDecimal: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val keys = if (showDecimal) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )
    } else {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("Borrar", "0", actionLabel)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val isSpecial = key == "Borrar" || key == "⌫"
                    val isAction = key == actionLabel && !showDecimal

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    !enabled -> AgroSurface.copy(alpha = 0.4f)
                                    isAction -> if (actionEnabled) AgroButtonDark else AgroButtonDark.copy(alpha = 0.4f)
                                    else -> AgroSurface
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (!enabled || (isAction && !actionEnabled)) AgroDivider else AgroButtonBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                enabled = if (!enabled) false else if (isAction) actionEnabled else true
                            ) {
                                when {
                                    isSpecial -> onDelete()
                                    isAction -> onAction?.invoke()
                                    else -> onKeyPress(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = if (isSpecial || isAction) 16.sp else 22.sp,
                            fontWeight = if (isAction) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                !enabled -> AgroTextHint
                                isAction && !actionEnabled -> AgroTextHint
                                else -> AgroTextPrimary
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
