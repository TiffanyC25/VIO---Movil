package com.abr.prueba

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase

class ConfigurarPuntosRutaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var tvNombreRutaConfigurar: TextView
    private lateinit var btnRecargarPuntos: Button
    private lateinit var recyclerPuntosRuta: RecyclerView
    private lateinit var puntoRutaAdapter: PuntoRutaAdapter

    private val db = FirebaseDatabase.getInstance().reference
    private var rutaId     = ""
    private var rutaNombre = ""
    private var rutaRadio  = 30f
    private val listaPuntos = mutableListOf<PuntoRuta>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configurar_puntos_ruta)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configurar Puntos"

        tvNombreRutaConfigurar = findViewById(R.id.tvNombreRutaConfigurar)
        btnRecargarPuntos      = findViewById(R.id.btnRecargarPuntos)
        recyclerPuntosRuta     = findViewById(R.id.recyclerPuntosRuta)

        rutaId     = intent.getStringExtra("rutaId")    ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre") ?: ""
        rutaRadio  = intent.getFloatExtra("rutaRadio", 30f)

        tvNombreRutaConfigurar.text = "Ruta: $rutaNombre"

        puntoRutaAdapter = PuntoRutaAdapter(
            listaPuntos,
            onEditarClick   = { mostrarDialogoEditarPunto(it) },
            onEliminarClick = { confirmarEliminarPunto(it) }
        )
        recyclerPuntosRuta.layoutManager = LinearLayoutManager(this)
        recyclerPuntosRuta.adapter = puntoRutaAdapter

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapConfigurarPuntos) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnRecargarPuntos.setOnClickListener { cargarPuntosRuta() }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // ── Mapa ────────────────────────────────────────────────────────────────
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled      = true
        habilitarUbicacion()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(4.8697, -74.0436), 13f)) // Chía
        googleMap.setOnMapClickListener { latLng -> mostrarDialogoAgregarPunto(latLng) }
        cargarPuntosRuta()
    }

    private fun habilitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        }
    }

    // ── Agregar punto ───────────────────────────────────────────────────────
    private fun mostrarDialogoAgregarPunto(latLng: LatLng) {
        val vista = layoutInflater.inflate(R.layout.dialog_agregar_punto, null)
        val etNombre = vista.findViewById<EditText>(R.id.etNombrePunto)
        val etOrden  = vista.findViewById<EditText>(R.id.etOrdenPunto)
        val spTipo   = vista.findViewById<Spinner>(R.id.spTipoPunto)
        val tvCoord  = vista.findViewById<TextView>(R.id.tvCoordenadasPunto)

        tvCoord.text = "Lat: ${latLng.latitude}\nLng: ${latLng.longitude}"
        val tipos = listOf("origen", "marca", "fin")
        spTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        AlertDialog.Builder(this)
            .setTitle("Agregar punto a la ruta")
            .setView(vista)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = etNombre.text.toString().trim()
                        val ordenTxt = etOrden.text.toString().trim()
                        if (nombre.isEmpty()) { etNombre.error = "Requerido"; return@setOnClickListener }
                        if (ordenTxt.isEmpty()) { etOrden.error = "Requerido"; return@setOnClickListener }
                        val orden = ordenTxt.toIntOrNull()
                        if (orden == null || orden <= 0) { etOrden.error = "Valor inválido"; return@setOnClickListener }
                        validarYGuardar(nombre, latLng.latitude, latLng.longitude, orden,
                            spTipo.selectedItem.toString(), dialog)
                    }
                }
                dialog.show()
            }
    }

    private fun validarYGuardar(nombre: String, lat: Double, lng: Double, orden: Int,
                                tipo: String, dialog: AlertDialog, idExcluir: String = "") {
        if (listaPuntos.any { it.orden == orden && it.id != idExcluir }) {
            Toast.makeText(this, "Ya existe un punto con ese orden", Toast.LENGTH_LONG).show(); return
        }
        if (tipo == "origen" && listaPuntos.count { it.tipo == "origen" && it.id != idExcluir } >= 1) {
            Toast.makeText(this, "Solo puede haber un punto origen", Toast.LENGTH_LONG).show(); return
        }
        if (tipo == "fin" && listaPuntos.count { it.tipo == "fin" && it.id != idExcluir } >= 1) {
            Toast.makeText(this, "Solo puede haber un punto final", Toast.LENGTH_LONG).show(); return
        }
        val ref   = db.child("rutas").child(rutaId).child("puntos").push()
        val punto = PuntoRuta(id = ref.key ?: "", nombre = nombre, latitud = lat,
            longitud = lng, orden = orden, tipo = tipo)
        ref.setValue(punto)
            .addOnSuccessListener { Toast.makeText(this, "Punto guardado", Toast.LENGTH_SHORT).show(); dialog.dismiss(); cargarPuntosRuta() }
            .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // ── Editar punto ────────────────────────────────────────────────────────
    private fun mostrarDialogoEditarPunto(punto: PuntoRuta) {
        val vista    = layoutInflater.inflate(R.layout.dialog_editar_punto, null)
        val etNombre = vista.findViewById<EditText>(R.id.etNombrePuntoEditar)
        val etOrden  = vista.findViewById<EditText>(R.id.etOrdenPuntoEditar)
        val spTipo   = vista.findViewById<Spinner>(R.id.spTipoPuntoEditar)
        val etLat    = vista.findViewById<EditText>(R.id.etLatitudPuntoEditar)
        val etLng    = vista.findViewById<EditText>(R.id.etLongitudPuntoEditar)
        val tvCoord  = vista.findViewById<TextView>(R.id.tvCoordenadasEditar)

        tvCoord.text = "Lat: ${punto.latitud}   |   Lng: ${punto.longitud}"
        etNombre.setText(punto.nombre)
        etOrden.setText(punto.orden.toString())
        etLat.setText(punto.latitud.toString())
        etLng.setText(punto.longitud.toString())

        val tipos = listOf("origen", "marca", "fin")
        spTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spTipo.setSelection(tipos.indexOf(punto.tipo).coerceAtLeast(0))

        AlertDialog.Builder(this)
            .setTitle("Editar punto")
            .setView(vista)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombre   = etNombre.text.toString().trim()
                        val ordenTxt = etOrden.text.toString().trim()
                        val latTxt   = etLat.text.toString().trim()
                        val lngTxt   = etLng.text.toString().trim()
                        if (nombre.isEmpty())   { etNombre.error = "Requerido"; return@setOnClickListener }
                        if (ordenTxt.isEmpty()) { etOrden.error = "Requerido";  return@setOnClickListener }
                        val orden   = ordenTxt.toIntOrNull(); if (orden == null || orden <= 0) { etOrden.error = "Inválido"; return@setOnClickListener }
                        val lat     = latTxt.toDoubleOrNull(); if (lat == null) { etLat.error = "Inválido"; return@setOnClickListener }
                        val lng     = lngTxt.toDoubleOrNull(); if (lng == null) { etLng.error = "Inválido"; return@setOnClickListener }
                        val tipo    = spTipo.selectedItem.toString()
                        if (listaPuntos.any { it.orden == orden && it.id != punto.id }) {
                            Toast.makeText(this, "Orden duplicado", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                        if (tipo == "origen" && listaPuntos.count { it.tipo == "origen" && it.id != punto.id } >= 1) {
                            Toast.makeText(this, "Solo puede haber un origen", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                        if (tipo == "fin" && listaPuntos.count { it.tipo == "fin" && it.id != punto.id } >= 1) {
                            Toast.makeText(this, "Solo puede haber un fin", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                        actualizarPunto(punto.id, nombre, orden, tipo, lat, lng, dialog)
                    }
                }
                dialog.show()
            }
    }

    private fun actualizarPunto(id: String, nombre: String, orden: Int, tipo: String,
                                lat: Double, lng: Double, dialog: AlertDialog) {
        db.child("rutas").child(rutaId).child("puntos").child(id)
            .updateChildren(mapOf("nombre" to nombre, "orden" to orden, "tipo" to tipo,
                "latitud" to lat, "longitud" to lng))
            .addOnSuccessListener { Toast.makeText(this, "Punto actualizado", Toast.LENGTH_SHORT).show(); dialog.dismiss(); cargarPuntosRuta() }
            .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // ── Eliminar punto ──────────────────────────────────────────────────────
    private fun confirmarEliminarPunto(punto: PuntoRuta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar punto")
            .setMessage("¿Seguro que deseas eliminar \"${punto.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.child("rutas").child(rutaId).child("puntos").child(punto.id)
                    .removeValue()
                    .addOnSuccessListener { Toast.makeText(this, "Punto eliminado", Toast.LENGTH_SHORT).show(); cargarPuntosRuta() }
                    .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Cargar y dibujar ────────────────────────────────────────────────────
    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos").get()
            .addOnSuccessListener { snap ->
                listaPuntos.clear()
                for (ps in snap.children) ps.getValue(PuntoRuta::class.java)?.let { listaPuntos.add(it) }
                listaPuntos.sortBy { it.orden }
                puntoRutaAdapter.notifyDataSetChanged()
                dibujarEnMapa()
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun dibujarEnMapa() {
        googleMap.clear()
        habilitarUbicacion()
        if (listaPuntos.isEmpty()) return
        val poly = PolylineOptions()
        for (p in listaPuntos) {
            val pos = LatLng(p.latitud, p.longitud)
            poly.add(pos)
            val hue = when (p.tipo) {
                "origen" -> BitmapDescriptorFactory.HUE_GREEN
                "fin"    -> BitmapDescriptorFactory.HUE_RED
                else     -> BitmapDescriptorFactory.HUE_AZURE
            }
            googleMap.addMarker(MarkerOptions().position(pos)
                .title("${p.orden}. ${p.nombre}").snippet("Tipo: ${p.tipo}")
                .icon(BitmapDescriptorFactory.defaultMarker(hue)))
        }
        googleMap.addPolyline(poly)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(listaPuntos.first().latitud, listaPuntos.first().longitud), 16f))
    }
}
