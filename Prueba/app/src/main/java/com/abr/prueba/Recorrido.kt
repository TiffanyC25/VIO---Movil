package com.abr.prueba

data class Recorrido(
    var id: String = "",
    var rutaId: String = "",
    val rutaNombre: String = "",
    var usuarioId: String = "",
    var inicioTiempo: Long = 0L,
    var finTiempo: Long = 0L,
    var tiempoTotal: Long = 0L,
    var estado: String = "en_proceso"
)
