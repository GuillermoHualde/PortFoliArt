package com.example.portfoliart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SliderAdapter(
    private val secciones: List<Seccion>,
    private val onClick: (Seccion) -> Unit,
    private val onLongClick: (Seccion) -> Unit // Añadido para el menú de edición
) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imgSlider)
        val titulo: TextView = view.findViewById(R.id.txtTituloSlider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slider, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val seccion = secciones[position]
        holder.titulo.text = seccion.nombre

        Glide.with(holder.itemView.context)
            .load(seccion.imagenPortada)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imagen)

        // Click normal
        holder.itemView.setOnClickListener { onClick(seccion) }

        // Click largo para editar/borrar
        holder.itemView.setOnLongClickListener {
            onLongClick(seccion)
            true // Indica que el click largo fue procesado
        }
    }

    override fun getItemCount(): Int = secciones.size
}