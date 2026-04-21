package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Signup : AppCompatActivity() {

    private lateinit var nombre: EditText
    private lateinit var apellidos: EditText
    private lateinit var telefono: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordVerif: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tabLogin: Button
    private lateinit var tabSignup: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = FirebaseAuth.getInstance()

        nombre        = findViewById(R.id.nombre)
        apellidos     = findViewById(R.id.apellidos)
        telefono      = findViewById(R.id.telefono)
        email         = findViewById(R.id.email)
        password      = findViewById(R.id.password)
        passwordVerif = findViewById(R.id.passwordVerif)
        btnGuardar    = findViewById(R.id.guardar)
        tabLogin      = findViewById(R.id.tabLogin)
        tabSignup     = findViewById(R.id.tabSignup)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear cuenta"

        tabLogin.setOnClickListener { finish() }
        tabSignup.setOnClickListener { /* ya activo */ }
        btnGuardar.setOnClickListener { validarYRegistrar() }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun validarYRegistrar() {
        val nom   = nombre.text.toString().trim()
        val ape   = apellidos.text.toString().trim()
        val tel   = telefono.text.toString().trim()
        val em    = email.text.toString().trim()
        val pass  = password.text.toString().trim()
        val verif = passwordVerif.text.toString().trim()

        if (nom.isEmpty())   { nombre.error = "Requerido";              return }
        if (ape.isEmpty())   { apellidos.error = "Requerido";           return }
        if (tel.isEmpty())   { telefono.error = "Requerido";            return }
        if (em.isEmpty())    { email.error = "Requerido";               return }
        if (pass.isEmpty())  { password.error = "Requerido";            return }
        if (pass.length < 6) { password.error = "Mínimo 6 caracteres"; return }
        if (pass != verif)   { passwordVerif.error = "No coinciden";    return }

        btnGuardar.isEnabled = false
        btnGuardar.text      = "Creando cuenta…"

        auth.createUserWithEmailAndPassword(em, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    guardarEnFirebase(nom, ape, tel, em, pass, uid)
                } else {
                    btnGuardar.isEnabled = true
                    btnGuardar.text      = "Crear Cuenta →"
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun guardarEnFirebase(
        nombre: String, apellidos: String, telefono: String,
        email: String, password: String, uid: String
    ) {
        val user = User(
            name      = nombre,
            email     = email,
            password  = password,
            uid       = uid,
            apellidos = apellidos,
            telefono  = telefono
        )

        FirebaseDatabase.getInstance().reference
            .child("users").child(uid)
            .setValue(user)
            .addOnCompleteListener {
                btnGuardar.isEnabled = true
                btnGuardar.text      = "Crear Cuenta →"
                Toast.makeText(this, "¡Cuenta creada! Inicia sesión", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnGuardar.isEnabled = true
                btnGuardar.text      = "Crear Cuenta →"
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}