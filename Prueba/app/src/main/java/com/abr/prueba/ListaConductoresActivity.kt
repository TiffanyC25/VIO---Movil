package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ListaConductoresActivity : AppCompatActivity() {

    private lateinit var etBuscar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSin: TextView
    private lateinit var tvContador: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ConductoresAdapter

    private val db = FirebaseDatabase.getInstance().reference
    private val listaConductores = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_conductores)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Conductores"

        etBuscar    = findViewById(R.id.etBuscarConductor)
        progressBar = findViewById(R.id.progressBarConductores)
        tvSin       = findViewById(R.id.tvSinConductores)
        tvContador  = findViewById(R.id.tvContadorConductores)
        recycler    = findViewById(R.id.recyclerConductores)

        adapter = ConductoresAdapter(listaConductores) { conductor ->
            val intent = Intent(this, HistorialConductorActivity::class.java).apply {
                putExtra("uid",      conductor.uid)
                putExtra("nombre",   "${conductor.name} ${conductor.apellidos}".trim())
                putExtra("email",    conductor.email)
                putExtra("telefono", conductor.telefono)
            }
            startActivity(intent)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Filtrado en tiempo real
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cargarConductores()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun cargarConductores() {
        progressBar.visibility = View.VISIBLE
        tvSin.visibility = View.GONE

        // Leemos TODOS los users y filtramos los que NO son admin
        db.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conductores = mutableListOf<User>()

                for (snap in snapshot.children) {
                    val user = snap.getValue(User::class.java) ?: continue
                    if (!user.esAdmin) {
                        conductores.add(user)
                    }
                }

                // Ordenar: primero por apellido, luego por nombre
                conductores.sortWith(compareBy({ it.apellidos.lowercase() }, { it.name.lowercase() }))

                adapter.setData(conductores)
                progressBar.visibility = View.GONE

                val n = conductores.size
                tvContador.text = "$n CONDUCTOR${if (n != 1) "ES" else ""} REGISTRADO${if (n != 1) "S" else ""}"

                if (conductores.isEmpty()) {
                    tvSin.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ListaConductoresActivity,
                    "Error al cargar conductores: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}