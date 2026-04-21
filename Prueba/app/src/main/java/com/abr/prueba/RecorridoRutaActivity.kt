package com.abr.prueba

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class RecorridoRutaActivity : AppCompatActivity() {

    // ── Views ────────────────────────────────────────────────────────────────
    private lateinit var tvRuta: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvProgreso: TextView
    private lateinit var tvSiguiente: TextView
    private lateinit var tvInicio: TextView
    private lateinit var tvUbicacion: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var recyclerPuntos: RecyclerView
    private lateinit var puntoAdapter: PuntoSeguimientoAdapter

    // ── Firebase / datos ─────────────────────────────────────────────────────
    private val db   = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var rutaId     = ""
    private var rutaNombre = ""
    private var rutaRadio  = 30f
    private val listaPuntos = mutableListOf<PuntoRuta>()
    private val puntosUI    = mutableListOf<PuntoSeguimientoUI>()

    // ── Estado del recorrido ─────────────────────────────────────────────────
    private var recorridoId  = ""
    private var enRecorrido  = false
    private var inicioMs     = 0L
    private var tiempoAnteriorPunto = 0L
    private val timerHandler = Handler(Looper.getMainLooper())

    // ── GPS ──────────────────────────────────────────────────────────────────
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorrido_ruta)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recorrido Activo"

        rutaId     = intent.getStringExtra("rutaId")     ?: ""
        rutaNombre = intent.getStringExtra("rutaNombre")  ?: ""
        rutaRadio  = intent.getFloatExtra("rutaRadio", 30f)

        tvRuta      = findViewById(R.id.tvRutaRecorrido)
        tvEstado    = findViewById(R.id.tvEstadoRecorrido)
        tvProgreso  = findViewById(R.id.tvProgresoRecorrido)
        tvSiguiente = findViewById(R.id.tvPuntoSiguienteRecorrido)
        tvInicio    = findViewById(R.id.tvTiempoInicioRecorrido)
        tvUbicacion = findViewById(R.id.tvUbicacionActualRecorrido)
        btnIniciar  = findViewById(R.id.btnIniciarRecorrido)
        btnFinalizar= findViewById(R.id.btnFinalizarManualRecorrido)
        recyclerPuntos = findViewById(R.id.recyclerPuntosSeguimiento)

        tvRuta.text    = "Ruta: $rutaNombre"
        tvEstado.text  = "Pendiente"
        tvProgreso.text = "0 / 0 puntos"
        tvSiguiente.text = "—"
        tvInicio.text  = "—"

        puntoAdapter = PuntoSeguimientoAdapter(puntosUI)
        recyclerPuntos.layoutManager = LinearLayoutManager(this)
        recyclerPuntos.adapter       = puntoAdapter

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        configurarGPS()
        cargarPuntosRuta()

        btnIniciar.setOnClickListener  { iniciarRecorrido() }
        btnFinalizar.setOnClickListener { mostrarConfirmarFinalizar() }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (enRecorrido) { mostrarConfirmarFinalizar(); return false }
        finish(); return true
    }

    // ── GPS ──────────────────────────────────────────────────────────────────
    private fun configurarGPS() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    tvUbicacion.text = "%.6f, %.6f".format(loc.latitude, loc.longitude)
                    if (enRecorrido) verificarProximidad(loc)
                }
            }
        }
    }

    private fun iniciarActualizacionGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
    }

    // ── Cargar puntos ────────────────────────────────────────────────────────
    private fun cargarPuntosRuta() {
        db.child("rutas").child(rutaId).child("puntos").get()
            .addOnSuccessListener { snap ->
                listaPuntos.clear()
                for (ps in snap.children) ps.getValue(PuntoRuta::class.java)?.let { listaPuntos.add(it) }
                listaPuntos.sortBy { it.orden }
                construirUIpuntos()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar puntos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun construirUIpuntos() {
        puntosUI.clear()
        for (p in listaPuntos) {
            puntosUI.add(PuntoSeguimientoUI(
                id = p.id, nombre = p.nombre, tipo = p.tipo, orden = p.orden,
                latitud = p.latitud, longitud = p.longitud,
                estado = ConstantesRutas.ESTADO_PENDIENTE
            ))
        }
        puntoAdapter.notifyDataSetChanged()
        actualizarResumen()
    }

    // ── Iniciar recorrido ────────────────────────────────────────────────────
    private fun iniciarRecorrido() {
        if (listaPuntos.isEmpty()) {
            Toast.makeText(this, "Esta ruta no tiene puntos configurados", Toast.LENGTH_LONG).show()
            return
        }
        enRecorrido = true
        inicioMs    = System.currentTimeMillis()
        tiempoAnteriorPunto = inicioMs

        btnIniciar.isEnabled   = false
        btnFinalizar.isEnabled = true
        btnFinalizar.alpha     = 1f
        tvInicio.text  = sdf.format(Date(inicioMs))
        tvEstado.text  = "En curso"

        // Primer punto → siguiente
        puntosUI.firstOrNull()?.estado = ConstantesRutas.ESTADO_SIGUIENTE
        puntoAdapter.notifyDataSetChanged()
        actualizarResumen()

        // Crear recorrido en Firebase
        val ref = db.child("recorridos").push()
        recorridoId = ref.key ?: ""
        ref.setValue(Recorrido(
            id = recorridoId, rutaId = rutaId, rutaNombre = rutaNombre,
            usuarioId = auth.currentUser?.uid ?: "",
            inicioTiempo = inicioMs, estado = "en_proceso"
        ))

        iniciarActualizacionGPS()
        iniciarTimer()
    }

    // ── Timer ────────────────────────────────────────────────────────────────
    private fun iniciarTimer() {
        timerHandler.postDelayed(object : Runnable {
            override fun run() {
                if (!enRecorrido) return
                val completados = puntosUI.count { it.estado == ConstantesRutas.ESTADO_COMPLETADO }
                val elapsed = System.currentTimeMillis() - inicioMs
                val h = elapsed / 3600000; val m = (elapsed % 3600000) / 60000; val s = (elapsed % 60000) / 1000
                tvProgreso.text = "$completados / ${puntosUI.size} pts  •  %02d:%02d:%02d".format(h, m, s)
                timerHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    // ── Verificar proximidad ─────────────────────────────────────────────────
    private fun verificarProximidad(ubicacion: Location) {
        val sigUI = puntosUI.firstOrNull { it.estado == ConstantesRutas.ESTADO_SIGUIENTE } ?: return
        val dest  = Location("dest").apply { latitude = sigUI.latitud; longitude = sigUI.longitud }
        if (ubicacion.distanceTo(dest) <= rutaRadio) marcarPuntoCompletado(sigUI)
    }

    private fun marcarPuntoCompletado(puntoUI: PuntoSeguimientoUI) {
        val ahora   = System.currentTimeMillis()
        val desdePrev = ahora - tiempoAnteriorPunto
        val acum    = ahora - inicioMs
        tiempoAnteriorPunto = ahora

        puntoUI.estado = ConstantesRutas.ESTADO_COMPLETADO
        puntoUI.tiempoDesdeAnterior = formatMs(desdePrev)
        puntoUI.tiempoAcumulado     = formatMs(acum)

        // Siguiente punto pendiente → siguiente
        puntosUI.firstOrNull { it.estado == ConstantesRutas.ESTADO_PENDIENTE }
            ?.estado = ConstantesRutas.ESTADO_SIGUIENTE

        // Registrar en Firebase
        db.child("recorridos").child(recorridoId).child("puntosRegistrados").push()
            .setValue(PuntoRegistrado(
                puntoId = puntoUI.id, nombre = puntoUI.nombre, orden = puntoUI.orden,
                tiempoLlegada = ahora, tiempoDesdeAnterior = desdePrev, tiempoAcumulado = acum
            ))

        puntoAdapter.notifyDataSetChanged()
        actualizarResumen()
        Toast.makeText(this, "✓ ${puntoUI.nombre}", Toast.LENGTH_SHORT).show()

        if (puntosUI.all { it.estado == ConstantesRutas.ESTADO_COMPLETADO }) {
            finalizarRecorrido("finalizado_automatico")
        }
    }

    // ── Finalizar ────────────────────────────────────────────────────────────
    private fun mostrarConfirmarFinalizar() {
        AlertDialog.Builder(this)
            .setTitle("Finalizar recorrido")
            .setMessage("¿Seguro que quieres finalizar el recorrido manualmente?")
            .setPositiveButton("Finalizar") { _, _ -> finalizarRecorrido("finalizado_manual") }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun finalizarRecorrido(estado: String) {
        enRecorrido = false
        timerHandler.removeCallbacksAndMessages(null)
        if (::locationCallback.isInitialized) fusedClient.removeLocationUpdates(locationCallback)

        val fin   = System.currentTimeMillis()
        val total = fin - inicioMs
        db.child("recorridos").child(recorridoId).updateChildren(
            mapOf("finTiempo" to fin, "tiempoTotal" to total, "estado" to estado)
        )

        tvEstado.text = if (estado == "finalizado_automatico") "✓ Completado" else "Finalizado"
        btnFinalizar.isEnabled = false
        btnFinalizar.alpha     = 0.45f
        Toast.makeText(this, "Recorrido finalizado — ${formatMs(total)}", Toast.LENGTH_LONG).show()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun actualizarResumen() {
        val completados = puntosUI.count { it.estado == ConstantesRutas.ESTADO_COMPLETADO }
        tvProgreso.text  = "$completados / ${puntosUI.size} puntos"
        tvSiguiente.text = puntosUI.firstOrNull { it.estado == ConstantesRutas.ESTADO_SIGUIENTE }?.nombre ?: "—"
    }

    private fun formatMs(ms: Long): String {
        val h = ms / 3600000; val m = (ms % 3600000) / 60000; val s = (ms % 60000) / 1000
        return if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
        if (::locationCallback.isInitialized) fusedClient.removeLocationUpdates(locationCallback)
    }
}
