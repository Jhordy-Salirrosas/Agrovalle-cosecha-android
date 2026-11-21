package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.DatosMemoria
import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.dao.PesajeDao
import com.agrovalle.cosecha.data.local.dao.ReasignacionDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoIncidencia
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import com.agrovalle.cosecha.data.local.entity.PesajeEntity
import com.agrovalle.cosecha.data.local.entity.ReasignacionEntity
import com.agrovalle.cosecha.util.NotificacionTiempoUtil
import com.agrovalle.cosecha.util.QrUtil
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class ParihuelaRepositoryTest {

    private lateinit var parihuelaDao: FakeParihuelaDao
    private lateinit var bitacoraDao: FakeBitacoraDao
    private lateinit var incidenciaDao: FakeIncidenciaDao
    private lateinit var pesajeDao: FakePesajeDao
    private lateinit var reasignacionDao: FakeReasignacionDao
    private lateinit var repository: ParihuelaRepository

    @Before
    fun setUp() {
        DatosMemoria.reiniciarTurno()
        parihuelaDao = FakeParihuelaDao()
        bitacoraDao = FakeBitacoraDao { parihuelaDao.existe(it) }
        incidenciaDao = FakeIncidenciaDao { parihuelaDao.existe(it) }
        pesajeDao = FakePesajeDao { parihuelaDao.existe(it) }
        reasignacionDao = FakeReasignacionDao()
        repository = ParihuelaRepository(
            parihuelaDao = parihuelaDao,
            bitacoraDao = bitacoraDao,
            incidenciaDao = incidenciaDao,
            pesajeDao = pesajeDao,
            reasignacionDao = reasignacionDao
        )
    }

    @Test
    fun `con 119 jabas no se sella automaticamente`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)

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
        val primera = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", inicio)

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
        assertEquals("CLP-05", segunda.clp)
        assertEquals("CUA-02", segunda.cuadrilla)
        assertNotEquals(primera.codigo, segunda.codigo)

        val eventosSellada = bitacoraDao.items.filter { it.parihuelaId == primera.id }
        assertEquals(2, eventosSellada.size)
        val registro = eventosSellada.first { it.evento == ParihuelaRepository.EVENTO_SELLADO_LIMITE }
        assertEquals(primera.id, registro.parihuelaId)
        assertEquals(sellado, registro.fechaHora)
        val registroRetencion = eventosSellada.first { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION }
        assertEquals(sellado, registroRetencion.fechaHora)
    }

    @Test
    fun `sellado anticipado sin motivo es rechazado`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 12", "CLP-12", "CUA-01", Date(1_700_000_000_000L))

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
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 18", "CLP-18", "CUA-03", inicio)
        repository.registrarJabas(parihuela.id, 45, "Ventura", "Cuartel 18", Date(1_700_000_100_000L))

        val sellada = repository.sellarAnticipadamente(
            parihuelaId = parihuela.id,
            motivo = "Fin de turno",
            ahora = horaSellado
        )

        assertEquals(EstadoParihuela.SELLADA_ANTICIPADA, sellada.estado)
        assertEquals(horaSellado, sellada.horaSellado)
        assertEquals("Fin de turno", sellada.motivoSelladoAnticipado)

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertEquals(2, eventos.size)
        val registro = eventos.first { it.evento == ParihuelaRepository.EVENTO_SELLADO_ANTICIPADO }
        assertEquals(parihuela.id, registro.parihuelaId)
        assertEquals("Fin de turno", registro.detalle)
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION })

        try {
            repository.registrarJabas(parihuela.id, 1, "Ventura", "Cuartel 18")
            fail("No debería permitir modificar el conteo de una parihuela sellada")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
    }

    @Test
    fun `registro con variedad y cuartel coincidentes procede sin incidencias`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        val resultado = repository.registrarJabas(parihuela.id, 10, "Biloxi", "Cuartel 01")

        assertFalse(resultado.rechazadoPorMezcla)
        assertNull(resultado.incidenciaGenerada)
        assertEquals(10, resultado.parihuela.conteoJabas)
        assertTrue(repository.observarIncidenciasPorParihuela(parihuela.id).first().isEmpty())
    }

    @Test
    fun `variedad distinta se rechaza, mantiene conteo y registra incidencia MEZCLA_VARIEDAD`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
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
    fun `sin pesajes no corre el tiempo ni se dispara alerta y al primer pesaje inicia cronometro de 45 min`() = runBlocking {
        val horaApertura = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", horaApertura)

        assertNull(parihuelaDao.getById(parihuela.id)!!.horaInicio)
        assertNull(
            repository.obtenerEstadoTiempoParihuela(
                parihuela.id,
                Date(horaApertura.time + 60 * 60_000L)
            )
        )
        val estadoSinPesaje = repository.calcularTiempoEspera(
            parihuelaDao.getById(parihuela.id)!!,
            Date(horaApertura.time + 60 * 60_000L)
        )
        assertFalse(estadoSinPesaje.cronometroIniciado)
        assertEquals(45L * 60L * 1000L, estadoSinPesaje.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estadoSinPesaje.estado)
        assertTrue(repository.evaluarAlertasTiempo(Date(horaApertura.time + 60 * 60_000L)).isEmpty())

        val horaPrimerPesaje = Date(horaApertura.time + 15 * 60_000L)
        val res = repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01", horaPrimerPesaje)
        assertTrue((res as ResultadoPesaje.Registrado).resultadoJabas.cronometroIniciadoEnEstePesaje)
        assertEquals(horaPrimerPesaje, parihuelaDao.getById(parihuela.id)!!.horaInicio)

        val estadoAlIniciar = repository.obtenerEstadoTiempoParihuela(parihuela.id, horaPrimerPesaje)!!
        assertTrue(estadoAlIniciar.cronometroIniciado)
        assertEquals(45L * 60L * 1000L, estadoAlIniciar.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estadoAlIniciar.estado)
    }

    @Test
    fun `a mitad del limite desde el primer pesaje el calculo es exacto y estado EN_ESPERA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val mitad = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS / 2)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, mitad)!!

        assertEquals(ParihuelaRepository.LIMITE_ESPERA_MS / 2, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estado.estado)
    }

    @Test
    fun `con exactamente 5 minutos restantes desde el primer pesaje el estado es POR_VENCER`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val cincoMinAntes = Date(
            inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - ParihuelaRepository.UMBRAL_POR_VENCER_MS
        )
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

        val estado = repository.obtenerEstadoTiempoParihuela(parihuela.id, cincoMinAntes)!!

        assertEquals(ParihuelaRepository.UMBRAL_POR_VENCER_MS, estado.restanteMs)
        assertEquals(EstadoTiempoEspera.POR_VENCER, estado.estado)
    }

    @Test
    fun `pasados los 45 minutos desde el primer pesaje el estado es EXCEDIDA y restante es cero`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val pasado = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 60_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarJabas(parihuela.id, 120, "Biloxi", "Cuartel 01", sellado)

        try {
            repository.registrarJabas(parihuela.id, 5, "Biloxi", "Cuartel 01", intento)
            fail("Debió rechazar la edición sobre una parihuela SELLADA")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertEquals(3, eventos.size)
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION })
        assertEquals(ParihuelaRepository.EVENTO_INTENTO_EDICION, eventos.last().evento)
        assertEquals(intento, eventos.last().fechaHora)
    }

    @Test
    fun `intento de edicion sobre parihuela SELLADA_ANTICIPADA se rechaza y registra INTENTO_EDICION`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", inicio)
        repository.sellarAnticipadamente(parihuela.id, "Lluvia", Date(1_700_000_060_000L))

        try {
            repository.sellarAnticipadamente(parihuela.id, "Otro motivo", Date(1_700_000_120_000L))
            fail("Debió rechazar el re-sellado sobre una parihuela SELLADA_ANTICIPADA")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertEquals(3, eventos.size)
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION })
        assertEquals(ParihuelaRepository.EVENTO_INTENTO_EDICION, eventos.last().evento)
    }

    @Test
    fun `consulta de origen por codigo existente devuelve todos los datos`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val primerPesaje = Date(1_700_000_100_000L)
        val sellado = Date(1_700_000_300_000L)
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 18", "CLP-18", "CUA-04", inicio)
        repository.registrarJabas(parihuela.id, 40, "Ventura", "Cuartel 18", primerPesaje)
        repository.sellarAnticipadamente(parihuela.id, "Fin de jornada", sellado)

        val resultado = repository.consultarOrigenPorCodigo(parihuela.codigo)

        assertTrue(resultado is ConsultaOrigenParihuela.Encontrado)
        val origen = (resultado as ConsultaOrigenParihuela.Encontrado).origen
        assertEquals(parihuela.codigo, origen.codigo)
        assertEquals("Ventura", origen.variedad)
        assertEquals("Cuartel 18", origen.cuartel)
        assertEquals("CLP-18", origen.clp)
        assertEquals("CUA-04", origen.cuadrilla)
        assertEquals(40, origen.conteoJabas)
        assertEquals(primerPesaje, origen.horaInicio)
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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
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
        val p3 = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-01", Date(base))
        repository.registrarJabas(p3.id, 30, "Ventura", "Cuartel 05", Date(base + 10_000L))
        repository.sellarAnticipadamente(p3.id, "Fin de turno", Date(base + 20_000L))

        val p1 = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(base + 30_000L))
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
                horaInicio = Date(ahora.time - 42 * 60_000L)
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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        val resultado = repository.registrarPesaje("Juan Perez", 12, 24.5, "Biloxi", "Cuartel 01")

        assertTrue(resultado is ResultadoPesaje.Registrado)
        val registrado = resultado as ResultadoPesaje.Registrado
        assertEquals("Juan Perez", registrado.pesaje.jornalero)
        assertEquals(12, registrado.pesaje.cantidadJabas)
        assertEquals(24.5, registrado.pesaje.pesoKg, 0.001)
        assertEquals(12, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
    }

    @Test
    fun `pesaje sin parihuela abierta es rechazado`() = runBlocking {
        val resultado = repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01")

        assertTrue(resultado is ResultadoPesaje.SinParihuelaAbierta)
        assertTrue(pesajeDao.items.isEmpty())
    }

    @Test
    fun `pesaje que completa las 120 jabas sella la parihuela`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", Date(1_700_000_000_000L))
        repository.registrarPesaje("Maria Lopez", 100, 180.0, "Ventura", "Cuartel 05")

        val resultado = repository.registrarPesaje("Maria Lopez", 20, 36.0, "Ventura", "Cuartel 05")

        assertTrue(resultado is ResultadoPesaje.Registrado)
        val registrado = resultado as ResultadoPesaje.Registrado
        assertTrue(registrado.resultadoJabas.selladaPorLimite)
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(parihuela.id)!!.estado)
    }

    @Test
    fun `avance de parihuela abierta es correcto en 0, 100 y 120`() = runBlocking {
        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        val avance0 = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(0, avance0.jabasActuales)
        assertEquals(100, avance0.faltanPara100)
        assertEquals(120, avance0.faltanPara120)
        assertEquals(0, avance0.porcentajeHacia120)

        repository.registrarPesaje("Juan Perez", 100, 200.0, "Biloxi", "Cuartel 01")
        val avance100 = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(100, avance100.jabasActuales)
        assertEquals(0, avance100.faltanPara100)
        assertEquals(20, avance100.faltanPara120)
        assertEquals(83, avance100.porcentajeHacia120)

        // Al llegar a 120 se sella la actual y se abre una nueva en 0; verificamos la nueva y la sellada
        val res120 = repository.registrarPesaje("Juan Perez", 20, 40.0, "Biloxi", "Cuartel 01") as ResultadoPesaje.Registrado
        assertEquals(120, res120.resultadoJabas.parihuela.conteoJabas)
        val avanceNueva = repository.observarAvanceParihuelaAbierta().first()!!
        assertEquals(0, avanceNueva.jabasActuales)
        assertEquals(120, avanceNueva.faltanPara120)
        assertEquals(0, avanceNueva.porcentajeHacia120)
    }

    @Test
    fun `rendimiento suma bien por jornalero`() = runBlocking {
        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 15, 30.0, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Maria Lopez", 20, 40.0, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01")

        val rendimiento = repository.observarRendimientoPorJornalero().first()

        assertEquals(mapOf("Juan Perez" to 25, "Maria Lopez" to 20), rendimiento)
    }

    @Test
    fun `reasignacion en parihuela abierta funciona y en sellada se rechaza con registro en bitacora`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
        val res = repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01") as ResultadoPesaje.Registrado

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
        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

        val porNotificar = repository.evaluarAlertasTiempo(Date(inicio.time + 10 * 60_000L))

        assertTrue(porNotificar.isEmpty())
        assertTrue(bitacoraDao.items.isEmpty())
    }

    @Test
    fun `evaluarAlertasTiempo avisa una sola vez en POR_VENCER y no repite`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)
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
        val parihuela = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Ventura", "Cuartel 05", inicio)
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
    fun `calcularDemoraAlertaExactaMs programa a los 40 minutos desde el primer pesaje`() {
        val inicio = Date(1_700_000_000_000L)

        val demora = ParihuelaRepository.calcularDemoraAlertaExactaMs(inicio, inicio)

        assertEquals(40L * 60L * 1000L, demora)
    }

    @Test
    fun `calcularDemoraAlertaExactaMs devuelve cero si el momento ya paso`() {
        val inicio = Date(1_700_000_000_000L)
        val pasado = Date(inicio.time + 41L * 60L * 1000L)

        val demora = ParihuelaRepository.calcularDemoraAlertaExactaMs(inicio, pasado)

        assertEquals(0L, demora)
    }

    @Test
    fun `cuartel distinto se rechaza, mantiene conteo y registra incidencia MEZCLA_CUARTEL`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        val resultado = repository.registrarJabas(parihuela.id, 2, "Ventura", "Cuartel 18")

        assertTrue(resultado.rechazadoPorMezcla)
        assertEquals(0, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        val incidencias = repository.observarIncidenciasPorParihuela(parihuela.id).first()
        assertEquals(1, incidencias.size)
        assertEquals(ParihuelaRepository.TIPO_MEZCLA_VARIEDAD, incidencias.first().tipo)
    }

    @Test
    fun `pesaje que supera las 120 jabas es rechazado por capacidad sin alterar conteo ni crear incidencia`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 118, 220.0, "Biloxi", "Cuartel 01")

        val resPesaje = repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01")

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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))
        repository.registrarPesaje("Juan Perez", 118, 220.0, "Biloxi", "Cuartel 01")
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01") // rechazado por capacidad

        val exacto = repository.registrarPesaje("Juan Perez", 2, 4.0, "Biloxi", "Cuartel 01")

        assertTrue(exacto is ResultadoPesaje.Registrado)
        val registrado = exacto as ResultadoPesaje.Registrado
        assertTrue(registrado.resultadoJabas.selladaPorLimite)
        assertEquals(120, parihuelaDao.getById(parihuela.id)!!.conteoJabas)
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(parihuela.id)!!.estado)
        assertNotNull(registrado.resultadoJabas.nuevaParihuela)
    }

    @Test
    fun `abrirNuevaParihuela rechaza crear una segunda si ya existe una ABIERTA`() = runBlocking {
        val primera = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        try {
            repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", Date(1_700_000_010_000L))
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
        val parihuela = repository.abrirNuevaParihuela("  Biloxi ", " Cuartel 01  ", " CLP-01 ", " CUA-01 ", Date(1_700_000_000_000L))

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
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

        repository.sellarAnticipadamente(parihuela.id, "Cierre tardío", despuesDelLimite)

        val eventos = bitacoraDao.items.filter { it.parihuelaId == parihuela.id }
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA })
        assertTrue(eventos.any { it.evento == ParihuelaRepository.EVENTO_SELLADO_ANTICIPADO })
    }

    @Test
    fun `incidencia de mezcla nace PENDIENTE y al resolverla queda REVISADA con accion, hora y responsable`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val tRev = Date(1_700_000_060_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        val res = repository.registrarJabas(parihuela.id, 4, "Ventura", "Cuartel 01", t0)
        val incidencia = res.incidenciaGenerada!!

        assertEquals(EstadoIncidencia.PENDIENTE, incidencia.estado)
        assertNull(incidencia.accionTomada)
        assertNull(incidencia.horaRevision)
        assertNull(incidencia.responsable)

        val revisada = repository.resolverIncidencia(
            incidenciaId = incidencia.id,
            accion = ParihuelaRepository.ACCION_SEPARAR_JABA,
            responsable = "Capataz de cuadrilla",
            ahora = tRev
        )

        assertEquals(EstadoIncidencia.REVISADA, revisada.estado)
        assertEquals(ParihuelaRepository.ACCION_SEPARAR_JABA, revisada.accionTomada)
        assertEquals(tRev, revisada.horaRevision)
        assertEquals("Capataz de cuadrilla", revisada.responsable)
        assertEquals(EstadoParihuela.ABIERTA, parihuelaDao.getById(parihuela.id)!!.estado)

        try {
            repository.resolverIncidencia(
                incidenciaId = incidencia.id,
                accion = ParihuelaRepository.ACCION_DESCARTAR_ERROR,
                responsable = "Otro",
                ahora = tRev
            )
            fail("No debería permitir resolver una incidencia ya REVISADA")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
    }

    @Test
    fun `accion Revisar el lote o la parihuela retiene por calidad, bloquea sellado y permite Liberar luego`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val tRet = Date(1_700_000_030_000L)
        val tLib = Date(1_700_000_090_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje("Juan Perez", 20, 40.0, "Biloxi", "Cuartel 01", t0)
        val mezcla = repository.registrarJabas(parihuela.id, 5, "Ventura", "Cuartel 01", t0)
        val incId = mezcla.incidenciaGenerada!!.id

        repository.resolverIncidencia(
            incidenciaId = incId,
            accion = ParihuelaRepository.ACCION_REVISAR_LOTE_PARIHUELA,
            responsable = "Capataz Maria",
            ahora = tRet
        )

        val retenida = parihuelaDao.getById(parihuela.id)!!
        assertEquals(EstadoParihuela.RETENIDA_CALIDAD, retenida.estado)
        assertTrue(bitacoraDao.items.any {
            it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_RETENIDA_CALIDAD
        })

        // Se muestra como retenida en el panel y en consulta de origen
        val resumen = repository.observarResumenPanel { tRet }.first()
        assertEquals(1, resumen.totalRetenidasCalidad)
        assertEquals(listOf(parihuela.id), resumen.retenidasCalidad.map { it.id })
        val origen = repository.consultarOrigenPorCodigo(parihuela.codigo) as ConsultaOrigenParihuela.Encontrado
        assertEquals(EstadoParihuela.RETENIDA_CALIDAD, origen.origen.estado)

        // Una parihuela retenida no se puede sellar ni recibir pesajes ni abrir otra mientras sigue retenida
        try {
            repository.sellarAnticipadamente(parihuela.id, "Intentando sellar retenida", tRet)
            fail("No debería permitir sellar una parihuela retenida por calidad")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
        try {
            repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01", tRet)
            fail("No debería permitir registrar pesajes sobre una parihuela retenida por calidad")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        // Liberar desde Incidencias devuelve ABIERTA, registra en bitacora y habilita sellado
        val liberada = repository.liberarParihuelaRetenida(parihuela.id, "Capataz Maria", tLib)
        assertEquals(EstadoParihuela.ABIERTA, liberada.estado)
        assertTrue(bitacoraDao.items.any {
            it.parihuelaId == parihuela.id &&
                it.evento == ParihuelaRepository.EVENTO_LIBERACION_CALIDAD &&
                it.detalle.contains(ParihuelaRepository.ACCION_LIBERAR)
        })

        val sellada = repository.sellarAnticipadamente(parihuela.id, "Fin de turno", tLib)
        assertEquals(EstadoParihuela.SELLADA_ANTICIPADA, sellada.estado)
    }

    @Test
    fun `motivos predefinidos de reasignacion son exactamente 4`() {
        assertEquals(4, ParihuelaRepository.MOTIVOS_REASIGNACION.size)
        assertEquals(4, ParihuelaRepository.MOTIVOS_REASIGNACION.distinct().size)
    }

    @Test
    fun `reasignarJornaleroACuartel inserta Reasignacion no sincronizada, actualiza cuartel y refleja destino en panel`() = runBlocking {
        val j1 = DatosMemoria.listaJornaleros.first()
        DatosMemoria.asignarCuartel(j1.id, "Cuartel 01")
        val tPes = Date(1_700_000_000_000L)
        val tReasig = Date(1_700_000_120_000L)

        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", tPes)
        repository.registrarPesaje(j1.nombreCompleto, 15, 30.0, "Biloxi", "Cuartel 01", tPes)

        val registrada = repository.reasignarJornaleroACuartel(
            jornalero = j1.nombreCompleto,
            cuartelDestino = "Cuartel 05",
            motivo = ParihuelaRepository.MOTIVO_REASIG_REFUERZO_CARGA,
            hora = tReasig
        )

        assertEquals(j1.nombreCompleto, registrada.jornalero)
        assertEquals("Cuartel 01", registrada.cuartelOrigen)
        assertEquals("Cuartel 05", registrada.cuartelDestino)
        assertEquals(ParihuelaRepository.MOTIVO_REASIG_REFUERZO_CARGA, registrada.motivo)
        assertEquals(tReasig, registrada.hora)
        assertFalse(registrada.sincronizado)
        assertEquals("Cuartel 05", DatosMemoria.obtenerCuartelAsignado(j1.id))

        val resumen = repository.observarResumenPanel { tReasig }.first()
        assertNull(resumen.jornalerosPorCuartel["Cuartel 01"])
        val enDestino = resumen.jornalerosPorCuartel["Cuartel 05"].orEmpty()
        assertEquals(1, enDestino.size)
        assertEquals(j1.nombreCompleto, enDestino.first().nombre)
        assertEquals(15, enDestino.first().jabasAcumuladas)

        val historial = repository.observarReasignaciones().first()
        assertEquals(1, historial.size)
        assertEquals(registrada.id, historial.first().id)
    }

    @Test
    fun `reasignarJornaleroACuartel rechaza motivo invalido o mismo cuartel de destino`() = runBlocking {
        val j1 = DatosMemoria.listaJornaleros.first()
        DatosMemoria.asignarCuartel(j1.id, "Cuartel 01")

        try {
            repository.reasignarJornaleroACuartel(
                jornalero = j1.nombreCompleto,
                cuartelDestino = "Cuartel 01",
                motivo = ParihuelaRepository.MOTIVO_REASIG_COBERTURA_AUSENCIA
            )
            fail("Debió rechazar reasignación al mismo cuartel")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        try {
            repository.reasignarJornaleroACuartel(
                jornalero = j1.nombreCompleto,
                cuartelDestino = "Cuartel 05",
                motivo = "Motivo inventado libre"
            )
            fail("Debió rechazar un motivo fuera de los 4 predefinidos")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        assertTrue(reasignacionDao.items.isEmpty())
        assertEquals("Cuartel 01", DatosMemoria.obtenerCuartelAsignado(j1.id))
    }

    @Test
    fun `reasignarJornaleroACuartel es una sola transaccion y revierte cambios ante fallo`() = runBlocking {
        val j1 = DatosMemoria.listaJornaleros.first()
        DatosMemoria.asignarCuartel(j1.id, "Cuartel 01")

        val transaccionConRollback = object : TransaccionRunner {
            override suspend fun <T> ejecutar(bloque: suspend () -> T): T {
                val snapshotReasig = reasignacionDao.items.toList()
                return try {
                    bloque()
                } catch (t: Throwable) {
                    reasignacionDao.items.clear()
                    reasignacionDao.items.addAll(snapshotReasig)
                    throw t
                }
            }
        }

        val storeConFallo = object : CuartelJornaleroStore {
            override fun obtenerAsignacionesPorNombre() = DatosMemoriaCuartelStore.obtenerAsignacionesPorNombre()
            override fun obtenerCuartel(jornalero: String) = DatosMemoriaCuartelStore.obtenerCuartel(jornalero)
            override fun actualizarCuartel(jornalero: String, cuartelDestino: String): String? {
                throw IllegalStateException("Fallo simulado al actualizar cuartel")
            }
            override fun restaurarCuartel(jornalero: String, cuartelAnterior: String?) =
                DatosMemoriaCuartelStore.restaurarCuartel(jornalero, cuartelAnterior)
        }

        val repoAtomico = ParihuelaRepository(
            parihuelaDao = parihuelaDao,
            bitacoraDao = bitacoraDao,
            incidenciaDao = incidenciaDao,
            pesajeDao = pesajeDao,
            transaccionRunner = transaccionConRollback,
            reasignacionDao = reasignacionDao,
            cuartelJornaleroStore = storeConFallo
        )

        try {
            repoAtomico.reasignarJornaleroACuartel(
                jornalero = j1.nombreCompleto,
                cuartelDestino = "Cuartel 05",
                motivo = ParihuelaRepository.MOTIVO_REASIG_AVANCE_CUARTEL
            )
            fail("Debió propagar el error de la transacción")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        assertTrue(reasignacionDao.items.isEmpty())
        assertEquals("Cuartel 01", DatosMemoria.obtenerCuartelAsignado(j1.id))
    }

    @Test
    fun `registrarPesaje exige pesoKg mayor a cero sin alterar conteo ni cronometro`() = runBlocking {
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", Date(1_700_000_000_000L))

        try {
            repository.registrarPesaje("Juan Perez", 10, 0.0, "Biloxi", "Cuartel 01")
            fail("Debió rechazar pesoKg igual a cero")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        try {
            repository.registrarPesaje("Juan Perez", 10, -2.5, "Biloxi", "Cuartel 01")
            fail("Debió rechazar pesoKg negativo")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        val almacenada = parihuelaDao.getById(parihuela.id)!!
        assertEquals(0, almacenada.conteoJabas)
        assertNull(almacenada.horaInicio)
        assertTrue(pesajeDao.items.isEmpty())
    }

    @Test
    fun `calcularTasaRendimiento con tiempo transcurrido casi cero no marca alerta`() {
        val t0 = Date(1_700_000_000_000L)

        val enCero = ParihuelaRepository.calcularTasaRendimiento(0.5, t0, t0)
        val en30Seg = ParihuelaRepository.calcularTasaRendimiento(0.5, t0, Date(t0.time + 30_000L))

        assertNull(enCero.first)
        assertFalse(enCero.second)
        assertNull(en30Seg.first)
        assertFalse(en30Seg.second)
    }

    @Test
    fun `calcularTasaRendimiento aplica umbral proporcional de 8_75 kg por hora (35 kg en 4h)`() {
        val t0 = Date(1_700_000_000_000L)
        val en2h = Date(t0.time + 2L * 60L * 60L * 1000L)
        val en4h = Date(t0.time + 4L * 60L * 60L * 1000L)

        // Exacto en el umbral: 35 kg / 4 h = 8.75 kg/h -> no es bajo rendimiento
        val exacto4h = ParihuelaRepository.calcularTasaRendimiento(35.0, t0, en4h)
        assertEquals(8.75, exacto4h.first!!, 0.001)
        assertFalse(exacto4h.second)

        // Por debajo en 4 h: 34.0 kg / 4 h = 8.5 kg/h -> Bajo rendimiento
        val bajo4h = ParihuelaRepository.calcularTasaRendimiento(34.0, t0, en4h)
        assertEquals(8.5, bajo4h.first!!, 0.001)
        assertTrue(bajo4h.second)

        // Proporcional a 2 h: 17.5 kg -> 8.75 kg/h (OK); 16.0 kg -> 8.0 kg/h (Bajo rendimiento)
        val exacto2h = ParihuelaRepository.calcularTasaRendimiento(17.5, t0, en2h)
        assertEquals(8.75, exacto2h.first!!, 0.001)
        assertFalse(exacto2h.second)

        val bajo2h = ParihuelaRepository.calcularTasaRendimiento(16.0, t0, en2h)
        assertEquals(8.0, bajo2h.first!!, 0.001)
        assertTrue(bajo2h.second)
    }

    @Test
    fun `panel calcula kilos netos, jabas, primer pesaje, alerta y actualiza tras pesaje y reasignacion`() = runBlocking {
        val jBajo = DatosMemoria.listaJornaleros[0]
        val jAlto = DatosMemoria.listaJornaleros[1]
        DatosMemoria.asignarCuartel(jBajo.id, "Cuartel 01")
        DatosMemoria.asignarCuartel(jAlto.id, "Cuartel 01")

        val t0 = Date(1_700_000_000_000L)
        val t1h = Date(t0.time + 60L * 60L * 1000L)
        val t2h = Date(t0.time + 2L * 60L * 60L * 1000L)

        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje(jBajo.nombreCompleto, 5, 10.0, "Biloxi", "Cuartel 01", t0)
        repository.registrarPesaje(jAlto.nombreCompleto, 10, 20.0, "Biloxi", "Cuartel 01", t1h)

        // En t2h: jBajo lleva 10 kg en 2h (5.0 kg/h < 8.75 -> Bajo rendimiento); jAlto lleva 20 kg en 1h (20 kg/h -> OK)
        val resumenInicial = repository.observarResumenPanel { t2h }.first()
        assertEquals(30.0, resumenInicial.totalKilosNetos, 0.001)
        assertEquals(15, resumenInicial.totalJabas)
        assertEquals(2, resumenInicial.totalJornalerosActivos)
        assertEquals(30.0, resumenInicial.kilosPorCuartel["Cuartel 01"]!!, 0.001)

        val rendBajo = resumenInicial.rendimientoDetallado.first { it.jornalero == jBajo.nombreCompleto }
        assertEquals(10.0, rendBajo.totalKilos, 0.001)
        assertEquals(5, rendBajo.totalJabas)
        assertEquals(t0, rendBajo.horaPrimerPesaje)
        assertEquals(5.0, rendBajo.tasaKgPorHora!!, 0.001)
        assertTrue(rendBajo.bajoRendimiento)

        val rendAlto = resumenInicial.rendimientoDetallado.first { it.jornalero == jAlto.nombreCompleto }
        assertEquals(t1h, rendAlto.horaPrimerPesaje)
        assertEquals(20.0, rendAlto.tasaKgPorHora!!, 0.001)
        assertFalse(rendAlto.bajoRendimiento)

        // Se agrega nuevo pesaje de 10 kg a jBajo (total 20 kg en 2h = 10 kg/h >= 8.75 -> deja de estar en bajo rendimiento)
        repository.registrarPesaje(jBajo.nombreCompleto, 5, 10.0, "Biloxi", "Cuartel 01", t2h)
        val resumenTrasPesaje = repository.observarResumenPanel { t2h }.first()
        val actBajo = resumenTrasPesaje.jornalerosActivos.first { it.nombre == jBajo.nombreCompleto }
        assertEquals(20.0, actBajo.kilosNetos, 0.001)
        assertEquals(10.0, actBajo.tasaKgPorHora!!, 0.001)
        assertFalse(actBajo.bajoRendimiento)

        // Si pasan 2 horas más sin nuevos pesajes (t4h: 20 kg en 4h = 5.0 kg/h), vuelve a Bajo rendimiento y se reasigna
        val t4h = Date(t0.time + 4L * 60L * 60L * 1000L)
        repository.reasignarJornaleroACuartel(
            jornalero = jBajo.nombreCompleto,
            cuartelDestino = "Cuartel 05",
            motivo = ParihuelaRepository.MOTIVO_REASIG_REFUERZO_CARGA,
            hora = t4h
        )
        val resumenTrasReasig = repository.observarResumenPanel { t4h }.first()
        assertEquals(40.0, resumenTrasReasig.kilosPorCuartel["Cuartel 01"]!!, 0.001)
        assertNull(resumenTrasReasig.kilosPorCuartel["Cuartel 05"])
        val enCuartel05 = resumenTrasReasig.jornalerosPorCuartel["Cuartel 05"]!!.single()
        assertEquals(jBajo.nombreCompleto, enCuartel05.nombre)
        assertEquals(20.0, enCuartel05.kilosNetos, 0.001)
        assertTrue(enCuartel05.bajoRendimiento)
    }

    @Test
    fun `abrirNuevaParihuela exige CLP y cuadrilla obligatorios sin valores por defecto`() = runBlocking {
        try {
            repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "   ", "CUA-01")
            fail("Debió rechazar CLP en blanco")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        try {
            repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-101", "   ")
            fail("Debió rechazar cuadrilla en blanco")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        assertTrue(parihuelaDao.observeAll().first().isEmpty())
    }

    @Test
    fun `consulta de origen devuelve CLP, cuadrilla, cuartel, variedad, fecha y jabas desde codigo y trama QR`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val sellado = Date(1_700_000_240_000L)
        val parihuela = repository.abrirNuevaParihuela(
            variedad = "Ventura",
            cuartel = "Cuartel 12",
            clp = "CLP-550",
            cuadrilla = "CUA-07",
            ahora = inicio
        )
        repository.registrarPesaje("Juan Perez", 120, 230.5, "Ventura", "Cuartel 12", sellado)

        val trama = QrUtil.generarTramaQr(
            codigo = parihuela.codigo,
            clp = "CLP-550",
            cuartel = "Cuartel 12",
            variedad = "Ventura",
            jabas = 120,
            horaSellado = sellado
        )

        val desdeTrama = repository.consultarOrigenPorCodigo(trama) as ConsultaOrigenParihuela.Encontrado
        val desdeCodigoAntiguo = repository.consultarOrigenPorCodigo(parihuela.codigo) as ConsultaOrigenParihuela.Encontrado

        listOf(desdeTrama.origen, desdeCodigoAntiguo.origen).forEach { origen ->
            assertEquals(parihuela.codigo, origen.codigo)
            assertEquals("CLP-550", origen.clp)
            assertEquals("CUA-07", origen.cuadrilla)
            assertEquals("Cuartel 12", origen.cuartel)
            assertEquals("Ventura", origen.variedad)
            assertEquals(sellado, origen.horaSellado)
            assertEquals(120, origen.conteoJabas)
            assertEquals(EstadoParihuela.SELLADA, origen.estado)
        }
    }

    @Test
    fun `al sellar por limite registra en bitacora el tiempo total de retencion desde el primer pesaje`() = runBlocking {
        val apertura = Date(1_700_000_000_000L)
        val primerPesaje = Date(apertura.time + 10L * 60_000L)
        val sellado = Date(primerPesaje.time + 22L * 60_000L + 15_000L)

        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", apertura)
        repository.registrarPesaje("Juan Perez", 100, 180.0, "Biloxi", "Cuartel 01", primerPesaje)
        repository.registrarPesaje("Juan Perez", 20, 36.0, "Biloxi", "Cuartel 01", sellado)

        val retencion = bitacoraDao.items.firstOrNull {
            it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION
        }
        assertNotNull(retencion)
        assertEquals(sellado, retencion!!.fechaHora)
        // Debe medir 22 min 15 s (desde primerPesaje), no 32 min 15 s (desde apertura)
        assertTrue(retencion.detalle.contains("22 min 15 s"))
    }

    @Test
    fun `al sellar anticipadamente registra en bitacora el tiempo total de retencion o cero sin pesajes`() = runBlocking {
        val apertura = Date(1_700_000_000_000L)
        val primerPesaje = Date(apertura.time + 5L * 60_000L)
        val sellado = Date(primerPesaje.time + 15L * 60_000L + 30_000L)

        val conPesaje = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", apertura)
        repository.registrarPesaje("Juan Perez", 30, 54.0, "Biloxi", "Cuartel 01", primerPesaje)
        repository.sellarAnticipadamente(conPesaje.id, "Cierre de lote", sellado)

        val logConPesaje = bitacoraDao.items.firstOrNull {
            it.parihuelaId == conPesaje.id && it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION
        }
        assertNotNull(logConPesaje)
        assertTrue(logConPesaje!!.detalle.contains("15 min 30 s"))

        // Parihuela sellada anticipadamente sin ningun pesaje previo
        val sinPesaje = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", apertura)
        repository.sellarAnticipadamente(sinPesaje.id, "No se utilizó", Date(apertura.time + 60_000L))

        val logSinPesaje = bitacoraDao.items.firstOrNull {
            it.parihuelaId == sinPesaje.id && it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION
        }
        assertNotNull(logSinPesaje)
        assertTrue(logSinPesaje!!.detalle.contains("0 min 0 s"))
    }

    @Test
    fun `alerta de tiempo fallida no se marca como enviada, registra intento y reintenta al recuperar permiso`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)

        val enPorVencer = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - 3 * 60_000L)
        val sinPermiso = { _: ParihuelaEntity, _: EstadoTiempoEspera ->
            NotificacionTiempoUtil.ResultadoNotificacion(
                mostrada = false,
                motivoFallo = "Permiso POST_NOTIFICATIONS no concedido"
            )
        }

        // Primer intento falla: NO debe registrar ALERTA_POR_VENCER ni devolverla como notificada
        val resultadoFallido = repository.evaluarAlertasTiempo(enPorVencer, sinPermiso)
        assertTrue(resultadoFallido.isEmpty())
        assertFalse(bitacoraDao.existeEvento(parihuela.id, ParihuelaRepository.EVENTO_ALERTA_POR_VENCER))
        val intentosFallidos = bitacoraDao.items.filter {
            it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_INTENTO_ALERTA_FALLIDA
        }
        assertEquals(1, intentosFallidos.size)
        assertTrue(intentosFallidos.first().detalle.contains("POST_NOTIFICATIONS"))

        // Reintento cuando ya hay permiso: ahora si notifica y registra ALERTA_POR_VENCER una vez
        val reintentoExitoso = repository.evaluarAlertasTiempo(
            Date(enPorVencer.time + 15_000L)
        ) { _, _ -> NotificacionTiempoUtil.ResultadoNotificacion(mostrada = true) }
        assertEquals(listOf(parihuela.id), reintentoExitoso.map { it.id })
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_ALERTA_POR_VENCER
            }
        )

        // También aplica para EXCEDIDA si falla primero y luego se recupera
        val enExcedida = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 60_000L)
        assertTrue(repository.evaluarAlertasTiempo(enExcedida, sinPermiso).isEmpty())
        assertFalse(bitacoraDao.existeEvento(parihuela.id, ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA))
        assertEquals(
            listOf(parihuela.id),
            repository.evaluarAlertasTiempo(enExcedida) { _, _ ->
                NotificacionTiempoUtil.ResultadoNotificacion(mostrada = true)
            }.map { it.id }
        )
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA
            }
        )
    }

    @Test
    fun `generarCodigoUnico deriva secuencia de la base sin colisionar tras reinicio con mismo timestamp`() = runBlocking {
        val mismoInstante = Date(1_700_000_000_000L)
        val primera = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", mismoInstante)
        repository.sellarAnticipadamente(primera.id, "Lote completo", mismoInstante)

        // Nueva instancia simulando reinicio de app sobre los mismos DAOs
        val repoTrasReinicio = ParihuelaRepository(
            parihuelaDao = parihuelaDao,
            bitacoraDao = bitacoraDao,
            incidenciaDao = incidenciaDao,
            pesajeDao = pesajeDao,
            reasignacionDao = reasignacionDao
        )
        val segunda = repoTrasReinicio.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", mismoInstante)
        assertNotEquals(primera.codigo, segunda.codigo)
        assertTrue(segunda.codigo.endsWith("-0002"))
    }

    @Test
    fun `evaluarAlertasTiempo concurrente entre multiples repositorios notifica una sola vez`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 5, 10.0, "Biloxi", "Cuartel 01", inicio)
        val enPorVencer = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - 2 * 60_000L)

        val repoWorkerExacto = ParihuelaRepository(parihuelaDao, bitacoraDao, incidenciaDao, pesajeDao, reasignacionDao = reasignacionDao)
        val repoWorkerPeriodico = ParihuelaRepository(parihuelaDao, bitacoraDao, incidenciaDao, pesajeDao, reasignacionDao = reasignacionDao)

        val resultados = coroutineScope {
            listOf(
                async { repoWorkerExacto.evaluarAlertasTiempo(enPorVencer) },
                async { repoWorkerPeriodico.evaluarAlertasTiempo(enPorVencer) }
            ).awaitAll()
        }

        val totalAvisos = resultados.sumOf { it.size }
        assertEquals(1, totalAvisos)
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_ALERTA_POR_VENCER
            }
        )
    }

    @Test
    fun `parihuela autoabierta al sellar 120 nace sin cronometro y lo inicia en su primer pesaje`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val primera = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        val sellado = repository.registrarPesaje("Juan Perez", 120, 240.0, "Biloxi", "Cuartel 01", t0)
            as ResultadoPesaje.Registrado
        val segunda = sellado.resultadoJabas.nuevaParihuela!!

        assertNull(segunda.horaInicio)
        assertFalse(repository.calcularTiempoEspera(segunda, Date(t0.time + 60L * 60_000L)).cronometroIniciado)

        val tPrimerPesajeSegunda = Date(t0.time + 20L * 60_000L)
        val resSegunda = repository.registrarPesaje(
            "Juan Perez",
            10,
            20.0,
            "Biloxi",
            "Cuartel 01",
            tPrimerPesajeSegunda
        ) as ResultadoPesaje.Registrado

        assertTrue(resSegunda.resultadoJabas.cronometroIniciadoEnEstePesaje)
        assertEquals(tPrimerPesajeSegunda, resSegunda.resultadoJabas.parihuela.horaInicio)
        assertNotEquals(primera.id, resSegunda.resultadoJabas.parihuela.id)
    }

    @Test
    fun `daos falsos aplican unicidad de codigo, claves foraneas, orden DESC y flows reactivos`() = runBlocking {
        val t1 = Date(1_700_000_000_000L)
        val t2 = Date(1_700_000_060_000L)
        val id = parihuelaDao.insert(
            ParihuelaEntity(codigo = "PAR-UNICO", variedad = "Biloxi", cuartel = "Cuartel 01", clp = "CLP-01", cuadrilla = "CUA-01")
        )

        // Unicidad de codigo
        try {
            parihuelaDao.insert(
                ParihuelaEntity(codigo = "PAR-UNICO", variedad = "Biloxi", cuartel = "Cuartel 01", clp = "CLP-01", cuadrilla = "CUA-01")
            )
            fail("Debió rechazar código duplicado")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        // Claves foraneas en bitacora, incidencias y pesajes
        try {
            bitacoraDao.insert(BitacoraEntity(parihuelaId = 9999L, evento = "TEST", detalle = "huerfano", fechaHora = t1))
            fail("Debió rechazar bitácora sin parihuela padre")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
        try {
            incidenciaDao.insert(IncidenciaEntity(parihuelaId = 9999L, tipo = "TEST", detalle = "huerfano", fechaHora = t1))
            fail("Debió rechazar incidencia sin parihuela padre")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
        try {
            pesajeDao.insert(PesajeEntity(parihuelaId = 9999L, jornalero = "X", cantidadJabas = 1, pesoKg = 2.0, fechaHora = t1))
            fail("Debió rechazar pesaje sin parihuela padre")
        } catch (expected: IllegalStateException) {
            // Esperado
        }

        // Orden DESC y reactividad real
        bitacoraDao.insert(BitacoraEntity(parihuelaId = id, evento = "E1", detalle = "primero", fechaHora = t1))
        bitacoraDao.insert(BitacoraEntity(parihuelaId = id, evento = "E2", detalle = "segundo", fechaHora = t2))
        val ordenBitacora = bitacoraDao.observeByParihuela(id).first()
        assertEquals(listOf("E2", "E1"), ordenBitacora.map { it.evento })

        val emisiones = mutableListOf<List<PesajeEntity>>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            pesajeDao.observeByParihuela(id).take(2).toList(emisiones)
        }
        pesajeDao.insert(PesajeEntity(parihuelaId = id, jornalero = "Ana", cantidadJabas = 2, pesoKg = 4.0, fechaHora = t1))
        withTimeout(5_000L) {
            collector.join()
        }
        assertEquals(2, emisiones.size)
        assertEquals(0, emisiones[0].size)
        assertEquals(1, emisiones[1].size)
    }

    @Test
    fun `pesaje de ayer no cuenta como primer pesaje ni alimenta tasa ni alerta de hoy en America Lima`() = runBlocking {
        val lima = TimeZone.getTimeZone("America/Lima")
        fun fechaLima(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Date =
            Calendar.getInstance(lima).apply {
                set(anio, mes - 1, dia, hora, minuto, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

        val j1 = DatosMemoria.listaJornaleros[0]
        DatosMemoria.asignarCuartel(j1.id, "Cuartel 01")

        // 22:30 Lima del 19/11 ya es 03:30 UTC del 20/11: valida filtro por America/Lima (no UTC)
        val ayer2230 = fechaLima(2026, 11, 19, 22, 30)
        val hoy0800 = fechaLima(2026, 11, 20, 8, 0)
        val hoy0900 = fechaLima(2026, 11, 20, 9, 0)
        val hoy1000 = fechaLima(2026, 11, 20, 10, 0)

        val pAyer = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", ayer2230)
        repository.registrarPesaje(j1.nombreCompleto, 2, 5.0, "Biloxi", "Cuartel 01", ayer2230)
        repository.sellarAnticipadamente(pAyer.id, "Cierre día anterior", ayer2230)

        repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", hoy0800)
        repository.registrarPesaje(j1.nombreCompleto, 5, 10.0, "Biloxi", "Cuartel 01", hoy0800)
        repository.registrarPesaje(j1.nombreCompleto, 5, 10.0, "Biloxi", "Cuartel 01", hoy0900)

        // En hoy1000: solo hoy -> 20 kg / 2 h = 10.0 kg/h >= 8.75 (OK). Con ayer sería 25 kg / 11.5 h = 2.17 kg/h (falsa alerta).
        val resumen = repository.observarResumenPanel { hoy1000 }.first()
        assertEquals(1, resumen.totalAbiertas)
        assertEquals(1, resumen.totalSelladasAnticipadas)

        val rend = resumen.rendimientoDetallado.single { it.jornalero == j1.nombreCompleto }
        assertEquals(hoy0800, rend.horaPrimerPesaje)
        assertEquals(20.0, rend.totalKilos, 0.001)
        assertEquals(10, rend.totalJabas)
        assertEquals(10.0, rend.tasaKgPorHora!!, 0.001)
        assertFalse(rend.bajoRendimiento)

        val activo = resumen.jornalerosActivos.single { it.nombre == j1.nombreCompleto }
        assertEquals(hoy0800, activo.horaPrimerPesaje)
        assertEquals(20.0, activo.kilosNetos, 0.001)
        assertEquals(10.0, activo.tasaKgPorHora!!, 0.001)
        assertFalse(activo.bajoRendimiento)

        val rendDirecto = repository.observarRendimientoDetalladoPorJornalero { hoy1000 }.first().single()
        assertEquals(hoy0800, rendDirecto.horaPrimerPesaje)
        assertEquals(20.0, rendDirecto.totalKilos, 0.001)
        assertFalse(rendDirecto.bajoRendimiento)
    }

    @Test
    fun `tras reasignar jornalero sus kilos previos quedan en cuartel origen y kilosPorCuartel cuadra con jabasPorCuartel`() = runBlocking {
        val j1 = DatosMemoria.listaJornaleros[0]
        val j2 = DatosMemoria.listaJornaleros[1]
        DatosMemoria.asignarCuartel(j1.id, "Cuartel 01")
        DatosMemoria.asignarCuartel(j2.id, "Cuartel 01")

        val t0 = Date(1_700_000_000_000L)
        val t1 = Date(t0.time + 30L * 60_000L)
        val t2 = Date(t0.time + 60L * 60_000L)

        val p1 = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje(j1.nombreCompleto, 10, 20.0, "Biloxi", "Cuartel 01", t0)
        repository.registrarPesaje(j2.nombreCompleto, 10, 25.0, "Biloxi", "Cuartel 01", t0)

        repository.reasignarJornaleroACuartel(
            jornalero = j1.nombreCompleto,
            cuartelDestino = "Cuartel 05",
            motivo = ParihuelaRepository.MOTIVO_REASIG_REFUERZO_CARGA,
            hora = t1
        )

        val resumenTrasReasig = repository.observarResumenPanel { t1 }.first()
        assertEquals(mapOf("Cuartel 01" to 20), resumenTrasReasig.jabasPorCuartel)
        assertEquals(mapOf("Cuartel 01" to 45.0), resumenTrasReasig.kilosPorCuartel)
        assertNull(resumenTrasReasig.kilosPorCuartel["Cuartel 05"])
        assertEquals(resumenTrasReasig.jabasPorCuartel.keys, resumenTrasReasig.kilosPorCuartel.keys)
        assertEquals(j1.nombreCompleto, resumenTrasReasig.jornalerosPorCuartel["Cuartel 05"]!!.single().nombre)

        // Al abrir parihuela en Cuartel 05 y registrar un nuevo pesaje allí, ambos cuarteles cuadran
        repository.sellarAnticipadamente(p1.id, "Cambio de frente", t2)
        repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-01", t2)
        repository.registrarPesaje(j1.nombreCompleto, 5, 12.5, "Ventura", "Cuartel 05", t2)

        val resumenConDestino = repository.observarResumenPanel { t2 }.first()
        assertEquals(mapOf("Cuartel 01" to 20, "Cuartel 05" to 5), resumenConDestino.jabasPorCuartel)
        assertEquals(mapOf("Cuartel 01" to 45.0, "Cuartel 05" to 12.5), resumenConDestino.kilosPorCuartel)
        assertEquals(resumenConDestino.jabasPorCuartel.keys, resumenConDestino.kilosPorCuartel.keys)
    }

    @Test
    fun `parihuela SELLADA recibe alerta POR_VENCER y marca RETENCION_EXCEDIDA una sola vez`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val sellado = Date(inicio.time + 15L * 60_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 100, 180.0, "Biloxi", "Cuartel 01", inicio)
        repository.registrarPesaje("Juan Perez", 20, 36.0, "Biloxi", "Cuartel 01", sellado)
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(parihuela.id)!!.estado)

        val enPorVencer = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - 3L * 60_000L)
        val avisoPorVencer = repository.evaluarAlertasTiempo(enPorVencer)
        assertEquals(listOf(parihuela.id), avisoPorVencer.map { it.id })
        assertTrue(repository.evaluarAlertasTiempo(Date(enPorVencer.time + 30_000L)).isEmpty())
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_ALERTA_POR_VENCER
            }
        )

        val enExcedida = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 60_000L)
        val avisoExcedida = repository.evaluarAlertasTiempo(enExcedida)
        assertEquals(listOf(parihuela.id), avisoExcedida.map { it.id })
        assertTrue(repository.evaluarAlertasTiempo(Date(enExcedida.time + 60_000L)).isEmpty())
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA
            }
        )
    }

    @Test
    fun `parihuela RETENIDA_CALIDAD recibe alerta POR_VENCER y marca RETENCION_EXCEDIDA`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val tMezcla = Date(inicio.time + 10L * 60_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", inicio)
        repository.registrarPesaje("Juan Perez", 10, 18.0, "Biloxi", "Cuartel 01", inicio)

        val rechazo = repository.registrarPesaje("Juan Perez", 2, 3.6, "Ventura", "Cuartel 01", tMezcla)
            as ResultadoPesaje.RechazadoPorMezcla
        repository.resolverIncidencia(
            incidenciaId = rechazo.incidencia.id,
            accion = ParihuelaRepository.ACCION_REVISAR_LOTE_PARIHUELA,
            responsable = "Calidad",
            ahora = tMezcla
        )
        assertEquals(EstadoParihuela.RETENIDA_CALIDAD, parihuelaDao.getById(parihuela.id)!!.estado)

        val enPorVencer = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS - 4L * 60_000L)
        val avisoPorVencer = repository.evaluarAlertasTiempo(enPorVencer)
        assertEquals(listOf(parihuela.id), avisoPorVencer.map { it.id })
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_ALERTA_POR_VENCER
            }
        )

        val enExcedida = Date(inicio.time + ParihuelaRepository.LIMITE_ESPERA_MS + 2L * 60_000L)
        val avisoExcedida = repository.evaluarAlertasTiempo(enExcedida)
        assertEquals(listOf(parihuela.id), avisoExcedida.map { it.id })
        assertEquals(
            1,
            bitacoraDao.items.count {
                it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_RETENCION_EXCEDIDA
            }
        )
    }

    @Test
    fun `obtenerEstadoTiempoParihuela devuelve estado para SELLADA y RETENIDA_CALIDAD y null sin pesajes`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val tConsulta = Date(t0.time + 20L * 60_000L)

        // Sin pesajes -> null
        val sinPesajes = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        assertNull(repository.obtenerEstadoTiempoParihuela(sinPesajes.id, tConsulta))

        // Con primer pesaje y luego SELLADA -> devuelve estado de tiempo activo
        repository.registrarPesaje("Juan Perez", 100, 180.0, "Biloxi", "Cuartel 01", t0)
        val resSellado = repository.registrarPesaje("Juan Perez", 20, 36.0, "Biloxi", "Cuartel 01", Date(t0.time + 5L * 60_000L))
            as ResultadoPesaje.Registrado
        assertEquals(EstadoParihuela.SELLADA, parihuelaDao.getById(sinPesajes.id)!!.estado)

        val estadoSellada = repository.obtenerEstadoTiempoParihuela(sinPesajes.id, tConsulta)
        assertNotNull(estadoSellada)
        assertTrue(estadoSellada!!.cronometroIniciado)
        assertEquals(25L * 60_000L, estadoSellada.restanteMs)
        assertEquals(EstadoTiempoEspera.EN_ESPERA, estadoSellada.estado)

        // Siguiente parihuela autoabierta nace sin pesajes -> null; tras pesaje y RETENIDA_CALIDAD -> devuelve estado
        val segunda = resSellado.resultadoJabas.nuevaParihuela!!
        assertNull(repository.obtenerEstadoTiempoParihuela(segunda.id, tConsulta))
        repository.registrarPesaje("Juan Perez", 10, 18.0, "Biloxi", "Cuartel 01", t0)
        val mezcla = repository.registrarPesaje("Juan Perez", 1, 2.0, "Ventura", "Cuartel 01", Date(t0.time + 60_000L))
            as ResultadoPesaje.RechazadoPorMezcla
        repository.resolverIncidencia(
            incidenciaId = mezcla.incidencia.id,
            accion = ParihuelaRepository.ACCION_REVISAR_LOTE_PARIHUELA,
            responsable = "Calidad",
            ahora = Date(t0.time + 60_000L)
        )
        assertEquals(EstadoParihuela.RETENIDA_CALIDAD, parihuelaDao.getById(segunda.id)!!.estado)

        val estadoRetenida = repository.obtenerEstadoTiempoParihuela(
            segunda.id,
            Date(t0.time + ParihuelaRepository.LIMITE_ESPERA_MS - 2L * 60_000L)
        )
        assertNotNull(estadoRetenida)
        assertTrue(estadoRetenida!!.cronometroIniciado)
        assertEquals(2L * 60_000L, estadoRetenida.restanteMs)
        assertEquals(EstadoTiempoEspera.POR_VENCER, estadoRetenida.estado)
    }

    @Test
    fun `sellado en meta a las 100 y a las 119 jabas queda SELLADA sin motivo y con evento en bitacora`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val tSellado100 = Date(t0.time + 12L * 60_000L)
        val p100 = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje("Juan Perez", 100, 180.0, "Biloxi", "Cuartel 01", t0)

        val sellada100 = repository.sellarEnMeta(p100.id, tSellado100)
        assertEquals(EstadoParihuela.SELLADA, sellada100.estado)
        assertEquals(p100.codigo, sellada100.codigo)
        assertEquals(tSellado100, sellada100.horaSellado)
        assertNull(sellada100.motivoSelladoAnticipado)
        assertEquals(10, sellada100.pisos)
        val eventos100 = bitacoraDao.items.filter { it.parihuelaId == p100.id }
        assertTrue(eventos100.any { it.evento == ParihuelaRepository.EVENTO_SELLADO_META && it.fechaHora == tSellado100 })
        assertTrue(eventos100.any { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION })

        val t1 = Date(tSellado100.time + 60_000L)
        val tSellado119 = Date(t1.time + 20L * 60_000L)
        val p119 = repository.abrirNuevaParihuela("Ventura", "Cuartel 05", "CLP-05", "CUA-02", t1)
        repository.registrarPesaje("Maria Lopez", 119, 214.2, "Ventura", "Cuartel 05", t1)

        val sellada119 = repository.sellarEnMeta(p119.id, tSellado119)
        assertEquals(EstadoParihuela.SELLADA, sellada119.estado)
        assertEquals(p119.codigo, sellada119.codigo)
        assertEquals(tSellado119, sellada119.horaSellado)
        assertNull(sellada119.motivoSelladoAnticipado)
        assertEquals(11, sellada119.pisos)
        val eventos119 = bitacoraDao.items.filter { it.parihuelaId == p119.id }
        assertTrue(eventos119.any { it.evento == ParihuelaRepository.EVENTO_SELLADO_META && it.fechaHora == tSellado119 })
        assertTrue(eventos119.any { it.evento == ParihuelaRepository.EVENTO_TIEMPO_RETENCION })
    }

    @Test
    fun `sellado en meta con 99 jabas es rechazado con error claro sin alterar la parihuela`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje("Juan Perez", 99, 178.2, "Biloxi", "Cuartel 01", t0)

        try {
            repository.sellarEnMeta(parihuela.id, Date(t0.time + 60_000L))
            fail("Debió rechazar sellado en meta con 99 jabas")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("sellado anticipado"))
        }

        val persistida = parihuelaDao.getById(parihuela.id)!!
        assertEquals(EstadoParihuela.ABIERTA, persistida.estado)
        assertNull(persistida.horaSellado)
        assertTrue(bitacoraDao.items.isEmpty())
    }

    @Test
    fun `sellado anticipado bajo 100 jabas sigue exigiendo motivo y deja estado SELLADA_ANTICIPADA`() = runBlocking {
        val t0 = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        repository.registrarPesaje("Juan Perez", 60, 108.0, "Biloxi", "Cuartel 01", t0)

        try {
            repository.sellarAnticipadamente(parihuela.id, "   ", Date(t0.time + 60_000L))
            fail("El sellado anticipado bajo 100 debe seguir exigiendo motivo")
        } catch (expected: IllegalArgumentException) {
            // Esperado
        }

        val sellada = repository.sellarAnticipadamente(parihuela.id, "Cierre por clima", Date(t0.time + 120_000L))
        assertEquals(EstadoParihuela.SELLADA_ANTICIPADA, sellada.estado)
        assertEquals("Cierre por clima", sellada.motivoSelladoAnticipado)
        assertTrue(bitacoraDao.items.any { it.parihuelaId == parihuela.id && it.evento == ParihuelaRepository.EVENTO_SELLADO_ANTICIPADO })
    }

    @Test
    fun `calculo de pisos y actualizacion por pesaje cumple techo de jabas entre 11`() = runBlocking {
        assertEquals(0, ParihuelaRepository.calcularPisos(0))
        assertEquals(1, ParihuelaRepository.calcularPisos(1))
        assertEquals(1, ParihuelaRepository.calcularPisos(11))
        assertEquals(2, ParihuelaRepository.calcularPisos(12))
        assertEquals(10, ParihuelaRepository.calcularPisos(100))
        assertEquals(10, ParihuelaRepository.calcularPisos(110))
        assertEquals(11, ParihuelaRepository.calcularPisos(119))
        assertEquals(11, ParihuelaRepository.calcularPisos(120))

        val t0 = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", "CLP-01", "CUA-01", t0)
        assertEquals(0, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 1, 2.0, "Biloxi", "Cuartel 01", t0)
        assertEquals(1, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01", t0) // 11
        assertEquals(1, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 1, 2.0, "Biloxi", "Cuartel 01", t0) // 12
        assertEquals(2, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 88, 176.0, "Biloxi", "Cuartel 01", t0) // 100
        assertEquals(10, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 10, 20.0, "Biloxi", "Cuartel 01", t0) // 110
        assertEquals(10, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 9, 18.0, "Biloxi", "Cuartel 01", t0) // 119
        assertEquals(11, parihuelaDao.getById(parihuela.id)!!.pisos)

        repository.registrarPesaje("Juan Perez", 1, 2.0, "Biloxi", "Cuartel 01", t0) // 120 (sellada)
        assertEquals(11, parihuelaDao.getById(parihuela.id)!!.pisos)
        val origen = repository.consultarOrigenPorCodigo(parihuela.codigo) as ConsultaOrigenParihuela.Encontrado
        assertEquals(11, origen.origen.pisos)
    }

    private class FakeParihuelaDao : ParihuelaDao {
        private var seq = 0L
        private val data = linkedMapOf<Long, ParihuelaEntity>()
        private val cambios = MutableStateFlow(0L)

        fun existe(id: Long): Boolean = data.containsKey(id)

        override suspend fun insert(parihuela: ParihuelaEntity): Long {
            check(data.values.none { it.codigo == parihuela.codigo }) {
                "UNIQUE constraint failed: parihuelas.codigo"
            }
            val id = ++seq
            data[id] = parihuela.copy(id = id)
            cambios.value++
            return id
        }

        override suspend fun update(parihuela: ParihuelaEntity) {
            if (!data.containsKey(parihuela.id)) return
            check(data.values.none { it.id != parihuela.id && it.codigo == parihuela.codigo }) {
                "UNIQUE constraint failed: parihuelas.codigo"
            }
            data[parihuela.id] = parihuela
            cambios.value++
        }

        override suspend fun getById(id: Long): ParihuelaEntity? = data[id]

        override suspend fun getByCodigo(codigo: String): ParihuelaEntity? =
            data.values.firstOrNull { it.codigo == codigo }

        override suspend fun obtenerMaxId(): Long = data.keys.maxOrNull() ?: 0L

        override fun observeByEstado(estado: EstadoParihuela): Flow<List<ParihuelaEntity>> =
            cambios.map {
                data.values
                    .filter { it.estado == estado }
                    .sortedWith(
                        compareByDescending<ParihuelaEntity> { it.horaInicio?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
            }

        override fun observeAll(): Flow<List<ParihuelaEntity>> =
            cambios.map {
                data.values.sortedWith(
                    compareByDescending<ParihuelaEntity> { it.horaInicio?.time ?: 0L }
                        .thenByDescending { it.id }
                )
            }
    }

    private class FakeBitacoraDao(
        private val existeParihuela: (Long) -> Boolean
    ) : BitacoraDao {
        private var seq = 0L
        val items = mutableListOf<BitacoraEntity>()
        private val cambios = MutableStateFlow(0L)

        override suspend fun insert(bitacora: BitacoraEntity): Long {
            check(existeParihuela(bitacora.parihuelaId)) {
                "FOREIGN KEY constraint failed: bitacora.parihuela_id=${bitacora.parihuelaId}"
            }
            val id = ++seq
            items.add(bitacora.copy(id = id))
            cambios.value++
            return id
        }

        override suspend fun existeEvento(parihuelaId: Long, evento: String): Boolean =
            items.any { it.parihuelaId == parihuelaId && it.evento == evento }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<BitacoraEntity>> =
            cambios.map {
                items.filter { it.parihuelaId == parihuelaId }
                    .sortedByDescending { it.fechaHora.time }
            }

        override fun observeAll(): Flow<List<BitacoraEntity>> =
            cambios.map { items.sortedByDescending { it.fechaHora.time } }
    }

    private class FakeIncidenciaDao(
        private val existeParihuela: (Long) -> Boolean
    ) : IncidenciaDao {
        private var seq = 0L
        val items = mutableListOf<IncidenciaEntity>()
        private val cambios = MutableStateFlow(0L)

        override suspend fun insert(incidencia: IncidenciaEntity): Long {
            check(existeParihuela(incidencia.parihuelaId)) {
                "FOREIGN KEY constraint failed: incidencias.parihuela_id=${incidencia.parihuelaId}"
            }
            val id = ++seq
            items.add(incidencia.copy(id = id))
            cambios.value++
            return id
        }

        override suspend fun update(incidencia: IncidenciaEntity) {
            val idx = items.indexOfFirst { it.id == incidencia.id }
            if (idx >= 0) {
                items[idx] = incidencia
                cambios.value++
            }
        }

        override suspend fun getById(id: Long): IncidenciaEntity? =
            items.firstOrNull { it.id == id }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<IncidenciaEntity>> =
            cambios.map {
                items.filter { it.parihuelaId == parihuelaId }
                    .sortedByDescending { it.fechaHora.time }
            }

        override fun observeAll(): Flow<List<IncidenciaEntity>> =
            cambios.map { items.sortedByDescending { it.fechaHora.time } }
    }

    private class FakePesajeDao(
        private val existeParihuela: (Long) -> Boolean
    ) : PesajeDao {
        private var seq = 0L
        val items = mutableListOf<PesajeEntity>()
        private val cambios = MutableStateFlow(0L)

        override suspend fun insert(pesaje: PesajeEntity): Long {
            check(existeParihuela(pesaje.parihuelaId)) {
                "FOREIGN KEY constraint failed: pesajes.parihuela_id=${pesaje.parihuelaId}"
            }
            val id = ++seq
            items.add(pesaje.copy(id = id))
            cambios.value++
            return id
        }

        override suspend fun update(pesaje: PesajeEntity) {
            val idx = items.indexOfFirst { it.id == pesaje.id }
            if (idx >= 0) {
                items[idx] = pesaje
                cambios.value++
            }
        }

        override suspend fun getById(id: Long): PesajeEntity? = items.firstOrNull { it.id == id }

        override fun observeByParihuela(parihuelaId: Long): Flow<List<PesajeEntity>> =
            cambios.map {
                items.filter { it.parihuelaId == parihuelaId }
                    .sortedByDescending { it.fechaHora.time }
            }

        override fun observeAll(): Flow<List<PesajeEntity>> =
            cambios.map { items.sortedByDescending { it.fechaHora.time } }
    }

    private class FakeReasignacionDao : ReasignacionDao {
        private var seq = 0L
        val items = mutableListOf<ReasignacionEntity>()
        private val cambios = MutableStateFlow(0L)

        override suspend fun insert(reasignacion: ReasignacionEntity): Long {
            val id = ++seq
            items.add(reasignacion.copy(id = id))
            cambios.value++
            return id
        }

        override fun observeAll(): Flow<List<ReasignacionEntity>> =
            cambios.map {
                items.sortedWith(
                    compareByDescending<ReasignacionEntity> { it.hora.time }
                        .thenByDescending { it.id }
                )
            }

        override suspend fun getUltimaPorJornalero(jornalero: String): ReasignacionEntity? =
            items.filter { it.jornalero.equals(jornalero, ignoreCase = true) }
                .maxWithOrNull(compareBy<ReasignacionEntity> { it.hora.time }.thenBy { it.id })
    }
}
