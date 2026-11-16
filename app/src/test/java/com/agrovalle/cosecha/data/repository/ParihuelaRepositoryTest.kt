package com.agrovalle.cosecha.data.repository

import com.agrovalle.cosecha.data.local.dao.BitacoraDao
import com.agrovalle.cosecha.data.local.dao.ParihuelaDao
import com.agrovalle.cosecha.data.local.entity.BitacoraEntity
import com.agrovalle.cosecha.data.local.entity.EstadoParihuela
import com.agrovalle.cosecha.data.local.entity.ParihuelaEntity
import kotlinx.coroutines.flow.Flow
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
    private lateinit var repository: ParihuelaRepository

    @Before
    fun setUp() {
        parihuelaDao = FakeParihuelaDao()
        bitacoraDao = FakeBitacoraDao()
        repository = ParihuelaRepository(parihuelaDao, bitacoraDao)
    }

    @Test
    fun `con 119 jabas no se sella automaticamente`() = runBlocking {
        val inicio = Date(1_700_000_000_000L)
        val parihuela = repository.abrirNuevaParihuela("Biloxi", "Cuartel 01", inicio)

        val resultado = repository.registrarJabas(parihuela.id, 119, Date(1_700_000_060_000L))

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

        repository.registrarJabas(primera.id, 119, Date(1_700_000_060_000L))
        val resultado = repository.registrarJabas(primera.id, 1, sellado)

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
        repository.registrarJabas(parihuela.id, 45, Date(1_700_000_100_000L))

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
            repository.registrarJabas(parihuela.id, 1)
            fail("No debería permitir modificar el conteo de una parihuela sellada")
        } catch (expected: IllegalStateException) {
            // Esperado
        }
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
}
