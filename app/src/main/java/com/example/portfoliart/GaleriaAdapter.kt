package com.example.portfoliart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class GaleriaAdapter(
    private val listaFotos: List<Foto>,
    private val onClick: (Foto) -> Unit,
    private val onLongClick: (Foto) -> Unit
) : RecyclerView.Adapter<GaleriaAdapter.GaleriaViewHolder>() {

    class GaleriaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imgFotoGaleria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GaleriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto_xml, parent, false)
        return GaleriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: GaleriaViewHolder, position: Int) {
        val foto = listaFotos[position]
        holder.itemView.setOnLongClickListener { //para hacer el menu si lo dejas apretado
            onLongClick(foto)
            true
        }
        // Glide se encarga de todo: abrir la ruta, redimensionar y mostrar
        Glide.with(holder.itemView.context)
            .load(foto.ruta)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Almacena una versión optimizada para que los TIFF no pesen al hacer scroll
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.stat_notify_error)
            .into(holder.imagen)

        holder.itemView.setOnClickListener { onClick(foto) }
    }

    override fun getItemCount(): Int = listaFotos.size
}