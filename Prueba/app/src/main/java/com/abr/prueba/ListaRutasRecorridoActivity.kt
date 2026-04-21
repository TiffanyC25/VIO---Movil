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

class ListaRutasRecorridoActivity : AppCompatActivity() {

    private lateinit var recyclerRutasRecorrido: RecyclerView
    private lateinit var tvSinRutasRecorrido: TextView
    private lateinit var progressBarRutasRecorrido: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRutas = mutableListOf<Ruta>()
    private lateinit var rutaRecorridoAdapter: RutaRecorridoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_rutas_recorrido)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Iniciar Recorrido"

        recyclerRutasRecorrido    = findViewById(R.id.recyclerRutasRecorrido)
        tvSinRutasRecorrido       = findViewById(R.id.tvSinRutasRecorrido)
        progressBarRutasRecorrido = findViewById(R.id.progressBarRutasRecorrido)

        recyclerRutasRecorrido.layoutManager = LinearLayoutManager(this)
        rutaRecorridoAdapter = RutaRecorridoAdapter(listaRutas) { ruta ->
            startActivity(Intent(this, RecorridoRutaActivity::class.java).apply {
                putExtra("rutaId", ruta.id)
                putExtra("rutaNombre", ruta.nombre)
                putExtra("rutaRadio", ruta.radioDeteccion)
            })
        }
        recyclerRutasRecorrido.adapter = rutaRecorridoAdapter

        cargarRutas()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarRutas() {
        progressBarRutasRecorrido.visibility = View.VISIBLE
        tvSinRutasRecorrido.visibility       = View.GONE

        db.child("rutas").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaRutas.clear()
                for (snap in snapshot.children) {
                    val ruta = snap.getValue(Ruta::class.java)
                    if (ruta != null && ruta.activa) listaRutas.add(ruta)
                }
                listaRutas.sortByDescending { it.creadaEn }
                rutaRecorridoAdapter.notifyDataSetChanged()
                progressBarRutasRecorrido.visibility = View.GONE
                tvSinRutasRecorrido.visibility =
                    if (listaRutas.isEmpty()) View.VISIBLE else View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                progressBarRutasRecorrido.visibility = View.GONE
                Toast.makeText(this@ListaRutasRecorridoActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
