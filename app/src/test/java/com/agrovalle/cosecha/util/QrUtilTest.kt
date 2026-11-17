package com.agrovalle.cosecha.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class QrUtilTest {

    @Test
    fun `generarMatrizQr produce una matriz valida del tamano esperado`() {
        val matriz = QrUtil.generarMatrizQr("PAR-1700000000000-0001", 256)

        assertEquals(256, matriz.width)
        assertEquals(256, matriz.height)

        var tieneModulosActivos = false
        for (x in 0 until matriz.width) {
            for (y in 0 until matriz.height) {
                if (matriz[x, y]) {
                    tieneModulosActivos = true
                    break
                }
            }
            if (tieneModulosActivos) break
        }
        assertTrue(tieneModulosActivos)
    }

    @Test
    fun `generarTramaQr codifica codigo, CLP, cuartel, variedad, jabas y hora de sellado`() {
        val horaSellado = Date(1_700_000_300_000L)
        val trama = QrUtil.generarTramaQr(
            codigo = "PAR-1700000000000-0009",
            clp = "CLP-204",
            cuartel = "Cuartel 05",
            variedad = "Ventura",
            jabas = 120,
            horaSellado = horaSellado
        )

        assertTrue(trama.contains("CODIGO=PAR-1700000000000-0009"))
        assertTrue(trama.contains("CLP=CLP-204"))
        assertTrue(trama.contains("CUARTEL=Cuartel 05"))
        assertTrue(trama.contains("VARIEDAD=Ventura"))
        assertTrue(trama.contains("JABAS=120"))
        assertTrue(trama.contains("SELLADO="))

        // Verifica que la matriz QR generada desde la trama sea legible y devuelva la trama intacta
        val matriz = QrUtil.generarMatrizQr(trama, 256)
        val pixels = IntArray(matriz.width * matriz.height) { idx ->
            val x = idx % matriz.width
            val y = idx / matriz.width
            if (matriz[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val source = com.google.zxing.RGBLuminanceSource(matriz.width, matriz.height, pixels)
        val decodificado = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text

        assertEquals(trama, decodificado)
        assertEquals("PAR-1700000000000-0009", QrUtil.extraerCodigo(decodificado))
    }

    @Test
    fun `extraerCodigo soporta tramas nuevas y QR antiguos que solo traen el codigo`() {
        val desdeTrama = QrUtil.extraerCodigo(
            "CODIGO=PAR-1700000000000-0001|CLP=CLP-01|CUARTEL=Cuartel 01|VARIEDAD=Biloxi|JABAS=120|SELLADO=2026-11-16 23:55:00"
        )
        val desdeQrAntiguo = QrUtil.extraerCodigo("  PAR-1700000000000-0001  ")

        assertEquals("PAR-1700000000000-0001", desdeTrama)
        assertEquals("PAR-1700000000000-0001", desdeQrAntiguo)
    }
}
