package com.abr.prueba

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConductoresAdapter(
    private val lista: MutableList<User>,
    private val onClickConductor: (User) -> Unit
) : RecyclerView.Adapter<ConductoresAdapter.ViewHolder>() {

    // Copia completa para poder filtrar sin perder datos
    private val listaCompleta = mutableListOf<User>()

    fun setData(nuevaLista: List<User>) {
        listaCompleta.clear()
        listaCompleta.addAll(nuevaLista)
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun filtrar(query: String) {
        lista.clear()
        if (query.isEmpty()) {
            lista.addAll(listaCompleta)
        } else {
            val q = query.lowercase()
            listaCompleta.filterTo(lista) {
                it.name.lowercase().contains(q) ||
                        it.apellidos.lowercase().contains(q) ||
                        it.email.lowercase().contains(q)
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIniciales: TextView  = view.findViewById(R.id.tvInicialesConductor)
        val tvNombre: TextView     = view.findViewById(R.id.tvNombreConductor)
        val tvEmail: TextView      = view.findViewById(R.id.tvEmailConductor)
        val tvTelefono: TextView   = view.findViewById(R.id.tvTelefonoConductor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conductor, parent, false))

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = lista[position]

        val nombre = "${user.name} ${user.apellidos}".trim().ifEmpty { "Sin nombre" }
        val iniciales = buildString {
            if (user.name.isNotEmpty())      append(user.name.first().uppercaseChar())
            if (user.apellidos.isNotEmpty()) append(user.apellidos.first().uppercaseChar())
        }.ifEmpty { "?" }

        holder.tvIniciales.text  = iniciales
        holder.tvNombre.text     = nombre
        holder.tvEmail.text      = user.email.ifEmpty { "—" }
        holder.tvTelefono.text   = user.telefono.ifEmpty { "Sin teléfono" }

        holder.itemView.setOnClickListener { onClickConductor(user) }
    }
}