package com.abr.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class CrearRutaActivity : AppCompatActivity() {

    private lateinit var etNombreRuta: EditText
    private lateinit var etDescripcionRuta: EditText
    private lateinit var etRadioDeteccion: EditText
    private lateinit var btnGuardarRuta: Button
    private lateinit var btnCancelar: Button
    private lateinit var tvEstadoRuta: TextView

    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_ruta)

        // Flecha atrás en toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear Ruta"

        etNombreRuta      = findViewById(R.id.etNombreRuta)
        etDescripcionRuta = findViewById(R.id.etDescripcionRuta)
        etRadioDeteccion  = findViewById(R.id.etRadioDeteccion)
        btnGuardarRuta    = findViewById(R.id.btnGuardarRuta)
        tvEstadoRuta      = findViewById(R.id.tvEstadoRuta)

        // El layout tiene un botón "Cancelar" (primer botón del pair)
        // Si no existe en el layout actual, se ignora con try
        try {
            btnCancelar = findViewById(R.id.btnCancelar)
            btnCancelar.setOnClickListener { finish() }
        } catch (e: Exception) { /* no está en el layout */ }

        btnGuardarRuta.setOnClickListener { guardarRuta() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun guardarRuta() {
        val nombre      = etNombreRuta.text.toString().trim()
        val descripcion = etDescripcionRuta.text.toString().trim()
        val radioTexto  = etRadioDeteccion.text.toString().trim()

        if (nombre.isEmpty()) { etNombreRuta.error = "Ingresa el nombre"; etNombreRuta.requestFocus(); return }
        if (descripcion.isEmpty()) { etDescripcionRuta.error = "Ingresa la descripción"; etDescripcionRuta.requestFocus(); return }
        if (radioTexto.isEmpty()) { etRadioDeteccion.error = "Ingresa el radio"; etRadioDeteccion.requestFocus(); return }

        val radio = radioTexto.toFloatOrNull()
        if (radio == null || radio <= 0f) { etRadioDeteccion.error = "Valor inválido"; return }

        btnGuardarRuta.isEnabled = false
        tvEstadoRuta.text = "Guardando…"

        val rutaRef = db.child("rutas").push()
        val rutaId  = rutaRef.key ?: ""
        val ruta = Ruta(
            id = rutaId, nombre = nombre, descripcion = descripcion,
            activa = true, radioDeteccion = radio, creadaEn = System.currentTimeMillis()
        )

        rutaRef.setValue(ruta)
            .addOnSuccessListener {
                tvEstadoRuta.text = "✓ Ruta guardada correctamente"
                Toast.makeText(this, "Ruta creada con éxito", Toast.LENGTH_SHORT).show()
                limpiarCampos()
                btnGuardarRuta.isEnabled = true
            }
            .addOnFailureListener { e ->
                tvEstadoRuta.text = "Error al guardar"
                btnGuardarRuta.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun limpiarCampos() {
        etNombreRuta.setText("")
        etDescripcionRuta.setText("")
        etRadioDeteccion.setText("")
        etNombreRuta.requestFocus()
    }
}
