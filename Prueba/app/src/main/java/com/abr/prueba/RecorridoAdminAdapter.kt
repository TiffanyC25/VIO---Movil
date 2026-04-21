package com.abr.prueba

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecorridoAdmin(
    val recorrido: Recorrido,
    val puntosRegistrados: List<PuntoRegistrado>,
    val totalPuntosRuta: Int          // cuántos puntos tiene la ruta configurada
)

class RecorridoAdminAdapter(
    private val lista: List<RecorridoAdmin>
) : RecyclerView.Adapter<RecorridoAdminAdapter.ViewHolder>() {

    // Controla qué items están expandidos
    private val expandidos = mutableSetOf<Int>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewBorde: View               = view.findViewById(R.id.viewBordeEstado)
        val layoutCabecera: LinearLayout  = view.findViewById(R.id.layoutCabecera)
        val layoutIconoEstado: LinearLayout = view.findViewById(R.id.layoutIconoEstado)
        val tvIconoEstado: TextView       = view.findViewById(R.id.tvIconoEstado)
        val tvNombreRuta: TextView        = view.findViewById(R.id.tvNombreRutaRecorrido)
        val tvBadge: TextView             = view.findViewById(R.id.tvBadgeEstadoRecorrido)
        val tvFecha: TextView             = view.findViewById(R.id.tvFechaRecorrido)
        val tvFlecha: TextView            = view.findViewById(R.id.tvFlecha)
        val tvDuracion: TextView          = view.findViewById(R.id.tvDuracionRecorrido)
        val tvPuntos: TextView            = view.findViewById(R.id.tvPuntosRecorrido)
        val tvCobertura: TextView         = view.findViewById(R.id.tvCoberturaPuntos)
        val layoutDetalle: LinearLayout   = view.findViewById(R.id.layoutDetallePuntos)
        val containerPuntos: LinearLayout = view.findViewById(R.id.containerPuntos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recorrido_admin, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item      = lista[position]
        val recorrido = item.recorrido
        val puntos    = item.puntosRegistrados
        val totalPts  = item.totalPuntosRuta
        val registrados = puntos.size
        val completado  = recorrido.estado == "finalizado_automatico"

        // ── Color según estado ─────────────────────────────────────────────
        when (recorrido.estado) {
            "finalizado_automatico" -> {
                holder.viewBorde.setBackgroundResource(R.drawable.strip_green)
                holder.layoutIconoEstado.setBackgroundResource(R.drawable.bg_icon_green)
                holder.tvIconoEstado.text = "✓"
                holder.tvIconoEstado.setTextColor(Color.parseColor("#30D158"))
                holder.tvBadge.text = "Completo"
                holder.tvBadge.setTextColor(Color.parseColor("#1D9B3C"))
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_success)
            }
            "finalizado_manual" -> {
                holder.viewBorde.setBackgroundResource(R.drawable.strip_orange)
                holder.layoutIconoEstado.setBackgroundResource(R.drawable.bg_icon_amber)
                holder.tvIconoEstado.text = "!"
                holder.tvIconoEstado.setTextColor(Color.parseColor("#E05A1E"))
                holder.tvBadge.text = "Manual"
                holder.tvBadge.setTextColor(Color.parseColor("#E05A1E"))
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_warning)
            }
            else -> {
                holder.viewBorde.setBackgroundResource(R.drawable.strip_gray)
                holder.layoutIconoEstado.setBackgroundResource(R.drawable.bg_icon_blue)
                holder.tvIconoEstado.text = "…"
                holder.tvIconoEstado.setTextColor(Color.parseColor("#007AFF"))
                holder.tvBadge.text = "En curso"
                holder.tvBadge.setTextColor(Color.parseColor("#007AFF"))
                holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_info)
            }
        }

        // ── Datos principales ──────────────────────────────────────────────
        holder.tvNombreRuta.text = recorrido.rutaNombre.ifEmpty { "Ruta sin nombre" }
        holder.tvFecha.text = "📅 ${formatFecha(recorrido.inicioTiempo)}"
        holder.tvDuracion.text = if (recorrido.tiempoTotal > 0) formatMs(recorrido.tiempoTotal) else "—"
        holder.tvPuntos.text = "$registrados/$totalPts"

        // Cobertura: % de puntos cubiertos
        val pct = if (totalPts > 0) (registrados * 100) / totalPts else 0
        holder.tvCobertura.text = "$pct%"
        holder.tvCobertura.setTextColor(
            if (pct == 100) Color.parseColor("#30D158")
            else if (pct >= 50) Color.parseColor("#E05A1E")
            else Color.parseColor("#FF3B30")
        )

        // ── Expandir / colapsar ────────────────────────────────────────────
        val estaExpandido = expandidos.contains(position)
        holder.layoutDetalle.visibility = if (estaExpandido) View.VISIBLE else View.GONE
        holder.tvFlecha.text = if (estaExpandido) "⌄" else "›"

        holder.layoutCabecera.setOnClickListener {
            if (expandidos.contains(position)) {
                expandidos.remove(position)
            } else {
                expandidos.add(position)
                poblarDetallePuntos(holder.containerPuntos, puntos, totalPts)
            }
            notifyItemChanged(position)
        }
    }

    private fun poblarDetallePuntos(
        container: LinearLayout,
        puntos: List<PuntoRegistrado>,
        totalPuntosRuta: Int
    ) {
        container.removeAllViews()
        val ctx = container.context

        if (puntos.isEmpty()) {
            val tv = TextView(ctx).apply {
                text = "No se registró ningún punto en este recorrido"
                textSize = 13f
                setTextColor(Color.parseColor("#B0978E"))
                setPadding(0, 8, 0, 0)
            }
            container.addView(tv)
            return
        }

        // Ordenar por tiempo de llegada
        val puntosOrdenados = puntos.sortedBy { it.tiempoLlegada }

        puntosOrdenados.forEachIndexed { i, punto ->
            // Contenedor de fila
            val fila = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 6)
            }

            // Círculo numerado
            val circulo = TextView(ctx).apply {
                text = "${i + 1}"
                textSize = 11f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                background = ctx.getDrawable(R.drawable.btn_success)
                val size = (28 * ctx.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (10 * ctx.resources.displayMetrics.density).toInt()
                }
            }
            fila.addView(circulo)

            // Datos del punto
            val datos = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val tvNombre = TextView(ctx).apply {
                text = punto.nombre.ifEmpty { "Punto sin nombre" }
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#1A0800"))
            }
            datos.addView(tvNombre)

            val tvHora = TextView(ctx).apply {
                text = "Llegada: ${formatHora(punto.tiempoLlegada)}"
                textSize = 12f
                setTextColor(Color.parseColor("#B0978E"))
            }
            datos.addView(tvHora)

            if (punto.tiempoDesdeAnterior > 0) {
                val tvDesde = TextView(ctx).apply {
                    text = "Desde anterior: ${formatMs(punto.tiempoDesdeAnterior)}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#E05A1E"))
                }
                datos.addView(tvDesde)
            }

            fila.addView(datos)
            container.addView(fila)

            // Separador entre puntos (excepto el último)
            if (i < puntosOrdenados.lastIndex) {
                val sep = View(ctx).apply {
                    setBackgroundColor(Color.parseColor("#F0E0D6"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * ctx.resources.displayMetrics.density).toInt()
                    ).apply {
                        topMargin   = (4 * ctx.resources.displayMetrics.density).toInt()
                        bottomMargin = (4 * ctx.resources.displayMetrics.density).toInt()
                        marginStart  = (38 * ctx.resources.displayMetrics.density).toInt()
                    }
                }
                container.addView(sep)
            }
        }
    }

    // ── Formato de tiempo ──────────────────────────────────────────────────
    private fun formatMs(ms: Long): String {
        val h = ms / 3600000; val m = (ms % 3600000) / 60000; val s = (ms % 60000) / 1000
        return if (h > 0) "%dh %02dm %02ds".format(h, m, s) else "%dm %02ds".format(m, s)
    }

    private fun formatFecha(ms: Long) =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ms))

    private fun formatHora(ms: Long) =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ms))
}