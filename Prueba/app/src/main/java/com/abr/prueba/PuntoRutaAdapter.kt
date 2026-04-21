package com.abr.prueba

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PuntoRutaAdapter(
    private val listaPuntos: List<PuntoRuta>,
    private val onEditarClick: (PuntoRuta) -> Unit,
    private val onEliminarClick: (PuntoRuta) -> Unit
) : RecyclerView.Adapter<PuntoRutaAdapter.PuntoViewHolder>() {

    class PuntoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePuntoItem)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipoPuntoItem)
        val tvLatitud: TextView = itemView.findViewById(R.id.tvLatitudPuntoItem)
        val tvLongitud: TextView = itemView.findViewById(R.id.tvLongitudPuntoItem)
        val btnEditar: Button = itemView.findViewById(R.id.btnEditarPunto)
        val btnEliminar: Button = itemView.findViewById(R.id.btnEliminarPunto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_punto_ruta, parent, false)
        return PuntoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoViewHolder, position: Int) {
        val punto = listaPuntos[position]

        holder.tvNombre.text = "${punto.orden}. ${punto.nombre}"
        holder.tvTipo.text = "Tipo: ${punto.tipo}"
        holder.tvLatitud.text = "Latitud: ${punto.latitud}"
        holder.tvLongitud.text = "Longitud: ${punto.longitud}"

        holder.btnEditar.setOnClickListener { onEditarClick(punto) }
        holder.btnEliminar.setOnClickListener { onEliminarClick(punto) }
    }

    override fun getItemCount(): Int = listaPuntos.size
}