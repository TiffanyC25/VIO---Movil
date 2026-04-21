package com.abr.prueba

data class Ruta(
    var id: String = "",
    var nombre: String = "",
    var descripcion: String = "",
    var activa: Boolean = true,
    var radioDeteccion: Float = 30f, //Distancia de recepcion al punto de marca
    var creadaEn: Long = System.currentTimeMillis()
)
//Sin layout
