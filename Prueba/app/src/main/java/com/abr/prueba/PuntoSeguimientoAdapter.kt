package com.abr.prueba

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PuntoSeguimientoAdapter(
    private val listaPuntos: List<PuntoSeguimientoUI>
) : RecyclerView.Adapter<PuntoSeguimientoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutRoot: LinearLayout  = itemView.findViewById(R.id.layoutItemPuntoSeguimiento)
        val tvNombre: TextView        = itemView.findViewById(R.id.tvNombrePuntoSeguimiento)
        val tvTipo: TextView          = itemView.findViewById(R.id.tvTipoPuntoSeguimiento)
        val tvEstado: TextView        = itemView.findViewById(R.id.tvEstadoPuntoSeguimiento)
        val tvDesdeAnterior: TextView = itemView.findViewById(R.id.tvTiempoDesdeAnteriorSeguimiento)
        val tvAcumulado: TextView     = itemView.findViewById(R.id.tvTiempoAcumuladoSeguimiento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_seguimiento, parent, false))

    override fun getItemCount(): Int = listaPuntos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = listaPuntos[position]
        holder.tvNombre.text = "${p.orden}. ${p.nombre}"
        holder.tvTipo.text   = "Tipo: ${p.tipo}"

        when (p.estado) {
            ConstantesRutas.ESTADO_COMPLETADO -> {
                holder.tvEstado.text = "✓ Completado"
                holder.tvEstado.setTextColor(Color.parseColor("#1D6B3A"))
                holder.layoutRoot.setBackgroundColor(Color.parseColor("#EDFFF2"))
                holder.tvDesdeAnterior.text = "Desde anterior: ${p.tiempoDesdeAnterior}"
                holder.tvAcumulado.text     = "Acumulado: ${p.tiempoAcumulado}"
            }
            ConstantesRutas.ESTADO_SIGUIENTE -> {
                holder.tvEstado.text = "→ Siguiente"
                holder.tvEstado.setTextColor(Color.parseColor("#E05A1E"))
                holder.layoutRoot.setBackgroundColor(Color.parseColor("#FFF0E8"))
                holder.tvDesdeAnterior.text = "Desde anterior: --"
                holder.tvAcumulado.text     = "Acumulado: --"
            }
            else -> {
                holder.tvEstado.text = "Pendiente"
                holder.tvEstado.setTextColor(Color.parseColor("#B0978E"))
                holder.layoutRoot.setBackgroundColor(Color.parseColor("#FFFFFF"))
                holder.tvDesdeAnterior.text = "Desde anterior: --"
                holder.tvAcumulado.text     = "Acumulado: --"
            }
        }
    }
}
