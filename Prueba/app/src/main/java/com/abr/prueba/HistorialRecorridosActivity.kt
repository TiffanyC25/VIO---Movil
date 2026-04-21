package com.abr.prueba

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class HistorialRecorridosActivity : AppCompatActivity() {

    private lateinit var recyclerHistorialRecorridos: RecyclerView
    private lateinit var tvSinHistorial: TextView
    private lateinit var progressBarHistorial: ProgressBar

    private val db = FirebaseDatabase.getInstance().reference
    private val listaRecorridos = mutableListOf<Pair<Recorrido, Int>>()
    private lateinit var historialRecorridosAdapter: HistorialRecorridosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_recorridos)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial"

        recyclerHistorialRecorridos = findViewById(R.id.recyclerHistorialRecorridos)
        tvSinHistorial              = findViewById(R.id.tvSinHistorial)
        progressBarHistorial        = findViewById(R.id.progressBarHistorial)

        recyclerHistorialRecorridos.layoutManager = LinearLayoutManager(this)
        historialRecorridosAdapter = HistorialRecorridosAdapter(listaRecorridos)
        recyclerHistorialRecorridos.adapter = historialRecorridosAdapter

        cargarHistorial()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarHistorial() {
        progressBarHistorial.visibility = View.VISIBLE
        tvSinHistorial.visibility       = View.GONE

        db.child("recorridos").get()
            .addOnSuccessListener { snapshot ->
                listaRecorridos.clear()
                for (snap in snapshot.children) {
                    val rec = snap.getValue(Recorrido::class.java)
                    if (rec != null) {
                        val pts = snap.child("puntosRegistrados").childrenCount.toInt()
                        listaRecorridos.add(Pair(rec, pts))
                    }
                }
                listaRecorridos.sortByDescending { it.first.inicioTiempo }
                historialRecorridosAdapter.notifyDataSetChanged()
                progressBarHistorial.visibility = View.GONE
                tvSinHistorial.visibility =
                    if (listaRecorridos.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                progressBarHistorial.visibility = View.GONE
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
