package com.abr.prueba

data class PuntoRuta(
    var id: String = "",
    var nombre: String = "",
    var latitud: Double = 0.0,
    var longitud: Double = 0.0,
    var orden: Int = 0,
    var tipo: String = "marca"
)
