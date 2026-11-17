package com.agrovalle.cosecha.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidad para construir y leer tramas QR y generar códigos QR usando ZXing (AGV-009).
 */
object QrUtil {

    private const val SEPARADOR = "|"
    private const val PREFIJO_CODIGO = "CODIGO="

    /**
     * Genera la trama estructurada de la parihuela sellada para codificar en el QR:
     * código, CLP, cuartel, variedad, jabas y hora de sellado.
     */
    fun generarTramaQr(
        codigo: String,
        clp: String,
        cuartel: String,
        variedad: String,
        jabas: Int,
        horaSellado: Date?
    ): String {
        val codigoLimpio = sanitizarCampo(codigo)
        require(codigoLimpio.isNotEmpty()) { "El código para el QR no puede estar vacío" }
        val formatoSellado = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val horaTxt = horaSellado?.let { formatoSellado.format(it) } ?: "PENDIENTE"
        return listOf(
            "CODIGO=$codigoLimpio",
            "CLP=${sanitizarCampo(clp)}",
            "CUARTEL=${sanitizarCampo(cuartel)}",
            "VARIEDAD=${sanitizarCampo(variedad)}",
            "JABAS=$jabas",
            "SELLADO=$horaTxt"
        ).joinToString(SEPARADOR)
    }

    /**
     * Extrae el código de parihuela desde la trama QR estructurada o desde
     * un QR legado que únicamente contiene el código directo.
     */
    fun extraerCodigo(contenidoQr: String): String {
        val limpio = contenidoQr.trim()
        if (limpio.isEmpty()) return ""
        val partes = limpio.split("|", ";", "\n", "\r")
        val tokenCodigo = partes.firstOrNull {
            it.trim().startsWith(PREFIJO_CODIGO, ignoreCase = true)
        }
        if (tokenCodigo != null) {
            return tokenCodigo.trim().substringAfter("=").trim()
        }
        return limpio
    }

    fun generarMatrizQr(contenido: String, tamanioPx: Int = 512): BitMatrix {
        require(contenido.isNotBlank()) { "El código para el QR no puede estar vacío" }
        require(tamanioPx > 0) { "El tamaño del QR debe ser mayor a 0" }
        return QRCodeWriter().encode(contenido.trim(), BarcodeFormat.QR_CODE, tamanioPx, tamanioPx)
    }

    fun generarQrBitmap(contenido: String, tamanioPx: Int = 512): Bitmap {
        val matriz = generarMatrizQr(contenido, tamanioPx)
        val bitmap = Bitmap.createBitmap(matriz.width, matriz.height, Bitmap.Config.RGB_565)
        for (x in 0 until matriz.width) {
            for (y in 0 until matriz.height) {
                bitmap.setPixel(x, y, if (matriz[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun sanitizarCampo(valor: String): String =
        valor.trim()
            .replace("|", " ")
            .replace(";", " ")
            .replace("\r", " ")
            .replace("\n", " ")
}
