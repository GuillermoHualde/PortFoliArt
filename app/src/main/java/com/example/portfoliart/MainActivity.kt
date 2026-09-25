package com.example.portfoliart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private var rutaTemporal: String = ""
    private var usuarioLogueadoId: Int = -1
    private var estaOrdenado = false

    // Launcher para GALERÍA
    private val seleccionarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            rutaTemporal = it.toString()
            Toast.makeText(this, "Imagen de portada lista", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para CÁMARA
    private val tomarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Seccion_${System.currentTimeMillis()}", null)
            rutaTemporal = path.toString()
            Toast.makeText(this, "Foto de portada capturada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Estética visual
        window.statusBarColor = android.graphics.Color.parseColor("#F5F5F5")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setContentView(R.layout.activity_main)

        // Recuperar ID del usuario
        val prefs = getSharedPreferences("PortfoliArtPrefs", Context.MODE_PRIVATE)
        usuarioLogueadoId = prefs.getInt("USUARIO_ID", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        cargarSecciones()
    }

    private fun cargarSecciones() {
        val dbHelper = DatabaseHelper(this)
        val listaSecciones = dbHelper.obtenerSeccionesPorUsuario(usuarioLogueadoId, estaOrdenado)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerSlider)

        val adapter = SliderAdapter(listaSecciones, { seccion ->
            val intent = Intent(this, GaleriaActivity::class.java)
            intent.putExtra("SECCION_ID", seccion.id)
            intent.putExtra("SECCION_NOMBRE", seccion.nombre)
            startActivity(intent)
        }, { seccion ->
            mostrarOpcionesSeccion(seccion)
        })

        viewPager.adapter = adapter
        configurarEfectoSlider(viewPager)
    }

    private fun mostrarSelectorImagen() {
        val opciones = arrayOf("Galería", "Cámara")
        android.app.AlertDialog.Builder(this)
            .setTitle("Origen de la portada")
            .setItems(opciones) { _, which ->
                if (which == 0) seleccionarFotoLauncher.launch("image/*")
                else abrirCamaraConPermiso()
            }.show()
    }

    private fun abrirCamaraConPermiso() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            tomarFotoLauncher.launch(null)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
    }

    private fun mostrarDialogoNuevaSeccion() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Nueva Técnica")
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }

        val btnFoto = android.widget.Button(this).apply { text = "Elegir Portada" }
        btnFoto.setOnClickListener { mostrarSelectorImagen() }
        layout.addView(btnFoto)

        val inputNombre = android.widget.EditText(this).apply { hint = "Ej: Grabado en Linóleo" }
        layout.addView(inputNombre)

        builder.setView(layout)
        builder.setPositiveButton("Crear") { _, _ ->
            val nombre = inputNombre.text.toString()
            if (nombre.isNotEmpty()) {
                DatabaseHelper(this).insertarSeccion(nombre, rutaTemporal, usuarioLogueadoId)
                rutaTemporal = ""
                cargarSecciones()
            }
        }
        builder.show()
    }

    private fun mostrarOpcionesSeccion(seccion: Seccion) {
        val opciones = arrayOf("Editar", "Eliminar")
        android.app.AlertDialog.Builder(this)
            .setTitle(seccion.nombre.uppercase())
            .setItems(opciones) { _, which ->
                if (which == 0) editarSeccion(seccion)
                else {
                    DatabaseHelper(this).eliminarSeccion(seccion.id)
                    cargarSecciones()
                }
            }.show()
    }

    private fun editarSeccion(seccion: Seccion) {
        val builder = android.app.AlertDialog.Builder(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }
        val btnFoto = android.widget.Button(this).apply { text = "Cambiar Portada" }
        btnFoto.setOnClickListener { mostrarSelectorImagen() }
        layout.addView(btnFoto)

        val input = android.widget.EditText(this).apply { setText(seccion.nombre) }
        layout.addView(input)
        rutaTemporal = seccion.imagenPortada

        builder.setView(layout)
        builder.setPositiveButton("Actualizar") { _, _ ->
            DatabaseHelper(this).actualizarSeccion(seccion.id, input.text.toString(), rutaTemporal)
            cargarSecciones()
        }
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_seccion -> { mostrarDialogoNuevaSeccion(); true }
            R.id.action_sort -> { estaOrdenado = !estaOrdenado; cargarSecciones(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun configurarEfectoSlider(viewPager: ViewPager2) {
        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.offscreenPageLimit = 3
        val transformer = ViewPager2.PageTransformer { page, position ->
            val r = 1 - Math.abs(position)
            page.scaleY = 0.85f + r * 0.15f
            page.alpha = 0.5f + r * 0.5f
        }
        viewPager.setPageTransformer(transformer)
        viewPager.getChildAt(0).overScrollMode = android.view.View.OVER_SCROLL_NEVER
    }
}