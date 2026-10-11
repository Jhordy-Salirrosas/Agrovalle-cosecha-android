package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Selector desplegable con ícono temático para selección de cuartel, CLP, etc.
 * Soporta estado bloqueado (cuando la parihuela está abierta).
 */
@Composable
fun DropdownSelector(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Seleccionar"
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = AgroTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, if (enabled) AgroButtonBorder else AgroDivider, RoundedCornerShape(10.dp))
                    .background(AgroSurface)
                    .then(
                        if (enabled) Modifier.clickable { expanded = true }
                        else Modifier
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = if (enabled) AgroTextSecondary else AgroTextHint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedValue ?: placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedValue != null && enabled) AgroTextPrimary
                            else AgroTextHint,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Desplegar",
                    tint = if (enabled) AgroTextSecondary else AgroTextHint
                )
            }

            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AgroSurface)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = AgroTextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
