package com.abr.prueba

data class PuntoSeguimientoUI(
    var id: String = "",
    var nombre: String = "",
    var orden: Int = 0,
    var tipo: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var estado: String = ConstantesRutas.ESTADO_PENDIENTE,   // pendiente | siguiente | completado
    var tiempoDesdeAnterior: String = "--",
    var tiempoAcumulado: String = "--"
)
