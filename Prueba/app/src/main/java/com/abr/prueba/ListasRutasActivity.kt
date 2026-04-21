package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ListasRutasActivity : AppCompatActivity() {

    private lateinit var recyclerRutas: RecyclerView
    private lateinit var tvSinRutas: TextView
    private lateinit var progressBarRutas: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaAdapter: RutaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listas_rutas)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Rutas Creadas"

        recyclerRutas     = findViewById(R.id.recyclerRutas)
        tvSinRutas        = findViewById(R.id.tvSinRutas)
        progressBarRutas  = findViewById(R.id.progressBarRutas)

        recyclerRutas.layoutManager = LinearLayoutManager(this)
        rutaAdapter = RutaAdapter(listaRutas) { ruta -> abrirConfigurarPuntos(ruta) }
        recyclerRutas.adapter = rutaAdapter

        cargarRutas()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarRutas() {
        progressBarRutas.visibility = View.VISIBLE
        tvSinRutas.visibility       = View.GONE

        db.child("rutas").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRutas.clear()
                for (snap in snapshot.children) {
                    snap.getValue(Ruta::class.java)?.let { listaRutas.add(it) }
                }
                listaRutas.sortByDescending { it.creadaEn }
                rutaAdapter.notifyDataSetChanged()
                progressBarRutas.visibility = View.GONE
                tvSinRutas.visibility = if (listaRutas.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                progressBarRutas.visibility = View.GONE
                Toast.makeText(this@ListasRutasActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun abrirConfigurarPuntos(ruta: Ruta) {
        startActivity(Intent(this, ConfigurarPuntosRutaActivity::class.java).apply {
            putExtra("rutaId", ruta.id)
            putExtra("rutaNombre", ruta.nombre)
            putExtra("rutaRadio", ruta.radioDeteccion)
        })
    }
}
