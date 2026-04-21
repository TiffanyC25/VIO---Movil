package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeAdminActivity : AppCompatActivity() {

    private lateinit var cardCrearRuta: LinearLayout
    private lateinit var cardConfigurarPuntos: LinearLayout
    private lateinit var cardConductores: LinearLayout
    private lateinit var cardDatosUsuario: LinearLayout
    private lateinit var tvSubtitulo: TextView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_admin)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        cardCrearRuta        = findViewById(R.id.btnIrCrearRuta)
        cardConfigurarPuntos = findViewById(R.id.btnIrConfigurarPuntos)
        cardConductores      = findViewById(R.id.btnIrConductores)
        cardDatosUsuario     = findViewById(R.id.btnIrDatosUsuario)
        tvSubtitulo          = findViewById(R.id.tvSubtituloHomeRutas)

        mostrarNombreAdmin()

        cardCrearRuta.setOnClickListener {
            startActivity(Intent(this, CrearRutaActivity::class.java))
        }
        cardConfigurarPuntos.setOnClickListener {
            startActivity(Intent(this, ListasRutasActivity::class.java))
        }
        cardConductores.setOnClickListener {
            startActivity(Intent(this, ListaConductoresActivity::class.java))
        }
        cardDatosUsuario.setOnClickListener {
            startActivity(Intent(this, PerfilAdminActivity::class.java))
        }
    }

    private fun mostrarNombreAdmin() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("name").getValue(String::class.java) ?: ""
                    if (nombre.isNotEmpty()) tvSubtitulo.text = "Admin: $nombre"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* raíz — no hacer nada */ }
}