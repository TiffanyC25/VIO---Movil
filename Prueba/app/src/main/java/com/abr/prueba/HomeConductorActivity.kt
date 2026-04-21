package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeConductorActivity : AppCompatActivity() {

    // Los 5 bloques del home son LinearLayouts (no Buttons) en el layout visual
    private lateinit var cardCrearRuta: LinearLayout
    private lateinit var cardConfigurarPuntos: LinearLayout
    private lateinit var cardIniciarRecorrido: LinearLayout
    private lateinit var cardHistorial: LinearLayout
    private lateinit var cardDatosUsuario: LinearLayout
    private lateinit var tvSubtitulo: TextView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_conductor)

        // No hay botón atrás en Home (es la pantalla raíz de la app)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        cardIniciarRecorrido = findViewById(R.id.btnIrIniciarRecorrido)
        cardHistorial        = findViewById(R.id.btnIrHistorialRecorridos)
        cardDatosUsuario     = findViewById(R.id.btnIrDatosUsuario)
        tvSubtitulo          = findViewById(R.id.tvSubtituloHomeRutas)

        // Mostrar nombre del usuario si está logueado
        mostrarNombreUsuario()

        cardIniciarRecorrido.setOnClickListener {
            startActivity(Intent(this, ListaRutasRecorridoActivity::class.java))
        }
        cardHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialRecorridosActivity::class.java))
        }
        cardDatosUsuario.setOnClickListener {
            startActivity(Intent(this, PerfilConductorActivity::class.java))
        }
    }

    private fun mostrarNombreUsuario() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().reference
            .child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: ""
                    if (nombre.isNotEmpty()) {
                        tvSubtitulo.text = "Bienvenido, $nombre"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // Deshabilitar el botón atrás del sistema en Home
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // No hacer nada: Home es la raíz, no hay a dónde ir atrás
    }
}
