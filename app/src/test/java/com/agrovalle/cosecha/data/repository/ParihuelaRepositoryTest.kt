package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Date

class ParihuelaRepositoryTest {

    private lateinit var parihuelaDao: FakeParihuelaDao
    private lateinit var bitacoraDao: FakeBitacoraDao
    private lateinit var incidenciaDao: FakeIncidenciaDao
    private lateinit var pesajeDao: FakePesajeDao
    private lateinit var repository: ParihuelaRepository

    @Before
    fun setUp() {
        parihuelaDao = FakeParihuelaDao()
        bitacoraDao = FakeBitacoraDao()
        incidenciaDao = FakeIncidenciaDao()
        pesajeDao = FakePesajeDao()
        repository = ParihuelaRepository(parihuelaDao, bitacoraDao, incidenciaDao, pesajeDao)
    }

    @Test
    fun `con 119 jabas no se sella automaticamente`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val resultado = repository.registrarJabas(
            parihuelaId = parihuela.id,
            cantidad = 119,
            variedad = "Biloxi",
            cuartel = "Cuartel 01",
            ahora = Date(1_700_000_060_000L)
        )

        assertFalse(resultado.selladaPorLimite)
        assertNull(resultado.nuevaParihuela)
        assertEquals(EstadoParihuela.ABIERTA, resultado.parihuela.estado)
        assertEquals(119, resultado.parihuela.conteoJabas)
        assertNull(resultado.parihuela.horaSellado)
        assertTrue(bitacoraDao.items.isEmpty())
    }

    @Test
    fun `al llegar a 120 jabas se sella automaticamente, registra bitacora y abre siguiente con codigo distinto`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val sellado = Date(1_700_000_120_000L)
        val primera = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", inicio)

        repository.registrarJabas(primera.id, 119, "Ventura", "Cuartel 05", Date(1_700_000_060_000L))
        val resultado = repository.registrarJabas(primera.id, 1, "Ventura", "Cuartel 05", sellado)

        assertTrue(resultado.selladaPorLimite)
        assertEquals(EstadoParihuela.SELLADA, resultado.parihuela.estado)
        assertEquals(120, resultado.parihuela.conteoJabas)
        assertEquals(sellado, resultado.parihuela.horaSellado)

        val segunda = resultado.nuevaParihuela
        assertNotNull(segunda)
        assertEquals(EstadoParihuela.ABIERTA, segunda!!.estado)
        assertEquals(0, segunda.conteoJabas)
        assertNotEquals(primera.codigo, segunda.codigo)

        assertEquals(1, bitacoraDao.items.size)
        val registro = bitacoraDao.items.first()
        assertEquals(primera.id, registro.parihuelaId)
        assertEquals(ParihuelaRepository.EVENTO_SELLADO_LIMITE, registro.evento)
        assertEquals(sellado, registro.fechaHora)
    }

    @Test
    fun `sellado anticipado sin motivo es rechazado`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 12", Date(1_700_000_000_000L))

        try {
            repository.sellarAnticipadamente(parihuela.id, "   ")
            fail("Debió lanzar IllegalArgumentException cuando el motivo está vacío")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        val almacenada = parihuelaDao.getById(parihuela.id)!!
        assertEquals(EstadoParihuela.ABIERTA, almacenada.estado)
        assertNull(almacenada.horaSellado)
        assertTrue(bitacoraDao.items.isEmpty())
    }

    @Test
    fun `sellado anticipado con motivo guarda estado, hora, motivo, bitacora y bloquea cambios`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val horaSellado = Date(1_700_000_300_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 18", inicio)
        repository.registrarJabas(parihuela.id, 45, "Ventura", "Cuartel 18", Date(1_700_000_100_000L))

        val sellada = repository.sellarAnticipadamente(
            parihuelaId = parihuela.id,
            motivo = "Fin de turno",
            ahora = horaSellado
        )

        assertEquals(EstadoParihuela.SELLADA_ANTICIPADA, sellada.estado)
        assertEquals(horaSellado, sellada.horaSellado)
        assertEquals("Fin de turno", sellada.motivoSelladoAnticipado)

        assertEquals(1, bitacoraDao.items.size)
        val registro = bitacoraDao.items.first()
        assertEquals(parihuela.id, registro.parihuelaId)
        assertEquals(ParihuelaRepository.EVENTO_SELLADO_ANTICIPADO, registro.evento)
        assertEquals("Fin de turno", registro.detalle)

        try {
            repository.registrarJabas(parihuela.id, 1, "Ventura", "Cuartel 18")
            fail("No debería permitir modificar el conteo de una parihuela sellada")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
    }

    @Test
    fun `registro con variedad y cuartel coincidentes procede sin incidencias`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))

        val resultado = repository.registrarJabas(parihuela.id, 10, "Biloxi", "Cuartel 01")

        assertFalse(resultado.rechazadoPorMezcla)
        assertNull(resultado.incidenciaGenerada)
        assertEquals(10, resultado.parihuela.conteoJabas)
        assertTrue(repository.observarIncidenciasPorParihuela(parihuela.id).first().isEmpty())
    }

    @Test
    fun `variedad distinta se rechaza, mantiene conteo y registra incidencia MEZCLA_VARIEDAD`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarJabas(parihuela.id, 5, "Biloxi", "Cuartel 01")

        val resultado = repository.registrarJabas(parihuela.id, 3, "Ventura", "Cuartel 01")

        assertTrue(resultado.rechazadoPorMezcla)
        assertEquals(5, resultado.parihuela.conteoJabas)
        assertEquals(5, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        assertNotNull(resultado.incidenciaGenerada)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_VARIEDAD, resultado.incidenciaGenerada!!.tipo)

        val incidencias = repository.observarIncidenciasPorParihuela(parihuela.id).first()
        assertEquals(1, incidencias.size)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_VARIEDAD, incidencias.first().tipo)
    }

    @Test
    fun `recien abierta el tiempo restante es el limite completo y estado EN_ESPERA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, inicio)!!

        assertEquals(ParihuelaRepository.LIMITE_ESPERA_MS, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estado.estado)
    }

    @Test
    fun `a mitad del limite el calculo es exacto y estado EN_ESPERA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val mitad = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS / 2)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, mitad)!!

        assertEquals(ParihuelaRepository.LIMITE_ESPERA_MS / 2, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estado.estado)
    }

    @Test
    fun `con exactamente 5 minutos restantes el estado es POR_VENCER`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val cincoMinAntes = Date(
            inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - ParihuelaRepository.UMBRAL_POR_VENCER_MS
        )
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, cincoMinAntes)!!

        assertEquals(ParihuelaRepository.UMBRAL_POR_VENCER_MS, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.POR_VENCER, estado.estado)
    }

    @Test
    fun `pasado el limite el estado es EXCEDIDA y restante es cero`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val pasado = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 60_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, pasado)!!

        assertEquals(0L, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.EXCEDIDA, estado.estado)
    }

    @Test
    fun `parihuela leida de la base con hora de inicio antigua calcula bien sin depender de memoria`() = runBlocking {
        val horaAntigua = Date(1_699_998_000_000L)
        val id = parihuelaDao.insert(
            ParihuelaEntity(
                codigo = "PAR-BD-001",
                estado = EstadoParihuela.ABIERTA,
                variedad = "Ventura",
                cuartel = "Cuartel 05",
                horaInicio = horaAntigua
            )
        )
        val nuevoRepositorio = ParihuelaRepository(parihuelaDao, bitacoraDao, incidenciaDao, pesajeDao)
        val ahora = Date(horaAntigua.time + ParihuelaRepository.LIMITE_ESPERA_MS - 120_000L)

        val estado = nuevoRepositorio.obtenerEstadoTiempoParihuela(id, ahora)!!

        assertEquals(120_000L, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.POR_VENCER, estado.estado)
    }

    @Test
    fun `intento de edicion sobre parihuela SELLADA se rechaza y registra INTENTO_EDICION en bitacora`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val sellado = Date(1_700_000_120_000L)
        val intento = Date(1_700_000_180_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)
        repository.registrarJabas(parihuela.id, 120, "Biloxi", "Cuartel 01", sellado)

        try {
            repository.registrarJabas(parihuela.id, 5, "Biloxi", "Cuartel 01", intento)
            fail("Debió rechazar la edición sobre una parihuela SELLADA")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertEquals(2, eventos.size)
        assertEquals(ParihuelaRepository.EVENTO_INTENTO_EDICION, eventos.last().evento)
        assertEquals(intento, eventos.last().fechaHora)
    }

    @Test
    fun `intento de edicion sobre parihuela SELLADA_ANTICIPADA se rechaza y registra INTENTO_EDICION`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", inicio)
        repository.sellarAnticipadamente(parihuela.id, "Lluvia", Date(1_700_000_060_000L))

        try {
            repository.sellarAnticipadamente(parihuela.id, "Otro motivo", Date(1_700_000_120_000L))
            fail("Debió rechazar el re-sellado sobre una parihuela SELLADA_ANTICIPADA")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertEquals(2, eventos.size)
        assertEquals(ParihuelaRepository.EVENTO_INTENTO_EDICION, eventos.last().evento)
    }

    @Test
    fun `consulta de origen por codigo existente devuelve todos los datos`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val sellado = Date(1_700_000_300_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 18", inicio)
        repository.registrarJabas(parihuela.id, 40, "Ventura", "Cuartel 18", Date(1_700_000_100_000L))
        repository.sellarAnticipadamente(parihuela.id, "Fin de jornada", sellado)

        val resultado = repository.consultarOrigenPorCodigo(parihuela.codigo)

        assertTrue(resultado is ConsultaOrigenParihuela.Encontrado)
        val origen = (resultado as ConsultaOrigenParihuela.Encontrado).origen
        assertEquals(parihuela.codigo, origen.codigo)
        assertEquals("Ventura", origen.variedad)
        assertEquals("Cuartel 18", origen.cuartel)
        assertEquals(40, origen.conteoJabas)
        assertEquals(0, origen.pisos)
        assertEquals(inicio, origen.horaInicio)
        assertEquals(sellado, origen.horaSellado)
        assertEquals(EstadoParihuela.SELLADA_ANTICIPADA, origen.estado)
        assertEquals("Fin de jornada", origen.motivoSelladoAnticipado)
    }

    @Test
    fun `consulta de origen por codigo inexistente devuelve NoEncontrado sin error`() = runBlocking {
        val resultado = repository.consultarOrigenPorCodigo("PAR-NO-EXISTE")

        assertTrue(resultado is ConsultaOrigenParihuela.NoEncontrado)
    }

    @Test
    fun `parihuela abierta acepta cambios sin generar eventos INTENTO_EDICION`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarJabas(parihuela.id, 10, "Biloxi", "Cuartel 01")

        assertTrue(bitacoraDao.items.none { it.evento == ParihuelaRepository.EVENTO_INTENTO_EDICION })
    }

    @Test
    fun `resumen del panel con base vacia sale en cero sin errores`() = runBlocking {
        val resumen = repository.observarResumenPanel().first()

        assertEquals(0, resumen.totalAbiertas)
        assertEquals(0, resumen.totalSelladas)
        assertEquals(0, resumen.totalSelladasAnticipadas)
        assertEquals(0, resumen.totalJabas)
        assertTrue(resumen.jabasPorVariedad.isEmpty())
        assertTrue(resumen.jabasPorCuartel.isEmpty())
        assertTrue(resumen.abiertasPorUrgencia.isEmpty())
    }

    @Test
    fun `resumen del panel calcula totales y agrupa jabas por variedad y cuartel`() = runBlocking {
        val base = 1_700_000_000_000L
        val p3 = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", Date(base))
        repository.registrarJabas(p3.id, 30, "Ventura", "Cuartel 05", Date(base + 10_000L))
        repository.sellarAnticipadamente(p3.id, "Fin de turno", Date(base + 20_000L))

        val p1 = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(base + 30_000L))
        repository.registrarJabas(p1.id, 120, "Biloxi", "Cuartel 01", Date(base + 40_000L))
        // Al llegar a 120 se abre p2 automaticamente con Biloxi / Cuartel 01 y 0 jabas

        val p4Id = parihuelaDao.insert(
            ParihuelaEntity(
                codigo = "PAR-PANEL-04",
                estado = EstadoParihuela.ABIERTA,
                variedad = "Biloxi",
                cuartel = "Cuartel 12",
                conteoJabas = 15,
                horaInicio = Date(base + 50_000L)
            )
        )
        assertTrue(p4Id > 0)

        val resumen = repository.observarResumenPanel { Date(base + 100_000L) }.first()

        assertEquals(2, resumen.totalAbiertas)
        assertEquals(1, resumen.totalSelladas)
        assertEquals(1, resumen.totalSelladasAnticipadas)
        assertEquals(165, resumen.totalJabas)
        assertEquals(mapOf("Biloxi" to 135, "Ventura" to 30), resumen.jabasPorVariedad)
        assertEquals(
            mapOf("Cuartel 01" to 120, "Cuartel 05" to 30, "Cuartel 12" to 15),
            resumen.jabasPorCuartel
        )
    }

    @Test
    fun `resumen del panel ordena parihuelas abiertas de menor a mayor tiempo restante`() = runBlocking {
        val ahora = Date(1_700_000_000_000L)
        val recienteId = parihuelaDao.insert(
            ParihuelaEntity(
                codigo = "PAR-REC-01",
                estado = EstadoParihuela.ABIERTA,
                variedad = "Biloxi",
                cuartel = "Cuartel 01",
                horaInicio = Date(ahora.time - 2 * 60_000L)
            )
        )
        val urgenteId = parihuelaDao.insert(
            ParihuelaEntity(
                codigo = "PAR-URG-02",
                estado = EstadoParihuela.ABIERTA,
                variedad = "Ventura",
                cuartel = "Cuartel 05",
                horaInicio = Date(ahora.time - 28 * 60_000L)
            )
        )
        val mediaId = parihuelaDao.insert(
            ParihuelaEntity(
                codigo = "PAR-MED-03",
                estado = EstadoParihuela.ABIERTA,
                variedad = "Biloxi",
                cuartel = "Cuartel 12",
                horaInicio = Date(ahora.time - 15 * 60_000L)
            )
        )

        val resumen = repository.observarResumenPanel { ahora }.first()

        assertEquals(
            listOf(urgenteId, mediaId, recienteId),
            resumen.abiertasPorUrgencia.map { it.parihuela.id }
        )
        assertEquals(EstadoTiempoEspera.POR_VENCER, resumen.abiertasPorUrgencia.first().tiempoEspera.estado)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, resumen.abiertasPorUrgencia.last().tiempoEspera.estado)
    }

    @Test
    fun `pesaje suma jabas y queda guardado con su jornalero`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))

        val resultado = repository.registrarPesaje("Juan Perez", 12, "Biloxi", "Cuartel 01")

        assertTrue(resultado is ResultadoPesaje.Registrado)
        val registrado = resultado as ResultadoPesaje.Registrado
        assertEquals("Juan Perez", registrado.pesaje.jornalero)
        assertEquals(12, registrado.pesaje.cantidadJabas)
        assertEquals(12, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
    }

    @Test
    fun `pesaje sin parihuela abierta es rechazado`() = runBlocking {
        val resultado = repository.registrarPesaje("Juan Perez", 10, "Biloxi", "Cuartel 01")

        assertTrue(resultado is ResultadoPesaje.SinParihuelaAbierta)
        assertTrue(pesajeDao.items.isEmpty())
    }

    @Test
    fun `pesaje que completa las 120 jabas sella la parihuela`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", Date(1_700_000_000_000L))
        repository.registrarPesaje("Maria Lopez", 100, "Ventura", "Cuartel 05")

        val resultado = repository.registrarPesaje("Maria Lopez", 20, "Ventura", "Cuartel 05")

        assertTrue(resultado is ResultadoPesaje.Registrado)
        val registrado = resultado as ResultadoPesaje.Registrado
        assertTrue(registrado.resultadoJabas.selladaPorLimite)
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(parihuela.id)!!.estado)
    }

    @Test
    fun `avance de parihuela abierta es correcto en 0, 100 y 120`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))

        val avance0 = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(0, avance0.jabasActuales)
        assertEquals(100, avance0.faltanPara100)
        assertEquals(120, avance0.faltanPara120)
        assertEquals(0, avance0.porcentajeHacia120)

        repository.registrarPesaje("Juan Perez", 100, "Biloxi", "Cuartel 01")
        val avance100 = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(100, avance100.jabasActuales)
        assertEquals(0, avance100.faltanPara100)
        assertEquals(20, avance100.faltanPara120)
        assertEquals(83, avance100.porcentajeHacia120)

        // Al llegar a 120 se sella la actual y se abre una nueva en 0; verificamos la nueva y la sellada
        val res120 = repository.registrarPesaje("Juan Perez", 20, "Biloxi", "Cuartel 01") as ResultadoPesaje.Registrado
        assertEquals(120, res120.resultadoJabas.parihuela.conteoJabas)
        val avanceNueva = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(0, avanceNueva.jabasActuales)
        assertEquals(120, avanceNueva.faltanPara120)
        assertEquals(0, avanceNueva.porcentajeHacia120)
    }

    @Test
    fun `rendimiento suma bien por jornalero`() = runBlocking {
        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 15, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Maria Lopez", 20, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Juan Perez", 10, "Biloxi", "Cuartel 01")

        val rendimiento = repository.observarRendimientoPorJornalero().first()

        assertEquals(mapOf("Juan Perez" to 25, "Maria Lopez" to 20), rendimiento)
    }

    @Test
    fun `reasignacion en parihuela abierta funciona y en sellada se rechaza con registro en bitacora`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        val res = repository.registrarPesaje("Juan Perez", 10, "Biloxi", "Cuartel 01") as ResultadoPesaje.Registrado

        val reasignado = repository.reasignarJornalero(res.pesaje.id, "Pedro Gomez")
        assertEquals("Pedro Gomez", reasignado.jornalero)
        assertEquals("Pedro Gomez", pesajeDao.getById(res.pesaje.id)!!.jornalero)

        repository.sellarAnticipadamente(parihuela.id, "Fin de turno")

        try {
            repository.reasignarJornalero(res.pesaje.id, "Otro Jornalero")
            fail("Debió rechazar la reasignación en parihuela sellada")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        assertTrue(bitacoraDao.items.any { it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_INTENTO_EDICION })
    }

    @Test
    fun `evaluarAlertasTiempo no avisa en EN_ESPERA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val porNotificar = repository.evaluarAlertasTiempo(Date(inicio.time + 10 * 60_000L))

        assertTrue(porNotificar.isEmpty())
        assertTrue(bitacoraDao.items.isEmpty())
    }

    @Test
    fun `evaluarAlertasTiempo avisa una sola vez en POR_VENCER y no repite`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)
        val enPorVencer = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - 3 * 60_000L)

        val primeraVez = repository.evaluarAlertasTiempo(enPorVencer)
        val segundaVez = repository.evaluarAlertasTiempo(Date(enPorVencer.time + 30_000L))

        assertEquals(listOf(parihuela.id), primeraVez.map { it.id })
        assertTrue(segundaVez.isEmpty())
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_ALERTA_POR_VENCER
            }
        )
    }

    @Test
    fun `evaluarAlertasTiempo marca RETENCION_EXCEDIDA y notifica una sola vez`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", inicio)
        val excedida = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 60_000L)

        val primera = repository.evaluarAlertasTiempo(excedida)
        val segunda = repository.evaluarAlertasTiempo(Date(excedida.time + 60_000L))

        assertEquals(listOf(parihuela.id), primera.map { it.id })
        assertTrue(segunda.isEmpty())
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA
            }
        )
    }

    @Test
    fun `calcularDemoraAlertaExactaMs programa a los 25 minutos para parihuela recien abierta`() {
        val inicio = Date(1_700_000_000_000L)

        val demora = ParihuelaRepository.calcularDemoraAlertaExactaMs(inicio, inicio)

        assertEquals(25L * 60L * 1000L, demora)
    }

    @Test
    fun `calcularDemoraAlertaExactaMs devuelve cero si el momento ya paso`() {
        val inicio = Date(1_700_000_000_000L)
        val pasado = Date(inicio.time + 26L * 60L * 1000L)

        val demora = ParihuelaRepository.calcularDemoraAlertaExactaMs(inicio, pasado)

        assertEquals(0L, demora)
    }

    @Test
    fun `cuartel distinto se rechaza, mantiene conteo y registra incidencia MEZCLA_CUARTEL`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarJabas(parihuela.id, 5, "Biloxi", "Cuartel 01")

        val resultado = repository.registrarJabas(parihuela.id, 3, "Biloxi", "Cuartel 05")

        assertTrue(resultado.rechazadoPorMezcla)
        assertEquals(5, resultado.parihuela.conteoJabas)
        assertEquals(5, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_CUARTEL, resultado.incidenciaGenerada!!.tipo)

        val incidencias = repository.observarIncidenciasPorParihuela(parihuela.id).first()
        assertEquals(1, incidencias.size)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_CUARTEL, incidencias.first().tipo)
    }

    @Test
    fun `variedad y cuartel distintos generan una sola incidencia MEZCLA_VARIEDAD`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))

        val resultado = repository.registrarJabas(parihuela.id, 2, "Ventura", "Cuartel 18")

        assertTrue(resultado.rechazadoPorMezcla)
        assertEquals(0, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        val incidencias = repository.observarIncidenciasPorParihuela(parihuela.id).first()
        assertEquals(1, incidencias.size)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_VARIEDAD, incidencias.first().tipo)
    }

    @Test
    fun `pesaje que supera las 120 jabas es rechazado por capacidad sin alterar conteo ni crear incidencia`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 118, "Biloxi", "Cuartel 01")

        val resPesaje = repository.registrarPesaje("Juan Perez", 5, "Biloxi", "Cuartel 01")

        assertTrue(resPesaje is ResultadoPesaje.RechazadoPorCapacidad)
        val rechazado = resPesaje as ResultadoPesaje.RechazadoPorCapacidad
        assertEquals(2, rechazado.jabasFaltantes)
        assertEquals(118, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        assertEquals(EstadoParihuela.ABIERTA, parihuelaDao.getById(parihuela.id)!!.estado)
        assertTrue(incidenciaDao.items.isEmpty())
        assertEquals(1, pesajeDao.items.size)
    }

    @Test
    fun `ajuste exacto a 120 jabas tras rechazo por capacidad sella la parihuela y abre la siguiente`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 118, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Juan Perez", 5, "Biloxi", "Cuartel 01") // rechazado por capacidad

        val exacto = repository.registrarPesaje("Juan Perez", 2, "Biloxi", "Cuartel 01")

        assertTrue(exacto is ResultadoPesaje.Registrado)
        val registrado = exacto as ResultadoPesaje.Registrado
        assertTrue(registrado.resultadoJabas.selladaPorLimite)
        assertEquals(120, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(parihuela.id)!!.estado)
        assertNotNull(registrado.resultadoJabas.nuevaParihuela)
    }

    @Test
    fun `abrirNuevaParihuela rechaza crear una segunda si ya existe una ABIERTA`() = runBlocking {
        val primera = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", Date(1_700_000_000_000L))

        try {
            repository.abrirNuevaParihuela("Ventura", "Cuartel 05", Date(1_700_000_010_000L))
            fail("Debió rechazar abrir una segunda parihuela mientras la primera sigue abierta")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        val abiertas = parihuelaDao.observeByEstado(EstadoParihuela.ABIERTA).first()
        assertEquals(1, abiertas.size)
        assertEquals(primera.id, abiertas.first().id)
    }

    @Test
    fun `comparacion anti-mezcla recorta espacios y rechaza variedad o cuartel en blanco sin incidencia`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("  Biloxi ", " Cuartel 01  ", Date(1_700_000_000_000L))

        val resultado = repository.registrarJabas(
            parihuelaId = parihuela.id,
            cantidad = 10,
            variedad = "  biloxi  ",
            cuartel = "  cuartel 01 "
        )
        assertFalse(resultado.rechazadoPorMezcla)
        assertEquals(10, resultado.parihuela.conteoJabas)
        assertTrue(incidenciaDao.items.isEmpty())

        try {
            repository.registrarJabas(parihuela.id, 5, "   ", "Cuartel 01")
            fail("Debió rechazar variedad en blanco")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }
        assertTrue(incidenciaDao.items.isEmpty())
        assertEquals(10, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
    }

    @Test
    fun `sellar parihuela con retencion ya excedida sin marca previa registra RETENCION_EXCEDIDA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val despuesDelLimite = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 120_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        repository.sellarAnticipadamente(parihuela.id, "Cierre tardío", despuesDelLimite)

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA })
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_SELLADO_ANTICIPADO })
    }

    private class FakeParihuelaDao : ParihuelaDao {
        private var seq = 0L
        private val data = linkedMapOf<Long, ParihuelaEntity>()

        override suspend fun insert(parihuela: ParihuelaEntity): Long {
            val id = ++seq
            data[id] = parihuela.copy(id = id)
            return id
        }

        override suspend fun update(parihuela: ParihuelaEntity) {
            data[parihuela.id] = parihuela
        }

        override suspend fun getById(id: Long): ParihuelaEntity? = data[id]

        override suspend fun getByCodigo(codigo: String): ParihuelaEntity? =
            data.values.firstOrNull { it.codigo == codigo }

        override fun observeByEstado(estado: EstadoParihuela): Flow<List<ParihuelaEntity>> =
            flowOf(
                data.values
                    .filter { it.estado == estado }
                    .sortedByDescending { it.horaInicio.time }
            )

        override fun observeAll(): Flow<List<ParihuelaEntity>> =
            flowOf(data.values.sortedByDescending { it.horaInicio.time })
    }

    private class FakeBitacoraDao : BitacoraDao {
        private var seq = 0L
        val items = mutableListOf<BitacoraEntity>()

        override suspend fun insert(bitacora: BitacoraEntity): Long {
            val id = ++seq
            items.add(bitacora.copy(id = id))
            return id
        }

        override suspend fun existeEvento(parihuelaId: Long, evento: String): Boolean =
            items.any { it.parihuelaId == parihuelaId && it.evento == evento }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<BitacoraEntity>> =
            flowOf(items.filter { it.parihuelaId == parihuelaId })

        override fun observeAll(): Flow<List<BitacoraEntity>> = flowOf(items.toList())
    }

    private class FakeIncidenciaDao : IncidenciaDao {
        private var seq = 0L
        val items = mutableListOf<IncidenciaEntity>()

        override suspend fun insert(incidencia: IncidenciaEntity): Long {
            val id = ++seq
            items.add(incidencia.copy(id = id))
            return id
        }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<IncidenciaEntity>> =
            flowOf(items.filter { it.parihuelaId == parihuelaId })

        override fun observeAll(): Flow<List<IncidenciaEntity>> = flowOf(items.toList())
    }

    private class FakePesajeDao : PesajeDao {
        private var seq = 0L
        val items = mutableListOf<PesajeEntity>()

        override suspend fun insert(pesaje: PesajeEntity): Long {
            val id = ++seq
            items.add(pesaje.copy(id = id))
            return id
        }

        override suspend fun update(pesaje: PesajeEntity) {
            val idx = items.indexOfFirst { it.id == pesaje.id }
            if (idx >= 0) items[idx] = pesaje
        }

        override suspend fun getById(id: Long): PesajeEntity? = items.firstOrNull { it.id == id }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<PesajeEntity>> =
            flowOf(items.filter { it.parihuelaId == parihuelaId })

        override fun observeAll(): Flow<List<PesajeEntity>> = flowOf(items.toList())
    }
}
