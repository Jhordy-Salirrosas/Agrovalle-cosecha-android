package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.IncidenciaDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.IncidenciaEntity
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
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
    private lateinit var repository: ParihuelaRepository

    @Before
    fun setUp() {
        parihuelaDao = FakeParihuelaDao()
        bitacoraDao = FakeBitacoraDao()
        incidenciaDao = FakeIncidenciaDao()
        repository = ParihuelaRepository(parihuelaDao, bitacoraDao, incidenciaDao)
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
            flowOf(data.values.filter { it.estado == estado })

        override fun observeAll(): Flow<List<ParihuelaEntity>> =
            flowOf(data.values.toList())
    }

    private class FakeBitacoraDao : BitacoraDao {
        private var seq = 0L
        val items = mutableListOf<BitacoraEntity>()

        override suspend fun insert(bitacora: BitacoraEntity): Long {
            val id = ++seq
            items.add(bitacora.copy(id = id))
            return id
        }

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
}
