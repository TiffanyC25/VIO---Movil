package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: Button
    private lateinit var tabLogin: Button
    private lateinit var tabSignup: Button
    private lateinit var formLogin: LinearLayout
    private lateinit var btnGoToSignup: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        btnLogin = findViewById(R.id.login)
        tabLogin = findViewById(R.id.tabLogin)
        tabSignup = findViewById(R.id.tabSignup)

        // Botón principal: iniciar sesión
        btnLogin.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()
            if (e.isEmpty()) {
                email.error = "Ingresa tu correo"; return@setOnClickListener
            }
            if (p.isEmpty()) {
                password.error = "Ingresa tu contraseña"; return@setOnClickListener
            }
            iniciarSesion(e, p)
        }

        // Tab "Registrarse" → abre Signup
        tabSignup.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        // Tab "Iniciar Sesión" no hace nada (ya estamos aquí), sólo actualiza visual
        tabLogin.setOnClickListener { /* ya activo */ }
    }

    private fun iniciarSesion(email: String, password: String) {
        btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    verificarRolYRedirigir(uid)
                } else {
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun verificarRolYRedirigir(uid: String) {
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid).child("esAdmin")
            .get()
            .addOnSuccessListener { snapshot ->
                btnLogin.isEnabled = true
                val esAdmin = snapshot.getValue(Boolean::class.java) ?: false
                val destino = if (esAdmin) HomeAdminActivity::class.java
                else HomeConductorActivity::class.java
                val intent = Intent(this, destino)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                btnLogin.isEnabled = true
                // Si falla la lectura, abrir home de conductor por defecto
                val intent = Intent(this, HomeConductorActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
    }
}
