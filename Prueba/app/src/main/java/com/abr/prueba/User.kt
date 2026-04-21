package com.abr.prueba

data class User(
    var name: String      = "",
    var email: String     = "",
    var password: String  = "",
    var uid: String       = "",
    var apellidos: String = "",
    var telefono: String  = "",
    var esAdmin: Boolean = false
) {
    // Constructor vacío requerido por Firebase para deserializar
    constructor() : this("", "", "", "", "", "", false)

    // Constructor legacy (compatibilidad con código anterior que solo pasaba 4 campos)
    constructor(name: String, email: String, password: String, uid: String)
            : this(name, email, password, uid, "", "", false)
}