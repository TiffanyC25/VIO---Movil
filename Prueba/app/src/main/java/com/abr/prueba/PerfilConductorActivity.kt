package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PerfilConductorActivity : AppCompatActivity() {

    private lateinit var tvInicialesAvatar: TextView
    private lateinit var tvNombreCompleto: TextView
    private lateinit var tvRolUsuario: TextView
    private lateinit var tvInfoNombre: TextView
    private lateinit var tvInfoEmail: TextView
    private lateinit var tvInfoTelefono: TextView
    private lateinit var tvStatRecorridos: TextView
    private lateinit var tvStatPuntos: TextView
    private lateinit var tvStatRutas: TextView
    private lateinit var btnCerrarSesion: LinearLayout

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_conductor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi perfil"

        tvInicialesAvatar = findViewById(R.id.tvInicialesAvatar)
        tvNombreCompleto  = findViewById(R.id.tvTituloDatosUsuario)
        tvRolUsuario      = findViewById(R.id.tvRolUsuario)
        tvInfoNombre      = findViewById(R.id.tvInfoNombre)
        tvInfoEmail       = findViewById(R.id.tvInfoEmail)
        tvInfoTelefono    = findViewById(R.id.tvInfoTelefono)
        tvStatRecorridos  = findViewById(R.id.tvStatRecorridos)
        tvStatPuntos      = findViewById(R.id.tvStatPuntos)
        tvStatRutas       = findViewById(R.id.tvStatRutas)
        btnCerrarSesion   = findViewById(R.id.btnCerrarSesion)

        cargarPerfilUsuario()
        cargarEstadisticas()

        btnCerrarSesion.setOnClickListener { confirmarCerrarSesion() }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarPerfilUsuario() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_LONG).show()
            return
        }

        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        // Usuario en Auth pero sin nodo en DB — usar Auth como fallback
                        val correoAuth = auth.currentUser?.email ?: "—"
                        tvNombreCompleto.text  = correoAuth
                        tvInfoEmail.text       = correoAuth
                        tvInicialesAvatar.text = "?"
                        return
                    }

                    // Intentar deserializar directamente
                    val user = snapshot.getValue(User::class.java)

                    if (user != null) {
                        val nombreCompleto = buildString {
                            append(user.name.ifEmpty { "—" })
                            if (user.apellidos.isNotEmpty()) append(" ${user.apellidos}")
                        }
                        val iniciales = buildString {
                            if (user.name.isNotEmpty())      append(user.name.first().uppercaseChar())
                            if (user.apellidos.isNotEmpty()) append(user.apellidos.first().uppercaseChar())
                        }.ifEmpty { "?" }
                        val correo = user.email.ifEmpty { auth.currentUser?.email ?: "—" }

                        tvInicialesAvatar.text = iniciales
                        tvNombreCompleto.text  = nombreCompleto
                        tvInfoNombre.text      = nombreCompleto
                        tvInfoEmail.text       = correo
                        tvInfoTelefono.text    = user.telefono.ifEmpty { "No registrado" }

                    } else {
                        // Fallback campo a campo (usuarios registrados con versión anterior)
                        val nombre = snapshot.child("name").getValue(String::class.java)      ?: ""
                        val ape    = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                        val correo = snapshot.child("email").getValue(String::class.java)
                            ?: auth.currentUser?.email ?: "—"
                        val tel    = snapshot.child("telefono").getValue(String::class.java)  ?: "—"

                        val nombreCompleto = if (ape.isNotEmpty()) "$nombre $ape" else nombre
                        val iniciales = buildString {
                            if (nombre.isNotEmpty()) append(nombre.first().uppercaseChar())
                            if (ape.isNotEmpty())    append(ape.first().uppercaseChar())
                        }.ifEmpty { "?" }

                        tvInicialesAvatar.text = iniciales
                        tvNombreCompleto.text  = nombreCompleto.ifEmpty { correo }
                        tvInfoNombre.text      = nombreCompleto.ifEmpty { "—" }
                        tvInfoEmail.text       = correo
                        tvInfoTelefono.text    = tel.ifEmpty { "No registrado" }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PerfilConductorActivity,
                        "Error al cargar perfil: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun cargarEstadisticas() {
        val uid = auth.currentUser?.uid ?: return

        db.child("recorridos")
            .orderByChild("usuarioId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalRecorridos = 0
                    var totalPuntos     = 0
                    val rutasUnicas     = mutableSetOf<String>()

                    for (rec in snapshot.children) {
                        totalRecorridos++
                        totalPuntos += rec.child("puntosRegistrados").childrenCount.toInt()
                        rec.child("rutaId").getValue(String::class.java)?.let { rutasUnicas.add(it) }
                    }

                    tvStatRecorridos.text = totalRecorridos.toString()
                    tvStatPuntos.text     = totalPuntos.toString()
                    tvStatRutas.text      = rutasUnicas.size.toString()
                }

                override fun onCancelled(error: DatabaseError) { /* dejar los "—" */ }
            })
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        auth.signOut()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}