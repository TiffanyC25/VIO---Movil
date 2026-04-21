package com.abr.prueba

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.util.concurrent.atomic.AtomicInteger

class HistorialConductorActivity : AppCompatActivity() {

    private lateinit var tvInicialesDetalle: TextView
    private lateinit var tvNombreDetalle: TextView
    private lateinit var tvEmailDetalle: TextView
    private lateinit var tvTelefonoDetalle: TextView
    private lateinit var tvStatRecorridos: TextView
    private lateinit var tvStatCompletados: TextView
    private lateinit var tvStatTiempoTotal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSinRecorridos: TextView
    private lateinit var recycler: RecyclerView

    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_conductor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val uid      = intent.getStringExtra("uid")      ?: ""
        val nombre   = intent.getStringExtra("nombre")   ?: "Conductor"
        val email    = intent.getStringExtra("email")    ?: "—"
        val telefono = intent.getStringExtra("telefono") ?: "—"

        supportActionBar?.title = nombre

        tvInicialesDetalle = findViewById(R.id.tvInicialesDetalle)
        tvNombreDetalle    = findViewById(R.id.tvNombreDetalle)
        tvEmailDetalle     = findViewById(R.id.tvEmailDetalle)
        tvTelefonoDetalle  = findViewById(R.id.tvTelefonoDetalle)
        tvStatRecorridos   = findViewById(R.id.tvStatRecorridos)
        tvStatCompletados  = findViewById(R.id.tvStatCompletados)
        tvStatTiempoTotal  = findViewById(R.id.tvStatTiempoTotal)
        progressBar        = findViewById(R.id.progressBarHistorialConductor)
        tvSinRecorridos    = findViewById(R.id.tvSinRecorridos)
        recycler           = findViewById(R.id.recyclerHistorialConductor)

        // Poblar perfil en el banner
        val iniciales = buildString {
            val partes = nombre.split(" ")
            if (partes.isNotEmpty() && partes[0].isNotEmpty()) append(partes[0].first().uppercaseChar())
            if (partes.size > 1    && partes[1].isNotEmpty()) append(partes[1].first().uppercaseChar())
        }.ifEmpty { "?" }

        tvInicialesDetalle.text = iniciales
        tvNombreDetalle.text    = nombre
        tvEmailDetalle.text     = email
        tvTelefonoDetalle.text  = telefono.ifEmpty { "Sin teléfono" }

        recycler.layoutManager = LinearLayoutManager(this)

        cargarHistorial(uid)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarHistorial(uid: String) {
        progressBar.visibility  = View.VISIBLE
        tvSinRecorridos.visibility = View.GONE

        db.child("recorridos")
            .orderByChild("usuarioId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val recorridos = mutableListOf<Recorrido>()

                    for (snap in snapshot.children) {
                        snap.getValue(Recorrido::class.java)?.let { recorridos.add(it) }
                    }

                    recorridos.sortByDescending { it.inicioTiempo }

                    if (recorridos.isEmpty()) {
                        progressBar.visibility     = View.GONE
                        tvSinRecorridos.visibility = View.VISIBLE
                        mostrarStats(emptyList())
                        return
                    }

                    // Para cada recorrido necesitamos:
                    //  a) sus puntosRegistrados  → recorridos/{id}/puntosRegistrados
                    //  b) total puntos de la ruta → rutas/{rutaId}/puntos
                    // Usamos un contador atómico para saber cuándo terminaron todas las consultas
                    val resultado   = MutableList<RecorridoAdmin?>(recorridos.size) { null }
                    val pendientes  = AtomicInteger(recorridos.size)

                    recorridos.forEachIndexed { i, rec ->
                        cargarDetalleRecorrido(rec, i, resultado, pendientes) {
                            // Callback cuando TODOS los detalles están listos
                            val lista = resultado.filterNotNull()
                            runOnUiThread {
                                progressBar.visibility = View.GONE
                                mostrarStats(lista)

                                val adapter = RecorridoAdminAdapter(lista)
                                recycler.adapter = adapter
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@HistorialConductorActivity,
                        "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun cargarDetalleRecorrido(
        recorrido: Recorrido,
        index: Int,
        resultado: MutableList<RecorridoAdmin?>,
        pendientes: AtomicInteger,
        onComplete: () -> Unit
    ) {
        // Paso 1: cargar puntos registrados del recorrido
        db.child("recorridos").child(recorrido.id).child("puntosRegistrados")
            .get()
            .addOnSuccessListener { snapPuntos ->
                val puntosRegistrados = mutableListOf<PuntoRegistrado>()
                for (ps in snapPuntos.children) {
                    ps.getValue(PuntoRegistrado::class.java)?.let { puntosRegistrados.add(it) }
                }

                // Paso 2: contar puntos totales de la ruta
                db.child("rutas").child(recorrido.rutaId).child("puntos")
                    .get()
                    .addOnSuccessListener { snapRuta ->
                        val totalPuntos = snapRuta.childrenCount.toInt()

                        resultado[index] = RecorridoAdmin(
                            recorrido        = recorrido,
                            puntosRegistrados = puntosRegistrados,
                            totalPuntosRuta  = totalPuntos
                        )

                        if (pendientes.decrementAndGet() == 0) onComplete()
                    }
                    .addOnFailureListener {
                        // Si falla la ruta, igual mostramos con total = 0
                        resultado[index] = RecorridoAdmin(recorrido, puntosRegistrados, 0)
                        if (pendientes.decrementAndGet() == 0) onComplete()
                    }
            }
            .addOnFailureListener {
                resultado[index] = RecorridoAdmin(recorrido, emptyList(), 0)
                if (pendientes.decrementAndGet() == 0) onComplete()
            }
    }

    private fun mostrarStats(lista: List<RecorridoAdmin>) {
        val total       = lista.size
        val completados = lista.count { it.recorrido.estado == "finalizado_automatico" }
        val tiempoTotalMs = lista.sumOf { it.recorrido.tiempoTotal }

        tvStatRecorridos.text  = total.toString()
        tvStatCompletados.text = completados.toString()
        tvStatTiempoTotal.text = if (tiempoTotalMs > 0) formatMs(tiempoTotalMs) else "—"
    }

    private fun formatMs(ms: Long): String {
        val h = ms / 3600000; val m = (ms % 3600000) / 60000
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}