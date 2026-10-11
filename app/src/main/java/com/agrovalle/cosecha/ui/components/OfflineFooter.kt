package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Footer persistente que muestra el estado "Sin conexión"
 * con ícono de nube tachada para pantallas que operan offline.
 */
@Composable
fun OfflineFooter(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = AgroDivider, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Sin conexión",
                tint = AgroTextHint,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Sin conexión",
                style = MaterialTheme.typography.labelSmall,
                color = AgroTextHint
            )
        }
    }
}
