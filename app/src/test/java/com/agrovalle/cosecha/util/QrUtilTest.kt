package com.agrovalle.cosecha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
