package com.example.portfoliart

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class
DetalleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle)


        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarDetalle)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "Detalle de la obra"

        // 1. Recuperar los datos enviados desde la galería
        val ruta = intent.getStringExtra("FOTO_RUTA")
        val desc = intent.getStringExtra("FOTO_DESC")
        val fecha = intent.getStringExtra("FOTO_FECHA")

        // 2. Referenciar las vistas del XML
        val imgGrande = findViewById<ImageView>(R.id.imgFotoDetalle)
        val txtDesc = findViewById<TextView>(R.id.txtDescripcionDetalle)
        val txtFecha = findViewById<TextView>(R.id.txtFechaDetalle)

        // 3. Asignar los valores
        txtDesc.text = desc
        txtFecha.text = "Fecha: $fecha"

        Glide.with(this)
            .load(ruta) // Carga la URI que viene de la base de datos
            .placeholder(android.R.drawable.ic_menu_gallery) // Imagen mientras carga
            .error(android.R.drawable.stat_notify_error)     // Imagen si hay error
            .into(imgGrande)
    }
    }
