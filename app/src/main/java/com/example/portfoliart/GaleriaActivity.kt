package com.example.portfoliart

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream

class GaleriaActivity : AppCompatActivity() {

    private var seccionId: Int = -1
    private var usuarioId: Int = -1
    private var rutaTemporal: String = ""
    private var estaOrdenado = false

    private val seleccionarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            rutaTemporal = it.toString()
            Toast.makeText(this, "Imagen lista (Soporta PNG, JPG, TIFF)", Toast.LENGTH_SHORT).show()
        }
    }

    private val tomarFotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Obra_${System.currentTimeMillis()}", null)
            rutaTemporal = path.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#F5F5F5")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContentView(R.layout.activity_galeria)

        val prefs = getSharedPreferences("PortfoliArtPrefs", Context.MODE_PRIVATE)
        usuarioId = prefs.getInt("USUARIO_ID", -1)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarGaleria)
        setSupportActionBar(toolbar)
        supportActionBar?.elevation = 0f

        seccionId = intent.getIntExtra("SECCION_ID", -1)
        supportActionBar?.title = intent.getStringExtra("SECCION_NOMBRE")

        cargarGaleria()
    }

    private fun cargarGaleria() {
        val dbHelper = DatabaseHelper(this)
        val listaFotos = dbHelper.obtenerFotosPorSeccion(seccionId, usuarioId, estaOrdenado)
        val rv = findViewById<RecyclerView>(R.id.rvGaleria)
        rv.layoutManager = GridLayoutManager(this, 3)
        rv.adapter = GaleriaAdapter(listaFotos, { foto ->
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra("FOTO_RUTA", foto.ruta)
                putExtra("FOTO_DESC", foto.descripcion)
                putExtra("FOTO_FECHA", foto.fecha)
            }
            startActivity(intent)
        }, { foto ->
            val opciones = arrayOf("Editar", "Eliminar")
            android.app.AlertDialog.Builder(this).setItems(opciones) { _, which ->
                if (which == 0) mostrarDialogoEditarFoto(foto)
                else { dbHelper.eliminarFoto(foto.id); cargarGaleria() }
            }.show()
        })
    }

    private fun mostrarDialogoNuevaFoto() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Nueva Obra")
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }
        val btn = android.widget.Button(this).apply { text = "Seleccionar Imagen" }
        btn.setOnClickListener {
            val opciones = arrayOf("Galería", "Cámara")
            android.app.AlertDialog.Builder(this).setItems(opciones) { _, w ->
                if (w == 0) {
                    // Esto permite que el sistema muestre JPG, PNG y TIFF
                    seleccionarFotoLauncher.launch("image/*")
                } else {
                    abrirCamaraConPermiso()
                }
            }.show()
        }
        layout.addView(btn)
        val inDesc = android.widget.EditText(this).apply { hint = "Descripción" }
        layout.addView(inDesc)
        val inFecha = android.widget.EditText(this).apply { hint = "Fecha" }
        layout.addView(inFecha)
        builder.setView(layout)
        builder.setPositiveButton("Añadir") { _, _ ->
            if (rutaTemporal.isNotEmpty()) {
                DatabaseHelper(this).insertarFoto(rutaTemporal, inDesc.text.toString(), inFecha.text.toString(), seccionId, usuarioId)
                rutaTemporal = ""; cargarGaleria()
            }
        }
        builder.show()
    }

    private fun mostrarDialogoEditarFoto(foto: Foto) {

        rutaTemporal = foto.ruta

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Editar Obra")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }

        val btn = android.widget.Button(this).apply { text = "Cambiar Imagen" }
        btn.setOnClickListener {
            val opciones = arrayOf("Galería", "Cámara")
            android.app.AlertDialog.Builder(this).setItems(opciones) { _, w ->
                if (w == 0) seleccionarFotoLauncher.launch("image/*")
                else abrirCamaraConPermiso()
            }.show()
        }


        val inDesc = android.widget.EditText(this).apply {
            hint = "Descripción"
            setText(foto.descripcion)
        }
        val inFecha = android.widget.EditText(this).apply {
            hint = "Fecha"
            setText(foto.fecha)
        }

        layout.addView(btn)
        layout.addView(inDesc)
        layout.addView(inFecha)

        builder.setView(layout)
        builder.setPositiveButton("Guardar Cambios") { _, _ ->

            DatabaseHelper(this).actualizarFoto(
                foto.id,
                rutaTemporal,
                inDesc.text.toString(),
                inFecha.text.toString()
            )
            cargarGaleria()
            Toast.makeText(this, "Obra actualizada", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Descartar", null)
        builder.show()
    }

    private fun abrirCamaraConPermiso() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == 0) tomarFotoLauncher.launch(null)
        else requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
    }

    private fun exportarSeccionAPdf(nombreSeccion: String, listaFotos: List<Foto>) {
        val pdfDocument = PdfDocument()

        // Estilos
        val tituloPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E63946")
            textSize = 28f
            isFakeBoldText = true
        }
        val descPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        val fechaPaint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f
        }
        val lineaPaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        canvas.drawText("PORTFOLIART: $nombreSeccion", 50f, 60f, tituloPaint)
        canvas.drawLine(50f, 80f, 545f, 80f, lineaPaint)

        var yPos = 120f

        listaFotos.forEach { foto ->
            // Si no cabe en la página, cerramos y abrimos otra
            if (yPos > 600f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 60f
            }

            try {
                val uri = Uri.parse(foto.ruta)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmapOriginal = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmapOriginal != null) {
                    val tamañoFijo = 180 // Un poco más pequeño para que quepan más por página
                    val resolucionInterna = 500

                    val minSide = Math.min(bitmapOriginal.width, bitmapOriginal.height)
                    val xOffset = (bitmapOriginal.width - minSide) / 2
                    val yOffset = (bitmapOriginal.height - minSide) / 2

                    val bitmapCuadrado = android.graphics.Bitmap.createBitmap(
                        bitmapOriginal, xOffset, yOffset, minSide, minSide
                    )

                    val bitmapFinal = android.graphics.Bitmap.createScaledBitmap(
                        bitmapCuadrado, resolucionInterna, resolucionInterna, true
                    )

                    val rectDestino = android.graphics.RectF(50f, yPos, 50f + tamañoFijo, yPos + tamañoFijo)
                    canvas.drawBitmap(bitmapFinal, null, rectDestino, null)

                    canvas.drawText(foto.descripcion.uppercase(), 250f, yPos + 40f, descPaint)
                    canvas.drawText("FECHA: ${foto.fecha}", 250f, yPos + 65f, fechaPaint)

                    bitmapOriginal.recycle()
                    bitmapCuadrado.recycle()
                    bitmapFinal.recycle()

                    yPos += tamañoFijo + 40f
                }
            } catch (e: Exception) {
                canvas.drawRect(50f, yPos, 230f, yPos + 180f, Paint().apply { color = android.graphics.Color.LTGRAY })
                canvas.drawText("Error carga imagen", 60f, yPos + 90f, fechaPaint)
                yPos += 220f
            }
        }

        // --- ESTO ES LO QUE TE FALTABA ---
        pdfDocument.finishPage(page) // Cerramos la última página

        val nombreArchivo = "Catalogo_${nombreSeccion.replace(" ", "_")}.pdf"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), nombreArchivo)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "Catálogo guardado en Descargas", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close() // Cerramos el documento para liberar memoria
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.galeria_menu, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_foto -> { mostrarDialogoNuevaFoto(); true }
            R.id.action_sort -> { estaOrdenado = !estaOrdenado; cargarGaleria(); true }
            R.id.action_pdf -> {
                val lista = DatabaseHelper(this).obtenerFotosPorSeccion(seccionId, usuarioId, estaOrdenado)
                exportarSeccionAPdf(supportActionBar?.title.toString(), lista)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}