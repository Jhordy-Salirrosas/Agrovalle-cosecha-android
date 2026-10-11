package com.agrovalle.cosecha.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agrovalle.cosecha.ui.theme.*

/**
 * Barra superior con título de pantalla y flecha de retorno.
 * Variante con botón de cerrar (X) para pantalla de confirmación.
 */
@Composable
fun HeaderBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = AgroTextPrimary
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = AgroTextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (onClose != null) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = AgroTextPrimary
                )
            }
        }
    }
}
