package com.agrovalle.cosecha.data.local

import com.agrovalle.cosecha.data.local.entity.CuadrillaEntity
import com.agrovalle.cosecha.data.local.entity.JornaleroEntity

/**
 * Precarga del padron local de la cuadrilla (25 a 35 jornaleros) - AGV-001.
 *
 * Se ejecuta una sola vez, al inicializar la aplicacion. Si ya existen datos,
 * no vuelve a sembrar (asi el padron persiste entre reinicios).
 */
object PadronSeeder {

    const val CUADRILLA_CODIGO = "CUA-01"
    private const val SUPERVISOR = "Jacinto Quispe Huaman"
    private const val FUNDO = "Fundo Principal Viru"

    /** Tamano del padron precargado (dentro del rango 25-35 de la HU). */
    const val TOTAL_PADRON = 30

    private val NOMBRES = listOf(
        "Juan", "Maria", "Pedro", "Luis", "Ana", "Carlos", "Jose", "Rosa",
        "Miguel", "Carmen", "Jorge", "Elena", "Victor", "Gloria", "Fernando",
        "Lucia", "Roberto", "Yolanda", "Julio", "Marisol", "Segundo", "Rita",
        "Manuel", "Elvira", "Segundo", "Teresa", "Angel", "Marleny", "Santos", "Julia"
    )

    private val APELLIDOS = listOf(
        "Perez", "Sanchez", "Lopez", "Mendoza", "Garcia", "Alva", "Ramirez",
        "Torres", "Huaman", "Diaz", "Rodriguez", "Quispe", "Flores", "Rojas",
        "Vasquez", "Chavez", "Castro", "Vargas", "Salirrosas", "Toledo",
        "Cabanillas", "Esquivel", "Namay", "Bocanegra", "Vereau", "Plasencia"
    )

    suspend fun sembrarSiVacio(db: AgroValleDatabase) {
        if (db.cuadrillaDao().contar() > 0 || db.jornaleroDao().contar() > 0) return

        db.cuadrillaDao().insertar(
            CuadrillaEntity(
                codigo = CUADRILLA_CODIGO,
                nombreSupervisor = SUPERVISOR,
                fundoCuartel = FUNDO
            )
        )

        val jornaleros = (0 until TOTAL_PADRON).map { i ->
            val nombres = NOMBRES[i % NOMBRES.size]
            val apellidos = "${APELLIDOS[i % APELLIDOS.size]} ${APELLIDOS[(i + 7) % APELLIDOS.size]}"
            val dni = "%08d".format(40_000_000L + (i * 137L) + 1234L)
            JornaleroEntity(
                dni = dni,
                codigoQr = "QR_AGV_%03d".format(i + 1),
                nombres = nombres,
                apellidos = apellidos,
                cuadrillaCodigo = CUADRILLA_CODIGO
            )
        }
        db.jornaleroDao().insertarTodos(jornaleros)
    }
}
