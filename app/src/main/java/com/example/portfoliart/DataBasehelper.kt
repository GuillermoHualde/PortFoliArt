package com.example.portfoliart

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, "portfolio.db", null, 6) { // Versión 6 para aplicar cambios

    companion object {
        // TABLA USUARIOS
        const val TABLE_USUARIOS = "usuarios"
        const val COLUMN_USER_ID = "id_usuario"
        const val COLUMN_USER_NOMBRE = "nombre"
        const val COLUMN_USER_PASS = "password"

        // TABLA SECCIONES
        const val TABLE_SECCIONES = "secciones"
        const val COLUMN_ID = "id"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_IMAGEN_PORTADA = "imagen_portada"
        const val COLUMN_SEC_USER_ID = "usuario_id" // Relación con usuario

        // TABLA FOTOS
        const val TABLE_FOTOS = "fotos"
        const val COLUMN_FOTO_ID = "id_foto"
        const val COLUMN_FOTO_RUTA = "ruta"
        const val COLUMN_FOTO_DESC = "descripcion"
        const val COLUMN_FOTO_FECHA = "fecha"
        const val COLUMN_FOTO_SECCION_ID = "seccion_id"
        const val COLUMN_FOTO_USER_ID = "usuario_id_foto" // Relación con usuario
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear Tabla Usuarios
        db.execSQL("CREATE TABLE $TABLE_USUARIOS ($COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_USER_NOMBRE TEXT, $COLUMN_USER_PASS TEXT)")

        // Crear Tabla Secciones
        db.execSQL("CREATE TABLE $TABLE_SECCIONES ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_NOMBRE TEXT, $COLUMN_IMAGEN_PORTADA TEXT, $COLUMN_SEC_USER_ID INTEGER)")

        // Crear Tabla Fotos
        db.execSQL("CREATE TABLE $TABLE_FOTOS ($COLUMN_FOTO_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_FOTO_RUTA TEXT, $COLUMN_FOTO_DESC TEXT, $COLUMN_FOTO_FECHA TEXT, $COLUMN_FOTO_SECCION_ID INTEGER, $COLUMN_FOTO_USER_ID INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SECCIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }

    // --- FUNCIONES DE USUARIO ---

    fun registrarUsuario(nombre: String, passwordCifrada: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_NOMBRE, nombre)
            put(COLUMN_USER_PASS, passwordCifrada)
        }
        return db.insert(TABLE_USUARIOS, null, values)
    }

    // Devuelve el ID del usuario si existe, o -1 si no
    fun obtenerIdUsuario(nombre: String, passwordCifrada: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_USER_ID FROM $TABLE_USUARIOS WHERE $COLUMN_USER_NOMBRE = ? AND $COLUMN_USER_PASS = ?", arrayOf(nombre, passwordCifrada))
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        return id
    }

    // --- FUNCIONES DE SECCIONES (Filtradas por Usuario) ---

    fun insertarSeccion(nombre: String, rutaImagen: String, usuarioId: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOMBRE, nombre)
            put(COLUMN_IMAGEN_PORTADA, rutaImagen)
            put(COLUMN_SEC_USER_ID, usuarioId)
        }
        db.insert(TABLE_SECCIONES, null, values)
    }

    fun obtenerSeccionesPorUsuario(usuarioId: Int, ordenarPorNombre: Boolean = false): List<Seccion> {
        val lista = mutableListOf<Seccion>()
        val db = this.readableDatabase

        var query = "SELECT * FROM $TABLE_SECCIONES WHERE $COLUMN_SEC_USER_ID = ?"
        if (ordenarPorNombre) query += " ORDER BY $COLUMN_NOMBRE ASC"

        val cursor = db.rawQuery(query, arrayOf(usuarioId.toString()))
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE))
                val foto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN_PORTADA)) ?: ""
                lista.add(Seccion(id, nombre, foto))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // --- FUNCIONES DE FOTOS (Filtradas por Usuario y Sección) ---

    fun insertarFoto(ruta: String, descripcion: String, fecha: String, seccionId: Int, usuarioId: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FOTO_RUTA, ruta)
            put(COLUMN_FOTO_DESC, descripcion)
            put(COLUMN_FOTO_FECHA, fecha)
            put(COLUMN_FOTO_SECCION_ID, seccionId)
            put(COLUMN_FOTO_USER_ID, usuarioId)
        }
        db.insert(TABLE_FOTOS, null, values)
    }

    fun obtenerFotosPorSeccion(seccionId: Int, usuarioId: Int, ordenarPorDesc: Boolean = false): List<Foto> {
        val lista = mutableListOf<Foto>()
        val db = this.readableDatabase

        var query = "SELECT * FROM $TABLE_FOTOS WHERE $COLUMN_FOTO_SECCION_ID = ? AND $COLUMN_FOTO_USER_ID = ?"
        if (ordenarPorDesc) query += " ORDER BY $COLUMN_FOTO_DESC ASC"

        val cursor = db.rawQuery(query, arrayOf(seccionId.toString(), usuarioId.toString()))
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOTO_ID))
                val ruta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO_RUTA))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO_DESC))
                val fecha = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO_FECHA))
                lista.add(Foto(id, ruta, desc, fecha, seccionId))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // --- MÉTODOS DE ELIMINACIÓN Y ACTUALIZACIÓN ---

    fun eliminarSeccion(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_FOTOS, "$COLUMN_FOTO_SECCION_ID = ?", arrayOf(id.toString()))
        db.delete(TABLE_SECCIONES, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun eliminarFoto(idFoto: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_FOTOS, "$COLUMN_FOTO_ID = ?", arrayOf(idFoto.toString()))
    }

    fun actualizarSeccion(id: Int, nuevoNombre: String, nuevaRuta: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOMBRE, nuevoNombre)
            put(COLUMN_IMAGEN_PORTADA, nuevaRuta)
        }
        db.update(TABLE_SECCIONES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun actualizarFoto(idFoto: Int, nuevaRuta: String, nuevaDesc: String, nuevaFecha: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FOTO_RUTA, nuevaRuta)
            put(COLUMN_FOTO_DESC, nuevaDesc)
            put(COLUMN_FOTO_FECHA, nuevaFecha)
        }
        db.update(TABLE_FOTOS, values, "$COLUMN_FOTO_ID = ?", arrayOf(idFoto.toString()))
    }
}