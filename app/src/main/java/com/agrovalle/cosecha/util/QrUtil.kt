package com.agrovalle.cosecha.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utilidad para generar códigos QR a partir del código de parihuela usando ZXing.
 */
object QrUtil {

    fun generarMatrizQr(codigo: String, tamanioPx: Int = 512): BitMatrix {
        require(codigo.isNotBlank()) { "El código para el QR no puede estar vacío" }
        require(tamanioPx > 0) { "El tamaño del QR debe ser mayor a 0" }
        return QRCodeWriter().encode(codigo.trim(), BarcodeFormat.QR_CODE, tamanioPx, tamanioPx)
    }

    fun generarQrBitmap(codigo: String, tamanioPx: Int = 512): Bitmap {
        val matriz = generarMatrizQr(codigo, tamanioPx)
        val bitmap = Bitmap.createBitmap(matriz.width, matriz.height, Bitmap.Config.RGB_565)
        for (x in 0 until matriz.width) {
            for (y in 0 until matriz.height) {
                bitmap.setPixel(x, y, if (matriz[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
