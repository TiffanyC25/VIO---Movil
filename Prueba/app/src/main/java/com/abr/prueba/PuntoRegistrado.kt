package com.abr.prueba

data class PuntoRegistrado(
    var puntoId: String = "",
    var nombre: String = "",
    var orden: Int = 0,
    var tiempoLlegada: Long = 0L,
    var tiempoDesdeAnterior: Long = 0L,
    var tiempoAcumulado: Long = 0L
)
